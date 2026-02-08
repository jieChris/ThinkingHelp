package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meal_plan_feedback")
public class MealPlanFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String mealType;
    private String direction;
    private String note;
    private LocalDateTime createdAt;
}
