package com.thinkinghelp.system.entity.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MealPlanDTO {
    private String title;
    private String advice;
    private List<DailyPlan> weeklyPlan;
    private Map<String, String> shoppingList; // 食材 -> 数量

    @Data
    public static class DailyPlan {
        private String day; // 第一天, 周一, 等等
        private Meal breakfast;
        private Meal lunch;
        private Meal dinner;
        private Meal snack;
    }

    @Data
    public static class Meal {
        private String name;
        private List<String> foods;
        private String calories;
    }
}
