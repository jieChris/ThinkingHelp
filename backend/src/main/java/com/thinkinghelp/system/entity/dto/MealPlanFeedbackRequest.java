package com.thinkinghelp.system.entity.dto;

import lombok.Data;

@Data
public class MealPlanFeedbackRequest {
    private String mealType;
    private String direction;
    private String note;
}
