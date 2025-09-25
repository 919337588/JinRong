package com.jinrong.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jinrong.mapper.*;
import com.jinrong.entity.*;
import com.jinrong.util.FinancialMetricCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.jinrong.util.FinancialMetricCalculator.*;

@Service
public class WallScoreService {
    @Autowired
    private WallScoreStandardMapper standardMapper;
    @Autowired
    private BalanceSheetMapper balanceSheetMapper;
    @Autowired
    private IncomeStatementMapper incomeStatementMapper;
    @Autowired
    private CashFlowStatementMapper cashFlowStatementMapper;
    private static final Map<String, String> INDUSTRY_MAP = new HashMap<>();

    @Autowired
    private FinancialScoreMapper financialScoreMapper;

    @Autowired
    FinIndicatorMapper finIndicatorMapper;

    public void calculateWallScore(StockBasic stockBasic,
                                   LocalDate reportDate) {
        String companyCode = stockBasic.getTsCode();
        LocalDate priorDate = reportDate.minusYears(1);
        // 3. 获取行业评分标准
        List<WallScoreStandard> standards = standardMapper.selectByIndustry(
                "default"
        );

        List<String> metricCodes = standards.stream().map(WallScoreStandard::getMetricCode).distinct().toList();

        // 1. 获取当前报告期财务数据
        List<BalanceSheet> balanceSheets = balanceSheetMapper.selectList(new LambdaQueryWrapper<BalanceSheet>()
                .eq(BalanceSheet::getTsCode, companyCode)
                .eq(BalanceSheet::getEndDate, reportDate)
                .eq(BalanceSheet::getReportType, "1")
                .orderByDesc(BalanceSheet::getUpdateFlag)
        );
        if (balanceSheets.isEmpty()) {
            return;
        }
        List<IncomeStatement> incomeStatements = incomeStatementMapper.selectList(new LambdaQueryWrapper<IncomeStatement>()
                .eq(IncomeStatement::getTsCode, companyCode)
                .eq(IncomeStatement::getEndDate, reportDate)
                .eq(IncomeStatement::getReportType, "1")
                .orderByDesc(IncomeStatement::getUpdateFlag));
        Map<String, BigDecimal> metrics;
        if (incomeStatements.isEmpty()) {
            return;
        }


        // 2. 获取上年同期数据（用于增长率计算）
        List<IncomeStatement> priorIncomes = incomeStatementMapper.selectList(new LambdaQueryWrapper<IncomeStatement>()
                .orderByDesc(IncomeStatement::getUpdateFlag)
                .eq(IncomeStatement::getTsCode, companyCode)
                .eq(IncomeStatement::getEndDate, priorDate)
                .eq(IncomeStatement::getReportType, "1"));
        if (priorIncomes.isEmpty()) {
            return;
        }
        List<BalanceSheet> priorBalanceSheets = balanceSheetMapper.selectList(new LambdaQueryWrapper<BalanceSheet>()
                .orderByDesc(BalanceSheet::getUpdateFlag)
                .eq(BalanceSheet::getTsCode, companyCode)
                .eq(BalanceSheet::getEndDate, priorDate)
                .eq(BalanceSheet::getReportType, "1"));
        if (priorBalanceSheets.isEmpty()) {
            return;
        }
        List<CashFlowStatement> priorCashFlows = cashFlowStatementMapper.selectList(new LambdaQueryWrapper<CashFlowStatement>()
                .orderByDesc(CashFlowStatement::getUpdateFlag)
                .eq(CashFlowStatement::getTsCode, companyCode)
                .eq(CashFlowStatement::getEndDate, priorDate)
                .eq(CashFlowStatement::getReportType, "1")
        );
        if (priorCashFlows.isEmpty()) {
            return;
        }
        List<CashFlowStatement> cCashFlows = cashFlowStatementMapper.selectList(new LambdaQueryWrapper<CashFlowStatement>()
                .orderByDesc(CashFlowStatement::getUpdateFlag)
                .eq(CashFlowStatement::getTsCode, companyCode)
                .eq(CashFlowStatement::getEndDate, reportDate)
                .eq(CashFlowStatement::getReportType, "1")
        );
        if (cCashFlows.isEmpty()) {
            return;
        }
        // 4. 计算所有财务指标
        metrics = calculateAllMetrics(metricCodes,
                balanceSheets.get(0), incomeStatements.get(0), cCashFlows.get(0),
                priorBalanceSheets.get(0), priorIncomes.get(0), priorCashFlows.get(0)
        );
        // 5. 计算总分
        HashMap<String, HashMap<String, Object>> stringHashMapHashMap = new HashMap<>();
        FinancialScore financialScore = new FinancialScore();
        BigDecimal bigDecimal = computeTotalScore(metrics, standards, financialScore, stringHashMapHashMap);
        financialScore.setScoreYear(reportDate.getYear());
        financialScore.setSocre(bigDecimal.doubleValue());
        financialScore.setTsCode(stockBasic.getTsCode());
        financialScore.setName(stockBasic.getName());
        financialScore.setDetail(stringHashMapHashMap);
        financialScoreMapper.insert(financialScore);
    }

