package com.thinkinghelp.system.entity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GlucoseAnalysisDTO {
    private String riskLevel;
    private List<String> alerts = new ArrayList<>();
    private List<MealImpactItem> mealImpacts = new ArrayList<>();

    @Data
    public static class MealImpactItem {
        private String glucoseTime;
        private Double glucoseValue;
        private String measureType;
        private String mealTime;
        private String mealType;
        private String mealName;
        private Double mealCalories;
        private Double mealCarbs;
        private String suggestion;
    }
}
