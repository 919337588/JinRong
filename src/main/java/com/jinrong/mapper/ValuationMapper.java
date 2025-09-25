package com.jinrong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinrong.entity.Valuation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ValuationMapper extends BaseMapper<Valuation> {

    @Select("<script>" +
            "${sql}" +
            "</script>")
    List<Map<String, Object>> selectValuationWithConditions(String sql);

    @Select("<script>select close from tushare_index_daily where trade_date between ${begin} and ${end} and ts_code=#{code} order by trade_date</script>")
    List<Map<String, Object>> getIndexDaily(int  begin,int end , String code);
}