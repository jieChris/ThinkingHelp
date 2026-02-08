package com.thinkinghelp.system.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class HealthProfileDTO {
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
    private List<String> diseases;
    private List<String> allergies;
    private String otherRestrictions;
}
