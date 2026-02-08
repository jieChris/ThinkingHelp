package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.thinkinghelp.system.utils.LocalDateTimeFlexibleDeserializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("glucose_records")
public class GlucoseRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Double glucoseValue;
    private String measureType;
    private String eventType;
    private String medicationNote;
    private String symptoms;
    private String relatedMeal;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeFlexibleDeserializer.class)
    private LocalDateTime recordedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
