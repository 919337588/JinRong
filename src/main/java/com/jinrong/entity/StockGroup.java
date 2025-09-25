package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("stock_group")
public class StockGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String gupiaoName;
    private String gupiaoCode;
    private Long groupId;
}