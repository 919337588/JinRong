package com.jinrong.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.config.ParameterOptimizationConfig;
import com.jinrong.config.TechnicalIndicatorConfig;
import com.jinrong.entity.StockTechnicalIndicators;
import com.jinrong.service.StockTechnicalIndicatorsServiceImpl;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ParameterOptimizationService {

    @Autowired
    private StockTechnicalIndicatorsServiceImpl stockTechnicalIndicatorsService;

    @Autowired
    private ParameterOptimizationConfig parameterOptimizationConfig;

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * 优化回落企稳上涨形态参数
     */
    public OptimizationResult optimizeFallStabilizeRise(String tsCode, LocalDate startDate, LocalDate endDate) {
        log.info("开始优化回落企稳上涨形态参数，股票：{}，时间范围：{} 到 {}", tsCode, startDate, endDate);

        List<StockTechnicalIndicators> stockData = getStockDataInRange(tsCode, startDate, endDate);
        if (stockData.isEmpty()) {
            log.warn("没有找到股票 {} 在指定时间范围内的数据", tsCode);
            return new OptimizationResult();
        }

        ParameterOptimizationConfig.FallStabilizeRiseSearch searchSpace =
                parameterOptimizationConfig.getFallStabilizeRise();

        List<CompletableFuture<ParameterEvaluation>> futures = new ArrayList<>();

        // 生成所有参数组合
        int totalCombinations = generateFallStabilizeRiseCombinations(searchSpace);
        log.info("将测试 {} 种参数组合", totalCombinations);

        // 遍历所有参数组合
        for (Double fallThreshold : searchSpace.getSignificantFallThresholds()) {
            for (Double noNewLowThreshold : searchSpace.getNoNewLowThresholds()) {
                for (Double volatilityThreshold : searchSpace.getVolatilityThresholds()) {
                    for (Double volumeThreshold : searchSpace.getVolumeThresholds()) {
                        for (Double priceRiseThreshold : searchSpace.getPriceRiseThresholds()) {
                            for (Double volumeSurgeThreshold : searchSpace.getVolumeSurgeThresholds()) {
                                for (Boolean requireBreakMa : searchSpace.getRequireBreakMas()) {
                                    for (Boolean requireTechnicalIndicator : searchSpace.getRequireTechnicalIndicators()) {
                                        TechnicalIndicatorConfig.FallStabilizeRiseConfig params =
                                                createFallStabilizeRiseConfig(
                                                        fallThreshold, noNewLowThreshold, volatilityThreshold,
                                                        volumeThreshold, priceRiseThreshold, volumeSurgeThreshold,
                                                        requireBreakMa, requireTechnicalIndicator
                                                );

                                        CompletableFuture<ParameterEvaluation> future =
                                                CompletableFuture.supplyAsync(() ->
                                                        evaluateFallStabilizeRiseParameters(stockData, params), executorService);
                                        futures.add(future);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 等待所有评估完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 收集结果并排序
        List<ParameterEvaluation> evaluations = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(eval -> eval.getTotalSignals() > 0) // 过滤掉没有信号的组合
                .sorted((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()))
                .toList();

        log.info("参数优化完成，共评估 {} 种组合，有效组合 {}", totalCombinations, evaluations.size());

        OptimizationResult result = new OptimizationResult();
        result.setEvaluations(evaluations);
        result.setStrategyType("fall-stabilize-rise");
        if (!evaluations.isEmpty()) {
            result.setBestParameters(evaluations.get(0));
            log.info("最佳参数组合：胜率 {:.2f}%", evaluations.get(0).getWinRate() * 100);
        }

        return result;
    }

    /**
     * 优化均线粘滞突破形态参数
     */
    public OptimizationResult optimizeMaConsistencyBreakout(String tsCode, LocalDate startDate, LocalDate endDate) {
        log.info("开始优化均线粘滞突破形态参数，股票：{}，时间范围：{} 到 {}", tsCode, startDate, endDate);

        List<StockTechnicalIndicators> stockData = getStockDataInRange(tsCode, startDate, endDate);
        if (stockData.isEmpty()) {
            log.warn("没有找到股票 {} 在指定时间范围内的数据", tsCode);
            return new OptimizationResult();
        }

        ParameterOptimizationConfig.MaConsistencyBreakoutSearch searchSpace =
                parameterOptimizationConfig.getMaConsistencyBreakout();

        List<CompletableFuture<ParameterEvaluation>> futures = new ArrayList<>();

        // 生成所有参数组合
        int totalCombinations = generateMaConsistencyBreakoutCombinations(searchSpace);
        log.info("将测试 {} 种参数组合", totalCombinations);

        // 遍历所有参数组合
        for (Double variationThreshold : searchSpace.getVariationThresholds()) {
            for (Double breakoutRatio : searchSpace.getBreakoutRatios()) {
                for (Double previousDayRatio : searchSpace.getPreviousDayRatios()) {
                    for (Boolean requireBreakAllMa : searchSpace.getRequireBreakAllMas()) {

                        TechnicalIndicatorConfig.MaConsistencyBreakoutConfig params =
                                createMaConsistencyBreakoutConfig(
                                        variationThreshold, breakoutRatio, previousDayRatio, requireBreakAllMa
                                );

                        CompletableFuture<ParameterEvaluation> future =
                                CompletableFuture.supplyAsync(() ->
                                        evaluateMaConsistencyBreakoutParameters(stockData, params), executorService);
                        futures.add(future);
                    }
                }
            }
        }

        // 等待所有评估完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 收集结果并排序
        List<ParameterEvaluation> evaluations = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(eval -> eval.getTotalSignals() > 0) // 过滤掉没有信号的组合
                .sorted((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()))
                .toList();

        log.info("参数优化完成，共评估 {} 种组合，有效组合 {}", totalCombinations, evaluations.size());

        OptimizationResult result = new OptimizationResult();
        result.setEvaluations(evaluations);
        result.setStrategyType("ma-consistency-breakout");
        if (!evaluations.isEmpty()) {
            result.setBestParameters(evaluations.get(0));
            log.info("最佳参数组合：胜率 {:.2f}%", evaluations.get(0).getWinRate() * 100);
        }

        return result;
    }

    /**
     * 评估回落企稳上涨参数组合
     */
    private ParameterEvaluation evaluateFallStabilizeRiseParameters(List<StockTechnicalIndicators> stockData,
                                                                    TechnicalIndicatorConfig.FallStabilizeRiseConfig params) {
        try {
            int totalSignals = 0;
            int successfulSignals = 0;
            double totalProfit = 0;
            List<Double> profits = new ArrayList<>();

            // 滑动窗口测试
            int windowSize = 60; // 60个交易日的分析窗口
            int holdDays = 5;    // 持有5个交易日

            for (int i = windowSize; i < stockData.size() - holdDays; i++) {
                StockTechnicalIndicators signalDay = stockData.get(i);
                StockTechnicalIndicators resultDay = stockData.get(i + holdDays);

                // 使用当前参数检查信号
                Map<String, Object> signalResult = stockTechnicalIndicatorsService
                        .checkFallStabilizeRiseSignal(signalDay.getTsCode(), windowSize, params,false);

                if (Boolean.TRUE.equals(signalResult.get("success")) &&
                        Boolean.TRUE.equals(signalResult.get("isFallStabilizeRisePattern"))) {
                    totalSignals++;

                    // 计算收益率
                    double entryPrice = signalDay.getCloseQfq();
                    double exitPrice = resultDay.getCloseQfq();
                    double profit = (exitPrice - entryPrice) / entryPrice;
                    totalProfit += profit;
                    profits.add(profit);

                    if (profit > 0.02) { // 收益率超过2%算成功
                        successfulSignals++;
                    }
                }
            }

            if (totalSignals == 0) {
                return null;
            }

            double winRate = (double) successfulSignals / totalSignals;
            double avgProfit = totalProfit / totalSignals;

            // 计算风险指标
            double maxDrawdown = calculateMaxDrawdown(profits);
            double sharpeRatio = calculateSharpeRatio(profits);

            ParameterEvaluation evaluation = new ParameterEvaluation();
            evaluation.setParams(params);
            evaluation.setTotalSignals(totalSignals);
            evaluation.setSuccessfulSignals(successfulSignals);
            evaluation.setWinRate(winRate);
            evaluation.setAvgProfit(avgProfit);
            evaluation.setMaxDrawdown(maxDrawdown);
            evaluation.setSharpeRatio(sharpeRatio);
            evaluation.setStrategyType("fall-stabilize-rise");

            return evaluation;

        } catch (Exception e) {
            log.error("回落企稳上涨参数评估失败", e);
            return null;
        }
    }

    /**
     * 评估均线粘滞突破参数组合
     */
    private ParameterEvaluation evaluateMaConsistencyBreakoutParameters(List<StockTechnicalIndicators> stockData,
                                                                        TechnicalIndicatorConfig.MaConsistencyBreakoutConfig params) {
        try {
            int totalSignals = 0;
            int successfulSignals = 0;
            double totalProfit = 0;
            List<Double> profits = new ArrayList<>();

            // 滑动窗口测试
            int windowSize = 40; // 40个交易日的分析窗口
            int holdDays = 5;    // 持有5个交易日

            for (int i = windowSize; i < stockData.size() - holdDays; i++) {
                List<StockTechnicalIndicators> windowData = stockData.subList(i - windowSize, i);
                StockTechnicalIndicators signalDay = stockData.get(i);
                StockTechnicalIndicators resultDay = stockData.get(i + holdDays);

                // 使用当前参数检查信号
                Map<String, Object> signalResult = stockTechnicalIndicatorsService
                        .checkMaConsistencyAndBreakout(signalDay.getTsCode(), windowSize, params,false);

                if (Boolean.TRUE.equals(signalResult.get("success")) &&
                        Boolean.TRUE.equals(signalResult.get("isMaConsistencyBreakout"))) {
                    totalSignals++;

                    // 计算收益率
                    double entryPrice = signalDay.getCloseQfq();
                    double exitPrice = resultDay.getCloseQfq();
                    double profit = (exitPrice - entryPrice) / entryPrice;
                    totalProfit += profit;
                    profits.add(profit);

                    if (profit > 0.02) { // 收益率超过2%算成功
                        successfulSignals++;
                    }
                }
            }

            if (totalSignals == 0) {
                return null;
            }

            double winRate = (double) successfulSignals / totalSignals;
            double avgProfit = totalProfit / totalSignals;

            // 计算风险指标
            double maxDrawdown = calculateMaxDrawdown(profits);
            double sharpeRatio = calculateSharpeRatio(profits);

            ParameterEvaluation evaluation = new ParameterEvaluation();
            evaluation.setParams(params);
            evaluation.setTotalSignals(totalSignals);
            evaluation.setSuccessfulSignals(successfulSignals);
            evaluation.setWinRate(winRate);
            evaluation.setAvgProfit(avgProfit);
            evaluation.setMaxDrawdown(maxDrawdown);
            evaluation.setSharpeRatio(sharpeRatio);
            evaluation.setStrategyType("ma-consistency-breakout");

            return evaluation;

        } catch (Exception e) {
            log.error("均线粘滞突破参数评估失败", e);
            return null;
        }
    }

    /**
     * 创建回落企稳上涨配置
     */
    private TechnicalIndicatorConfig.FallStabilizeRiseConfig createFallStabilizeRiseConfig(
            Double fallThreshold, Double noNewLowThreshold, Double volatilityThreshold,
            Double volumeThreshold, Double priceRiseThreshold, Double volumeSurgeThreshold,
            Boolean requireBreakMa, Boolean requireTechnicalIndicator) {

        TechnicalIndicatorConfig.FallStabilizeRiseConfig config = new TechnicalIndicatorConfig.FallStabilizeRiseConfig();

        // 设置回落阶段参数
        TechnicalIndicatorConfig.FallConfig fallConfig = new TechnicalIndicatorConfig.FallConfig();
        fallConfig.setSignificantFallThreshold(fallThreshold);
        fallConfig.setMinDataDays(30);
        fallConfig.setAdditionalDays(20);
        config.setFall(fallConfig);

        // 设置企稳阶段参数
        TechnicalIndicatorConfig.StabilizeConfig stabilizeConfig = new TechnicalIndicatorConfig.StabilizeConfig();
        stabilizeConfig.setAnalysisDays(10);
        stabilizeConfig.setNoNewLowThreshold(noNewLowThreshold);
        stabilizeConfig.setVolatilityThreshold(volatilityThreshold);
        stabilizeConfig.setVolumeThreshold(volumeThreshold);
        config.setStabilize(stabilizeConfig);

        // 设置上涨信号参数
        TechnicalIndicatorConfig.RiseConfig riseConfig = new TechnicalIndicatorConfig.RiseConfig();
        riseConfig.setAnalysisDays(3);
        riseConfig.setPriceRiseThreshold(priceRiseThreshold);
        riseConfig.setVolumeSurgeThreshold(volumeSurgeThreshold);
        riseConfig.setVolumeAvgDays(10);
        riseConfig.setRequireBreakMa(requireBreakMa);
        riseConfig.setRequireTechnicalIndicator(requireTechnicalIndicator);
        config.setRise(riseConfig);

        // 设置形态强度权重
        TechnicalIndicatorConfig.PatternStrengthConfig patternStrengthConfig = new TechnicalIndicatorConfig.PatternStrengthConfig();
        patternStrengthConfig.setFallWeight(0.4);
        patternStrengthConfig.setStabilizeWeight(0.3);
        patternStrengthConfig.setRiseWeight(0.3);
        config.setPatternStrength(patternStrengthConfig);

        return config;
    }

    /**
     * 创建均线粘滞突破配置
     */
    private TechnicalIndicatorConfig.MaConsistencyBreakoutConfig createMaConsistencyBreakoutConfig(
            Double variationThreshold, Double breakoutRatio, Double previousDayRatio, Boolean requireBreakAllMa) {

        TechnicalIndicatorConfig.MaConsistencyBreakoutConfig config = new TechnicalIndicatorConfig.MaConsistencyBreakoutConfig();

        config.setMinDataDays(20);

        // 设置均线粘滞参数
        TechnicalIndicatorConfig.ConsistencyConfig consistencyConfig = new TechnicalIndicatorConfig.ConsistencyConfig();
        consistencyConfig.setAnalysisDays(5);
        consistencyConfig.setVariationThreshold(variationThreshold);
        config.setConsistency(consistencyConfig);

        // 设置成交量参数
        TechnicalIndicatorConfig.VolumeConfig volumeConfig = new TechnicalIndicatorConfig.VolumeConfig();
        volumeConfig.setAvgDays(20);
        volumeConfig.setBreakoutRatio(breakoutRatio);
        volumeConfig.setPreviousDayRatio(previousDayRatio);
        config.setVolume(volumeConfig);

        // 设置价格突破参数
        TechnicalIndicatorConfig.PriceConfig priceConfig = new TechnicalIndicatorConfig.PriceConfig();
        priceConfig.setRequireBreakAllMa(requireBreakAllMa);
        config.setPrice(priceConfig);

        return config;
    }

    /**
     * 计算最大回撤
     */
    private double calculateMaxDrawdown(List<Double> profits) {
        if (profits.isEmpty()) return 0;

        double maxDrawdown = 0;
        double peak = profits.get(0);
        double currentDrawdown;

        for (double profit : profits) {
            if (profit > peak) {
                peak = profit;
            }
            currentDrawdown = (peak - profit) / peak;
            if (currentDrawdown > maxDrawdown) {
                maxDrawdown = currentDrawdown;
            }
        }

        return maxDrawdown;
    }

    /**
     * 计算夏普比率（简化版）
     */
    private double calculateSharpeRatio(List<Double> profits) {
        if (profits.isEmpty()) return 0;

        double avgReturn = profits.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = calculateStandardDeviation(profits);

        // 假设无风险利率为0
        return stdDev > 0 ? avgReturn / stdDev : 0;
    }

    /**
     * 计算标准差
     */
    private double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) return 0;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average().orElse(0);

        return Math.sqrt(variance);
    }

    /**
     * 计算回落企稳上涨参数组合总数
     */
    private int generateFallStabilizeRiseCombinations(ParameterOptimizationConfig.FallStabilizeRiseSearch searchSpace) {
        return searchSpace.getSignificantFallThresholds().size() *
                searchSpace.getNoNewLowThresholds().size() *
                searchSpace.getVolatilityThresholds().size() *
                searchSpace.getVolumeThresholds().size() *
                searchSpace.getPriceRiseThresholds().size() *
                searchSpace.getVolumeSurgeThresholds().size() *
                searchSpace.getRequireBreakMas().size() *
                searchSpace.getRequireTechnicalIndicators().size();
    }

    /**
     * 计算均线粘滞突破参数组合总数
     */
    private int generateMaConsistencyBreakoutCombinations(ParameterOptimizationConfig.MaConsistencyBreakoutSearch searchSpace) {
        return searchSpace.getVariationThresholds().size() *
                searchSpace.getBreakoutRatios().size() *
                searchSpace.getPreviousDayRatios().size() *
                searchSpace.getRequireBreakAllMas().size();
    }

    /**
     * 获取指定时间范围的股票数据
     */
    private List<StockTechnicalIndicators> getStockDataInRange(String tsCode, LocalDate startDate, LocalDate endDate) {
        QueryWrapper<StockTechnicalIndicators> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ts_code", tsCode)
                .ge("trade_date", startDate)
                .le("trade_date", endDate)
                .orderByAsc("trade_date");
        return stockTechnicalIndicatorsService.list(queryWrapper);

    }

    @Data
    public static class OptimizationResult {
        private ParameterEvaluation bestParameters;
        private List<ParameterEvaluation> evaluations;
        private String strategyType;

        public OptimizationResult() {
            this.evaluations = new ArrayList<>();
        }
    }

    @Data
    public static class ParameterEvaluation {
        private Object params; // 可能是 FallStabilizeRiseConfig 或 MaConsistencyBreakoutConfig
        private int totalSignals;
        private int successfulSignals;
        private double winRate;
        private double avgProfit;
        private double maxDrawdown;
        private double sharpeRatio;
        private String strategyType;

        public String getParameterSummary() {
            if ("fall-stabilize-rise".equals(strategyType)) {
                TechnicalIndicatorConfig.FallStabilizeRiseConfig fsr = (TechnicalIndicatorConfig.FallStabilizeRiseConfig) params;
                return String.format("回落:%.1f%%, 企稳:%.3f, 波动:%.3f, 量比:%.1f, 涨幅:%.1f%%, 放量:%.1f, 突破:%s, 技术:%s",
                        fsr.getFall().getSignificantFallThreshold(),
                        fsr.getStabilize().getNoNewLowThreshold(),
                        fsr.getStabilize().getVolatilityThreshold(),
                        fsr.getStabilize().getVolumeThreshold(),
                        fsr.getRise().getPriceRiseThreshold() * 100,
                        fsr.getRise().getVolumeSurgeThreshold(),
                        fsr.getRise().isRequireBreakMa() ? "是" : "否",
                        fsr.getRise().isRequireTechnicalIndicator() ? "是" : "否");
            } else if ("ma-consistency-breakout".equals(strategyType)) {
                TechnicalIndicatorConfig.MaConsistencyBreakoutConfig mc = (TechnicalIndicatorConfig.MaConsistencyBreakoutConfig) params;
                return String.format("差异:%.3f, 突破:%.1f, 前日:%.1f, 全突破:%s",
                        mc.getConsistency().getVariationThreshold(),
                        mc.getVolume().getBreakoutRatio(),
                        mc.getVolume().getPreviousDayRatio(),
                        mc.getPrice().isRequireBreakAllMa() ? "是" : "否");
            }
            return "未知策略类型";
        }
    }
}