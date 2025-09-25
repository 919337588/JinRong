package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_request_format")
public class AiRequestFormat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String formatContent;
    private String unionId;
    private String sysinfo;
    private String botid;
}