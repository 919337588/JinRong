package com.jinrong.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TableMetaService {

    private final JdbcTemplate jdbcTemplate;

    public TableMetaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取指定表的所有字段名，并按字母顺序排序后拼接成逗号分隔的字符串
     *
     * @param tableName 表名（注意 SQL 注入风险，确保表名可信或经过校验）
     * @return 字段名字符串，如 "name,age,class,score"
     * @throws IllegalArgumentException 如果表不存在或查询失败
     */
    public String getTableColumnsAsString(String tableName) {
        // 1. 验证表名参数（简单示例，实际应根据需要增强校验，如白名单校验）
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("表名不能为空");
        }

        // 2. 构建查询元数据的 SQL（这里以 MySQL 为例）
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME!='id' and  TABLE_NAME = ? AND TABLE_SCHEMA = DATABASE() ORDER BY ORDINAL_POSITION";
        // 如果你的应用支持多种数据库，你需要根据当前数据源类型调整 SQL。
        // 例如，对于 PostgreSQL，你可能需要查询 `information_schema.columns` 并调整条件。

        // 3. 执行查询
        List<String> columns;
        try {
            columns = jdbcTemplate.queryForList(sql, String.class, tableName.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("获取表字段信息失败，表名可能不存在或数据库错误: " + e.getMessage(), e);
        }

        // 4. 检查是否查到数据
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("未找到表 '" + tableName + "' 的字段信息，请检查表名是否正确");
        }

        // 5. 将字段列表拼接成字符串
        return columns.stream()
                .collect(Collectors.joining(","));
    }
}