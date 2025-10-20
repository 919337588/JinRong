package com.jinrong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinrong.entity.StockDailyBasic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockDailyBasicMapper extends BaseMapper<StockDailyBasic> {
    // 继承BaseMapper后自动获得CRUD方法
    // 无需编写XML，MyBatis-Plus动态生成SQL
    @Select("SELECT ts_code, MAX(trade_date) AS trade_date " +
            "FROM stock_daily_basic GROUP BY ts_code")
    List<StockDailyBasic> selectMaxTradeDateByGroup();

    @Select("SELECT MAX(trade_date) AS trade_date  FROM stock_daily_basic  ")
    StockDailyBasic selectMaxTradeDate();
}