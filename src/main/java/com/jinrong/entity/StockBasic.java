package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("stock_basic") // 映射数据库表名
public class StockBasic {
    @TableId(type = IdType.AUTO) // 标识自增主键
    private Long id; 
    private String tsCode;      // TS代码（驼峰自动映射）
    private String symbol;      // 股票代码
    private String name;        // 股票名称
    private String area;        // 地域
    private String industry;    // 所属行业
    private String fullname;    // 股票全称
    private String enname;      // 英文全称
    private String cnspell;     // 拼音缩写
    private String market;      // 市场类型
    private String exchange;    // 交易所代码
    private String currType;    // 交易货币
    private String listStatus;  // 上市状态
    private String listDate;    // 上市日期
    private String delistDate;  // 退市日期
    private String isHs;        // 是否沪深港通标的
    private String actName;     // 实控人名称
    private String actEntType;  // 实控人企业性质


}