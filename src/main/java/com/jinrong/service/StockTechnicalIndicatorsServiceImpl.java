package com.jinrong.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinrong.common.InitComon;
import com.jinrong.common.SDKforTushare;
import com.jinrong.common.ThreadPoolComom;
import com.jinrong.config.TechnicalIndicatorConfig;
import com.jinrong.entity.*;
import com.jinrong.mapper.StockFallStabilizeRiseAnalysisMapper;
import com.jinrong.mapper.StockMaBreakoutAnalysisMapper;
import com.jinrong.mapper.StockTechnicalIndicatorsMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Future;

/**
 * <p>
 * 股票技术指标数据表 服务实现类
 * </p>
 *
 * @author example
 * @since 2024-01-01
 */
@Service
public class StockTechnicalIndicatorsServiceImpl extends ServiceImpl<StockTechnicalIndicatorsMapper, StockTechnicalIndicators> {

    @Autowired
    private SDKforTushare sdKforTushare;

    @Autowired
    private TableMetaService tableMetaService;

    @Autowired
    private StockMaBreakoutAnalysisMapper stockMaBreakoutAnalysisMapper;

    @Autowired
    private StockFallStabilizeRiseAnalysisMapper stockFallStabilizeRiseAnalysisMapper;

    @Autowired
    private TechnicalIndicatorConfig technicalIndicatorConfig;

    public  List<Future> checkAll(LocalDate date) {
        List<Future> list = new ArrayList<>();
        stockMaBreakoutAnalysisMapper.delete(new QueryWrapper<StockMaBreakoutAnalysis>()
                .eq("trade_date", date));
        stockFallStabilizeRiseAnalysisMapper.delete(new QueryWrapper<StockFallStabilizeRiseAnalysis>()
                .eq("trade_date", date));
        for (StockTechnicalIndicators stockTechnicalIndicators : baseMapper.selectList(new QueryWrapper<StockTechnicalIndicators>().lambda()
                .eq(StockTechnicalIndicators::getTradeDate, date))) {
            list.add(ThreadPoolComom.executorService.submit(() -> {
                checkMaConsistencyAndBreakout(stockTechnicalIndicators.getTsCode(), 30,null,true);
                checkFallStabilizeRiseSignal(stockTechnicalIndicators.getTsCode(), 30,null,true);
            }));
        }
        return list;
    }

    public void init(String date) {
        List<StockTechnicalIndicators> stockTechnicalIndicators = new InitComon<StockTechnicalIndicators>().parse(StockTechnicalIndicators.class
                , sdKforTushare.getApiResponse("stk_factor_pro",
                        new HashMap<>() {{
                            put("trade_date", date);
                        }},
                        tableMetaService.getTableColumnsAsString("stock_technical_indicators")
                ));
        if (!stockTechnicalIndicators.isEmpty()) {
            baseMapper.delete(new QueryWrapper<StockTechnicalIndicators>().lambda()
                    .eq(StockTechnicalIndicators::getTradeDate, LocalDate.parse(date, InitComon.formatter)));
            baseMapper.insert(stockTechnicalIndicators);
        }
    }

