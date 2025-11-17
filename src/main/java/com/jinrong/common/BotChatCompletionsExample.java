package com.jinrong.common;


import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.entity.AiRequestFormat;
import com.jinrong.entity.AiRequestLog;
import com.jinrong.entity.FinIndicator;
import com.jinrong.entity.FinancialScore;
import com.jinrong.mapper.AiRequestFormatMapper;
import com.jinrong.mapper.AiRequestLogMapper;
import com.jinrong.mapper.FinIndicatorMapper;
import com.jinrong.mapper.FinancialScoreMapper;
import com.volcengine.ark.runtime.model.bot.completion.chat.BotChatCompletionRequest;
import com.volcengine.ark.runtime.model.bot.completion.chat.BotChatCompletionResult;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class BotChatCompletionsExample {
    static String apiKey = "88af8140-130a-4d40-a262-535094f67a80";
    static String baseUrl = "https://ark.cn-beijing.volces.com/api/v3/bots/chat/completions";
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private FinIndicatorChineseMapper chineseMapper;
    @Autowired
    FinancialScoreMapper financialScoreMapper;
    @Autowired
    FinIndicatorMapper finIndicatorMapper;
    @Autowired
    AiRequestFormatMapper aiRequestFormatMapper;
    @Autowired
    private AiRequestLogMapper aiRequestLogMapper;

    public void requestStock(AiRequestLog aiRequestLog, String type, Map<String, Object> map) {
        String name = (String) map.get("name");
        aiRequestLog.setRequestFormatId(type);

        AiRequestFormat aiRequestFormat = aiRequestFormatMapper.selectOne(
                new QueryWrapper<AiRequestFormat>().lambda().eq(AiRequestFormat::getUnionId, type));
        if (aiRequestFormat == null) {
            throw new RuntimeException(type + " aiRequestFormat is null");
        }

        String format = format(aiRequestFormat.getFormatContent(), aiRequestLog.getTsCode() + "," + name,
                aiRequestLog.getTsCode(), map);
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        format += " 当前时间：" + time;
        aiRequestLog.setRequestMsg(format);

        // 构建请求消息
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", aiRequestFormat.getSysinfo());
        messages.add(systemMessage);

        // 用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", format);
        messages.add(userMessage);

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiRequestFormat.getBotid());
        requestBody.put("stream", false);
        requestBody.put("max_tokens",32*1024);
        // 启用思考过程
        requestBody.put("thinking", Map.of("type", "enabled"));
        requestBody.put("messages", messages);
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        StringBuilder reason = new StringBuilder();
        StringBuilder answer = new StringBuilder();

        try {
            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // 解析响应
                if (responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    for (Map<String, Object> choice : choices) {
                        if (choice.containsKey("message")) {
                            Map<String, Object> message = (Map<String, Object>) choice.get("message");

                            if (message.containsKey("reasoning_content")) {
                                reason.append(message.get("reasoning_content").toString());
                            }
                            if (message.containsKey("content")) {
                                answer.append(message.get("content").toString());
                            }
                        }
                    }
                }
            } else {
                throw new RuntimeException("API请求失败，状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("调用AI服务失败: " + e.getMessage(), e);
        }

        aiRequestLog.setRequestTime(LocalDateTime.now());
        aiRequestLog.setReasonMsg(reason.toString());
        aiRequestLog.setResponseMsg(answer.toString());
    }

    public String format(String format, String stock, String tsCode, Map<String, Object> map) {
//        List<FinancialScore> financialScores = financialScoreMapper.selectList(new QueryWrapper<FinancialScore>()
//                .lambda().eq(FinancialScore::getTsCode, tsCode)
//                .orderByDesc(FinancialScore::getEndDate).last("limit 1"));
        List<FinIndicator> finIndicators = finIndicatorMapper.selectList(new QueryWrapper<FinIndicator>().lambda()
                .eq(FinIndicator::getTsCode, tsCode).orderByDesc(FinIndicator::getEndDate).last("limit 1"));

        StringBuilder detail = new StringBuilder();
        if (!finIndicators.isEmpty()) {
            detail.append("\r\n");
            FinIndicator finIndicator = finIndicators.get(0);
            Map<String, Object> chineseKeyValueMap = chineseMapper.getChineseKeyValueMap(finIndicator);
            detail.append("根据财报日：").append(finIndicator.getEndDate()).append("最新财报数据:").append(JSON.toJSONString(chineseKeyValueMap));
        }

//        if (!financialScores.isEmpty()) {
//            detail.append("\r\n");
//            FinancialScore financialScore = financialScores.get(0);
//            detail.append("根据财报日：").append(financialScore.getEndDate()).append("计算过去滚动4个季度财务合并计算数据:").append(financialScore.getDetail());
//        }
//        detail.append("\r\n").append("过去3个月内的研报预测年利润增长率百分值平均值=").append(map.get("预计年利润增长"));
//        detail.append("\r\n财报预测业绩完成率=今年已出财报总业绩/(机构预测业绩*(当前财报季度/4))=").append(map.get("今年实际业绩与预计业绩比值")).append(" \r\n根据估值方法  动态市盈率/财报预测业绩完成率/(过去3个月内的研报预测年利润增长率百分值平均值 ").append(map.get("预计年利润增长")).append("*0.7+过去10年平均市盈率*0.3) = ").append(map.get("估值分位"));
        detail.append("\r\n");
        return String.format(format, stock, detail);
    }
}