    private Map<String, BigDecimal> calculateAllMetrics(
            List<String> metricCodes,
            BalanceSheet bs, IncomeStatement is, CashFlowStatement cf,
            BalanceSheet pbs, IncomeStatement pis, CashFlowStatement pcf
    ) {
        Map<String, BigDecimal> metrics = new HashMap<>();

        // 预计算常用平均值（新增流动资产平均值）
        BigDecimal avgAccountsReceivable = FinancialMetricCalculator.calculateAverage(
                pbs != null ? pbs.getAccountsReceiv() : bs.getAccountsReceiv(),
                bs.getAccountsReceiv()
        );
        BigDecimal avgInventory = FinancialMetricCalculator.calculateAverage(
                pbs != null ? pbs.getInventories() : bs.getInventories(),
                bs.getInventories()
        );
        BigDecimal avgTotalAssets = FinancialMetricCalculator.calculateAverage(
                pbs != null ? pbs.getTotalAssets() : bs.getTotalAssets(),
                bs.getTotalAssets()
        );
        BigDecimal avgFixedAssets = FinancialMetricCalculator.calculateAverage(
                pbs != null ? pbs.getFixAssets() : bs.getFixAssets(),
                bs.getFixAssets()
        );
        // 新增流动资产平均值（用于流动资产周转率）
        BigDecimal avgCurrentAssets = FinancialMetricCalculator.calculateAverage(
                pbs != null ? pbs.getTotalCurAssets() : bs.getTotalCurAssets(),
                bs.getTotalCurAssets()
        );

        for (String metricCode : metricCodes) {
            try {
                switch (metricCode) {
                    // === 偿债能力（5个）===
                    case "CR":
                        metrics.put("CR", calculateCurrentRatio(bs.getTotalCurAssets(), bs.getTotalCurLiab()));
                        break;
                    case "CASH_RATIO":
                        metrics.put("CASH_RATIO", calculateCashRatio(bs.getMoneyCap(), bs.getTotalCurLiab()));
                        break;
                    case "DEBT_RATIO":
                        metrics.put("DEBT_RATIO", calculateDebtRatio(bs.getTotalLiab(), bs.getTotalAssets()));
                        break;
                    case "INTEREST_BEARING_DEBT_RATIO":
                        metrics.put("INTEREST_BEARING_DEBT_RATIO", calculateInterestBearingDebtRatio(bs));
                        break;
                    case "LONG_TERM_DEBT_RATIO":
                        metrics.put("LONG_TERM_DEBT_RATIO", calculateLongTermDebtRatio(bs));
                        break;

                    // === 盈利能力（6个）===
                    case "ROA":
                        metrics.put("ROA", calculateROA(is.getNIncomeAttrP(), bs.getTotalAssets()));
                        break;
                    case "ROE":
                        metrics.put("ROE", calculateROE(is.getNIncomeAttrP(), bs.getTotalHldrEqyExcMinInt()));
                        break;
                    case "GROSS_MARGIN":
                        metrics.put("GROSS_MARGIN", calculateGrossMargin(is.getRevenue(), is.getOperCost()));
                        break;
                    // 新增营业净利率
                    case "NET_PROFIT_MARGIN":
                        metrics.put("NET_PROFIT_MARGIN", calculateNetProfitMargin(
                                is.getNIncomeAttrP(),
                                is.getRevenue()
                        ));
                        break;
                    case "OPERATING_PROFIT_MARGIN":
                        metrics.put("OPERATING_PROFIT_MARGIN", calculateOperatingProfitMargin(
                                is.getOperateProfit(),
                                is.getRevenue()
                        ));
                        break;
                    case "ROIC":
                        BigDecimal nonInterestLiab = safeValue(bs.getTotalCurLiab()).subtract(safeValue(bs.getStBorr()));
                        metrics.put("ROIC", calculateROIC(is.getEbit(), BigDecimal.valueOf(0.3),
                                bs.getTotalAssets(), nonInterestLiab));
                        break;

                    // === 营运能力（5个）===
                    case "AR_TO":
                        metrics.put("AR_TO", calculateARTurnover(is.getRevenue(), avgAccountsReceivable));
                        break;
                    case "INV_TO":
                        metrics.put("INV_TO", calculateInventoryTurnover(is.getOperCost(), avgInventory));
                        break;
                    // 新增流动资产周转率
                    case "CURRENT_ASSET_TO":
                        metrics.put("CURRENT_ASSET_TO", calculateCurrentAssetTurnover(
                                is.getRevenue(),
                                avgCurrentAssets
                        ));
                        break;
                    case "FIXED_ASSET_TO":
                        metrics.put("FIXED_ASSET_TO", calculateFixedAssetTurnover(is.getRevenue(), avgFixedAssets));
                        break;
                    case "TOTAL_ASSET_TO":
                        metrics.put("TOTAL_ASSET_TO", calculateAssetTurnover(is.getRevenue(), avgTotalAssets));
                        break;

                    // === 成长能力（4个）===
                    case "SALES_CAGR":
                        metrics.put("SALES_CAGR", calculateSalesGrowth(
                                is.getRevenue(), pis != null ? pis.getRevenue() : null));
                        break;
                    case "NET_PROFIT_CAGR":
                        metrics.put("NET_PROFIT_CAGR", calculateNetIncomeGrowth(
                                safeValue(is.getNIncomeAttrP()), pis != null ? safeValue(pis.getNIncomeAttrP()) : null));
                        break;
                    case "OP_PROFIT_CAGR":
                        BigDecimal opCAGR = pis != null ? calculateCagr(is.getOperateProfit(), pis.getOperateProfit())
                                : BigDecimal.ZERO;
                        metrics.put("OP_PROFIT_CAGR", opCAGR);
                        break;
                    case "FCF_CAGR":
                        BigDecimal currentFCF = calculateFreeCashFlow(cf.getNCashflowAct(), cf.getCPayAcqConstFiolta());
                        BigDecimal priorFCF = pcf != null ? calculateFreeCashFlow(pcf.getNCashflowAct(),
                                pcf.getCPayAcqConstFiolta()) : null;
                        metrics.put("FCF_CAGR", calculateNetIncomeGrowth(currentFCF, priorFCF));
                        break;

                    // === 现金流量（3个）===
                    case "CASH_FLOW_RATIO":
                        metrics.put("CASH_FLOW_RATIO", calculateCashFlowRatio(
                                cf.getNCashflowAct(),
                                is.getNIncomeAttrP()
                        ));
                        break;
                    case "OCF_CAGR":
                        BigDecimal ocfCAGR = pcf != null ? calculateCagr(cf.getNCashflowAct(), pcf.getNCashflowAct())
                                : BigDecimal.ZERO;
                        metrics.put("OCF_CAGR", ocfCAGR);
                        break;
                    case "ROOA":
                        BigDecimal operatingAssets = safeValue(bs.getTotalAssets())
                                .subtract(safeValue(bs.getFixAssets()))
                                .subtract(safeValue(bs.getTradAsset()));
                        metrics.put("ROOA", safeDivide(is.getOperateProfit(), operatingAssets, 4));
                        break;

                    // === 其他（2个）===
                    case "FIXED_ASSET_RATIO":
                        metrics.put("FIXED_ASSET_RATIO", calculateFixedAssetRatio(
                                bs.getFixAssets(),
                                bs.getTotalAssets()
                        ));
                        break;
                }
            } catch (Exception e) {
                metrics.put(metricCode, null);
            }
        }
        return metrics;
    }

