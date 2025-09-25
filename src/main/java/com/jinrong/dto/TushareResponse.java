package com.jinrong.dto;

import lombok.Data;
import java.util.List;

@Data
public class TushareResponse {
    private Integer code;
    private String msg;
    private StockBasicData data;
}

// 针对 stock_basic API 的 data 部分进行专门建模
