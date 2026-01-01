package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_metrics")
public class HealthMetric {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String metricType; // weight, systolic, diastolic, glucose, heart_rate
    private Double value;
    private String unit;
    private LocalDateTime recordedAt;
    private String note;
}
