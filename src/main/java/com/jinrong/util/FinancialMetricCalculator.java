package com.jinrong.util;

import com.jinrong.entity.BalanceSheet;
import com.jinrong.entity.CashFlowStatement;
import com.jinrong.entity.FinIndicator;
import com.jinrong.entity.IncomeStatement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinancialMetricCalculator {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    // ==================== 安全计算工具 ====================
    public static BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor, int scale) {
        if (dividend == null || divisor == null ||
                divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal safeValue(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ==================== 核心计算方法 ====================

    // 1. 偿债能力指标
    public static BigDecimal calculateCurrentRatio(BigDecimal currentAssets, BigDecimal currentLiabilities) {
        return safeDivide(safeValue(currentAssets), safeValue(currentLiabilities), 4);
    }

    public static BigDecimal calculateCashRatio(BigDecimal cash, BigDecimal currentLiabilities) {
        return safeDivide(safeValue(cash), safeValue(currentLiabilities), 4);
    }

    public static BigDecimal calculateDebtRatio(BigDecimal totalLiabilities, BigDecimal totalAssets) {
        return safeDivide(safeValue(totalLiabilities), safeValue(totalAssets), 4);
    }
    public static BigDecimal calculateInterestBearingDebt(BalanceSheet bs) {
        BigDecimal shortTermBorrowings = safeValue(bs.getStBorr());
        BigDecimal longTermBorrowings = safeValue(bs.getLtBorr());
        BigDecimal bondsPayable = safeValue(bs.getBondPayable());
        BigDecimal interestPayable = safeValue(bs.getIntPayable());
        BigDecimal leaseLiability = safeValue(bs.getLeaseLiab());

        // 有息负债总额 = 短期借款+长期借款+应付债券+应付利息+租赁负债
        return shortTermBorrowings
                .add(longTermBorrowings)
                .add(bondsPayable)
                .add(interestPayable)
                .add(leaseLiability);
    }
    public static BigDecimal calculateInterestBearingDebtRatio(BalanceSheet bs) {
        BigDecimal totalDebt = calculateInterestBearingDebt(bs);
        BigDecimal totalAssets = safeValue(bs.getTotalAssets());
        return safeDivide(totalDebt, totalAssets, 4);
    }
    public static BigDecimal calculateLongTermDebtRatio(BalanceSheet bs) {
        // 长期负债 = 长期借款 + 应付债券 + 租赁负债
        BigDecimal longTermDebt = safeValue(bs.getLtBorr())
                .add(safeValue(bs.getBondPayable()))
                .add(safeValue(bs.getLeaseLiab()));

        BigDecimal equity = safeValue(bs.getTotalHldrEqyExcMinInt());
        BigDecimal denominator = longTermDebt.add(equity);

        // 避免除零错误
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return safeDivide(longTermDebt, denominator, 4);
    }
    public static BigDecimal calculateLongTermDebtRatio(BigDecimal longTermDebt, BigDecimal equity) {
        BigDecimal denominator = safeValue(longTermDebt).add(safeValue(equity));
        return safeDivide(safeValue(longTermDebt), denominator, 4);
    }

    // 2. 盈利能力指标
    public static BigDecimal calculateROA(BigDecimal netProfit, BigDecimal totalAssets) {
        return safeDivide(safeValue(netProfit), safeValue(totalAssets), 4);
    }

    public static BigDecimal calculateROE(BigDecimal netProfit, BigDecimal equity) {
        return safeDivide(safeValue(netProfit), safeValue(equity), 4);
    }

    public static BigDecimal calculateGrossMargin(BigDecimal revenue, BigDecimal operatingCost) {
        BigDecimal grossProfit = safeValue(revenue).subtract(safeValue(operatingCost));
        return safeDivide(grossProfit, safeValue(revenue), 4);
    }

    // 3. 营运能力指标
    public static BigDecimal calculateARTurnover(BigDecimal revenue, BigDecimal avgAccountsReceivable) {
        return safeDivide(safeValue(revenue), safeValue(avgAccountsReceivable), 4);
    }

    public static BigDecimal calculateInventoryTurnover(BigDecimal operatingCost, BigDecimal avgInventory) {
        return safeDivide(safeValue(operatingCost), safeValue(avgInventory), 4);
    }

    public static BigDecimal calculateAssetTurnover(BigDecimal revenue, BigDecimal avgTotalAssets) {
        return safeDivide(safeValue(revenue), safeValue(avgTotalAssets), 4);
    }

    public static BigDecimal calculateFixedAssetTurnover(BigDecimal revenue, BigDecimal avgFixedAssets) {
        return safeDivide(safeValue(revenue), safeValue(avgFixedAssets), 4);
    }

    // 4. 增长率指标
    public static BigDecimal calculateSalesGrowth(BigDecimal currentRevenue, BigDecimal priorRevenue) {
        if (priorRevenue == null || priorRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal change = safeValue(currentRevenue).subtract(safeValue(priorRevenue));
        return safeDivide(change, priorRevenue.abs(), 4);
    }

    public static BigDecimal calculateNetIncomeGrowth(BigDecimal currentNetProfit, BigDecimal priorNetProfit) {
        if (priorNetProfit == null || priorNetProfit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal change = safeValue(currentNetProfit).subtract(safeValue(priorNetProfit));
        return safeDivide(change, priorNetProfit.abs(), 4);
    }

    // 5. 现金流量指标
    public static BigDecimal calculateCashFlowRatio(BigDecimal operatingCashFlow, BigDecimal netProfit) {
        return safeDivide(safeValue(operatingCashFlow), safeValue(netProfit), 4);
    }

    public static BigDecimal calculateFreeCashFlow(BigDecimal operatingCashFlow, BigDecimal capitalExpenditure) {
        return safeValue(operatingCashFlow).subtract(safeValue(capitalExpenditure));
    }

    // 6. 其他指标
    public static BigDecimal calculateFixedAssetRatio(BigDecimal fixedAssets, BigDecimal totalAssets) {
        return safeDivide(safeValue(fixedAssets), safeValue(totalAssets), 4);
    }

    // ==================== 辅助方法 ====================
    public static BigDecimal calculateAverage(BigDecimal begin, BigDecimal end) {
        begin = safeValue(begin);
        end = safeValue(end);
        return begin.add(end).divide(TWO, 4, RoundingMode.HALF_UP);
    }
    // 营业利润率
    public static BigDecimal calculateOperatingProfitMargin(
            BigDecimal operatingProfit,
            BigDecimal revenue
    ) {
        return safeDivide(safeValue(operatingProfit), safeValue(revenue), 4);
    }

    // 投入资本回报率(ROIC)
    public static BigDecimal calculateROIC(
            BigDecimal ebit,
            BigDecimal taxRate,
            BigDecimal totalAssets,
            BigDecimal nonInterestCurrentLiabilities
    ) {
        BigDecimal afterTaxProfit = ebit.multiply(BigDecimal.ONE.subtract(safeValue(taxRate)));
        BigDecimal investedCapital = safeValue(totalAssets).subtract(safeValue(nonInterestCurrentLiabilities));
        return safeDivide(safeValue(afterTaxProfit), safeValue(investedCapital), 4);
    }

    // 复合增长率计算（通用）
    public static BigDecimal calculateCagr(
            BigDecimal currentValue,
            BigDecimal priorValue
    ) {
        if (priorValue == null || priorValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return safeDivide(safeValue(currentValue), safeValue(priorValue), 4).subtract(BigDecimal.ONE);
    }
    public static BigDecimal calculateNetProfitMargin(
            BigDecimal netProfit,
            BigDecimal revenue
    ) {
        return safeDivide(safeValue(netProfit), safeValue(revenue), 4);
    }

    public static BigDecimal calculateCurrentAssetTurnover(
            BigDecimal revenue,
            BigDecimal avgCurrentAssets
    ) {
        return safeDivide(safeValue(revenue), safeValue(avgCurrentAssets), 4);
    }

}