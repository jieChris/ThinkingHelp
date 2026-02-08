package com.thinkinghelp.system.entity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MealPlanFeedbackSummaryDTO {
    private Integer recentFeedbackCount;
    private String feedbackText;
    private List<String> notes = new ArrayList<>();
}
