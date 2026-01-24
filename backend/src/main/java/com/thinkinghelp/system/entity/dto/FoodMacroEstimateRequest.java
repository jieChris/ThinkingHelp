package com.thinkinghelp.system.entity.dto;

import lombok.Data;

@Data
public class FoodMacroEstimateRequest {
    private String foodName;
    private Double weightGrams;
}
