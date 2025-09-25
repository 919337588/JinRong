package com.jinrong.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TushareRequest {
    private String api_name;
    private String token;
    private Map<String, Object> params;
    private String fields;
}