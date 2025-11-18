package com.jinrong.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
@Data
@Component@Slf4j
@ConfigurationProperties(prefix = "technical.indicators")
public class TechnicalIndicatorConfig {
    
    private FallStabilizeRiseConfig fallStabilizeRise;
    private MaConsistencyBreakoutConfig maConsistencyBreakout;
    
    @Data
    public static class FallStabilizeRiseConfig {
        private FallConfig fall;
        private StabilizeConfig stabilize;
        private RiseConfig rise;
        private PatternStrengthConfig patternStrength;
    }
    
    @Data
    public static class MaConsistencyBreakoutConfig {
        private int minDataDays;
        private ConsistencyConfig consistency;
        private VolumeConfig volume;
        private PriceConfig price;
    }
    
    @Data
    public static class FallConfig {
        private double significantFallThreshold;
        private int minDataDays;
        private int additionalDays;
    }
    
    @Data
    public static class StabilizeConfig {
        private int analysisDays;
        private double noNewLowThreshold;
        private double volatilityThreshold;
        private double volumeThreshold;
    }
    
    @Data
    public static class RiseConfig {
        private int analysisDays;
        private double priceRiseThreshold;
        private double volumeSurgeThreshold;
        private int volumeAvgDays;
        private boolean requireBreakMa;
        private boolean requireTechnicalIndicator;
    }
    
    @Data
    public static class PatternStrengthConfig {
        private double fallWeight;
        private double stabilizeWeight;
        private double riseWeight;
    }
    
    @Data
    public static class ConsistencyConfig {
        private int analysisDays;
        private double variationThreshold;
    }
    
    @Data
    public static class VolumeConfig {
        private int avgDays;
        private double breakoutRatio;
        private double previousDayRatio;
    }
    
    @Data
    public static class PriceConfig {
        private boolean requireBreakAllMa;
    }
}