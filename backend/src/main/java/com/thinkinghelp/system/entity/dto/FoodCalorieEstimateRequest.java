package com.thinkinghelp.system.entity.dto;

import lombok.Data;

@Data
public class FoodCalorieEstimateRequest {
    private String foodName;
    private Double weightGrams;
}
