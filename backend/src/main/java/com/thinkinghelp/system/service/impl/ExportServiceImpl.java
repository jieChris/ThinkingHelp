package com.thinkinghelp.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.thinkinghelp.system.entity.dto.MealPlanDTO;
import com.thinkinghelp.system.service.AIService;
import com.thinkinghelp.system.service.ExportService;
import com.thinkinghelp.system.service.PromptManager;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final AIService aiService;
    private final PromptManager promptManager;
    private final Configuration freemarkerConfig; // 从 Starter 自动装配

    @Override
    public MealPlanDTO generateWeeklyMealPlan(Long userId) {
        // 在实际应用中，首先获取用户健康档案
        String userProfile = "User has hypertension and needs low sodium diet."; // 模拟数据

        String prompt = "Please generate a weekly meal plan for a user with the following profile: " + userProfile +
                "\nInclude a shopping list at the end aggregating all ingredients." +
                "\nReturn strictly in JSON format matching the following structure: " +
                "{ title, advice, weeklyPlan: [{ day, breakfast: {name, foods:[], calories}, lunch... }], shoppingList: { ingredient: quantity } }";

        try {
            return aiService.chat(prompt, MealPlanDTO.class);
        } catch (Exception e) {
            log.warn("AI meal plan generation failed, using fallback plan", e);
            return buildFallbackMealPlan();
        }
    }

    @Override
    public void exportMealPlanToPdf(MealPlanDTO mealPlan, OutputStream outputStream) {
        try {
            Template template = freemarkerConfig.getTemplate("meal_plan.ftl");
            StringWriter writer = new StringWriter();
            Map<String, Object> data = new HashMap<>();
            data.put("plan", mealPlan);
            template.process(data, writer);

            String htmlContent = writer.toString();

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(outputStream);
            builder.run();

        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("PDF Generation Failed");
        }
    }

    private MealPlanDTO buildFallbackMealPlan() {
        MealPlanDTO plan = new MealPlanDTO();
        plan.setTitle("健康基础周食谱(备用)");
        plan.setAdvice("当前使用基础模板食谱，建议低盐、少油、多蔬果。");

        MealPlanDTO.Meal breakfast = new MealPlanDTO.Meal();
        breakfast.setName("燕麦粥 + 水煮蛋");
        breakfast.setCalories("350kcal");

        MealPlanDTO.Meal lunch = new MealPlanDTO.Meal();
        lunch.setName("清蒸鱼 + 糙米饭 + 青菜");
        lunch.setCalories("600kcal");

        MealPlanDTO.Meal dinner = new MealPlanDTO.Meal();
        dinner.setName("蔬菜汤 + 全麦面包");
        dinner.setCalories("400kcal");

        MealPlanDTO.DailyPlan day1 = new MealPlanDTO.DailyPlan();
        day1.setDay("周一");
        day1.setBreakfast(breakfast);
        day1.setLunch(lunch);
        day1.setDinner(dinner);

        MealPlanDTO.DailyPlan day2 = new MealPlanDTO.DailyPlan();
        day2.setDay("周二");
        day2.setBreakfast(breakfast);
        day2.setLunch(lunch);
        day2.setDinner(dinner);

        plan.setWeeklyPlan(java.util.Arrays.asList(day1, day2));

        Map<String, String> shopping = new HashMap<>();
        shopping.put("燕麦", "500g");
        shopping.put("鸡蛋", "6个");
        shopping.put("糙米", "1kg");
        shopping.put("青菜", "3把");
        shopping.put("鱼肉", "800g");
        plan.setShoppingList(shopping);

        return plan;
    }
}
