package com.jinrong.util;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Arrays;
import java.util.List;

public class StatisticsUtil {
    /**
     * 计算正态分布分位值
     * @param data 数据集
     * @return [均值, 标准差, 30%分位值, 70%分位值]
     */
    public static double[] calculateNormalDistribution(List<Double> data) {
        if (data == null || data.isEmpty()) return new double[4];
        
        // 计算均值
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        // 计算标准差
        double variance = data.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // 使用正态分布计算分位点
        NormalDistribution dist = new NormalDistribution(mean, stdDev);
        double p30 = dist.inverseCumulativeProbability(0.3);
        double p70 = dist.inverseCumulativeProbability(0.7);
        
        return new double[]{mean, stdDev, p30, p70};
    }


    /**
     * 计算基于中位数的正态分布分位值（非参数方法）
     * @param data 数据集
     * @return [中位数, 标准差, 30%分位值, 70%分位值]
     */
    public static double[] calculateNormalDistributionV2(List<Double> data) {
        if (data == null || data.isEmpty())
            return new double[4];

        // 将List转换为数组并排序
        double[] sortedData = data.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(sortedData);

        // 计算中位数
        double median = calculateMedian(sortedData);

        // 计算标准差（基于均值，注意统计意义的变化）
        double mean = Arrays.stream(sortedData).average().orElse(0);
        double variance = Arrays.stream(sortedData)
                .map(d -> Math.pow(d - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        // 使用正态分布计算分位点
        NormalDistribution dist = new NormalDistribution(median, stdDev); // 使用中位数作为位置参数
        double p30 = dist.inverseCumulativeProbability(0.3);
        double p70 = dist.inverseCumulativeProbability(0.7);

        return new double[]{median, stdDev, p30, p70};
    }

    /**
     * 计算中位数
     * @param sortedData 已排序的数组
     */
    private static double calculateMedian(double[] sortedData) {
        int n = sortedData.length;
        if (n % 2 == 0) {
            // 偶数个元素：取中间两个数的平均值
            return (sortedData[n/2 - 1] + sortedData[n/2]) / 2.0;
        } else {
            // 奇数个元素：取中间数
            return sortedData[n/2];
        }
    }
}