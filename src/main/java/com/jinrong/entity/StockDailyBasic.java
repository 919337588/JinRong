package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("stock_daily_basic")
public class StockDailyBasic {
    @TableId
    private String id; // 自增主键
    private String tsCode;       // TS股票代码
    private LocalDate tradeDate; // 交易日期
    private Double close;    // 当日收盘价
    private Double adjFactorClose;
    // 流动性指标
    private Double turnoverRate;    // 换手率(%)
    private Double turnoverRateF;  // 换手率(自由流通股)
    private Double volumeRatio;     // 量比
    
    // 估值指标
    private Double pe;      // 市盈率
    private Double peTtm;   // 市盈率(TTM)
    private Double pb;      // 市净率
    private Double ps;      // 市销率
    private Double psTtm;   // 市销率(TTM)
    
    // 股息指标
    private Double dvRatio; // 股息率(%)
    private Double dvTtm;  // 股息率(TTM)(%)
    
    // 股本与市值
    private Double totalShare;   // 总股本(万股)
    private Double floatShare;   // 流通股本(万股)
    private Double freeShare;    // 自由流通股本(万)
    private Double totalMv; // 总市值(万元)
    private Double circMv;  // 流通市值(万元)

    private Double adjFactor;



}