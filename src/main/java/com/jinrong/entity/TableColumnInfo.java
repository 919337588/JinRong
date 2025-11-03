package com.jinrong.entity;

import lombok.Data;

@Data
public class TableColumnInfo {
    private String columnName;    // 列名
    private String columnComment; // 列注释
    private String dataType;      // 数据类型
}