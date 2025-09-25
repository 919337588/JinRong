package com.jinrong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.jinrong.entity.StockReportPrediction;

@Mapper
public interface StockReportPredictionMapper 
    extends BaseMapper<StockReportPrediction> {
    // 继承BaseMapper获得CRUD方法
}