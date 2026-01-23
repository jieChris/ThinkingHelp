package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meal_plans")
public class MealPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String rangeType;
    private String title;
    private String advice;
    private String planJson;
    private LocalDateTime createdAt;
}
