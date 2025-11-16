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
 * 均线粘滞突破分析结果表
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("stock_ma_breakout_analysis")
public class StockMaBreakoutAnalysis implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @TableField("ts_code")
    private String tsCode;

    @TableField("trade_date")
    private LocalDate tradeDate;

    @TableField("close_price")
    private Double closePrice;

    private Double volume;

    @TableField("is_ma_consistency_breakout")
    private Boolean isMaConsistencyBreakout;

    @TableField("is_consistent")
    private Boolean isConsistent;

    @TableField("max_variation")
    private Double maxVariation;

    @TableField("ma5")
    private Double ma5;

    @TableField("ma10")
    private Double ma10;

    @TableField("ma20")
    private Double ma20;

    @TableField("ma30")
    private Double ma30;

    @TableField("is_volume_breakout")
    private Boolean isVolumeBreakout;

    @TableField("latest_volume")
    private Double latestVolume;

    @TableField("avg_volume20")
    private Double avgVolume20;

    @TableField("volume_ratio")
    private Double volumeRatio;

    @TableField("previous_volume_ratio")
    private Double previousVolumeRatio;

    @TableField("is_price_breakout")
    private Boolean isPriceBreakout;

    @TableField("breakout_percent")
    private Double breakoutPercent;

    private String analysis;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}