package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_profiles")
public class HealthProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;

    private String name;
    private String gender;
    private Integer age;
    private Double height;
    private Double weight;
    private String bmi;

    private String reportDate;
    private String activityLevel;
    private String goal;
    private Integer exerciseFrequency;
    private Integer exerciseDuration;
    private Double bpSystolic;
    private Double bpDiastolic;
    private Double fastingGlucose;
    private Double hba1c;
    private Double totalCholesterol;
    private Double triglycerides;
    private Double hdl;
    private Double ldl;
    private Double uricAcid;
    private Double alt;
    private Double ast;
    private Double creatinine;
    private Double bun;

    private String diseases;
    private String allergies;
    private String otherRestrictions;

    private LocalDateTime updatedAt;
}