    public Map<String, Object> checkFallStabilizeRiseSignal(String tsCode, int analysisDays,TechnicalIndicatorConfig.FallStabilizeRiseConfig config,boolean needinsert) {
        Map<String, Object> result = new HashMap<>();

        // 从配置获取参数
        if(config==null){
            config = technicalIndicatorConfig.getFallStabilizeRise();
        }
        int totalDays = analysisDays + config.getFall().getAdditionalDays();
        int minDataDays = config.getFall().getMinDataDays();

        // 获取股票历史数据
        List<StockTechnicalIndicators> stockData = getStockHistory(tsCode, totalDays);

        if (stockData.isEmpty() || stockData.size() < minDataDays) {
            result.put("success", false);
            result.put("message", "数据不足，至少需要" + minDataDays + "个交易日数据");
            return result;
        }

        // 按日期排序（从旧到新）
        stockData.sort(Comparator.comparing(StockTechnicalIndicators::getTradeDate));

        // 获取最近的数据
        StockTechnicalIndicators latestData = stockData.get(stockData.size() - 1);

        // 分析各个阶段
        FallPhaseResult fallResult = analyzeFallPhase(stockData, config);
        StabilizePhaseResult stabilizeResult = analyzeStabilizePhase(stockData, fallResult, config);
        RiseSignalResult riseResult = analyzeRiseSignal(stockData, stabilizeResult, config);

        // 综合判断
        boolean isFallStabilizeRisePattern = fallResult.hasSignificantFall &&
                stabilizeResult.isStabilized &&
                riseResult.hasRiseSignal;

        double patternStrength = calculatePatternStrength(fallResult, stabilizeResult, riseResult, config);
        String analysis = generateFallStabilizeRiseAnalysis(fallResult, stabilizeResult, riseResult);

        if(needinsert){
            // 保存分析结果到数据库
            StockFallStabilizeRiseAnalysis analysisRecord = new StockFallStabilizeRiseAnalysis();
            analysisRecord.setTsCode(tsCode);
            analysisRecord.setTradeDate(latestData.getTradeDate());
            analysisRecord.setClosePrice(latestData.getCloseQfq());
            analysisRecord.setIsFallStabilizeRisePattern(isFallStabilizeRisePattern);
            analysisRecord.setPatternStrength(patternStrength);
            analysisRecord.setFallPercent(fallResult.fallPercent);
            analysisRecord.setPeakPrice(fallResult.peakPrice);
            analysisRecord.setTroughPrice(fallResult.troughPrice);
            analysisRecord.setPeakDate(fallResult.peakDate);
            analysisRecord.setTroughDate(fallResult.troughDate);
            analysisRecord.setFallDuration(fallResult.fallDuration);
            analysisRecord.setIsStabilized(stabilizeResult.isStabilized);
            analysisRecord.setStabilizeDuration(stabilizeResult.stabilizeDuration);
            analysisRecord.setPriceVolatility(stabilizeResult.priceVolatility);
            analysisRecord.setVolumeChangeRatio(stabilizeResult.volumeChangeRatio);
            analysisRecord.setAvgStabilizePrice(stabilizeResult.avgStabilizePrice);
            analysisRecord.setHasRiseSignal(riseResult.hasRiseSignal);
            analysisRecord.setRisePercent(riseResult.risePercent);
            analysisRecord.setVolumeRatio(riseResult.volumeRatio);
            analysisRecord.setBreakMa5(riseResult.breakMa5);
            analysisRecord.setBreakMa10(riseResult.breakMa10);
            analysisRecord.setMacdBullish(riseResult.macdBullish);
            analysisRecord.setKdjBullish(riseResult.kdjBullish);
            analysisRecord.setAnalysis(analysis);
            analysisRecord.setCreatedTime(LocalDateTime.now());
            analysisRecord.setUpdatedTime(LocalDateTime.now());
            // 插入新的分析记录
            stockFallStabilizeRiseAnalysisMapper.insert(analysisRecord);
        }
        // 构建返回结果
        result.put("success", true);
        result.put("tsCode", tsCode);
        result.put("tradeDate", latestData.getTradeDate());
        result.put("closePrice", latestData.getCloseQfq());
        result.put("isFallStabilizeRisePattern", isFallStabilizeRisePattern);
        result.put("fallPhase", fallResult);
        result.put("stabilizePhase", stabilizeResult);
        result.put("riseSignal", riseResult);
        result.put("analysis", analysis);
        result.put("patternStrength", patternStrength);
        result.put("savedToDb", true);

        return result;
    }

