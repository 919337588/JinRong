package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("stock_report_prediction")
public class StockReportPrediction {
    @TableId(type = IdType.AUTO)
    private Long id; // 自增主键
    
    private String tsCode;      // 股票代码
    private String name;         // 股票名称
    private LocalDate reportDate; // 研报日期
    private String reportTitle;  // 报告标题
    private String reportType;   // 报告类型
    private String classify;     // 报告分类
    private String orgName;      // 机构名称
    private String authorName;   // 作者
    private String quarter;       // 预测报告期
    
    // 财务预测指标（单位：万元）
    private Double opRt;        // 预测营业收入
    private Double opPr;        // 预测营业利润
    private Double tp;          // 预测利润总额
    private Double np;          // 预测净利润
    private Double eps;         // 预测每股收益（元）
    
    // 估值指标
    private Double pe;          // 预测市盈率
    private Double rd;          // 预测股息率
    private Double roe;         // 预测净资产收益率
    private Double evEbitda;    // 预测EV/EBITDA
    
    // 评级与目标价
    private String rating;      // 卖方评级
    private Double maxPrice;    // 预测最高目标价
    private Double minPrice;    // 预测最低目标价
}