package com.thinkinghelp.system.entity.dto;

import lombok.Data;

@Data
public class MealPlanTaskStatus {
    private Long id;
    private String status;
    private String rangeType;
    private String errorMessage;
    private MealPlanDTO plan;
}
