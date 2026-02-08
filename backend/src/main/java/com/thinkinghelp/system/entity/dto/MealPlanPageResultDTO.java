package com.thinkinghelp.system.entity.dto;

import com.thinkinghelp.system.entity.MealPlan;
import lombok.Data;

import java.util.List;

@Data
public class MealPlanPageResultDTO {
    private List<MealPlan> records;
    private Long total;
    private Integer page;
    private Integer pageSize;
}