    /**
     * 分析回落阶段
     */
    private FallPhaseResult analyzeFallPhase(List<StockTechnicalIndicators> stockData,
                                             TechnicalIndicatorConfig.FallStabilizeRiseConfig config) {
        FallPhaseResult result = new FallPhaseResult();

        // 寻找近期高点
        double peakPrice = 0;
        LocalDate peakDate = null;
        int peakIndex = -1;

        for (int i = 0; i < stockData.size(); i++) {
            double price = stockData.get(i).getCloseQfq() != null ?
                    stockData.get(i).getCloseQfq() : 0;
            if (price > peakPrice) {
                peakPrice = price;
                peakDate = stockData.get(i).getTradeDate();
                peakIndex = i;
            }
        }

        // 寻找低点（从高点后的数据）
        double troughPrice = Double.MAX_VALUE;
        LocalDate troughDate = null;
        int troughIndex = -1;

        for (int i = peakIndex + 1; i < stockData.size(); i++) {
            double price = stockData.get(i).getCloseQfq() != null ?
                    stockData.get(i).getCloseQfq() : 0;
            if (price < troughPrice) {
                troughPrice = price;
                troughDate = stockData.get(i).getTradeDate();
                troughIndex = i;
            }
        }

        // 计算回落幅度
        if (peakPrice > 0 && troughPrice < Double.MAX_VALUE) {
            double fallPercent = (peakPrice - troughPrice) / peakPrice * 100;
            // 使用配置的回落阈值
            result.hasSignificantFall = fallPercent >= config.getFall().getSignificantFallThreshold();
            result.fallPercent = fallPercent;
            result.peakPrice = peakPrice;
            result.troughPrice = troughPrice;
            result.peakDate = peakDate;
            result.troughDate = troughDate;
            result.fallDuration = troughIndex - peakIndex;
        } else {
            result.hasSignificantFall = false;
        }

        return result;
    }

    /**
     * 分析企稳阶段
     */
    private StabilizePhaseResult analyzeStabilizePhase(List<StockTechnicalIndicators> stockData,
                                                       FallPhaseResult fallResult,
                                                       TechnicalIndicatorConfig.FallStabilizeRiseConfig config) {
        StabilizePhaseResult result = new StabilizePhaseResult();

        if (!fallResult.hasSignificantFall || fallResult.troughDate == null) {
            result.isStabilized = false;
            return result;
        }

        // 找到低点后的数据（企稳阶段）
        int troughIndex = -1;
        for (int i = 0; i < stockData.size(); i++) {
            if (stockData.get(i).getTradeDate().equals(fallResult.troughDate)) {
                troughIndex = i;
                break;
            }
        }

        if (troughIndex == -1 || troughIndex >= stockData.size() - 5) {
            result.isStabilized = false;
            return result;
        }

        // 分析低点后的企稳情况，使用配置的分析天数
        int stabilizeStart = troughIndex;
        int stabilizeEnd = Math.min(stockData.size(), troughIndex + config.getStabilize().getAnalysisDays());

        List<Double> stabilizePrices = new ArrayList<>();
        List<Double> stabilizeVolumes = new ArrayList<>();

        for (int i = stabilizeStart; i < stabilizeEnd; i++) {
            StockTechnicalIndicators data = stockData.get(i);
            stabilizePrices.add(data.getCloseQfq() != null ? data.getCloseQfq() : 0);
            stabilizeVolumes.add(data.getVol() != null ? data.getVol() : 0);
        }

        // 判断企稳条件
        double minStabilizePrice = stabilizePrices.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxStabilizePrice = stabilizePrices.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double avgStabilizePrice = stabilizePrices.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // 价格波动率
        double priceVolatility = (maxStabilizePrice - minStabilizePrice) / avgStabilizePrice;

        // 成交量变化（与回落期间相比）
        double fallPeriodVolumeAvg = calculateAverageVolume(stockData,
                Math.max(0, stabilizeStart - fallResult.fallDuration), stabilizeStart);
        double stabilizeVolumeAvg = stabilizeVolumes.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // 使用配置的企稳判断条件
        boolean noNewLow = minStabilizePrice >= fallResult.troughPrice * config.getStabilize().getNoNewLowThreshold();
        boolean lowVolatility = priceVolatility < config.getStabilize().getVolatilityThreshold();
        boolean volumeDecline = stabilizeVolumeAvg < fallPeriodVolumeAvg * config.getStabilize().getVolumeThreshold();

        result.isStabilized = noNewLow && lowVolatility && volumeDecline;
        result.stabilizeDuration = stabilizeEnd - stabilizeStart;
        result.priceVolatility = priceVolatility;
        result.volumeChangeRatio = fallPeriodVolumeAvg > 0 ? stabilizeVolumeAvg / fallPeriodVolumeAvg : 0;
        result.avgStabilizePrice = avgStabilizePrice;

        return result;
    }

