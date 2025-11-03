package com.jinrong.common;

import com.jinrong.entity.FinIndicator;
import com.jinrong.entity.TableColumnInfo;
import com.jinrong.mapper.TableColumnMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FinIndicatorChineseMapper implements CommandLineRunner {

    @Autowired
    private TableColumnMapper tableColumnMapper;

    // 存储列名到中文注释的映射
    private static final Map<String, String> COLUMN_CHINESE_MAP = new ConcurrentHashMap<>();
    
    // 存储字段名到列名的映射（处理驼峰转换）
    private static final Map<String, String> FIELD_TO_COLUMN_MAP = new ConcurrentHashMap<>();

    private static final String TABLE_NAME = "fin_indicator";

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化财务指标表中文字段映射...");
        initColumnChineseMap();
        log.info("财务指标表中文字段映射初始化完成，共加载 {} 个字段", COLUMN_CHINESE_MAP.size());
    }

    /**
     * 初始化列名与中文注释的映射关系
     */
    private void initColumnChineseMap() {
        try {
            List<TableColumnInfo> columns = tableColumnMapper.getTableColumns(TABLE_NAME);
            
            for (TableColumnInfo column : columns) {
                String columnName = column.getColumnName();
                String comment = column.getColumnComment();
                
                // 存储列名到中文的映射
                COLUMN_CHINESE_MAP.put(columnName, comment);
                
                // 构建字段名到列名的映射（驼峰转换）
                String fieldName = convertToCamelCase(columnName);
                FIELD_TO_COLUMN_MAP.put(fieldName, columnName);
                
                log.debug("字段映射: {} -> {} -> {}", fieldName, columnName, comment);
            }
        } catch (Exception e) {
            log.error("初始化表中文字段映射失败", e);
            // 可以在这里添加降级策略，比如使用默认映射
            initFallbackMapping();
        }
    }

    /**
     * 将数据库列名转换为Java字段名（驼峰命名）
     */
    private String convertToCamelCase(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }
        
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        
        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        
        return result.toString();
    }

    /**
     * 降级映射策略 - 如果数据库查询失败，使用硬编码映射
     */
    private void initFallbackMapping() {
        // 这里可以添加一些关键字段的硬编码映射
        COLUMN_CHINESE_MAP.put("ts_code", "TS代码");
        COLUMN_CHINESE_MAP.put("ann_date", "公告日期");
        COLUMN_CHINESE_MAP.put("end_date", "报告期");
        COLUMN_CHINESE_MAP.put("eps", "基本每股收益");
        COLUMN_CHINESE_MAP.put("roe", "净资产收益率");
        // 可以继续添加其他重要字段...
        
        // 同时构建字段名到列名的映射
        for (String columnName : COLUMN_CHINESE_MAP.keySet()) {
            String fieldName = convertToCamelCase(columnName);
            FIELD_TO_COLUMN_MAP.put(fieldName, columnName);
        }
    }

    /**
     * 获取财务指标的中文key-value映射
     */
    public Map<String, Object> getChineseKeyValueMap(FinIndicator indicator) {
        return getChineseKeyValueMap(indicator, false);
    }

    /**
     * 获取财务指标的中文key-value映射
     * @param indicator 财务指标对象
     * @param ignoreNull 是否忽略null值
     */
    public Map<String, Object> getChineseKeyValueMap(FinIndicator indicator, boolean ignoreNull) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        if (indicator == null) {
            return result;
        }

        try {
            Field[] fields = FinIndicator.class.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                
                // 跳过serialVersionUID等特殊字段
                if ("serialVersionUID".equals(fieldName)) {
                    continue;
                }

                Object value = field.get(indicator);
                
                // 如果忽略null值且当前值为null，则跳过
                if (ignoreNull && value == null) {
                    continue;
                }

                // 获取中文key
                String chineseKey = getChineseKey(fieldName);
                if (chineseKey != null) {
                    result.put(chineseKey, formatValue(value));
                }
            }
        } catch (IllegalAccessException e) {
            log.error("反射获取字段值失败", e);
        }
        
        return result;
    }

    /**
     * 根据字段名获取中文描述
     */
    public String getChineseKey(String fieldName) {
        String columnName = FIELD_TO_COLUMN_MAP.get(fieldName);
        if (columnName != null) {
            return COLUMN_CHINESE_MAP.get(columnName);
        }
        return null;
    }

    /**
     * 格式化值
     */
    private Object formatValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // 对BigDecimal进行格式化，避免科学计数法
        if (value instanceof BigDecimal) {
            BigDecimal decimalValue = (BigDecimal) value;
            // 去除末尾多余的0
            return decimalValue.stripTrailingZeros().toPlainString();
        }
        
        // 对LocalDate进行格式化
        if (value instanceof LocalDate) {
            return value.toString();
        }
        
        return value;
    }

    /**
     * 获取所有可用的中文字段名列表
     */
    public List<String> getAvailableChineseFields() {
        return new ArrayList<>(COLUMN_CHINESE_MAP.values());
    }

    /**
     * 获取字段映射统计信息
     */
    public Map<String, Object> getMappingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFields", COLUMN_CHINESE_MAP.size());
        stats.put("tableName", TABLE_NAME);
        stats.put("initialized", !COLUMN_CHINESE_MAP.isEmpty());
        return stats;
    }
}