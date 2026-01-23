package com.thinkinghelp.system.entity.dto;

import lombok.Data;

@Data
public class MealPlanImportResponse {
    private Long planId;
    private String rangeType;
    private MealPlanDTO plan;
}