    /**
     * 分析上涨信号
     */
    private RiseSignalResult analyzeRiseSignal(List<StockTechnicalIndicators> stockData,
                                               StabilizePhaseResult stabilizeResult,
                                               TechnicalIndicatorConfig.FallStabilizeRiseConfig config) {
        RiseSignalResult result = new RiseSignalResult();

        if (!stabilizeResult.isStabilized) {
            result.hasRiseSignal = false;
            return result;
        }

        // 获取最近的分析天数数据，使用配置的分析天数
        int recentStart = Math.max(0, stockData.size() - config.getRise().getAnalysisDays());
        List<StockTechnicalIndicators> recentData = stockData.subList(recentStart, stockData.size());

        StockTechnicalIndicators latestData = recentData.get(recentData.size() - 1);
        StockTechnicalIndicators prevData = recentData.size() > 1 ? recentData.get(recentData.size() - 2) : null;

        double latestClose = latestData.getCloseQfq() != null ? latestData.getCloseQfq() : 0;
        double prevClose = prevData != null && prevData.getCloseQfq() != null ? prevData.getCloseQfq() : 0;
        double latestVolume = latestData.getVol() != null ? latestData.getVol() : 0;

        // 计算技术指标
        double ma5 = latestData.getMaQfq5() != null ? latestData.getMaQfq5() : 0;
        double ma10 = latestData.getMaQfq10() != null ? latestData.getMaQfq10() : 0;

        // 上涨信号判断条件，使用配置的阈值
        boolean priceRise = prevClose > 0 && (latestClose - prevClose) / prevClose > config.getRise().getPriceRiseThreshold();

        // 计算量比，使用配置的平均天数
        double avgVolume = calculateAverageVolume(stockData,
                Math.max(0, stockData.size() - config.getRise().getVolumeAvgDays()), stockData.size());
        boolean volumeSurge = avgVolume > 0 && latestVolume / avgVolume > config.getRise().getVolumeSurgeThreshold();

        boolean breakMa = latestClose > ma5 && latestClose > ma10;

        // 技术指标判断
        boolean macdBullish = isMacdBullish(latestData);
        boolean kdjBullish = isKdjBullish(latestData);

        // 根据配置决定是否要求技术指标
        boolean technicalBullish = !config.getRise().isRequireTechnicalIndicator() || (macdBullish || kdjBullish);
        // 根据配置决定是否要求突破均线
        boolean maCondition = !config.getRise().isRequireBreakMa() || breakMa;

        result.hasRiseSignal = priceRise && volumeSurge && maCondition && technicalBullish;
        result.risePercent = prevClose > 0 ? (latestClose - prevClose) / prevClose * 100 : 0;
        result.volumeRatio = avgVolume > 0 ? latestVolume / avgVolume : 0;
        result.breakMa5 = latestClose > ma5;
        result.breakMa10 = latestClose > ma10;
        result.macdBullish = macdBullish;
        result.kdjBullish = kdjBullish;
        result.latestClose = latestClose;

        return result;
    }

    /**
     * 判断MACD是否看多
     */
    private boolean isMacdBullish(StockTechnicalIndicators data) {
        Double macd = data.getMacdQfq();
        Double macdDif = data.getMacdDifQfq();
        Double macdDea = data.getMacdDeaQfq();

        if (macdDif != null && macdDea != null) {
            return macdDif > macdDea;
        }

        if (macd != null) {
            return macd > 0;
        }

        return false;
    }

    /**
     * 判断KDJ是否看多
     */
    private boolean isKdjBullish(StockTechnicalIndicators data) {
        Double kdjK = data.getKdjKQfq();
        Double kdjD = data.getKdjDQfq();

        if (kdjK != null && kdjD != null) {
            return kdjK > kdjD;
        }

        return false;
    }

    /**
     * 计算指定区间的平均成交量
     */
    private double calculateAverageVolume(List<StockTechnicalIndicators> stockData, int start, int end) {
        if (start >= end || start < 0 || end > stockData.size()) {
            return 0;
        }

        return stockData.subList(start, end).stream()
                .mapToDouble(data -> data.getVol() != null ? data.getVol() : 0)
                .average()
                .orElse(0);
    }

    /**
     * 生成分析文本
     */
    private String generateFallStabilizeRiseAnalysis(FallPhaseResult fall, StabilizePhaseResult stabilize, RiseSignalResult rise) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("回落-企稳-上涨形态分析：\n\n");

