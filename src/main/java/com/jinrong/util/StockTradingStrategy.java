package com.jinrong.util;

import com.jinrong.entity.StockDailyBasic;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class StockTradingStrategy {
    public static Object[] executeStrategy(List<StockDailyBasic> stockDataList,
                                           String name,
                                           LocalDate now,
                                           int hands) {
        // 参数校验
        if (hands <= 0 ) {
            return new Object[]{0d, 0d, name};
        }

        // 初始化变量
        int totalShares = hands * 100;
        double buyPrice = 0.0;
        int currentShares = 0;
        double buyCostFee = 0.0;
        double totalSellFee = 0.0;
        LocalDate buyDate = null;
        LocalDate sellDate = null;

        // 1. 寻找符合条件的新高形态和买入点
        int purchaseIndex = -1;
        for (int i = 2; i < stockDataList.size(); i++) {
            StockDailyBasic currentData = stockDataList.get(i);

            // 跳过非当前日期的数据（如果指定了now）
            if (now != null && !currentData.getTradeDate().equals(now)) {
                continue;
            }

            // 检查是否形成有效的新高形态[7,8](@ref)
            if (isValidNewHighPattern(stockDataList, i)) {
                // 计算回撤买入位
                double retracementLevel = calculateRetracementLevel(stockDataList, i);
                double currentPrice = currentData.getAdjFactorClose();

                // 如果当前价格达到回撤位，执行买入
                if (currentPrice <= retracementLevel) {
                    buyPrice = currentPrice;
                    purchaseIndex = i;
                    buyDate = currentData.getTradeDate();
                    currentShares = totalShares;

                    // 计算买入费用
                    double buyAmount = totalShares * buyPrice;
                    buyCostFee = buyAmount * 0.0003;
                    break;
                }
            }
        }

        // 如果没有找到买入点，返回空结果
        if (purchaseIndex == -1) {
            return new Object[]{0d, 0d, name};
        }

        // 2. 监控卖出条件（半仓和清仓）
        boolean halfSold = false;
        double peakAfterBuy = buyPrice;
        double lastLow = buyPrice;

        for (int i = purchaseIndex + 1; i < stockDataList.size(); i++) {
            StockDailyBasic currentData = stockDataList.get(i);
            double currentPrice = currentData.getAdjFactorClose();

            // 更新买入后的峰值
            if (currentPrice > peakAfterBuy) {
                peakAfterBuy = currentPrice;
            }

            // 更新最近低点
            if (currentPrice < lastLow) {
                lastLow = currentPrice;
            }

            // 如果已清仓，退出循环
            if (currentShares == 0) {
                sellDate = currentData.getTradeDate();
                break;
            }

            // 半仓条件：跌破趋势线1%[6](@ref)
            if (!halfSold && isBelowTrendLine(stockDataList, purchaseIndex, i, 0.01)) {
                int sellShares = currentShares / 2;
                double sellAmount = sellShares * currentPrice;
                totalSellFee += sellAmount * 0.0003;
                currentShares -= sellShares;
                halfSold = true;
            }

            // 清仓条件：创新低低于上次低点[6](@ref)
            if (currentPrice < lastLow) {
                double sellAmount = currentShares * currentPrice;
                totalSellFee += sellAmount * 0.0003;
                currentShares = 0;
                sellDate = currentData.getTradeDate();
                break;
            }



        }

        // 如果遍历结束还有持股，则在最后一天清仓
        if (currentShares > 0) {
            StockDailyBasic lastData = stockDataList.get(stockDataList.size() - 1);
            double sellAmount = currentShares * lastData.getAdjFactorClose();
            totalSellFee += sellAmount * 0.0003;
            sellDate = lastData.getTradeDate();
        }

        // 返回结果
        return new Object[]{buyCostFee, totalSellFee, name};
    }

    // 辅助方法：检查是否形成有效的新高形态[7,8](@ref)
    private static boolean isValidNewHighPattern(List<StockDailyBasic> stockDataList, int currentIndex) {
        if (currentIndex < 2) return false;

        StockDailyBasic current = stockDataList.get(currentIndex);
        StockDailyBasic prev = stockDataList.get(currentIndex - 1);
        StockDailyBasic prevPrev = stockDataList.get(currentIndex - 2);

        // 检查前高是否超过前前高1%
        boolean previousHighValid = prev.getAdjFactorClose() > prevPrev.getAdjFactorClose() * 1.01;

        // 检查当前价格是否创新高超过1%
        boolean currentNewHighValid = current.getAdjFactorClose() > prev.getAdjFactorClose() * 1.01;

        // 检查是否经历了下跌或滞涨（形成W底或双踩形态）[7](@ref)
        boolean hasDeclineOrStagnation = hasDeclineOrStagnation(stockDataList, currentIndex);

        return (previousHighValid || currentNewHighValid) && hasDeclineOrStagnation;
    }

    // 辅助方法：检查是否经历了下跌或连续滞涨[7](@ref)
    private static boolean hasDeclineOrStagnation(List<StockDailyBasic> stockDataList, int currentIndex) {
        int stagnationDays = 0;
        double prevHigh = 0;

        // 向前查找至少5个交易日
        int lookback = Math.min(5, currentIndex);

        for (int i = 1; i <= lookback; i++) {
            StockDailyBasic current = stockDataList.get(currentIndex - i);
            StockDailyBasic prev = stockDataList.get(currentIndex - i - 1);

            // 记录前期高点
            if (i == 1) {
                prevHigh = prev.getAdjFactorClose();
            }

            // 检查是否有下跌
            if (current.getAdjFactorClose() < prevHigh * 0.98) {
                return true;
            }

            // 检查是否滞涨（价格变化很小）
            if (Math.abs(current.getAdjFactorClose() - prev.getAdjFactorClose()) < prev.getAdjFactorClose() * 0.005) {
                stagnationDays++;
            }
        }

        // 如果连续2天以上滞涨，视为有效形态
        return stagnationDays >= 2;
    }

    // 辅助方法：计算回撤买入位[7](@ref)
    private static double calculateRetracementLevel(List<StockDailyBasic> stockDataList, int currentIndex) {
        // 获取本次新高和上次新高
        double currentHigh = stockDataList.get(currentIndex).getAdjFactorClose();

        // 向前查找上次新高
        double previousHigh = findPreviousHigh(stockDataList, currentIndex);

        // 计算三分之一回撤位
        return previousHigh + (currentHigh - previousHigh) / 3;
    }

    // 辅助方法：寻找上次新高[7](@ref)
    private static double findPreviousHigh(List<StockDailyBasic> stockDataList, int currentIndex) {
        double currentHigh = stockDataList.get(currentIndex).getAdjFactorClose();
        double previousHigh = currentHigh;

        // 向前查找最近的一个明显高点（比当前高点低至少5%）
        for (int i = currentIndex - 1; i >= 0; i--) {
            double price = stockDataList.get(i).getAdjFactorClose();
            if (price < currentHigh * 0.95) {
                previousHigh = price;
                break;
            }
        }

        return previousHigh;
    }

    // 辅助方法：检查是否跌破趋势线1%[6](@ref)
    private static boolean isBelowTrendLine(List<StockDailyBasic> stockDataList, int startIndex, int currentIndex, double threshold) {
        if (currentIndex - startIndex < 2) return false;

        // 获取最近两个低点绘制趋势线
        double[] recentLows = findRecentLows(stockDataList, startIndex, currentIndex);

        if (recentLows == null) return false;

        // 计算趋势线斜率
        double slope = (recentLows[1] - recentLows[0]) / (currentIndex - startIndex);

        // 计算当前点的趋势线预期值
        double expectedValue = recentLows[0] + slope * (currentIndex - startIndex);

        // 检查当前价格是否跌破趋势线阈值
        double currentPrice = stockDataList.get(currentIndex).getAdjFactorClose();
        return currentPrice < expectedValue * (1 - threshold);
    }

    // 辅助方法：寻找最近两个低点[6](@ref)
    private static double[] findRecentLows(List<StockDailyBasic> stockDataList, int startIndex, int currentIndex) {
        double[] lows = new double[2];
        int found = 0;

        // 从当前点向前查找两个显著低点
        for (int i = currentIndex; i > startIndex && found < 2; i--) {
            boolean isLow = true;

            // 检查是否是局部低点（比前后价格都低）
            if (i > startIndex + 1 && i < currentIndex - 1) {
                double currentPrice = stockDataList.get(i).getAdjFactorClose();
                double prevPrice = stockDataList.get(i - 1).getAdjFactorClose();
                double nextPrice = stockDataList.get(i + 1).getAdjFactorClose();

                if (currentPrice > prevPrice || currentPrice > nextPrice) {
                    isLow = false;
                }
            }

            if (isLow) {
                lows[found] = stockDataList.get(i).getAdjFactorClose();
                found++;
            }
        }

        return found == 2 ? lows : null;
    }
}
