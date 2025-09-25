package com.jinrong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinrong.entity.StockBasic;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockBasicMapper extends BaseMapper<StockBasic> {
    // 继承 BaseMapper 后自动获得 CRUD 方法
    // 无需手动编写 XML，MyBatis-Plus 动态生成 SQL
}