        // 回落阶段分析
        analysis.append("【回落阶段】\n");
        if (fall.hasSignificantFall) {
            analysis.append("✓ 经历显著回落：从").append(String.format("%.2f", fall.peakPrice))
                    .append("回落至").append(String.format("%.2f", fall.troughPrice))
                    .append("，幅度").append(String.format("%.1f", fall.fallPercent)).append("%")
                    .append("，持续").append(fall.fallDuration).append("个交易日\n");
        } else {
            analysis.append("✗ 未经历显著回落（回落幅度：").append(String.format("%.1f", fall.fallPercent)).append("%）\n");
        }

        // 企稳阶段分析
        analysis.append("\n【企稳阶段】\n");
        if (stabilize.isStabilized) {
            analysis.append("✓ 价格企稳：波动率").append(String.format("%.1f", stabilize.priceVolatility * 100))
                    .append("%，成交量变化比率").append(String.format("%.2f", stabilize.volumeChangeRatio))
                    .append("，企稳").append(stabilize.stabilizeDuration).append("个交易日\n");
        } else {
            analysis.append("✗ 价格未有效企稳\n");
        }

        // 上涨信号分析
        analysis.append("\n【上涨信号】\n");
        if (rise.hasRiseSignal) {
            analysis.append("✓ 出现上涨信号：涨幅").append(String.format("%.2f", rise.risePercent))
                    .append("%，量比").append(String.format("%.2f", rise.volumeRatio))
                    .append("，突破5日/10日均线：").append(rise.breakMa5 ? "是" : "否").append("/").append(rise.breakMa10 ? "是" : "否")
                    .append("，技术指标：MACD=").append(rise.macdBullish ? "看多" : "中性")
                    .append("，KDJ=").append(rise.kdjBullish ? "看多" : "中性").append("\n");
        } else {
            analysis.append("✗ 未出现有效上涨信号\n");
        }

        // 综合判断
        analysis.append("\n【综合判断】\n");
        if (fall.hasSignificantFall && stabilize.isStabilized && rise.hasRiseSignal) {
            analysis.append("🎯 符合'回落-企稳-上涨'形态，建议重点关注！");
        } else {
            analysis.append("⚠️ 不完全符合'回落-企稳-上涨'形态");

            if (!fall.hasSignificantFall) {
                analysis.append("（主要问题：回落幅度不足）");
            } else if (!stabilize.isStabilized) {
                analysis.append("（主要问题：企稳不充分）");
            } else if (!rise.hasRiseSignal) {
                analysis.append("（主要问题：缺乏上涨信号）");
            }
        }

