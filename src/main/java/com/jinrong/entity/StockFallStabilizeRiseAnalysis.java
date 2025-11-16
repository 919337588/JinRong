package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 回落企稳上涨分析结果表
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("stock_fall_stabilize_rise_analysis")
public class StockFallStabilizeRiseAnalysis implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @TableField("ts_code")
    private String tsCode;

    @TableField("trade_date")
    private LocalDate tradeDate;

    @TableField("close_price")
    private Double closePrice;

    @TableField("is_fall_stabilize_rise_pattern")
    private Boolean isFallStabilizeRisePattern;

    @TableField("pattern_strength")
    private Double patternStrength;

    @TableField("fall_percent")
    private Double fallPercent;

    @TableField("peak_price")
    private Double peakPrice;

    @TableField("trough_price")
    private Double troughPrice;

    @TableField("peak_date")
    private LocalDate peakDate;

    @TableField("trough_date")
    private LocalDate troughDate;

    @TableField("fall_duration")
    private Integer fallDuration;

    @TableField("is_stabilized")
    private Boolean isStabilized;

    @TableField("stabilize_duration")
    private Integer stabilizeDuration;

    @TableField("price_volatility")
    private Double priceVolatility;

    @TableField("volume_change_ratio")
    private Double volumeChangeRatio;

    @TableField("avg_stabilize_price")
    private Double avgStabilizePrice;

    @TableField("has_rise_signal")
    private Boolean hasRiseSignal;

    @TableField("rise_percent")
    private Double risePercent;

    @TableField("volume_ratio")
    private Double volumeRatio;

    @TableField("break_ma5")
    private Boolean breakMa5;

    @TableField("break_ma10")
    private Boolean breakMa10;

    @TableField("macd_bullish")
    private Boolean macdBullish;

    @TableField("kdj_bullish")
    private Boolean kdjBullish;

    private String analysis;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}