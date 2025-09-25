package com.jinrong.common;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.entity.AiRequestFormat;
import com.jinrong.entity.AiRequestLog;
import com.jinrong.mapper.AiRequestFormatMapper;
import com.jinrong.mapper.AiRequestLogMapper;
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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class BotChatCompletionsExample {
    static String apiKey ="88af8140-130a-4d40-a262-535094f67a80";
    static ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
    static Dispatcher dispatcher = new Dispatcher();
    static ArkService service = ArkService.builder().dispatcher(dispatcher).connectionPool(connectionPool).baseUrl("https://ark.cn-beijing.volces.com/api/v3/").apiKey(apiKey).build();


    @Autowired
    AiRequestFormatMapper aiRequestFormatMapper;
    @Autowired
    private AiRequestLogMapper aiRequestLogMapper;
    public   void   requestStock(AiRequestLog aiRequestLog,String type, String stock) {
        aiRequestLog.setRequestFormatId(type);
        AiRequestFormat aiRequestFormat = aiRequestFormatMapper.selectOne(new QueryWrapper<AiRequestFormat>().lambda().eq(AiRequestFormat::getUnionId, type));
        if(aiRequestFormat==null){
            throw new RuntimeException(type+" aiRequestFormat is null");
        }
        String format = String.format(aiRequestFormat.getFormatContent(), stock);
        aiRequestLog.setRequestMsg(format);
        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(aiRequestFormat.getSysinfo()).build();
        final ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(format).build();
        messages.add(systemMessage);
        messages.add(userMessage);
        BotChatCompletionRequest chatCompletionRequest= BotChatCompletionRequest.builder()
                .botId(aiRequestFormat.getBotid())
                .messages(messages)
                .build();

        StringBuilder reason=new StringBuilder();
        StringBuilder answer=new StringBuilder();
        BotChatCompletionResult chatCompletionResult =  service.createBotChatCompletion(chatCompletionRequest);
        chatCompletionResult.getChoices().forEach(v->{
            reason.append(v.getMessage().getReasoningContent());
            answer.append(v.getMessage().getContent());
        });
        service.shutdownExecutor();
        aiRequestLog.setRequestTime(new Date());
        aiRequestLog.setReasonMsg(reason.toString());
        aiRequestLog.setResponseMsg(answer.toString());
        aiRequestLogMapper.insert(aiRequestLog);
    }


}