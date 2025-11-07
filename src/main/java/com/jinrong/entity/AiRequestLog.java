package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("ai_request_log")
public class AiRequestLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tsCode;
    private String requestMsg;
    private LocalDateTime requestTime;
    private String requestFormatId;
    private String responseMsg;
    private String reasonMsg;
}