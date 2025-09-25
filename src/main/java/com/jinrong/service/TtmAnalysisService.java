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
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jinrong.util.FinancialMetricCalculator.calculateCashFlowRatio;

@Service
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
    public void calculateAndSavePettm(LocalDate day) {


        // 获取所有股票代码
        List<String> tsCodes = basicMapper.selectList(new QueryWrapper<StockBasic>()
                        .select(" ts_code"))
                .stream().map(StockBasic::getTsCode).toList();

        for (String tsCode : tsCodes) {
            ThreadPoolComom.executorService.execute(() -> {
                try {
                    dosave(tsCode, day, "peTtm");
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            });

        }
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
        double month = reportDay.getMonth().getValue();
        List<StockDailyBasic> stockDailyBasics = dailyBasicMapper.selectList(new QueryWrapper<StockDailyBasic>().lambda()
                        .eq(StockDailyBasic::getTradeDate, reportDay));
//                .eq(StockDailyBasic::getTsCode,"300502.SZ")

        for (StockDailyBasic stockDailyBasic : stockDailyBasics) {
            ThreadPoolComom.executorService.execute(() -> {
                List<StockReportPrediction> getnpv3 = getnp(stockDailyBasic.getTsCode(), reportDay.getYear() + 2, null, reportDay);
                if (getnpv3.isEmpty()) {
                    return;
                }
                List<StockReportPrediction> getnpv2 = getnp(stockDailyBasic.getTsCode(), reportDay.getYear() + 1, null, reportDay);
                if (getnpv2.isEmpty()) {
                    return;
                }
                List<StockReportPrediction> getnpv1 = getnp(stockDailyBasic.getTsCode(), reportDay.getYear(), null, reportDay);
                if (getnpv1.isEmpty()) {
                    return;
                }
                List<IncomeStatement> incomeStatements = incomeStatementMapper.selectList(new QueryWrapper<IncomeStatement>().lambda()
                        .eq(IncomeStatement::getTsCode, stockDailyBasic.getTsCode())
                        .eq(IncomeStatement::getReportType, "1")
                        .le(IncomeStatement::getEndDate,   LocalDate.of(reportDay.getYear()-1, 12, 31))
                        .orderByDesc(IncomeStatement::getEndDate).orderByDesc(IncomeStatement::getUpdateFlag));
                if (incomeStatements.isEmpty()) {
                    return;
                }
                double v0 = incomeStatements.get(0).getNIncomeAttrP().divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP).doubleValue();

                double ocf = sxjxb(reportDay,stockDailyBasic.getTsCode());

                double k = ocf > 1.3 ? 1.3 : Math.max(ocf, 0.7);

                double v3 = getnpv3.stream().map(StockReportPrediction::getNp).mapToDouble(Double::doubleValue).average().orElse(0.0);

                double v2 = getnpv2.stream().map(StockReportPrediction::getNp).mapToDouble(Double::doubleValue).average().orElse(0.0);

                double v1 = getnpv1.stream().map(StockReportPrediction::getNp).mapToDouble(Double::doubleValue).average().orElse(0.0);

                double zx = ((v3 / v2 - 1) * 100 + (v2 / v1 - 1) * 100) / 2;

                double zxv2 = ((v3 / v2 - 1) * 100 + (v2 / v1 - 1) * 100 + (v1 / v0 - 1) * 100) / 3;

                double dincome = month / 12 * v1 + (12 - month) / 12 * v2;

                double dpe = stockDailyBasic.getTotalMv() / dincome;
                double dpev2=stockDailyBasic.getTotalMv()/v3;
                incomeStatements = incomeStatementMapper.selectList(new QueryWrapper<IncomeStatement>().lambda()
                        .eq(IncomeStatement::getTsCode, stockDailyBasic.getTsCode())
                        .eq(IncomeStatement::getReportType, "1")
                        .le(IncomeStatement::getEndDate, reportDay)
                        .orderByDesc(IncomeStatement::getEndDate).orderByDesc(IncomeStatement::getUpdateFlag));
                if (incomeStatements.isEmpty()) {
                    return;
                }
                IncomeStatement incomeStatement = incomeStatements.get(0);
                BigDecimal nIncomeAttrP = incomeStatement.getNIncomeAttrP();
                double incomeFinishedRatio = nIncomeAttrP.divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP).doubleValue() / (v1 * (((double) incomeStatement.getEndDate().getMonth().getValue()) / 12d));

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


                List<FinancialScore> financialScores = financialScoreMapper.selectList(new QueryWrapper<FinancialScore>().lambda()
                        .eq(FinancialScore::getTsCode, stockDailyBasic.getTsCode())
                        .le(FinancialScore::getScoreYear, reportDay.minusYears(1).getYear())
                        .orderByDesc(FinancialScore::getScoreYear));
                if (financialScores.isEmpty()) {
                    return;
                }
                Double socre = financialScores.get(0).getSocre();
                if (financialScores.size() > 5) {
                    financialScores = financialScores.subList(0, 5);
                }
                Double socrev5a = financialScores.stream().map(FinancialScore::getSocre).mapToDouble(Double::doubleValue)
                        .average().orElse(0);
                Double zsocre = socre * 0.6 + socrev5a * 0.4;
                double s = zsocre >= 60 ? 1.2 : zsocre < 40 ? 0.8 : 1;
                double pez = meanValue * 0.3 + s * zx * 0.7;
                double pev2 =  meanValue * 0.3 + zxv2 * 0.7;
                if (zx > 0 && pez > zx * 1.5) {
                    pez = zx * 1.5;
                }
                double v=(k*v3*pev2*s)/(1.03*1.03*1.03);
                double vac = dpe / pez;
                Valuation valuation = new Valuation();
                valuation.setTsCode(stockDailyBasic.getTsCode());
                valuation.setName(financialScores.get(0).getName());
                valuation.setDate(reportDay);
                valuation.setPed(dpe);
                valuation.setPedv2(dpev2);
                valuation.setIncomed(dincome);
                valuation.setIncomedv2(v3);
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
                valuation.setSafeMargin(v/2);
                try {
                    valuationMapper.insert(valuation);
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
        }
    }
    @Autowired
    private CashFlowStatementMapper cashFlowStatementMapper;
public double sxjxb(LocalDate localDate,String tscode){
    int year = localDate.getYear();
    List<Double> list=new LinkedList<>();
    for (int i = 1; i <=5 ; i++) {
        LocalDate lastDay = LocalDate.of(year-i, 12, 31);
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
        if(cCashFlows.isEmpty()||incomeStatements.isEmpty()){
            continue;
        }
        Double v = calculateCashFlowRatio(
                cCashFlows.get(0).getNCashflowAct(),
                incomeStatements.get(0).getNIncomeAttrP()).doubleValue();
        list.add(v);
    }
    double v = list.stream().mapToDouble(Double::doubleValue).average().orElse(0);

    return v;
}
    public List<StockReportPrediction> getnp(String tsCode, int year, List<StockReportPrediction> source, LocalDate reportDay) {
        LambdaQueryWrapper<StockReportPrediction> stockReportPredictionLambdaQueryWrapper = new QueryWrapper<StockReportPrediction>().lambda()
                .eq(StockReportPrediction::getTsCode, tsCode).eq(StockReportPrediction::getQuarter, year + "Q4")
                .le(StockReportPrediction::getReportDate, reportDay)
                .isNotNull(StockReportPrediction::getNp)
                .orderByDesc(StockReportPrediction::getReportDate);
        if (source != null) {
            LocalDate minReportDate = source.get(source.size() - 1).getReportDate();
            List<String> list = source.stream().map(StockReportPrediction::getOrgName).toList();
            stockReportPredictionLambdaQueryWrapper.in(StockReportPrediction::getOrgName, list)
                    .ge(StockReportPrediction::getReportDate, minReportDate);
        }
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
        if (stockReportPredictions.size() > 10) {
            stockReportPredictions = stockReportPredictions.subList(0, 10);
        }

        return stockReportPredictions;
    }




    public void initreport_rc(String reportDate){
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