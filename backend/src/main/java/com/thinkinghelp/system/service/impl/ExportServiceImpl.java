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

        return aiService.chat(prompt, MealPlanDTO.class);
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
}
