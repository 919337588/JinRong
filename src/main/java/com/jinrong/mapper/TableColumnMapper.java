package com.jinrong.mapper;

import com.jinrong.entity.TableColumnInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TableColumnMapper {
    
    /**
     * 查询表的列信息
     */
    @Select("SELECT column_name as columnName, column_comment as columnComment, data_type as dataType " +
            "FROM information_schema.COLUMNS " +
            "WHERE table_schema = DATABASE() AND table_name = #{tableName} " +
            "ORDER BY ordinal_position")
    List<TableColumnInfo> getTableColumns(String tableName);
}