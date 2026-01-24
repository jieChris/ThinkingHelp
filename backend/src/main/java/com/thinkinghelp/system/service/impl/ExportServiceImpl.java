package com.thinkinghelp.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.thinkinghelp.system.entity.HealthProfile;
import com.thinkinghelp.system.entity.KnowledgeBase;
import com.thinkinghelp.system.entity.dto.MealPlanDTO;
import com.thinkinghelp.system.mapper.HealthProfileMapper;
import com.thinkinghelp.system.mapper.KnowledgeBaseMapper;
import com.thinkinghelp.system.service.AIService;
import com.thinkinghelp.system.service.AiConfigKeys;
import com.thinkinghelp.system.service.ExportService;
import com.thinkinghelp.system.service.PromptManager;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final AIService aiService;
    private final PromptManager promptManager;
    private final Configuration freemarkerConfig; // 从 Starter 自动装配
    private final HealthProfileMapper healthProfileMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> DISEASE_LABELS = Map.of(
            "hypertension", "高血压",
            "diabetes_2", "糖尿病",
            "gout", "痛风"
    );

    @Override
    public MealPlanDTO generateWeeklyMealPlan(Long userId) {
        return generateMealPlan(userId, "WEEK", null);
    }

    @Override
    public MealPlanDTO generateMealPlan(Long userId, String range, String requirements) {
        String profileText = buildProfileText(userId);
        String knowledgeText = buildKnowledgeContext(profileText);
        String rangeText = normalizeRangeText(range);
        StringBuilder prompt = new StringBuilder();
        prompt.append("请生成").append(rangeText).append("食谱，输出符合中国人的饮食习惯，重点低盐、少油、多蔬果。\n");
        if (!profileText.isBlank()) {
            prompt.append("用户健康档案：").append(profileText).append("\n");
        }
        if (requirements != null && !requirements.isBlank()) {
            prompt.append("用户额外需求：").append(requirements).append("\n");
        }
        if (!knowledgeText.isBlank()) {
            prompt.append("参考资料：").append(knowledgeText).append("\n");
        }
        prompt.append("请包含采购清单，并严格返回 JSON，结构如下：")
                .append("{ title, advice, weeklyPlan: [{ day, breakfast: {name, foods:[], calories}, lunch: {name, foods:[], calories}, dinner: {name, foods:[], calories} }], shoppingList: { ingredient: quantity } }");

        try {
            MealPlanDTO plan = aiService.chat(prompt.toString(), MealPlanDTO.class, AiConfigKeys.MEAL_PLAN);
            augmentShoppingLists(plan);
            return plan;
        } catch (Exception e) {
            log.warn("AI meal plan generation failed, using fallback plan", e);
            return buildFallbackMealPlan(range);
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
            registerFonts(builder);
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(outputStream);
            builder.run();

        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("PDF Generation Failed");
        }
    }

    private MealPlanDTO buildFallbackMealPlan(String range) {
        MealPlanDTO plan = new MealPlanDTO();
        String rangeText = normalizeRangeText(range);
        plan.setTitle("健康基础" + rangeText + "食谱(备用)");
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

        if ("THREE_DAYS".equalsIgnoreCase(range)) {
            MealPlanDTO.DailyPlan day2 = new MealPlanDTO.DailyPlan();
            day2.setDay("周二");
            day2.setBreakfast(breakfast);
            day2.setLunch(lunch);
            day2.setDinner(dinner);

            MealPlanDTO.DailyPlan day3 = new MealPlanDTO.DailyPlan();
            day3.setDay("周三");
            day3.setBreakfast(breakfast);
            day3.setLunch(lunch);
            day3.setDinner(dinner);

            plan.setWeeklyPlan(java.util.Arrays.asList(day1, day2, day3));
        } else {
            plan.setWeeklyPlan(java.util.Collections.singletonList(day1));
        }

        Map<String, String> shopping = new HashMap<>();
        shopping.put("燕麦", "500g");
        shopping.put("鸡蛋", "6个");
        shopping.put("糙米", "1kg");
        shopping.put("青菜", "3把");
        shopping.put("鱼肉", "800g");
        plan.setShoppingList(shopping);

        augmentShoppingLists(plan);
        return plan;
    }

    private String normalizeRangeText(String range) {
        if (range == null) return "一周";
        switch (range.toUpperCase()) {
            case "MEAL":
                return "一顿";
            case "DAY":
                return "一天";
            case "THREE_DAYS":
                return "三天";
            case "WEEK":
            default:
                return "一周";
        }
    }

    private void registerFonts(PdfRendererBuilder builder) {
        String[] classpathFonts = {
                "fonts/阿里妈妈方圆体.ttf",
                "fonts/SourceHanSansCN-Regular.otf",
                "fonts/NotoSansCJKsc-Regular.otf",
                "fonts/SourceHanSansSC-Regular.otf",
                "fonts/SimHei.ttf"
        };
        for (String fontPath : classpathFonts) {
            try {
                InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fontPath);
                if (stream != null) {
                    builder.useFont(() -> stream, "CJK");
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        String[] systemFonts = {
                "C:\\\\Windows\\\\Fonts\\\\simhei.ttf",
                "C:\\\\Windows\\\\Fonts\\\\simsun.ttf",
                "C:\\\\Windows\\\\Fonts\\\\msyh.ttf",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.otf"
        };
        for (String fontPath : systemFonts) {
            try {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    builder.useFont(fontFile, "CJK");
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    private String buildProfileText(Long userId) {
        HealthProfile profile = healthProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HealthProfile>()
                        .eq(HealthProfile::getUserId, userId));
        if (profile == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (profile.getGender() != null) {
            sb.append("性别:").append(profile.getGender()).append(";");
        }
        if (profile.getAge() != null) {
            sb.append("年龄:").append(profile.getAge()).append(";");
        }
        if (profile.getHeight() != null) {
            sb.append("身高:").append(profile.getHeight()).append("cm;");
        }
        if (profile.getWeight() != null) {
            sb.append("体重:").append(profile.getWeight()).append("kg;");
        }
        if (profile.getBmi() != null) {
            sb.append("BMI:").append(profile.getBmi()).append(";");
        }

        List<String> diseases = parseJsonList(profile.getDiseases());
        List<String> allergies = parseJsonList(profile.getAllergies());
        List<String> diseaseLabels = new ArrayList<>();
        for (String item : diseases) {
            diseaseLabels.add(DISEASE_LABELS.getOrDefault(item, item));
        }
        if (!diseaseLabels.isEmpty()) {
            sb.append("慢病:").append(String.join("、", diseaseLabels)).append(";");
        }
        if (!allergies.isEmpty()) {
            sb.append("过敏:").append(String.join("、", allergies)).append(";");
        }
        if (profile.getOtherRestrictions() != null && !profile.getOtherRestrictions().isBlank()) {
            sb.append("忌口:").append(profile.getOtherRestrictions()).append(";");
        }
        return sb.toString();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String buildKnowledgeContext(String profileText) {
        if (profileText == null || profileText.isBlank()) {
            return "";
        }
        List<String> keywords = new ArrayList<>();
        for (String value : DISEASE_LABELS.values()) {
            if (profileText.contains(value)) {
                keywords.add(value);
            }
        }
        if (keywords.isEmpty()) {
            return "";
        }
        Set<String> snippets = new LinkedHashSet<>();
        for (String keyword : keywords) {
            List<KnowledgeBase> items = knowledgeBaseMapper.searchByKeyword(keyword);
            for (KnowledgeBase item : items) {
                if (item.getContent() == null) continue;
                snippets.add(item.getContent());
                if (snippets.size() >= 5) {
                    break;
                }
            }
            if (snippets.size() >= 5) {
                break;
            }
        }
        if (snippets.isEmpty()) {
            return "";
        }
        return String.join(" | ", snippets);
    }

    private void augmentShoppingLists(MealPlanDTO plan) {
        if (plan == null || plan.getWeeklyPlan() == null) {
            return;
        }
        if (plan.getDailyShopping() != null && plan.getPantryItems() != null) {
            return;
        }
        Set<String> pantry = new LinkedHashSet<>();
        Map<String, Set<String>> dayMap = new LinkedHashMap<>();
        for (MealPlanDTO.DailyPlan day : plan.getWeeklyPlan()) {
            if (day == null) continue;
            String dayLabel = day.getDay() == null ? "当天" : day.getDay();
            Set<String> items = dayMap.computeIfAbsent(dayLabel, key -> new LinkedHashSet<>());
            collectFoods(day.getBreakfast(), items, pantry);
            collectFoods(day.getLunch(), items, pantry);
            collectFoods(day.getDinner(), items, pantry);
            collectFoods(day.getSnack(), items, pantry);
        }
        List<MealPlanDTO.DailyShopping> dailyList = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : dayMap.entrySet()) {
            MealPlanDTO.DailyShopping daily = new MealPlanDTO.DailyShopping();
            daily.setDay(entry.getKey());
            daily.setItems(new ArrayList<>(entry.getValue()));
            dailyList.add(daily);
        }
        plan.setDailyShopping(dailyList);
        plan.setPantryItems(new ArrayList<>(pantry));
    }

    private void collectFoods(MealPlanDTO.Meal meal, Set<String> items, Set<String> pantry) {
        if (meal == null) return;
        List<String> foods = meal.getFoods();
        if (foods == null || foods.isEmpty()) {
            foods = splitFoodsFromName(meal.getName());
        }
        if (foods == null) return;
        for (String raw : foods) {
            String name = normalizeFoodName(raw);
            if (name.isBlank()) continue;
            if (isPantryItem(name)) {
                pantry.add(name);
            } else {
                items.add(name);
            }
        }
    }

    private List<String> splitFoodsFromName(String name) {
        if (name == null || name.isBlank()) return Collections.emptyList();
        String[] parts = name.split("[、,，;+|/]");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private String normalizeFoodName(String name) {
        if (name == null) return "";
        return name.replaceAll("\\s+", "").replaceAll("（.*?）", "").replaceAll("\\(.*?\\)", "");
    }

    private boolean isPantryItem(String name) {
        String[] pantryKeywords = {
                "盐", "酱油", "生抽", "老抽", "醋", "糖", "味精", "鸡精",
                "胡椒", "花椒", "辣椒", "辣椒粉", "孜然", "香油", "芝麻油",
                "食用油", "油", "料酒", "蚝油", "豆瓣酱", "黄豆酱",
                "葱", "姜", "蒜", "八角", "香叶", "桂皮"
        };
        for (String keyword : pantryKeywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
