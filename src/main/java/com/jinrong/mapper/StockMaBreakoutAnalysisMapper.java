package com.jinrong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinrong.entity.StockMaBreakoutAnalysis;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 均线粘滞突破分析结果表 Mapper 接口
 * </p>
 */
@Mapper
public interface StockMaBreakoutAnalysisMapper extends BaseMapper<StockMaBreakoutAnalysis> {

}