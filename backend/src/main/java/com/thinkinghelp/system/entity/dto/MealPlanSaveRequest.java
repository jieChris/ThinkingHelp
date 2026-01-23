package com.thinkinghelp.system.entity.dto;

import lombok.Data;

@Data
public class MealPlanSaveRequest {
    private String rangeType;
    private MealPlanDTO plan;
}
