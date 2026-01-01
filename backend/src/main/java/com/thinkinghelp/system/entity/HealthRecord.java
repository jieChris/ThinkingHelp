package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_records")
public class HealthRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;

    private Double height; // cm
    private Double weight; // kg
    private Double systolic; // mmHg
    private Double diastolic; // mmHg
    private Double glucose; // mmol/L
    private Integer heartRate; // bpm

    private LocalDateTime recordedAt;
}
