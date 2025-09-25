package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("wall_score_standard")
public class WallScoreStandard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String industryCode;
    private String metricCode;
    private String metricName;
    private BigDecimal weight;
    private BigDecimal standardValue;
    private Boolean isPositive;

}