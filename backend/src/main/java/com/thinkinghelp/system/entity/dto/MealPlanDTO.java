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
    private List<DailyShopping> dailyShopping;
    private List<String> pantryItems;
    private Double targetCalories;
    private Double targetCarbs;
    private Double targetProtein;
    private Double targetFat;
    private List<String> adjustmentNotes;

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
        private Double caloriesKcal;
        private Double carbsGrams;
        private Double proteinGrams;
        private Double fatGrams;
        private List<Ingredient> ingredients;
    }

    @Data
    public static class DailyShopping {
        private String day;
        private List<String> items;
    }

    @Data
    public static class Ingredient {
        private String name;
        private Double grams;
        private Double caloriesKcal;
        private Double carbsGrams;
        private Double proteinGrams;
        private Double fatGrams;
    }
}
