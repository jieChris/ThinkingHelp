package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meal_plan_tasks")
public class MealPlanTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String rangeType;
    private String requirements;
    private String status;
    private String planJson;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
