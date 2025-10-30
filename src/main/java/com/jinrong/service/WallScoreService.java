package com.jinrong.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.mapper.*;
import com.jinrong.entity.*;
import com.jinrong.util.FinancialMetricCalculator;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
    StockBasicMapper stockBasicMapper;
    @Autowired
    FinIndicatorMapper finIndicatorMapper;

    public void calculateWallScore( String companyCode,String name,
                                   LocalDate reportDate) {

        if(StringUtils.isBlank(name)){
            List<StockBasic> stockBasics = stockBasicMapper.selectList(new QueryWrapper<StockBasic>().lambda().eq(StockBasic::getTsCode, companyCode));
            if(stockBasics!=null&& !stockBasics.isEmpty()){
                name=stockBasics.get(0).getName();
            }
        }
        // 1. 获取过去4个季度的报告期日期
        List<LocalDate> quarterDates = getLastFourQuarterDates(reportDate);
        List<LocalDate> localDates = getbeforeYearLastFourQuarterDates(reportDate);
        if (quarterDates.size() < 4 || localDates.size() < 4) {
            return; // 如果没有足够的季度数据，直接返回
        }

        // 2. 获取行业评分标准
        List<WallScoreStandard> standards = standardMapper.selectByIndustry(
                "default"
        );

        List<String> metricCodes = standards.stream().map(WallScoreStandard::getMetricCode).distinct().toList();

        // 3. 获取过去4个季度的财务数据
        BalanceSheet balanceSheets = getQuarterlyBalanceSheets(companyCode, quarterDates.get(0));
        if(balanceSheets==null){
            return;
        }

        List<IncomeStatement> incomeStatements = getQuarterlyIncomeStatements(companyCode, quarterDates);
        if (incomeStatements.size() < 4) {
            return;
        }

        List<CashFlowStatement> cashFlowStatements = getQuarterlyCashFlowStatements(companyCode, quarterDates);
        if (cashFlowStatements.size() < 4) {
            return;
        }

        // 4. 获取去年同期数据用于增长率计算
        List<IncomeStatement> priorYearIncomes = getQuarterlyIncomeStatements(companyCode, localDates);
        if (priorYearIncomes.size() < 4) {
            return;
        }


        BalanceSheet priorbalanceSheet = getQuarterlyBalanceSheets(companyCode, localDates.get(0));
        if (priorbalanceSheet==null) {
            return;
        }

        List<CashFlowStatement> priorYearCashFlows = getQuarterlyCashFlowStatements(companyCode, localDates);
        if (priorYearCashFlows.size() < 4) {
            return;
        }

        // 5. 计算所有财务指标（使用4个季度累计数据）
        Map<String, BigDecimal> metrics = calculateAllMetricsFromQuarters(
                metricCodes,
                balanceSheets, incomeStatements, cashFlowStatements,
                priorbalanceSheet, priorYearIncomes, priorYearCashFlows
        );

        // 6. 计算总分
        HashMap<String, HashMap<String, Object>> stringHashMapHashMap = new HashMap<>();
        FinancialScore financialScore = new FinancialScore();
        BigDecimal bigDecimal = computeTotalScore(metrics, standards, financialScore, stringHashMapHashMap);
        financialScore.setScoreYear(reportDate.getYear());
        financialScore.setSocre(bigDecimal.doubleValue());
        financialScore.setTsCode(companyCode);
        financialScore.setEndDate(reportDate);
        financialScore.setName(name);
        financialScore.setDetail(stringHashMapHashMap);
        financialScoreMapper.insert(financialScore);
    }

    /**
     * 获取过去4个季度的报告期日期
     */
    private List<LocalDate> getLastFourQuarterDates(LocalDate reportDate) {
        List<LocalDate> quarterDates = new ArrayList<>();
        for (int i = 0; i <= 3; i++) {
            quarterDates.add(reportDate.minusMonths(3 * i).with(TemporalAdjusters.lastDayOfMonth()));
        }
        return quarterDates;
    }

    private List<LocalDate> getbeforeYearLastFourQuarterDates(LocalDate reportDate) {
        List<LocalDate> quarterDates = new ArrayList<>();
        for (int i = 4; i <= 7; i++) {
            quarterDates.add(reportDate.minusMonths(3 * i).with(TemporalAdjusters.lastDayOfMonth()));
        }
        return quarterDates;
    }

    /**
     * 获取季度资产负债表数据
     */
    private  BalanceSheet getQuarterlyBalanceSheets(String companyCode, LocalDate date) {
            List<BalanceSheet> sheets = balanceSheetMapper.selectList(new LambdaQueryWrapper<BalanceSheet>()
                    .eq(BalanceSheet::getTsCode, companyCode)
                    .eq(BalanceSheet::getEndDate, date)
                    .eq(BalanceSheet::getReportType, "1")
                    .orderByDesc(BalanceSheet::getUpdateFlag));
            if (!sheets.isEmpty()) {
               return sheets.get(0);
            }
        return null;
    }

    /**
     * 获取季度利润表数据
     */
    private List<IncomeStatement> getQuarterlyIncomeStatements(String companyCode, List<LocalDate> quarterDates) {
        List<IncomeStatement> result = new ArrayList<>();
        for (LocalDate date : quarterDates) {
            List<IncomeStatement> statements = incomeStatementMapper.selectList(new LambdaQueryWrapper<IncomeStatement>()
                    .eq(IncomeStatement::getTsCode, companyCode)
                    .eq(IncomeStatement::getEndDate, date)
                    .eq(IncomeStatement::getReportType, "2")
                    .orderByDesc(IncomeStatement::getUpdateFlag));
            if (!statements.isEmpty()) {
                result.add(statements.get(0));
            }
        }
        return result;
    }

    /**
     * 获取季度现金流量表数据
     */
    private List<CashFlowStatement> getQuarterlyCashFlowStatements(String companyCode, List<LocalDate> quarterDates) {
        List<CashFlowStatement> result = new ArrayList<>();
        for (LocalDate date : quarterDates) {
            List<CashFlowStatement> statements = cashFlowStatementMapper.selectList(new LambdaQueryWrapper<CashFlowStatement>()
                    .eq(CashFlowStatement::getTsCode, companyCode)
                    .eq(CashFlowStatement::getEndDate, date)
                    .eq(CashFlowStatement::getReportType, "2")
                    .orderByDesc(CashFlowStatement::getUpdateFlag));
            if (!statements.isEmpty()) {
                result.add(statements.get(0));
            }
        }
        return result;
    }

    /**
     * 基于4个季度数据计算财务指标
     */
    private Map<String, BigDecimal> calculateAllMetricsFromQuarters(
            List<String> metricCodes,
            BalanceSheet bs ,
            List<IncomeStatement> incomeStatements,
            List<CashFlowStatement> cashFlowStatements,
            BalanceSheet pbs ,
            List<IncomeStatement> priorYearIss,
            List<CashFlowStatement> priorYearCfs
    ) {
        Map<String, BigDecimal> metrics = new HashMap<>();

        if (incomeStatements.size() < 4 || cashFlowStatements.size() < 4) {
            return metrics;
        }
        if (  priorYearCfs.size() < 4 || priorYearIss.size() < 4) {
            return metrics;
        }

        // 获取当前季度和去年同期数据

        // 计算4个季度累计数据
        IncomeStatement is = accumulateIncomeStatements(incomeStatements);
        CashFlowStatement cf = accumulateCashFlowStatements(cashFlowStatements);

        // 计算4个季度累计数据
        IncomeStatement pis = accumulateIncomeStatements(priorYearIss);
        CashFlowStatement pcf = accumulateCashFlowStatements(priorYearCfs);
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
                        BigDecimal ebit;
                        if (is.getEbit() != null&&is.getEbit().compareTo(BigDecimal.ZERO) != 0) {
                            ebit = is.getEbit();
                        } else {
                            // 使用营业利润 + 利息费用
                            BigDecimal operatingProfit = safeValue(is.getOperateProfit());
                            BigDecimal interestExpense = safeValue(is.getFinExpIntExp());
                            ebit = operatingProfit.add(interestExpense);

                            // 如果利息费用为空，使用财务费用作为近似值
                            if (interestExpense.compareTo(BigDecimal.ZERO) == 0) {
                                interestExpense = safeValue(is.getFinExp());
                                ebit = operatingProfit.add(interestExpense);
                            }
                        }

                        BigDecimal nonInterestLiab = safeValue(bs.getTotalCurLiab()).subtract(safeValue(bs.getStBorr()));
                        metrics.put("ROIC", calculateROIC(ebit, BigDecimal.valueOf(0.3),
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

    /**
     * 累计4个季度的利润表数据
     */
    private IncomeStatement accumulateIncomeStatements(List<IncomeStatement> statements) {
        if (statements == null || statements.isEmpty()) {
            return new IncomeStatement();
        }

        // 确保输入是4个季度且按时间倒序排列
        if (statements.size() != 4) {
            throw new IllegalArgumentException("需要连续4个季度的利润表数据");
        }

        // 取最新的季度数据作为基础
        IncomeStatement latest = statements.get(0);
        IncomeStatement result = new IncomeStatement();

        // 生成新的ID
        result.setId(UUID.randomUUID().toString());
        result.setTsCode(latest.getTsCode());
        result.setAnnDate(latest.getAnnDate());
        result.setFAnnDate(latest.getFAnnDate());
        result.setEndDate(latest.getEndDate()); // 使用最新季度的结束日期
        result.setCompType(latest.getCompType());

        // 累加数值字段
        result.setTotalRevenue(sum(statements, IncomeStatement::getTotalRevenue));
        result.setRevenue(sum(statements, IncomeStatement::getRevenue));
        result.setIntIncome(sum(statements, IncomeStatement::getIntIncome));
        result.setPremEarned(sum(statements, IncomeStatement::getPremEarned));
        result.setCommIncome(sum(statements, IncomeStatement::getCommIncome));
        result.setNCommisIncome(sum(statements, IncomeStatement::getNCommisIncome));
        result.setNOthIncome(sum(statements, IncomeStatement::getNOthIncome));
        result.setNOthBIncome(sum(statements, IncomeStatement::getNOthBIncome));
        result.setPremIncome(sum(statements, IncomeStatement::getPremIncome));
        result.setOutPrem(sum(statements, IncomeStatement::getOutPrem));
        result.setUnePremReser(sum(statements, IncomeStatement::getUnePremReser));
        result.setReinsIncome(sum(statements, IncomeStatement::getReinsIncome));
        result.setNSecTbIncome(sum(statements, IncomeStatement::getNSecTbIncome));
        result.setNSecUwIncome(sum(statements, IncomeStatement::getNSecUwIncome));
        result.setNAssetMgIncome(sum(statements, IncomeStatement::getNAssetMgIncome));
        result.setOthBIncome(sum(statements, IncomeStatement::getOthBIncome));
        result.setFvValueChgGain(sum(statements, IncomeStatement::getFvValueChgGain));
        result.setInvestIncome(sum(statements, IncomeStatement::getInvestIncome));
        result.setAssInvestIncome(sum(statements, IncomeStatement::getAssInvestIncome));
        result.setForexGain(sum(statements, IncomeStatement::getForexGain));
        result.setTotalCogs(sum(statements, IncomeStatement::getTotalCogs));
        result.setOperCost(sum(statements, IncomeStatement::getOperCost));
        result.setIntExp(sum(statements, IncomeStatement::getIntExp));
        result.setCommExp(sum(statements, IncomeStatement::getCommExp));
        result.setBizTaxSurchg(sum(statements, IncomeStatement::getBizTaxSurchg));
        result.setSellExp(sum(statements, IncomeStatement::getSellExp));
        result.setAdminExp(sum(statements, IncomeStatement::getAdminExp));
        result.setFinExp(sum(statements, IncomeStatement::getFinExp));
        result.setAssetsImpairLoss(sum(statements, IncomeStatement::getAssetsImpairLoss));
        result.setPremRefund(sum(statements, IncomeStatement::getPremRefund));
        result.setCompensPayout(sum(statements, IncomeStatement::getCompensPayout));
        result.setReserInsurLiab(sum(statements, IncomeStatement::getReserInsurLiab));
        result.setDivPayt(sum(statements, IncomeStatement::getDivPayt));
        result.setReinsExp(sum(statements, IncomeStatement::getReinsExp));
        result.setOperExp(sum(statements, IncomeStatement::getOperExp));
        result.setCompensPayoutRefu(sum(statements, IncomeStatement::getCompensPayoutRefu));
        result.setInsurReserRefu(sum(statements, IncomeStatement::getInsurReserRefu));
        result.setReinsCostRefund(sum(statements, IncomeStatement::getReinsCostRefund));
        result.setOtherBusCost(sum(statements, IncomeStatement::getOtherBusCost));
        result.setOperateProfit(sum(statements, IncomeStatement::getOperateProfit));
        result.setNonOperIncome(sum(statements, IncomeStatement::getNonOperIncome));
        result.setNonOperExp(sum(statements, IncomeStatement::getNonOperExp));
        result.setNcaDisploss(sum(statements, IncomeStatement::getNcaDisploss));
        result.setTotalProfit(sum(statements, IncomeStatement::getTotalProfit));
        result.setIncomeTax(sum(statements, IncomeStatement::getIncomeTax));
        result.setNIncome(sum(statements, IncomeStatement::getNIncome));
        result.setNIncomeAttrP(sum(statements, IncomeStatement::getNIncomeAttrP));
        result.setMinorityGain(sum(statements, IncomeStatement::getMinorityGain));
        result.setOthComprIncome(sum(statements, IncomeStatement::getOthComprIncome));
        result.setTComprIncome(sum(statements, IncomeStatement::getTComprIncome));
        result.setComprIncAttrP(sum(statements, IncomeStatement::getComprIncAttrP));
        result.setComprIncAttrMS(sum(statements, IncomeStatement::getComprIncAttrMS));
        result.setEbit(sum(statements, IncomeStatement::getEbit));
        result.setEbitda(sum(statements, IncomeStatement::getEbitda));
        result.setInsuranceExp(sum(statements, IncomeStatement::getInsuranceExp));
        result.setRdExp(sum(statements, IncomeStatement::getRdExp));
        result.setFinExpIntExp(sum(statements, IncomeStatement::getFinExpIntExp));
        result.setFinExpIntInc(sum(statements, IncomeStatement::getFinExpIntInc));
        result.setCreditImpaLoss(sum(statements, IncomeStatement::getCreditImpaLoss));
        result.setNetExpoHedgingBenefits(sum(statements, IncomeStatement::getNetExpoHedgingBenefits));
        result.setOthImpairLossAssets(sum(statements, IncomeStatement::getOthImpairLossAssets));
        result.setTotalOpcost(sum(statements, IncomeStatement::getTotalOpcost));
        result.setAmodcostFinAssets(sum(statements, IncomeStatement::getAmodcostFinAssets));
        result.setOthIncome(sum(statements, IncomeStatement::getOthIncome));
        result.setAssetDispIncome(sum(statements, IncomeStatement::getAssetDispIncome));
        result.setContinuedNetProfit(sum(statements, IncomeStatement::getContinuedNetProfit));
        result.setEndNetProfit(sum(statements, IncomeStatement::getEndNetProfit));

        // 特殊处理字段 - 使用最新季度的值
        result.setBasicEps(latest.getBasicEps()); // 每股收益使用最新值
        result.setDilutedEps(latest.getDilutedEps()); // 稀释每股收益使用最新值

        // 未分配利润相关字段使用最新季度的值
        result.setUndistProfit(latest.getUndistProfit());
        result.setDistableProfit(latest.getDistableProfit());

        // 更新标识
        result.setUpdateFlag("1");

        return result;
    }

    // 辅助方法：对四个季度的某个字段进行累加
    private <T> BigDecimal sum(List<T> statements, java.util.function.Function<T, BigDecimal> extractor) {
        if (statements == null || statements.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return statements.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    /**
     * 累计4个季度的现金流量表数据
     */
    private CashFlowStatement accumulateCashFlowStatements(List<CashFlowStatement> statements) {
        if (statements == null || statements.isEmpty()) {
            return new CashFlowStatement();
        }

        // 确保输入是4个季度且按时间倒序排列
        if (statements.size() != 4) {
            throw new IllegalArgumentException("需要连续4个季度的现金流量表数据");
        }

        // 取最新的季度数据作为基础
        CashFlowStatement latest = statements.get(0);
        CashFlowStatement earliest = statements.get(3); // 最早季度，用于期初余额
        CashFlowStatement result = new CashFlowStatement();

        // 生成新的ID
        result.setId(UUID.randomUUID().toString());

        // 基本信息使用最新季度的数据
        result.setTsCode(latest.getTsCode());
        result.setAnnDate(latest.getAnnDate());
        result.setFAnnDate(latest.getFAnnDate());
        result.setEndDate(latest.getEndDate()); // 使用最新季度的结束日期
        result.setCompType(latest.getCompType());
        result.setReportType("4"); // 设置为年度报告类型
        result.setEndType("4"); // 年度

        // 累加流量项目（四个季度累加）
        result.setNetProfit(sum(statements, CashFlowStatement::getNetProfit));
        result.setFinanExp(sum(statements, CashFlowStatement::getFinanExp));
        result.setCFrSaleSg(sum(statements, CashFlowStatement::getCFrSaleSg));
        result.setRecpTaxRends(sum(statements, CashFlowStatement::getRecpTaxRends));
        result.setNDeposIncrFi(sum(statements, CashFlowStatement::getNDeposIncrFi));
        result.setNIncrLoansCb(sum(statements, CashFlowStatement::getNIncrLoansCb));
        result.setNIncBorrOthFi(sum(statements, CashFlowStatement::getNIncBorrOthFi));
        result.setPremFrOrigContr(sum(statements, CashFlowStatement::getPremFrOrigContr));
        result.setNIncrInsuredDep(sum(statements, CashFlowStatement::getNIncrInsuredDep));
        result.setNReinsurPrem(sum(statements, CashFlowStatement::getNReinsurPrem));
        result.setNIncrDispTfa(sum(statements, CashFlowStatement::getNIncrDispTfa));
        result.setIfcCashIncr(sum(statements, CashFlowStatement::getIfcCashIncr));
        result.setNIncrDispFaas(sum(statements, CashFlowStatement::getNIncrDispFaas));
        result.setNIncrLoansOthBank(sum(statements, CashFlowStatement::getNIncrLoansOthBank));
        result.setNCapIncrRepur(sum(statements, CashFlowStatement::getNCapIncrRepur));
        result.setCFrOthOperateA(sum(statements, CashFlowStatement::getCFrOthOperateA));
        result.setCInfFrOperateA(sum(statements, CashFlowStatement::getCInfFrOperateA));
        result.setCPaidGoodsS(sum(statements, CashFlowStatement::getCPaidGoodsS));
        result.setCPaidToForEmpl(sum(statements, CashFlowStatement::getCPaidToForEmpl));
        result.setCPaidForTaxes(sum(statements, CashFlowStatement::getCPaidForTaxes));
        result.setNIncrCltLoanAdv(sum(statements, CashFlowStatement::getNIncrCltLoanAdv));
        result.setNIncrDepCbob(sum(statements, CashFlowStatement::getNIncrDepCbob));
        result.setCPayClaimsOrigInco(sum(statements, CashFlowStatement::getCPayClaimsOrigInco));
        result.setPayHandlingChrg(sum(statements, CashFlowStatement::getPayHandlingChrg));
        result.setPayCommInsurPlcy(sum(statements, CashFlowStatement::getPayCommInsurPlcy));
        result.setOthCashPayOperAct(sum(statements, CashFlowStatement::getOthCashPayOperAct));
        result.setStCashOutAct(sum(statements, CashFlowStatement::getStCashOutAct));
        result.setNCashflowAct(sum(statements, CashFlowStatement::getNCashflowAct));
        result.setOthRecpRalInvAct(sum(statements, CashFlowStatement::getOthRecpRalInvAct));
        result.setCDispWithdrwlInvest(sum(statements, CashFlowStatement::getCDispWithdrwlInvest));
        result.setCRecpReturnInvest(sum(statements, CashFlowStatement::getCRecpReturnInvest));
        result.setNRecpDispFiolta(sum(statements, CashFlowStatement::getNRecpDispFiolta));
        result.setNRecpDispSobu(sum(statements, CashFlowStatement::getNRecpDispSobu));
        result.setStotInflowsInvAct(sum(statements, CashFlowStatement::getStotInflowsInvAct));
        result.setCPayAcqConstFiolta(sum(statements, CashFlowStatement::getCPayAcqConstFiolta));
        result.setCPaidInvest(sum(statements, CashFlowStatement::getCPaidInvest));
        result.setNDispSubsOthBiz(sum(statements, CashFlowStatement::getNDispSubsOthBiz));
        result.setOthPayRalInvAct(sum(statements, CashFlowStatement::getOthPayRalInvAct));
        result.setNIncrPledgeLoan(sum(statements, CashFlowStatement::getNIncrPledgeLoan));
        result.setStotOutInvAct(sum(statements, CashFlowStatement::getStotOutInvAct));
        result.setNCashflowInvAct(sum(statements, CashFlowStatement::getNCashflowInvAct));
        result.setCRecpBorrow(sum(statements, CashFlowStatement::getCRecpBorrow));
        result.setProcIssueBonds(sum(statements, CashFlowStatement::getProcIssueBonds));
        result.setOthCashRecpRalFncAct(sum(statements, CashFlowStatement::getOthCashRecpRalFncAct));
        result.setStotCashInFncAct(sum(statements, CashFlowStatement::getStotCashInFncAct));
        result.setFreeCashflow(sum(statements, CashFlowStatement::getFreeCashflow));
        result.setCPrepayAmtBorr(sum(statements, CashFlowStatement::getCPrepayAmtBorr));
        result.setCPayDistDpcpIntExp(sum(statements, CashFlowStatement::getCPayDistDpcpIntExp));
        result.setInclDvdProfitPaidScMs(sum(statements, CashFlowStatement::getInclDvdProfitPaidScMs));
        result.setOthCashpayRalFncAct(sum(statements, CashFlowStatement::getOthCashpayRalFncAct));
        result.setStotCashoutFncAct(sum(statements, CashFlowStatement::getStotCashoutFncAct));
        result.setNCashFlowsFncAct(sum(statements, CashFlowStatement::getNCashFlowsFncAct));
        result.setEffFxFluCash(sum(statements, CashFlowStatement::getEffFxFluCash));
        result.setNIncrCashCashEqu(sum(statements, CashFlowStatement::getNIncrCashCashEqu));
        result.setCRecpCapContrib(sum(statements, CashFlowStatement::getCRecpCapContrib));
        result.setInclCashRecSaims(sum(statements, CashFlowStatement::getInclCashRecSaims));
        result.setUnconInvestLoss(sum(statements, CashFlowStatement::getUnconInvestLoss));
        result.setProvDeprAssets(sum(statements, CashFlowStatement::getProvDeprAssets));
        result.setDeprFaCogaDpba(sum(statements, CashFlowStatement::getDeprFaCogaDpba));
        result.setAmortIntangAssets(sum(statements, CashFlowStatement::getAmortIntangAssets));
        result.setLtAmortDeferredExp(sum(statements, CashFlowStatement::getLtAmortDeferredExp));
        result.setDecrDeferredExp(sum(statements, CashFlowStatement::getDecrDeferredExp));
        result.setIncrAccExp(sum(statements, CashFlowStatement::getIncrAccExp));
        result.setLossDispFiolta(sum(statements, CashFlowStatement::getLossDispFiolta));
        result.setLossScrFa(sum(statements, CashFlowStatement::getLossScrFa));
        result.setLossFvChg(sum(statements, CashFlowStatement::getLossFvChg));
        result.setInvestLoss(sum(statements, CashFlowStatement::getInvestLoss));
        result.setDecrDefIncTaxAssets(sum(statements, CashFlowStatement::getDecrDefIncTaxAssets));
        result.setIncrDefIncTaxLiab(sum(statements, CashFlowStatement::getIncrDefIncTaxLiab));
        result.setDecrInventories(sum(statements, CashFlowStatement::getDecrInventories));
        result.setDecrOperPayable(sum(statements, CashFlowStatement::getDecrOperPayable));
        result.setIncrOperPayable(sum(statements, CashFlowStatement::getIncrOperPayable));
        result.setOthers(sum(statements, CashFlowStatement::getOthers));
        result.setImNetCashflowOperAct(sum(statements, CashFlowStatement::getImNetCashflowOperAct));
        result.setConvDebtIntoCap(sum(statements, CashFlowStatement::getConvDebtIntoCap));
        result.setConvCopbondsDueWithin1y(sum(statements, CashFlowStatement::getConvCopbondsDueWithin1y));
        result.setFaFncLeases(sum(statements, CashFlowStatement::getFaFncLeases));
        result.setImNIncrCashEqu(sum(statements, CashFlowStatement::getImNIncrCashEqu));
        result.setNetDismCapitalAdd(sum(statements, CashFlowStatement::getNetDismCapitalAdd));
        result.setNetCashReceSec(sum(statements, CashFlowStatement::getNetCashReceSec));
        result.setCreditImpaLoss(sum(statements, CashFlowStatement::getCreditImpaLoss));
        result.setUseRightAssetDep(sum(statements, CashFlowStatement::getUseRightAssetDep));
        result.setOthLossAsset(sum(statements, CashFlowStatement::getOthLossAsset));

        // 余额项目 - 使用最新季度的期末余额和最早季度的期初余额
        result.setCCashEquBegPeriod(earliest.getCCashEquBegPeriod()); // 期初余额使用最早季度的期初
        result.setCCashEquEndPeriod(latest.getCCashEquEndPeriod());   // 期末余额使用最新季度的期末
        result.setBegBalCash(earliest.getBegBalCash());              // 现金期初余额使用最早季度
        result.setEndBalCash(latest.getEndBalCash());                // 现金期末余额使用最新季度
        result.setBegBalCashEqu(earliest.getBegBalCashEqu());        // 现金等价物期初余额使用最早季度
        result.setEndBalCashEqu(latest.getEndBalCashEqu());          // 现金等价物期末余额使用最新季度

        // 更新标识
        result.setUpdateFlag("1");

        return result;
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
            // 忽略设置失败的字段
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