package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("stock_ttm_analysis")
public class StockTtmAnalysis {
    @TableId
    private String id;            // 主键(UUID)
    private String tsCode;        // 股票代码
    private String type;
    private String analysisPeriod;// 分析周期(1Y/5Y/10Y)
    private Integer dataPoints;   // 有效数据点数
    private Double meanValue;     // 均值
    private Double stdDev;        // 标准差
    private Double p30Value;      // 30%分位值
    private Double p70Value;      // 70%分位值
    private LocalDate calcDate;   // 计算日期
}