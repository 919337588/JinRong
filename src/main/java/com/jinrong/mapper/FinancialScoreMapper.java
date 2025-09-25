package com.jinrong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinrong.entity.FinancialScore;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 声明为 MyBatis 的 Mapper 接口
public interface FinancialScoreMapper extends BaseMapper<FinancialScore> {
    // 无需手动编写 CRUD 方法，BaseMapper 已提供基础方法（如 selectById, insert, update 等）
}