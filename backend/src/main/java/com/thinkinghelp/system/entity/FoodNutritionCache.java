package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("food_nutrition_cache")
public class FoodNutritionCache {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String normalizedName;
    private String displayName;

    @TableField("calories_per_100g")
    private Double caloriesPer100g;
    @TableField("carbs_per_100g")
    private Double carbsPer100g;
    @TableField("sugar_per_100g")
    private Double sugarPer100g;

    private String dataSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
