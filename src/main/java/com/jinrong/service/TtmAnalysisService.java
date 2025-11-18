package com.jinrong.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.common.InitComon;
import com.jinrong.common.SDKforTushare;
import com.jinrong.common.ThreadPoolComom;
import com.jinrong.entity.*;
import com.jinrong.mapper.*;
import com.jinrong.util.ShortUUID;
import com.jinrong.util.StatisticsUtil;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jinrong.util.FinancialMetricCalculator.calculateCashFlowRatio;

import lombok.extern.slf4j.Slf4j;
@Service@Slf4j
public class TtmAnalysisService {
    @Autowired
    SDKforTushare sdKforTushare;
    @Autowired
    private StockDailyBasicMapper dailyBasicMapper;
    @Autowired
    StockBasicMapper basicMapper;
    @Autowired
    private StockTtmAnalysisMapper analysisMapper;
    @Autowired
    private StockTtmAnalysisMapper stockTtmAnalysisMapper;
    @Autowired
    private FinancialScoreMapper financialScoreMapper;
    @Autowired
    private FinIndicatorMapper finIndicatorMapper;

    @SneakyThrows
    @Transactional
    public List<Future> calculateAndSavePettm(LocalDate day) {
        List<Future> list = new ArrayList<>();
        List<String> tsCodes = dailyBasicMapper
                .selectList(new QueryWrapper<StockDailyBasic>().lambda().eq(StockDailyBasic::getTradeDate, day))
                .stream().map(StockDailyBasic::getTsCode).toList();
        for (String tsCode : tsCodes) {
            list.add(ThreadPoolComom.executorService.submit(() -> {
                try {
                    dosave(tsCode, day, "peTtm");
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        return list;
    }


    public void dosave(String tsCode, LocalDate day, String type) throws NoSuchFieldException {
        Field declaredField = StockDailyBasic.class.getDeclaredField(type);
        declaredField.setAccessible(true);
        List<String> periods = Arrays.asList("1Y", "5Y", "10Y");
        for (String period : periods) {
            // 获取历史数据
            List<StockDailyBasic> historyData = getHistoryData(tsCode, period, day);
            List<Double> values = historyData.stream()
                    .map(v -> {
                        try {
                            return (Double) declaredField.get(v);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (values.size() < 10) continue; // 数据不足跳过

            // 计算正态分布指标
            double[] stats = StatisticsUtil.calculateNormalDistributionV2(values);

            // 构建存储对象
            StockTtmAnalysis analysis = new StockTtmAnalysis();
            analysis.setId(ShortUUID.compressToBase62());
            analysis.setTsCode(tsCode);
            analysis.setAnalysisPeriod(period);
            analysis.setDataPoints(values.size());
            analysis.setMeanValue(stats[0]);
            analysis.setStdDev(stats[1]);
            analysis.setP30Value(stats[2]);
            analysis.setP70Value(stats[3]);
            analysis.setCalcDate(day);
            analysis.setType(type);
            analysisMapper.insert(analysis);
        }
    }

    private List<StockDailyBasic> getHistoryData(String tsCode, String period, LocalDate endDate) {
        LocalDate startDate = switch (period) {
            case "1Y" -> endDate.minusYears(1);
            case "5Y" -> endDate.minusYears(5);
            case "10Y" -> endDate.minusYears(10);
            default -> throw new IllegalArgumentException("Invalid period");
        };

        return dailyBasicMapper.selectList(new QueryWrapper<StockDailyBasic>()
                .eq("ts_code", tsCode)
                .between("trade_date", startDate, endDate)
        );
    }

    @Autowired
    StockReportPredictionMapper stockReportPredictionMapper;
    @Autowired
    ValuationMapper valuationMapper;

    @Autowired
    private IncomeStatementMapper incomeStatementMapper;

    public void dxpez(LocalDate reportDay) {
        List<StockDailyBasic> stockDailyBasics = dailyBasicMapper.selectList(new QueryWrapper<StockDailyBasic>().lambda()
                .eq(StockDailyBasic::getTradeDate, reportDay));
//                .eq(StockDailyBasic::getTsCode,"300502.SZ")
        valuationMapper.delete(new QueryWrapper<Valuation>().lambda().eq(Valuation::getDate, reportDay));
        for (StockDailyBasic stockDailyBasic : stockDailyBasics) {
            ThreadPoolComom.executorService.execute(() -> {
                List[] sxjxb = sxjxb(reportDay, stockDailyBasic.getTsCode());
                double ocf=(( List<Double>)sxjxb[1]).stream().mapToDouble(Double::doubleValue).average().orElse(0);
                List<IncomeStatement> incomeStatements = sxjxb[0];
                if (incomeStatements.isEmpty()) {
                    return;
                }
                IncomeStatement incomeStatement = incomeStatementMapper.selectOne(new QueryWrapper<IncomeStatement>().lambda()
                        .eq(IncomeStatement::getTsCode, stockDailyBasic.getTsCode())
                        .eq(IncomeStatement::getReportType, "1")
                        .le(IncomeStatement::getEndDate, reportDay)
                        .orderByDesc(IncomeStatement::getEndDate).orderByDesc(IncomeStatement::getUpdateFlag).last(" limit 1"));
                if (incomeStatement==null) {
                    return;
                }
                //今年最近财报季的收入
                double v4 = incomeStatement.getNIncomeAttrP().divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP).doubleValue();
                double month =  incomeStatementMapper.selectOne(new QueryWrapper<IncomeStatement>().lambda()
                        .eq(IncomeStatement::getTsCode, stockDailyBasic.getTsCode())
                        .orderByDesc(IncomeStatement::getEndDate).select(IncomeStatement::getEndDate).last(" limit 1")).getEndDate().getMonth().getValue();
                //去年年报的收入
                double v0 = incomeStatements.get(0).getNIncomeAttrP().divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP).doubleValue();
                double k = ocf > 1.3 ? 1.3 : Math.max(ocf, 0.7);
                double zx ,zxv2,dpe,dpev2,dincome,dincomev2,v1;
                List<StockReportPrediction> getnpv3 = getnp(stockDailyBasic.getTsCode(), reportDay.getYear() + 2, reportDay);
                List<StockReportPrediction> getnpv2 = getnp(stockDailyBasic.getTsCode(), reportDay.getYear() + 1, reportDay);
                List<StockReportPrediction> getnpv1 = getnp(stockDailyBasic.getTsCode(), reportDay.getYear(), reportDay);
                String type;
                List<FinancialScore> financialScores = financialScoreMapper.selectList(new QueryWrapper<FinancialScore>().lambda()
                        .eq(FinancialScore::getTsCode, stockDailyBasic.getTsCode())
                        .orderByDesc(FinancialScore::getEndDate));
                if (financialScores.isEmpty()) {
                    return;
                }
                if(getnpv3.isEmpty()||getnpv2.isEmpty()||getnpv1.isEmpty()){
                    type="g";
                    List<Double> list = new ArrayList<>();
                    IncomeStatement incomeStatementls =null;
                    for (int i = 0; i < incomeStatements.size(); i++) {
                        if(incomeStatementls!=null){
                            list.add(incomeStatementls.getNIncomeAttrP().doubleValue()/incomeStatements.get(0).getNIncomeAttrP().doubleValue());
                        }
                        incomeStatementls=incomeStatements.get(i);
                    }
                    double zxr=list.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    zx =(zxr-1)*100 ;
                    zxv2=zx;
                    if(stockDailyBasic.getPeTtm()==null){
                        dpe=-1;
                    }else{
                        dpe=stockDailyBasic.getPeTtm()/zxr;
                    }

                    dpev2=dpe;
                    dincome=v4*zxr;
                    v1=v0*zxr;
                    dincomev2=v1*zxr*zxr;
                }else{
                    type="y";
                    double v3 = getnpv3.stream().map(StockReportPrediction::getNp).mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double v2 = getnpv2.stream().map(StockReportPrediction::getNp).mapToDouble(Double::doubleValue).average().orElse(0.0);
                     v1 = getnpv1.stream().map(StockReportPrediction::getNp).mapToDouble(Double::doubleValue).average().orElse(0.0);
                    zx = ((v3 / v2 - 1) * 100 + (v2 / v1 - 1) * 100) / 2;
                    zxv2 = ((v3 / v2 - 1) * 100 + (v2 / v1 - 1) * 100 + (v1 / v0 - 1) * 100) / 3;
                     dincome = (12 - month) / 12 * v1 + month / 12 * v2;
                     dpe = stockDailyBasic.getTotalMv() / dincome;
                     dpev2 = stockDailyBasic.getTotalMv() / v3;
                     dincomev2=v3;
                }


                double incomeFinishedRatio =  v4 / (v1 * (((double) incomeStatement.getEndDate().getMonth().getValue()) / 12d));

                List<StockTtmAnalysis> peTtm = stockTtmAnalysisMapper.selectList(new QueryWrapper<StockTtmAnalysis>()
                        .lambda()
                        .eq(StockTtmAnalysis::getTsCode, stockDailyBasic.getTsCode())
                        .eq(StockTtmAnalysis::getType, "peTtm")
                        .eq(StockTtmAnalysis::getAnalysisPeriod, "5Y")
                        .le(StockTtmAnalysis::getCalcDate, reportDay)
                        .orderByDesc(StockTtmAnalysis::getCalcDate));
                if (peTtm.isEmpty()) {
                    return;
                }
                Double meanValue = peTtm.get(0).getMeanValue();

                Double socre = financialScores.get(0).getSocre();
                if (financialScores.size() > 20) {
                    financialScores = financialScores.subList(0, 20);
                }
                Double socrev5a = financialScores.stream().map(FinancialScore::getSocre).mapToDouble(Double::doubleValue)
                        .average().orElse(0);
                Double zsocre = socre * 0.6 + socrev5a * 0.4;
                double s = zsocre >= 60 ? 1.2 : zsocre < 40 ? 0.8 : 1;
                double pez = meanValue * 0.3 + s * zx * 0.7;
                double pev2 = meanValue * 0.3 + zxv2 * 0.7;
                if (zx > 0 && pez > zx * 1.5) {
                    pez = zx * 1.5;
                }
                double v = (k * dincomev2 * pev2 * s) / (1.03 * 1.03 * 1.03);
                double vac = dpe / pez / incomeFinishedRatio;
                Valuation valuation = new Valuation();
                valuation.setTsCode(stockDailyBasic.getTsCode());
                valuation.setName(financialScores.get(0).getName());
                valuation.setDate(reportDay);
                valuation.setPed(dpe);
                valuation.setPedv2(dpev2);
                valuation.setIncomed(dincome);
                valuation.setIncomedv2(dincomev2);
                valuation.setHlpe(pez);
                valuation.setHlpev2(pev2);
                valuation.setPettmz(meanValue);
                valuation.setValuationPercentage(vac);
                valuation.setIncomeIncreatePercentage(zx);
                valuation.setIncomeIncreatePercentagev2(zxv2);
                valuation.setFScore(zsocre);
                valuation.setId(ShortUUID.compressToBase62());
                valuation.setIncomeFinishedRatio(incomeFinishedRatio);
                valuation.setRScore(socre);
                valuation.setReasonMarketVal(v);
                valuation.setSafeMargin(v / 2);
                valuation.setType(type);
                try {
                    valuationMapper.insert(valuation);
                } catch (Exception e) {
                    log.info(JSON.toJSONString(valuation));
                    e.printStackTrace();
                }
            });
        }
    }

    @Autowired
    private CashFlowStatementMapper cashFlowStatementMapper;

    public List[] sxjxb(LocalDate localDate, String tscode) {
        List<IncomeStatement> rincomeStatements=new ArrayList<>(5);
        int year = localDate.getYear();
        List<Double> list = new LinkedList<>();
        for (int i = 1; i <= 5; i++) {
            LocalDate lastDay = LocalDate.of(year - i, 12, 31);
            List<IncomeStatement> incomeStatements = incomeStatementMapper.selectList(new LambdaQueryWrapper<IncomeStatement>()
                    .eq(IncomeStatement::getTsCode, tscode)
                    .eq(IncomeStatement::getEndDate, lastDay)
                    .eq(IncomeStatement::getReportType, "1")
                    .orderByDesc(IncomeStatement::getUpdateFlag));
            List<CashFlowStatement> cCashFlows = cashFlowStatementMapper.selectList(new LambdaQueryWrapper<CashFlowStatement>()
                    .orderByDesc(CashFlowStatement::getUpdateFlag)
                    .eq(CashFlowStatement::getTsCode, tscode)
                    .eq(CashFlowStatement::getEndDate, lastDay)
                    .eq(CashFlowStatement::getReportType, "1")
            );
            if (cCashFlows.isEmpty() || incomeStatements.isEmpty()) {
                continue;
            }
            Double v = calculateCashFlowRatio(
                    cCashFlows.get(0).getNCashflowAct(),
                    incomeStatements.get(0).getNIncomeAttrP()).doubleValue();
            list.add(v);
            rincomeStatements.add(incomeStatements.get(0));
        }
        return new  List[]{rincomeStatements, list};
    }

    public List<StockReportPrediction> getnp(String tsCode, int year, LocalDate reportDay) {
        LambdaQueryWrapper<StockReportPrediction> stockReportPredictionLambdaQueryWrapper = new QueryWrapper<StockReportPrediction>().lambda()
                .eq(StockReportPrediction::getTsCode, tsCode).eq(StockReportPrediction::getQuarter, year + "Q4")
                .ge(StockReportPrediction::getReportDate, reportDay.minusMonths(3).minusDays(15))
                .isNotNull(StockReportPrediction::getNp)
                .orderByDesc(StockReportPrediction::getReportDate);

        Set<String> jg = new HashSet<>();
        List<StockReportPrediction> stockReportPredictions = stockReportPredictionMapper.selectList(stockReportPredictionLambdaQueryWrapper).stream().filter(v ->
                {
                    boolean contains = jg.contains(v.getOrgName());
                    if (contains) {
                        return false;
                    } else {
                        jg.add(v.getName());
                        return true;
                    }
                }
        ).toList();
//        if (stockReportPredictions.size() > 10) {
//            stockReportPredictions = stockReportPredictions.subList(0, 10);
//        }

        return stockReportPredictions;
    }


    public void initreport_rc(String reportDate) {
        List<HashMap<String, Object>> dailyBasic = sdKforTushare.getApiResponse("report_rc",
                new HashMap<>() {{
                    put("report_date", reportDate);
                }},
                "" // 如 "ts_code,symbol,name"
        );
        List<StockReportPrediction> parse = new InitComon<StockReportPrediction>().parse(StockReportPrediction.class, dailyBasic);
        stockReportPredictionMapper.delete(new QueryWrapper<StockReportPrediction>().lambda()
                .eq(StockReportPrediction::getReportDate, LocalDate.parse(reportDate, InitComon.formatter)));
        stockReportPredictionMapper.insert(parse);
    }
}