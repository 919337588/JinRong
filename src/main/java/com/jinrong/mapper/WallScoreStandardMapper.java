package com.jinrong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinrong.entity.WallScoreStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WallScoreStandardMapper extends BaseMapper<WallScoreStandard> {
    @Select("SELECT * FROM wall_score_standard WHERE industry_code = #{industryCode}")
    List<WallScoreStandard> selectByIndustry(String industryCode);
}
