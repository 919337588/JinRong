package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

@Data
@TableName("financial_socre") // 指定数据库表名
public class FinancialScore {
    @TableId(value = "id", type = IdType.AUTO) // 主键自增
    private Long id;

    @TableField("ts_code") // 映射字段 ts_code
    private String tsCode;

    @TableField("name") // 映射字段 name
    private String name;

    @TableField("socre") // 注意：数据库字段名可能存在拼写错误（应为 score）
    private Double socre;

    @TableField("score_year") // 映射字段 score_year
    private Integer scoreYear;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object detail; // JSON明细数据

    // ================= 24个财务指标字段 =================
    private Double cr; // 流动比率
    private Double cashRatio; // 现金比率
    private Double debtRatio; // 资产负债率
    private Double interestBearingDebtRatio; // 有息负债率
    private Double longTermDebtRatio; // 长期资本负债率
    private Double arTo; // 应收账款周转率
    private Double invTo; // 存货周转率
    private Double currentAssetTo; // 流动资产周转率
    private Double fixedAssetTo; // 固定资产周转率
    private Double totalAssetTo; // 总资产周转率
    private Double grossMargin; // 营业毛利率
    private Double operatingProfitMargin; // 营业利润率
    private Double netProfitMargin; // 营业净利率
    private Double roic; // 投入资本回报率
    private Double roa; // 总资产净利率
    private Double roe; // 净资产收益率
    private Double rooa; // 经营性资产报酬率
    private Double cashFlowRatio; // 净现比
    private Double salesCagr; // 营业收入复合增长率
    private Double opProfitCagr; // 营业利润复合增长率
    private Double netProfitCagr; // 净利润复合增长率
    private Double ocfCagr; // 经营活动现金流复合增长率
    private Double fcfCagr; // 自由现金流复合增长率
    private Double fixedAssetRatio; // 固定资产比率

}