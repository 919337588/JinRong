package com.jinrong.dto;

import lombok.Data;

import java.util.List;

@Data
public class StockBasicData {
    private String[] fields; // 字段列表，如 ["ts_code", "symbol", ...]
    private List<String[]> items;
    private boolean has_more;
}