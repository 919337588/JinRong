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
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class BotChatCompletionsExamplev2 {
    static String apiKey = "88af8140-130a-4d40-a262-535094f67a80";
    static String baseUrl = "https://ark.cn-beijing.volces.com/api/v3/bots/chat/completions";

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

    // 创建 OkHttpClient 实例
    private final OkHttpClient okHttpClient;

    public BotChatCompletionsExamplev2() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(20);

        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS) // 流式响应需要更长的超时时间
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
                .dispatcher(dispatcher)
                .build();
    }

    /**
     * 流式请求AI服务
     * @param aiRequestLog 请求日志
     * @param type 请求类型
     * @param map 参数映射
     */
    public void requestStock(AiRequestLog aiRequestLog, String type, Map<String, Object> map
                                  ) {
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
        requestBody.put("stream", true); // 启用流式
        requestBody.put("max_tokens", 32 * 1024);
        // 启用思考过程
        requestBody.put("thinking", Map.of("type", "enabled"));
        requestBody.put("messages", messages);

        // 构建请求
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                JSON.toJSONString(requestBody)
        );

        Request request = new Request.Builder()
                .url(baseUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        StringBuilder reason = new StringBuilder();
        StringBuilder answer = new StringBuilder();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("API请求失败，状态码: " + response.code() + ", 消息: " + response.message());
            }

            // 处理流式响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();

                    if ("[DONE]".equals(data)) {
                        // 流结束
                        break;
                    }

                    try {
                        // 解析JSON数据
                        Map<String, Object> chunkData = JSON.parseObject(data, Map.class);
                        StreamResponseChunk chunk = parseChunkData(chunkData);

                        if (chunk != null) {
                            // 拼接完整内容
                            if (chunk.getReasoningContent() != null) {
                                reason.append(chunk.getReasoningContent());
                                System.out.print(chunk.getReasoningContent());
                            }
                            if (chunk.getContent() != null) {
                                answer.append(chunk.getContent());
                                System.out.print(chunk.getContent());
                            }

                        }
                    } catch (Exception e) {
                        // JSON解析错误，跳过该行
                        System.err.println("解析SSE数据失败: " + data + ", 错误: " + e.getMessage());
                    }
                }
            }

            // 保存完整的响应到数据库
            aiRequestLog.setRequestTime(LocalDateTime.now());
            aiRequestLog.setReasonMsg(reason.toString());
            aiRequestLog.setResponseMsg(answer.toString());

        } catch (IOException e) {
            throw new RuntimeException("调用AI服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析流式响应块数据
     */
    private StreamResponseChunk parseChunkData(Map<String, Object> chunkData) {
        if (!chunkData.containsKey("choices")) {
            return null;
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) chunkData.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        Map<String, Object> choice = choices.get(0);
        if (!choice.containsKey("delta")) {
            return null;
        }

        Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
        String reasoningContent = null;
        String content = null;
        String role = null;
        boolean isFinish = false;

        if (delta.containsKey("reasoning_content")) {
            reasoningContent = delta.get("reasoning_content").toString();
        }
        if (delta.containsKey("content")) {
            content = delta.get("content").toString();
        }
        if (delta.containsKey("role")) {
            role = delta.get("role").toString();
        }
        if (delta.containsKey("finish_reason")) {
            isFinish = true;
        }

        // 如果没有任何内容，则返回null
        if (reasoningContent == null && content == null && role == null) {
            return null;
        }

        return new StreamResponseChunk(
                false,
                reasoningContent,
                content,
                role,
                isFinish
        );
    }

    public String format(String format, String stock, String tsCode, Map<String, Object> map) {
        List<FinIndicator> finIndicators = finIndicatorMapper.selectList(new QueryWrapper<FinIndicator>().lambda()
                .eq(FinIndicator::getTsCode, tsCode).orderByDesc(FinIndicator::getEndDate).last("limit 1"));

        StringBuilder detail = new StringBuilder();
        if (!finIndicators.isEmpty()) {
            detail.append("\r\n");
            FinIndicator finIndicator = finIndicators.get(0);
            Map<String, Object> chineseKeyValueMap = chineseMapper.getChineseKeyValueMap(finIndicator);
            detail.append("根据财报日：").append(finIndicator.getEndDate()).append("最新财报数据:").append(JSON.toJSONString(chineseKeyValueMap));
        }

        detail.append("\r\n");
        return String.format(format, stock, detail);
    }

    /**
     * 流式响应块数据封装类
     */
    public static class StreamResponseChunk {
        private final boolean isDone;
        private final String reasoningContent;
        private final String content;
        private final String role;
        private final boolean isFinish;

        public StreamResponseChunk(boolean isDone, String reasoningContent, String content, String role, boolean isFinish) {
            this.isDone = isDone;
            this.reasoningContent = reasoningContent;
            this.content = content;
            this.role = role;
            this.isFinish = isFinish;
        }

        // Getter方法
        public boolean isDone() { return isDone; }
        public String getReasoningContent() { return reasoningContent; }
        public String getContent() { return content; }
        public String getRole() { return role; }
        public boolean isFinish() { return isFinish; }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }
    }
}