        return analysis.toString();
    }

    /**
     * 计算形态强度
     */
    private double calculatePatternStrength(FallPhaseResult fall, StabilizePhaseResult stabilize, RiseSignalResult rise,
                                            TechnicalIndicatorConfig.FallStabilizeRiseConfig config) {
        double strength = 0;
        TechnicalIndicatorConfig.PatternStrengthConfig patternStrength = config.getPatternStrength();

        if (fall.hasSignificantFall) {
            strength += patternStrength.getFallWeight();
            // 回落幅度越大，强度越高（但不超过权重的一半）
            strength += Math.min(fall.fallPercent / 50, patternStrength.getFallWeight() / 2);
        }

        if (stabilize.isStabilized) {
            strength += patternStrength.getStabilizeWeight();
            // 波动率越低，强度越高
            strength += (config.getStabilize().getVolatilityThreshold() - stabilize.priceVolatility) * 2;
        }

        if (rise.hasRiseSignal) {
            strength += patternStrength.getRiseWeight();
            // 涨幅和量比越大，强度越高
            strength += Math.min(rise.risePercent / 10, patternStrength.getRiseWeight() / 3);
            strength += Math.min((rise.volumeRatio - 1) / 2, patternStrength.getRiseWeight() / 3);
        }

        return Math.min(strength, 1.0);
    }

    /**
     * 获取股票历史数据
     */
    private List<StockTechnicalIndicators> getStockHistory(String tsCode, int days) {
        QueryWrapper<StockTechnicalIndicators> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ts_code", tsCode)
                .orderByDesc("trade_date")
                .last("LIMIT " + days);
        return this.list(queryWrapper);
    }

    // 内部结果类
    @Data
    private static class FallPhaseResult {
        boolean hasSignificantFall;
        double fallPercent;
        double peakPrice;
        double troughPrice;
        LocalDate peakDate;
        LocalDate troughDate;
        int fallDuration;
    }

    @Data
    private static class StabilizePhaseResult {
        boolean isStabilized;
        int stabilizeDuration;
        double priceVolatility;
        double volumeChangeRatio;
        double avgStabilizePrice;
    }

    @Data
    private static class RiseSignalResult {
        boolean hasRiseSignal;
        double risePercent;
        double volumeRatio;
        boolean breakMa5;
        boolean breakMa10;
        boolean macdBullish;
        boolean kdjBullish;
        double latestClose;
    }

    /**
     * 分析均线粘滞和突破情况，并保存分析结果
     */
    public Map<String, Object> checkMaConsistencyAndBreakout(String tsCode, int days,TechnicalIndicatorConfig.MaConsistencyBreakoutConfig config,boolean needinsert) {
        Map<String, Object> result = new HashMap<>();

        // 从配置获取参数
        if(config==null){
            config= technicalIndicatorConfig.getMaConsistencyBreakout();
        }
        int minDataDays = config.getMinDataDays();

        // 获取股票历史数据
        List<StockTechnicalIndicators> stockData = getStockHistory(tsCode, days);
        if (stockData.isEmpty() || stockData.size() < minDataDays) {
            result.put("success", false);
            result.put("message", "数据不足，至少需要" + minDataDays + "个交易日数据");
            return result;
        }

        // 按日期排序（从旧到新）
        stockData.sort(Comparator.comparing(StockTechnicalIndicators::getTradeDate));

        // 获取最近的数据
        StockTechnicalIndicators latestData = stockData.get(stockData.size() - 1);

        // 分析各个指标
        MaConsistencyResult maConsistency = analyzeMaConsistency(stockData, config);
        VolumeBreakoutResult volumeBreakout = analyzeVolumeBreakout(stockData, config);
        PriceBreakoutResult priceBreakout = analyzePriceBreakout(stockData, config);

        // 综合判断
        boolean isMaConsistencyBreakout = maConsistency.isConsistent &&
                volumeBreakout.isBreakout &&
                priceBreakout.isBreakout;

        String analysis = generateAnalysisText(maConsistency, volumeBreakout, priceBreakout);

        if(needinsert){
            // 保存分析结果到数据库
            StockMaBreakoutAnalysis analysisRecord = new StockMaBreakoutAnalysis();
            analysisRecord.setTsCode(tsCode);
            analysisRecord.setTradeDate(latestData.getTradeDate());
            analysisRecord.setClosePrice(latestData.getCloseQfq());
            analysisRecord.setVolume(latestData.getVol());
            analysisRecord.setIsMaConsistencyBreakout(isMaConsistencyBreakout);
            analysisRecord.setIsConsistent(maConsistency.isConsistent);
            analysisRecord.setMaxVariation(maConsistency.maxVariation);
            analysisRecord.setMa5(maConsistency.ma5);
            analysisRecord.setMa10(maConsistency.ma10);
            analysisRecord.setMa20(maConsistency.ma20);
            analysisRecord.setMa30(maConsistency.ma30);
            analysisRecord.setIsVolumeBreakout(volumeBreakout.isBreakout);
            analysisRecord.setLatestVolume(volumeBreakout.latestVolume);
            analysisRecord.setAvgVolume20(volumeBreakout.avgVolume20);
            analysisRecord.setVolumeRatio(volumeBreakout.volumeRatio);
            analysisRecord.setPreviousVolumeRatio(volumeBreakout.previousVolumeRatio);
            analysisRecord.setIsPriceBreakout(priceBreakout.isBreakout);
            analysisRecord.setBreakoutPercent(priceBreakout.breakoutPercent);
            analysisRecord.setAnalysis(analysis);
            analysisRecord.setCreatedTime(LocalDateTime.now());
            analysisRecord.setUpdatedTime(LocalDateTime.now());
            // 插入新的分析记录
            stockMaBreakoutAnalysisMapper.insert(analysisRecord);
        }

        // 构建返回结果
        result.put("success", true);
        result.put("tsCode", tsCode);
        result.put("tradeDate", latestData.getTradeDate());
        result.put("closePrice", latestData.getCloseQfq());
        result.put("volume", latestData.getVol());
        result.put("isMaConsistencyBreakout", isMaConsistencyBreakout);
        result.put("maConsistency", maConsistency);
        result.put("volumeBreakout", volumeBreakout);
        result.put("priceBreakout", priceBreakout);
        result.put("analysis", analysis);
        result.put("savedToDb", true);

        return result;
    }

    /**
     * 分析均线粘滞情况
     */
    private MaConsistencyResult analyzeMaConsistency(List<StockTechnicalIndicators> stockData,
                                                     TechnicalIndicatorConfig.MaConsistencyBreakoutConfig config) {
        MaConsistencyResult result = new MaConsistencyResult();

        // 取最近的分析天数数据，使用配置的分析天数
        int analysisDays = Math.min(config.getConsistency().getAnalysisDays(), stockData.size());
        List<StockTechnicalIndicators> recentData = stockData.subList(stockData.size() - analysisDays, stockData.size());

        List<Double> ma5Values = new ArrayList<>();
        List<Double> ma10Values = new ArrayList<>();
        List<Double> ma20Values = new ArrayList<>();
        List<Double> ma30Values = new ArrayList<>();

        for (StockTechnicalIndicators data : recentData) {
            ma5Values.add(data.getMaQfq5() != null ? data.getMaQfq5() : 0);
            ma10Values.add(data.getMaQfq10() != null ? data.getMaQfq10() : 0);
            ma20Values.add(data.getMaQfq20() != null ? data.getMaQfq20() : 0);
            ma30Values.add(data.getMaQfq30() != null ? data.getMaQfq30() : 0);
        }

        // 计算均线之间的最大差异率
        double maxVariation = calculateMaVariation(ma5Values, ma10Values, ma20Values, ma30Values);

        // 使用配置的差异率阈值判断是否粘滞
        result.isConsistent = maxVariation < config.getConsistency().getVariationThreshold();
        result.maxVariation = maxVariation;
        result.ma5 = ma5Values.get(ma5Values.size() - 1);
        result.ma10 = ma10Values.get(ma10Values.size() - 1);
        result.ma20 = ma20Values.get(ma20Values.size() - 1);
        result.ma30 = ma30Values.get(ma30Values.size() - 1);

        return result;
    }

    /**
     * 计算均线差异率
     */
    private double calculateMaVariation(List<Double> ma5, List<Double> ma10, List<Double> ma20, List<Double> ma30) {
        double maxVariation = 0;

        for (int i = 0; i < ma5.size(); i++) {
            double[] mas = {ma5.get(i), ma10.get(i), ma20.get(i), ma30.get(i)};
            double max = Arrays.stream(mas).max().orElse(0);
            double min = Arrays.stream(mas).min().orElse(0);
            double avg = Arrays.stream(mas).average().orElse(0);

            if (avg > 0) {
                double variation = (max - min) / avg;
                maxVariation = Math.max(maxVariation, variation);
            }
        }

        return maxVariation;
    }

    /**
     * 分析成交量突破
     */
    private VolumeBreakoutResult analyzeVolumeBreakout(List<StockTechnicalIndicators> stockData,
                                                       TechnicalIndicatorConfig.MaConsistencyBreakoutConfig config) {
        VolumeBreakoutResult result = new VolumeBreakoutResult();

        StockTechnicalIndicators latestData = stockData.get(stockData.size() - 1);
        StockTechnicalIndicators previousData = stockData.get(stockData.size() - 2);

        // 计算最近的平均成交量，使用配置的平均天数
        double avgVolume = stockData.stream()
                .skip(Math.max(0, stockData.size() - config.getVolume().getAvgDays()))
                .mapToDouble(data -> data.getVol() != null ? data.getVol() : 0)
                .average()
                .orElse(0);

        double latestVolume = latestData.getVol() != null ? latestData.getVol() : 0;
        double previousVolume = previousData.getVol() != null ? previousData.getVol() : 0;

        // 使用配置的阈值判断是否放量
        result.isBreakout = latestVolume > avgVolume * config.getVolume().getBreakoutRatio() &&
                latestVolume > previousVolume * config.getVolume().getPreviousDayRatio();
        result.latestVolume = latestVolume;
        result.avgVolume20 = avgVolume;
        result.volumeRatio = avgVolume > 0 ? latestVolume / avgVolume : 0;
        result.previousVolumeRatio = previousVolume > 0 ? latestVolume / previousVolume : 0;

        return result;
    }

    /**
     * 分析价格突破
     */
    private PriceBreakoutResult analyzePriceBreakout(List<StockTechnicalIndicators> stockData,
                                                     TechnicalIndicatorConfig.MaConsistencyBreakoutConfig config) {
        PriceBreakoutResult result = new PriceBreakoutResult();

        StockTechnicalIndicators latestData = stockData.get(stockData.size() - 1);

        double closePrice = latestData.getCloseQfq() != null ? latestData.getCloseQfq() : 0;
        double ma5 = latestData.getMaQfq5() != null ? latestData.getMaQfq5() : 0;
        double ma10 = latestData.getMaQfq10() != null ? latestData.getMaQfq10() : 0;
        double ma20 = latestData.getMaQfq20() != null ? latestData.getMaQfq20() : 0;
        double ma30 = latestData.getMaQfq30() != null ? latestData.getMaQfq30() : 0;

        // 根据配置决定是否要求突破所有均线
        boolean breakoutCondition;
        if (config.getPrice().isRequireBreakAllMa()) {
            breakoutCondition = closePrice > ma5 && closePrice > ma10 && closePrice > ma20 && closePrice > ma30;
        } else {
            // 只要求突破主要均线
            breakoutCondition = closePrice > ma5 && closePrice > ma10 && closePrice > ma20;
        }

        result.isBreakout = breakoutCondition;
        result.closePrice = closePrice;
        result.ma5 = ma5;
        result.ma10 = ma10;
        result.ma20 = ma20;
        result.ma30 = ma30;
        result.breakoutPercent = calculateBreakoutPercent(closePrice, ma5, ma10, ma20, ma30);

        return result;
    }

    /**
     * 计算突破幅度
     */
    private double calculateBreakoutPercent(double closePrice, double ma5, double ma10, double ma20, double ma30) {
        double maxMa = Math.max(Math.max(ma5, ma10), Math.max(ma20, ma30));
        return maxMa > 0 ? (closePrice - maxMa) / maxMa * 100 : 0;
    }

    /**
     * 生成分析文本
     */
    private String generateAnalysisText(MaConsistencyResult ma, VolumeBreakoutResult volume, PriceBreakoutResult price) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("技术分析结果：\n");

        if (ma.isConsistent) {
            analysis.append("✓ 均线呈现粘滞状态（最大差异率：").append(String.format("%.2f", ma.maxVariation * 100)).append("%）\n");
        } else {
            analysis.append("✗ 均线未呈现明显粘滞状态（最大差异率：").append(String.format("%.2f", ma.maxVariation * 100)).append("%）\n");
        }

        if (volume.isBreakout) {
            analysis.append("✓ 成交量显著放大（量比：").append(String.format("%.2f", volume.volumeRatio)).append("倍）\n");
        } else {
            analysis.append("✗ 成交量未显著放大（量比：").append(String.format("%.2f", volume.volumeRatio)).append("倍）\n");
        }

        if (price.isBreakout) {
            analysis.append("✓ 价格突破所有均线（突破幅度：").append(String.format("%.2f", price.breakoutPercent)).append("%）\n");
        } else {
            analysis.append("✗ 价格未有效突破均线\n");
        }

        if (ma.isConsistent && volume.isBreakout && price.isBreakout) {
            analysis.append("\n🎯 符合均线粘滞后放量突破形态，建议关注！");
        } else {
            analysis.append("\n⚠️ 不符合均线粘滞后放量突破形态。");
        }

        return analysis.toString();
    }

    // 内部结果类
    @Data
    private static class MaConsistencyResult {
        boolean isConsistent;
        double maxVariation;
        double ma5;
        double ma10;
        double ma20;
        double ma30;
    }

    @Data
    private static class VolumeBreakoutResult {
        boolean isBreakout;
        double latestVolume;
        double avgVolume20;
        double volumeRatio;
        double previousVolumeRatio;
    }

    @Data
    private static class PriceBreakoutResult {
        boolean isBreakout;
        double closePrice;
        double ma5;
        double ma10;
        double ma20;
        double ma30;
        double breakoutPercent;
    }


}