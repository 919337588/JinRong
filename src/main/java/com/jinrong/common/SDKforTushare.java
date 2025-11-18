package com.jinrong.common;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jinrong.dto.StockBasicData;
import com.jinrong.dto.TushareRequest;
import com.jinrong.dto.TushareResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
@Component@Slf4j
public class SDKforTushare {
    @Autowired
    RestTemplate restTemplate;

    // 从 application.properties 中注入配置
    @Value("${tushare.api.url}")
    private String apiUrl;

    @Value("${tushare.api.token}")
    private String apiToken;


    /**
     * 通用调用方法
     * @param request 请求对象，包含 api_name, params 等
     * @return 完整的 Tushare 响应
     */
    public   TushareResponse callApi(TushareRequest request) {
        // 1. 设置请求头，指定内容类型为 JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 设置 token（如果请求体中还没设置）
        request.setToken(apiToken);

        // 3. 将请求对象封装为 HttpEntity
        HttpEntity<TushareRequest> entity = new HttpEntity<>(request, headers);

        // 4. 发起 POST 请求，并直接映射到自定义的响应类型
        // 使用 ParameterizedTypeReference 来处理泛型响应
        ResponseEntity<TushareResponse> response = restTemplate.postForEntity(
                apiUrl,
                entity,
                TushareResponse.class
        );

        // 5. 返回响应体
        return response.getBody();
    }

    /**
     * 获取股票列表的便捷方法
     */
    @SneakyThrows
    public List<HashMap<String, Object>> getApiResponse(String apiName, Map<String, Object> params, String fields) {
        TushareRequest request = new TushareRequest();
        request.setApi_name(apiName);
        request.setParams(params);
        request.setFields(fields);
        List<String[]> vals=new LinkedList<>();
        while(true){
            request.getParams().put("offset",vals.size());
            StockBasicData data = callApi(request).getData();
            while(data==null){
                Thread.sleep(61000);
                data = callApi(request).getData();
            }
            vals.addAll(data.getItems()) ;
            if(!data.isHas_more()){
                String[] fields1 = data.getFields();
                return vals.stream().map(v -> {
                            HashMap<String, Object> stringObjectHashMap = new HashMap<>();
                            for (int i = 0; i < fields1.length; i++) {
                                stringObjectHashMap.put(StringUtils.underlineToCamel(fields1[i]), v[i]);
                            }
                            return stringObjectHashMap;
                        }
                ).toList();
            }
        }
    }

}
