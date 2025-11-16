package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("valuation") // 指定数据库表名
public class Valuation {
    @TableId(value = "id", type = IdType.ASSIGN_ID) // 主键为字符串类型，使用 ASSIGN_ID 策略（雪花算法）
    private String id;

    @TableField("ts_code") // 字段映射（下划线转驼峰）
    private String tsCode;
    @TableField("type") //计算类型  y预测  g过去
    private String type;
    @TableField("name")
    private String name;

    @TableField("date") // 日期类型推荐使用 LocalDate
    private LocalDate date;

    @TableField("ped")
    private Double ped;
    @TableField("pedv2")
    private Double pedv2;

    private Double incomed;

    private Double incomedv2;

    @TableField("pettmz")
    private Double pettmz;

    @TableField("hlpe")
    private Double hlpe;

    @TableField("hlpev2")
    private Double hlpev2;

    @TableField("income_increate_percentage") // 字段名与表列名一致
    private Double incomeIncreatePercentage;
    @TableField("income_increate_percentage_v2") // 字段名与表列名一致
    private Double incomeIncreatePercentagev2;

    @TableField("valuation_percentage")
    private Double valuationPercentage;

    private Double fScore;

    private Double rScore;

    private Double incomeFinishedRatio;

    private Double reasonMarketVal;

    private Double safeMargin;



}