    private BigDecimal computeTotalScore(
            Map<String, BigDecimal> metrics,
            List<WallScoreStandard> standards,
            FinancialScore financialScore,
            HashMap<String, HashMap<String, Object>> stringHashMapHashMap
    ) {
        BigDecimal totalScore = BigDecimal.ZERO;

        for (WallScoreStandard standard : standards) {
            String metricCode = standard.getMetricCode();
            BigDecimal actualValue = metrics.get(metricCode);
            BigDecimal standardValue = standard.getStandardValue();
            HashMap<String, Object> hashMap = new HashMap<>() {{
                put("name", standard.getMetricName());
                put("actualValue", actualValue);
                put("standardValue", standardValue);
            }};
            stringHashMapHashMap.put(standard.getMetricCode(), hashMap);
            // 1. 设置指标值到实体对象（类型转换：BigDecimal → Double）
            setMetricValue(financialScore, metricCode, actualValue);

            // 2. 空值/负值跳过（得0分）
            if (actualValue == null || actualValue.compareTo(BigDecimal.ZERO) < 0) {
                continue;
            }

            // 3. 标准值异常保护
            if (standardValue == null || standardValue.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 4. 计算比例值（正向/负向指标差异化处理）
            BigDecimal ratio = null;
            if (Boolean.TRUE.equals(standard.getIsPositive())) {
                ratio = actualValue.compareTo(standardValue) >= 0 ?
                        BigDecimal.ONE :
                        actualValue.divide(standardValue, 4, RoundingMode.HALF_UP);
            } else {
                ratio = actualValue.compareTo(standardValue) <= 0 ?
                        BigDecimal.ONE :
                        standardValue.divide(actualValue, 4, RoundingMode.HALF_UP);
            }
            hashMap.put("score_c", (int) (ratio.doubleValue() * 100));
            // 5. 累加单项得分
            BigDecimal indicatorScore = ratio.multiply(standard.getWeight());
            totalScore = totalScore.add(indicatorScore);
        }

        // 6. 设置总分（保留2位小数）
        financialScore.setSocre(totalScore.setScale(2, RoundingMode.HALF_UP).doubleValue());
        return totalScore;
    }

    // 动态设置指标值（反射实现）
    private void setMetricValue(FinancialScore score, String metricCode, BigDecimal value) {
        try {
            // 指标代码转字段名映射（驼峰命名）
            String fieldName = convertToFieldName(metricCode);
            Field field = score.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(score, value != null ? value.doubleValue() : null);
        } catch (Exception e) {
        }
    }

    // 指标代码转实体字段名（示例：CASH_RATIO → cashRatio）
    private String convertToFieldName(String metricCode) {
        // 特殊缩写处理（如ROA/ROE等保持大写）
        if (metricCode.equals("ROA") || metricCode.equals("ROE") || metricCode.equals("ROIC") || metricCode.equals("ROOA")) {
            return metricCode.toLowerCase();
        }

        // 通用转换：下划线转驼峰
        String[] parts = metricCode.split("_");
        StringBuilder fieldName = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            fieldName.append(parts[i].substring(0, 1).toUpperCase())
                    .append(parts[i].substring(1).toLowerCase());
        }
        return fieldName.toString();
    }
}