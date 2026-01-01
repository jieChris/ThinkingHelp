package com.thinkinghelp.system.service;

import com.thinkinghelp.system.entity.dto.MealPlanDTO;
import java.io.OutputStream;

public interface ExportService {

    /**
     * 根据用户档案生成周食谱
     */
    MealPlanDTO generateWeeklyMealPlan(Long userId);

    /**
     * 将食谱导出为 PDF
     */
    void exportMealPlanToPdf(MealPlanDTO mealPlan, OutputStream outputStream);
}
