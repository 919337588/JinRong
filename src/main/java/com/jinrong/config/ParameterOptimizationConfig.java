package com.jinrong.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "parameter-optimization")
public class ParameterOptimizationConfig {

    // 回落企稳上涨形态参数搜索空间
    private FallStabilizeRiseSearch fallStabilizeRise;

    // 均线粘滞突破形态参数搜索空间
    private MaConsistencyBreakoutSearch maConsistencyBreakout;

    @Data
    public static class FallStabilizeRiseSearch {
        private List<Double> significantFallThresholds;        // 显著回落阈值
        private List<Double> noNewLowThresholds;               // 不再创新低阈值
        private List<Double> volatilityThresholds;             // 波动率阈值
        private List<Double> volumeThresholds;                 // 成交量阈值
        private List<Double> priceRiseThresholds;              // 价格上涨阈值
        private List<Double> volumeSurgeThresholds;            // 量比阈值
        private List<Boolean> requireBreakMas;                 // 是否要求突破均线
        private List<Boolean> requireTechnicalIndicators;      // 是否要求技术指标转强
        private int minTestDays = 30;                          // 最小测试天数
        private int maxTestDays = 250;                         // 最大测试天数
    }

    @Data
    public static class MaConsistencyBreakoutSearch {
        private List<Double> variationThresholds;              // 均线差异率阈值
        private List<Double> breakoutRatios;                   // 成交量突破比率
        private List<Double> previousDayRatios;                // 前一日成交量比率
        private List<Boolean> requireBreakAllMas;              // 是否要求突破所有均线
        private int minTestDays = 20;                          // 最小测试天数
    }
}