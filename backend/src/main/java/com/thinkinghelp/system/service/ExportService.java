package com.thinkinghelp.system.service;

import com.thinkinghelp.system.entity.dto.MealPlanDTO;
import java.io.OutputStream;

public interface ExportService {

    /**
     * 根据用户档案生成周食谱
     */
    MealPlanDTO generateWeeklyMealPlan(Long userId);

    /**
     * 根据用户档案生成指定周期食谱
     * range: MEAL/DAY/THREE_DAYS/WEEK
     */
    default MealPlanDTO generateMealPlan(Long userId, String range) {
        return generateMealPlan(userId, range, null);
    }

    /**
     * 根据用户档案生成指定周期食谱（可附加用户需求）
     * range: MEAL/DAY/THREE_DAYS/WEEK
     */
    MealPlanDTO generateMealPlan(Long userId, String range, String requirements);

    /**
     * 对已存在食谱补齐营养与采购清单信息（不触发 AI 估算）
     */
    void enrichMealPlan(MealPlanDTO mealPlan);

    /**
     * 将食谱导出为 PDF
     */
    void exportMealPlanToPdf(MealPlanDTO mealPlan, OutputStream outputStream);
}
