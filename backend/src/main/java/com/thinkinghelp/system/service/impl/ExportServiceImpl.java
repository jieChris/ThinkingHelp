package com.thinkinghelp.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.entity.FoodNutritionCache;
import com.thinkinghelp.system.entity.HealthProfile;
import com.thinkinghelp.system.entity.KnowledgeBase;
import com.thinkinghelp.system.entity.MealPlanFeedback;
import com.thinkinghelp.system.entity.dto.MealPlanDTO;
import com.thinkinghelp.system.mapper.FoodNutritionCacheMapper;
import com.thinkinghelp.system.mapper.HealthProfileMapper;
import com.thinkinghelp.system.mapper.KnowledgeBaseMapper;
import com.thinkinghelp.system.mapper.MealPlanFeedbackMapper;
import com.thinkinghelp.system.service.AIService;
import com.thinkinghelp.system.service.AiConfigKeys;
import com.thinkinghelp.system.service.ExportService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final AIService aiService;
    private final HealthProfileMapper healthProfileMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final MealPlanFeedbackMapper mealPlanFeedbackMapper;
    private final ObjectMapper objectMapper;
    @Autowired(required = false)
    private Configuration freemarkerConfig; // 从 Starter 自动装配（可选）
    @Autowired(required = false)
    private FoodNutritionCacheMapper foodNutritionCacheMapper; // 可选，缺失时降级为本地字典/AI估算

    private static final Map<String, String> DISEASE_LABELS = Map.of(
            "hypertension", "高血压",
            "diabetes_2", "糖尿病",
            "gout", "痛风"
    );
    private static final Pattern FOOD_WITH_WEIGHT_PATTERN = Pattern.compile("([\\p{IsHan}A-Za-z0-9·\\-\\s]+?)(\\d+(?:\\.\\d+)?)\\s*(g|克|ml|毫升)");
    private static final int MAX_AI_INGREDIENT_ESTIMATES = 8;

    @Override
    public MealPlanDTO generateWeeklyMealPlan(Long userId) {
        return generateMealPlan(userId, "WEEK", null);
    }

    @Override
    public MealPlanDTO generateMealPlan(Long userId, String range, String requirements) {
        HealthProfile profile = loadProfile(userId);
        String profileText = buildProfileText(profile);
        String knowledgeText = buildKnowledgeContext(profileText);
        MacroTarget target = buildTargets(profile);
        FeedbackInsights feedbackInsights = buildFeedbackInsights(userId);
        String rangeText = normalizeRangeText(range);
        StringBuilder prompt = new StringBuilder();
        prompt.append("请生成").append(rangeText).append("食谱，输出符合中国人的饮食习惯，重点低盐、少油、多蔬果。\n");
        if (!profileText.isBlank()) {
            prompt.append("用户健康档案：").append(profileText).append("\n");
        }
        if (target != null) {
            prompt.append("每日能量目标约").append(Math.round(target.calories)).append("kcal，")
                    .append("碳水约").append(Math.round(target.carbs)).append("g，")
                    .append("蛋白约").append(Math.round(target.protein)).append("g，")
                    .append("脂肪约").append(Math.round(target.fat)).append("g。\n");
            if ("MEAL".equalsIgnoreCase(range)) {
                prompt.append("本次仅生成一餐，请给出该餐参考能量范围，并与每日目标匹配。\n");
            }
        }
        if (requirements != null && !requirements.isBlank()) {
            prompt.append("用户额外需求：").append(requirements).append("\n");
        }
        if (feedbackInsights != null && feedbackInsights.promptText != null && !feedbackInsights.promptText.isBlank()) {
            prompt.append("用户用餐反馈：").append(feedbackInsights.promptText).append("，请在食谱中体现对应调整。\n");
        }
        if (!knowledgeText.isBlank()) {
            prompt.append("参考资料：").append(knowledgeText).append("\n");
        }
        prompt.append("请包含采购清单，并严格返回 JSON，结构如下：")
                .append("{ title, advice, weeklyPlan: [{ day, breakfast: {name, foods:[], ingredients:[{name,grams}], calories}, lunch: {name, foods:[], ingredients:[{name,grams}], calories}, dinner: {name, foods:[], ingredients:[{name,grams}], calories}, snack: {name, foods:[], ingredients:[{name,grams}], calories} }], shoppingList: { ingredient: quantity } }");

        try {
            MealPlanDTO plan = aiService.chat(prompt.toString(), MealPlanDTO.class, AiConfigKeys.MEAL_PLAN);
            applyTargets(plan, target);
            applyFeedbackAdjustments(plan, feedbackInsights);
            enrichPlanNutrition(plan, true);
            augmentShoppingLists(plan);
            return plan;
        } catch (Exception e) {
            log.warn("AI meal plan generation failed, using fallback plan", e);
            return buildFallbackMealPlan(range, target, feedbackInsights);
        }
    }

    @Override
    public void exportMealPlanToPdf(MealPlanDTO mealPlan, OutputStream outputStream) {
        try {
            if (freemarkerConfig == null) {
                throw new IllegalStateException("FreeMarker 未配置，无法导出 PDF");
            }
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

    @Override
    public void enrichMealPlan(MealPlanDTO mealPlan) {
        if (mealPlan == null) {
            return;
        }
        enrichPlanNutrition(mealPlan, false);
        augmentShoppingLists(mealPlan);
    }

    private MealPlanDTO buildFallbackMealPlan(String range, MacroTarget target, FeedbackInsights feedbackInsights) {
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
        shopping.put("糙米", "1千克");
        shopping.put("青菜", "3把");
        shopping.put("鱼肉", "800g");
        plan.setShoppingList(shopping);

        applyTargets(plan, target);
        applyFeedbackAdjustments(plan, feedbackInsights);
        enrichPlanNutrition(plan, true);
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

    private HealthProfile loadProfile(Long userId) {
        if (userId == null) {
            return null;
        }
        return healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>().eq(HealthProfile::getUserId, userId));
    }

    private String buildProfileText(HealthProfile profile) {
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
        if (profile.getActivityLevel() != null && !profile.getActivityLevel().isBlank()) {
            sb.append("活动量:").append(formatActivityLevel(profile.getActivityLevel())).append(";");
        }
        if (profile.getGoal() != null && !profile.getGoal().isBlank()) {
            sb.append("目标:").append(formatGoal(profile.getGoal())).append(";");
        }
        if (profile.getExerciseFrequency() != null || profile.getExerciseDuration() != null) {
            sb.append("运动:");
            if (profile.getExerciseFrequency() != null) {
                sb.append("每周").append(profile.getExerciseFrequency()).append("次");
            }
            if (profile.getExerciseDuration() != null) {
                sb.append(profile.getExerciseFrequency() != null ? "," : "")
                        .append("每次").append(profile.getExerciseDuration()).append("分钟");
            }
            sb.append(";");
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

    private MacroTarget buildTargets(HealthProfile profile) {
        if (profile == null || profile.getWeight() == null || profile.getHeight() == null || profile.getAge() == null) {
            return null;
        }
        double weight = profile.getWeight();
        double height = profile.getHeight();
        int age = profile.getAge();
        double genderAdjust = resolveGenderAdjust(profile.getGender());
        double bmr = 10 * weight + 6.25 * height - 5 * age + genderAdjust;
        double activity = resolveActivityFactor(profile);
        double tdee = bmr * activity + resolveGoalDelta(profile);
        if (tdee < 1200) {
            tdee = 1200;
        }
        List<String> diseases = parseJsonList(profile.getDiseases());
        boolean diabetes = hasDiabetes(diseases);
        double carbRatio = diabetes ? 0.4 : 0.5;
        double proteinRatio = diabetes ? 0.25 : 0.2;
        double fatRatio = 1 - carbRatio - proteinRatio;
        MacroTarget target = new MacroTarget();
        target.calories = tdee;
        target.carbs = (tdee * carbRatio) / 4.0;
        target.protein = (tdee * proteinRatio) / 4.0;
        target.fat = (tdee * fatRatio) / 9.0;
        return target;
    }

    private void applyTargets(MealPlanDTO plan, MacroTarget target) {
        if (plan == null || target == null) {
            return;
        }
        plan.setTargetCalories((double) Math.round(target.calories));
        plan.setTargetCarbs((double) Math.round(target.carbs));
        plan.setTargetProtein((double) Math.round(target.protein));
        plan.setTargetFat((double) Math.round(target.fat));
    }

    private FeedbackInsights buildFeedbackInsights(Long userId) {
        FeedbackInsights result = new FeedbackInsights();
        result.promptText = "";
        result.adjustmentNotes = new ArrayList<>();
        if (userId == null) {
            return result;
        }
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<MealPlanFeedback> feedbacks = mealPlanFeedbackMapper.selectList(
                new LambdaQueryWrapper<MealPlanFeedback>()
                        .eq(MealPlanFeedback::getUserId, userId)
                        .ge(MealPlanFeedback::getCreatedAt, since)
                        .orderByDesc(MealPlanFeedback::getCreatedAt));
        if (feedbacks == null || feedbacks.isEmpty()) {
            return result;
        }
        Map<String, int[]> stats = new HashMap<>();
        for (MealPlanFeedback item : feedbacks) {
            String mealType = normalizeMealType(item.getMealType());
            if (mealType == null) {
                continue;
            }
            int[] count = stats.computeIfAbsent(mealType, key -> new int[2]);
            if ("MORE".equalsIgnoreCase(item.getDirection())) {
                count[0]++;
            } else if ("LESS".equalsIgnoreCase(item.getDirection())) {
                count[1]++;
            }
        }
        if (stats.isEmpty()) {
            return result;
        }
        List<String> parts = new ArrayList<>();
        List<String> order = Arrays.asList("BREAKFAST", "LUNCH", "DINNER", "SNACK");
        for (String meal : order) {
            int[] count = stats.get(meal);
            if (count == null) continue;
            String label = mealTypeLabel(meal);
            int diff = count[0] - count[1];
            if (diff > 0) {
                parts.add(label + "量偏少");
                int percent = diff >= 3 ? 15 : 10;
                result.adjustmentNotes.add(label + "份量上调约" + percent + "%（依据近期“吃不饱”反馈）");
            } else if (diff < 0) {
                parts.add(label + "量偏多");
                int percent = Math.abs(diff) >= 3 ? 15 : 10;
                result.adjustmentNotes.add(label + "份量下调约" + percent + "%（依据近期“太多了”反馈）");
            }
        }
        result.promptText = String.join("；", parts);
        return result;
    }

    private void applyFeedbackAdjustments(MealPlanDTO plan, FeedbackInsights feedbackInsights) {
        if (plan == null || feedbackInsights == null) {
            return;
        }
        if (feedbackInsights.adjustmentNotes == null || feedbackInsights.adjustmentNotes.isEmpty()) {
            if (plan.getAdjustmentNotes() == null) {
                plan.setAdjustmentNotes(Collections.emptyList());
            }
            return;
        }
        if (plan.getAdjustmentNotes() == null || plan.getAdjustmentNotes().isEmpty()) {
            plan.setAdjustmentNotes(new ArrayList<>(feedbackInsights.adjustmentNotes));
            return;
        }
        List<String> merged = new ArrayList<>(plan.getAdjustmentNotes());
        for (String note : feedbackInsights.adjustmentNotes) {
            if (!merged.contains(note)) {
                merged.add(note);
            }
        }
        plan.setAdjustmentNotes(merged);
    }

    private String normalizeMealType(String mealType) {
        if (mealType == null) return null;
        String value = mealType.trim().toUpperCase();
        switch (value) {
            case "BREAKFAST":
            case "LUNCH":
            case "DINNER":
            case "SNACK":
                return value;
            default:
                return null;
        }
    }

    private String mealTypeLabel(String mealType) {
        switch (mealType) {
            case "BREAKFAST":
                return "早餐";
            case "LUNCH":
                return "午餐";
            case "DINNER":
                return "晚餐";
            case "SNACK":
                return "加餐";
            default:
                return "餐次";
        }
    }

    private double resolveGenderAdjust(String gender) {
        if (gender == null) return 0;
        String value = gender.toUpperCase();
        if (value.contains("MALE") || value.contains("男")) {
            return 5;
        }
        if (value.contains("FEMALE") || value.contains("女")) {
            return -161;
        }
        return 0;
    }

    private double resolveActivityFactor(HealthProfile profile) {
        String level = profile.getActivityLevel();
        if (level != null && !level.isBlank()) {
            switch (level.toUpperCase()) {
                case "SEDENTARY":
                    return 1.2;
                case "LIGHT":
                    return 1.375;
                case "MODERATE":
                    return 1.55;
                case "ACTIVE":
                    return 1.725;
                case "VERY_ACTIVE":
                    return 1.9;
                default:
                    break;
            }
        }
        Integer freq = profile.getExerciseFrequency();
        Integer duration = profile.getExerciseDuration();
        if (freq != null && duration != null && freq > 0 && duration > 0) {
            int minutes = freq * duration;
            if (minutes < 60) return 1.2;
            if (minutes < 150) return 1.375;
            if (minutes < 300) return 1.55;
            return 1.725;
        }
        return 1.2;
    }

    private int resolveGoalDelta(HealthProfile profile) {
        String goal = profile.getGoal();
        if (goal == null || goal.isBlank()) {
            return 0;
        }
        String value = goal.toUpperCase();
        if (value.contains("LOSE") || value.contains("减") || value.contains("降")) {
            return -300;
        }
        if (value.contains("GAIN") || value.contains("增")) {
            return 300;
        }
        return 0;
    }

    private boolean hasDiabetes(List<String> diseases) {
        for (String item : diseases) {
            if (item == null) continue;
            if (item.contains("diabetes") || item.contains("糖尿")) {
                return true;
            }
        }
        return false;
    }

    private String formatActivityLevel(String level) {
        if (level == null) return "";
        switch (level.toUpperCase()) {
            case "SEDENTARY":
                return "久坐/少动";
            case "LIGHT":
                return "轻度活动";
            case "MODERATE":
                return "中度活动";
            case "ACTIVE":
                return "高强度活动";
            case "VERY_ACTIVE":
                return "非常高强度";
            default:
                return level;
        }
    }

    private String formatGoal(String goal) {
        if (goal == null) return "";
        switch (goal.toUpperCase()) {
            case "MAINTAIN":
                return "维持体重";
            case "LOSE":
                return "减脂/减重";
            case "GAIN":
                return "增肌/增重";
            default:
                return goal;
        }
    }

    private static class MacroTarget {
        private double calories;
        private double carbs;
        private double protein;
        private double fat;
    }

    private static class FeedbackInsights {
        private String promptText;
        private List<String> adjustmentNotes;
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
        if (plan == null || plan.getWeeklyPlan() == null || plan.getWeeklyPlan().isEmpty()) {
            return;
        }

        Map<String, Map<String, ShoppingAmount>> dayMap = new LinkedHashMap<>();
        Map<String, ShoppingAmount> pantryMap = new LinkedHashMap<>();
        Map<String, ShoppingAmount> purchaseTotalMap = new LinkedHashMap<>();

        for (MealPlanDTO.DailyPlan day : plan.getWeeklyPlan()) {
            if (day == null) continue;
            String dayLabel = day.getDay() == null ? "当天" : day.getDay();
            Map<String, ShoppingAmount> items = dayMap.computeIfAbsent(dayLabel, key -> new LinkedHashMap<>());
            collectShopping(day.getBreakfast(), items, pantryMap, purchaseTotalMap);
            collectShopping(day.getLunch(), items, pantryMap, purchaseTotalMap);
            collectShopping(day.getDinner(), items, pantryMap, purchaseTotalMap);
            collectShopping(day.getSnack(), items, pantryMap, purchaseTotalMap);
        }

        List<MealPlanDTO.DailyShopping> dailyList = new ArrayList<>();
        for (Map.Entry<String, Map<String, ShoppingAmount>> entry : dayMap.entrySet()) {
            MealPlanDTO.DailyShopping daily = new MealPlanDTO.DailyShopping();
            daily.setDay(entry.getKey());
            List<String> items = new ArrayList<>();
            for (ShoppingAmount item : entry.getValue().values()) {
                items.add(formatShoppingItem(item));
            }
            daily.setItems(items);
            dailyList.add(daily);
        }
        plan.setDailyShopping(dailyList);

        List<String> pantryItems = new ArrayList<>();
        for (ShoppingAmount item : pantryMap.values()) {
            pantryItems.add(formatShoppingItem(item));
        }
        plan.setPantryItems(pantryItems);

        Map<String, String> shoppingList = new LinkedHashMap<>();
        for (ShoppingAmount item : purchaseTotalMap.values()) {
            shoppingList.put(item.displayName, formatWeight(item.grams));
        }
        plan.setShoppingList(shoppingList);
    }

    private void collectShopping(MealPlanDTO.Meal meal,
                                 Map<String, ShoppingAmount> dayItems,
                                 Map<String, ShoppingAmount> pantryItems,
                                 Map<String, ShoppingAmount> purchaseTotalMap) {
        if (meal == null) {
            return;
        }

        List<IngredientCandidate> candidates = collectIngredientCandidates(meal);
        if (candidates.isEmpty()) {
            return;
        }

        for (IngredientCandidate candidate : candidates) {
            if (candidate == null || candidate.normalizedName == null || candidate.normalizedName.isBlank()) {
                continue;
            }
            if (isPantryItem(candidate.normalizedName)) {
                mergeShoppingAmount(pantryItems, candidate.normalizedName, candidate.displayName, candidate.grams);
            } else {
                mergeShoppingAmount(dayItems, candidate.normalizedName, candidate.displayName, candidate.grams);
                mergeShoppingAmount(purchaseTotalMap, candidate.normalizedName, candidate.displayName, candidate.grams);
            }
        }
    }

    private void mergeShoppingAmount(Map<String, ShoppingAmount> target,
                                     String normalizedName,
                                     String displayName,
                                     double grams) {
        if (target == null || normalizedName == null || normalizedName.isBlank()) {
            return;
        }
        ShoppingAmount item = target.get(normalizedName);
        if (item == null) {
            item = new ShoppingAmount();
            item.displayName = (displayName == null || displayName.isBlank()) ? normalizedName : displayName;
            target.put(normalizedName, item);
        } else if ((item.displayName == null || item.displayName.isBlank()) && displayName != null && !displayName.isBlank()) {
            item.displayName = displayName;
        }
        if (grams > 0) {
            item.grams += grams;
        }
    }

    private String formatShoppingItem(ShoppingAmount item) {
        if (item == null || item.displayName == null || item.displayName.isBlank()) {
            return "";
        }
        if (item.grams <= 0) {
            return item.displayName;
        }
        return item.displayName + " " + formatWeight(item.grams);
    }

    private String formatWeight(double grams) {
        if (grams <= 0) {
            return "";
        }
        if (grams >= 1000) {
            return formatNumber(round1(grams / 1000.0)) + "千克";
        }
        return formatNumber(round1(grams)) + "克";
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
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

    private void enrichPlanNutrition(MealPlanDTO plan, boolean allowAiEstimate) {
        if (plan == null || plan.getWeeklyPlan() == null || plan.getWeeklyPlan().isEmpty()) {
            return;
        }
        Map<String, NutritionPer100> aiEstimateCache = new HashMap<>();
        int[] aiBudget = new int[]{MAX_AI_INGREDIENT_ESTIMATES};
        for (MealPlanDTO.DailyPlan day : plan.getWeeklyPlan()) {
            if (day == null) continue;
            enrichMealNutrition(day.getBreakfast(), aiEstimateCache, aiBudget, allowAiEstimate);
            enrichMealNutrition(day.getLunch(), aiEstimateCache, aiBudget, allowAiEstimate);
            enrichMealNutrition(day.getDinner(), aiEstimateCache, aiBudget, allowAiEstimate);
            enrichMealNutrition(day.getSnack(), aiEstimateCache, aiBudget, allowAiEstimate);
        }
    }

    private void enrichMealNutrition(MealPlanDTO.Meal meal,
                                     Map<String, NutritionPer100> aiEstimateCache,
                                     int[] aiBudget,
                                     boolean allowAiEstimate) {
        if (meal == null) {
            return;
        }

        List<IngredientCandidate> candidates = collectIngredientCandidates(meal);
        if (candidates.isEmpty()) {
            return;
        }

        List<MealPlanDTO.Ingredient> ingredients = new ArrayList<>();
        double totalCalories = 0;
        double totalCarbs = 0;
        double totalProtein = 0;
        double totalFat = 0;
        boolean hasAnySource = false;

        for (IngredientCandidate candidate : candidates) {
            NutritionPer100 profile = resolveNutritionPer100(candidate.normalizedName, aiEstimateCache, aiBudget, allowAiEstimate);
            if (profile == null) {
                continue;
            }
            double ratio = candidate.grams / 100.0;
            double calories = profile.calories * ratio;
            double carbs = profile.carbs * ratio;
            double protein = profile.protein * ratio;
            double fat = profile.fat * ratio;

            MealPlanDTO.Ingredient ingredient = new MealPlanDTO.Ingredient();
            ingredient.setName(candidate.displayName);
            ingredient.setGrams(round1(candidate.grams));
            ingredient.setCaloriesKcal(round1(calories));
            ingredient.setCarbsGrams(round1(carbs));
            ingredient.setProteinGrams(round1(protein));
            ingredient.setFatGrams(round1(fat));
            ingredients.add(ingredient);

            totalCalories += calories;
            totalCarbs += carbs;
            totalProtein += protein;
            totalFat += fat;
            hasAnySource = true;
        }

        if (!hasAnySource) {
            return;
        }

        meal.setIngredients(ingredients);
        meal.setCaloriesKcal(round1(totalCalories));
        meal.setCarbsGrams(round1(totalCarbs));
        meal.setProteinGrams(round1(totalProtein));
        meal.setFatGrams(round1(totalFat));
        meal.setCalories(Math.round(totalCalories) + "kcal");

        if (meal.getFoods() == null || meal.getFoods().isEmpty()) {
            List<String> foods = new ArrayList<>();
            for (IngredientCandidate candidate : candidates) {
                foods.add(candidate.displayName + " " + Math.round(candidate.grams) + "g");
            }
            meal.setFoods(foods);
        }
    }

    private List<IngredientCandidate> collectIngredientCandidates(MealPlanDTO.Meal meal) {
        List<MealPlanDTO.Ingredient> ingredientList = meal.getIngredients();
        if (ingredientList != null && !ingredientList.isEmpty()) {
            List<IngredientCandidate> fromIngredients = new ArrayList<>();
            for (MealPlanDTO.Ingredient ingredient : ingredientList) {
                if (ingredient == null) continue;
                String displayName = sanitizeIngredientDisplayName(ingredient.getName());
                String normalized = normalizeIngredientName(displayName);
                if (normalized.isBlank()) continue;
                double grams = ingredient.getGrams() != null && ingredient.getGrams() > 0
                        ? ingredient.getGrams()
                        : defaultIngredientGrams(normalized);
                fromIngredients.add(new IngredientCandidate(displayName, normalized, grams));
            }
            if (!fromIngredients.isEmpty()) {
                return fromIngredients;
            }
        }

        List<String> rawFoods = meal.getFoods();
        if (rawFoods == null || rawFoods.isEmpty()) {
            rawFoods = splitFoodsFromName(meal.getName());
        }
        if (rawFoods == null || rawFoods.isEmpty()) {
            return Collections.emptyList();
        }

        List<IngredientCandidate> list = new ArrayList<>();
        for (String raw : rawFoods) {
            if (raw == null || raw.isBlank()) continue;
            Matcher matcher = FOOD_WITH_WEIGHT_PATTERN.matcher(raw);
            if (matcher.find()) {
                String displayName = sanitizeIngredientDisplayName(matcher.group(1));
                String name = normalizeIngredientName(displayName);
                double grams = parseGrams(matcher.group(2), matcher.group(3));
                if (!name.isBlank() && grams > 0) {
                    list.add(new IngredientCandidate(displayName, name, grams));
                }
                continue;
            }
            String displayName = sanitizeIngredientDisplayName(raw);
            String normalized = normalizeIngredientName(displayName);
            if (normalized.isBlank()) continue;
            list.add(new IngredientCandidate(displayName, normalized, defaultIngredientGrams(normalized)));
        }
        return list;
    }

    private NutritionPer100 resolveNutritionPer100(String normalizedName,
                                                   Map<String, NutritionPer100> aiEstimateCache,
                                                   int[] aiBudget,
                                                   boolean allowAiEstimate) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return null;
        }

        NutritionPer100 catalog = fromCatalog(normalizedName);
        if (catalog != null) {
            return catalog;
        }

        if (foodNutritionCacheMapper != null) {
            FoodNutritionCache cache = foodNutritionCacheMapper.selectOne(
                    new LambdaQueryWrapper<FoodNutritionCache>().eq(FoodNutritionCache::getNormalizedName, normalizedName.toLowerCase(Locale.ROOT)));
            if (cache != null && cache.getCaloriesPer100g() != null && cache.getCaloriesPer100g() > 0) {
                double carbs = cache.getCarbsPer100g() != null ? Math.max(0, cache.getCarbsPer100g()) : estimateCarbsByName(normalizedName);
                double protein = estimateProteinByName(normalizedName);
                double fat = estimateFatByName(normalizedName);
                return new NutritionPer100(cache.getCaloriesPer100g(), carbs, protein, fat);
            }
        }

        NutritionPer100 cachedAi = aiEstimateCache.get(normalizedName);
        if (cachedAi != null) {
            return cachedAi;
        }

        if (!allowAiEstimate || aiBudget[0] <= 0) {
            return defaultNutritionByName(normalizedName);
        }

        NutritionPer100 ai = estimateNutritionByAi(normalizedName);
        if (ai != null) {
            aiEstimateCache.put(normalizedName, ai);
            aiBudget[0]--;
            return ai;
        }
        return defaultNutritionByName(normalizedName);
    }

    private NutritionPer100 estimateNutritionByAi(String foodName) {
        try {
            String prompt = "请估算该食物每100克营养，严格返回JSON："
                    + "{\"caloriesPer100g\":number,\"carbsPer100g\":number,\"proteinPer100g\":number,\"fatPer100g\":number}\n"
                    + "食物：" + foodName + "\n"
                    + "要求：四个字段均为非负数。";
            AiNutritionResponse result = aiService.chat(prompt, AiNutritionResponse.class, AiConfigKeys.MEAL_PLAN);
            if (result == null || result.caloriesPer100g == null || result.caloriesPer100g <= 0) {
                return null;
            }
            double calories = Math.max(1, result.caloriesPer100g);
            double carbs = nonNegative(result.carbsPer100g);
            double protein = nonNegative(result.proteinPer100g);
            double fat = nonNegative(result.fatPer100g);
            return new NutritionPer100(calories, carbs, protein, fat);
        } catch (Exception e) {
            return null;
        }
    }

    private NutritionPer100 fromCatalog(String name) {
        for (KeywordNutrition item : NUTRITION_CATALOG) {
            for (String keyword : item.keywords) {
                if (name.contains(keyword)) {
                    return item;
                }
            }
        }
        return null;
    }

    private String normalizeIngredientName(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = sanitizeIngredientDisplayName(value);
        cleaned = cleaned.replaceAll("[0-9.]+(g|克|ml|毫升)", "");
        return cleaned.toLowerCase(Locale.ROOT);
    }

    private String sanitizeIngredientDisplayName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("\\s+", "")
                .replaceAll("（.*?）", "")
                .replaceAll("\\(.*?\\)", "");
    }

    private double parseGrams(String numberText, String unitText) {
        try {
            double value = Double.parseDouble(numberText);
            if (value <= 0) return 0;
            String unit = unitText == null ? "g" : unitText.toLowerCase(Locale.ROOT);
            if ("ml".equals(unit) || "毫升".equals(unit)) {
                return value;
            }
            return value;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double defaultIngredientGrams(String normalizedName) {
        if (normalizedName.contains("油")) return 10;
        if (normalizedName.contains("盐")) return 3;
        if (normalizedName.contains("酱")) return 8;
        if (normalizedName.contains("鸡蛋") || normalizedName.equals("蛋")) return 50;
        if (normalizedName.contains("牛奶") || normalizedName.contains("豆浆") || normalizedName.contains("酸奶")) return 250;
        if (normalizedName.contains("米饭") || normalizedName.contains("面") || normalizedName.contains("粥")) return 150;
        if (normalizedName.contains("肉") || normalizedName.contains("鱼") || normalizedName.contains("虾")) return 120;
        if (normalizedName.contains("蔬") || normalizedName.contains("菜")) return 120;
        if (normalizedName.contains("水果") || normalizedName.contains("苹果") || normalizedName.contains("香蕉")) return 150;
        return 100;
    }

    private NutritionPer100 defaultNutritionByName(String name) {
        if (name.contains("蔬") || name.contains("菜")) {
            return new NutritionPer100(35, 6, 2, 0.3);
        }
        if (name.contains("肉") || name.contains("鱼") || name.contains("虾") || name.contains("蛋")) {
            return new NutritionPer100(160, 2, 20, 7);
        }
        return new NutritionPer100(120, 18, 4, 3);
    }

    private double estimateCarbsByName(String name) {
        if (name.contains("米") || name.contains("面") || name.contains("馒头") || name.contains("粥")) return 25;
        if (name.contains("水果")) return 14;
        if (name.contains("蔬") || name.contains("菜")) return 6;
        if (name.contains("肉") || name.contains("鱼") || name.contains("蛋")) return 2;
        return 12;
    }

    private double estimateProteinByName(String name) {
        if (name.contains("肉") || name.contains("鱼") || name.contains("虾") || name.contains("鸡胸")) return 20;
        if (name.contains("蛋")) return 13;
        if (name.contains("豆腐") || name.contains("豆浆")) return 7;
        if (name.contains("牛奶") || name.contains("酸奶")) return 3.5;
        if (name.contains("蔬") || name.contains("菜")) return 2;
        return 4;
    }

    private double estimateFatByName(String name) {
        if (name.contains("油")) return 100;
        if (name.contains("坚果") || name.contains("花生") || name.contains("核桃")) return 50;
        if (name.contains("肉") || name.contains("鱼") || name.contains("虾")) return 8;
        if (name.contains("蛋")) return 10;
        if (name.contains("牛奶") || name.contains("酸奶")) return 3.5;
        return 3;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double nonNegative(Double value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, value);
    }

    private static final List<KeywordNutrition> NUTRITION_CATALOG = Arrays.asList(
            new KeywordNutrition(new String[]{"米饭", "白米饭"}, 116, 25.9, 2.6, 0.3),
            new KeywordNutrition(new String[]{"糙米饭", "糙米"}, 111, 23, 2.6, 0.9),
            new KeywordNutrition(new String[]{"燕麦", "燕麦片"}, 367, 66.3, 15, 6.7),
            new KeywordNutrition(new String[]{"面条", "挂面", "面"}, 284, 58, 8.6, 1.6),
            new KeywordNutrition(new String[]{"馒头", "面包", "全麦面包"}, 230, 46.6, 7, 1.2),
            new KeywordNutrition(new String[]{"红薯", "地瓜"}, 86, 20.1, 1.6, 0.1),
            new KeywordNutrition(new String[]{"土豆", "马铃薯"}, 77, 17.2, 2, 0.1),
            new KeywordNutrition(new String[]{"鸡胸肉", "鸡肉"}, 165, 0, 31, 3.6),
            new KeywordNutrition(new String[]{"牛肉"}, 250, 0, 26, 15),
            new KeywordNutrition(new String[]{"猪肉"}, 242, 0, 27, 14),
            new KeywordNutrition(new String[]{"鱼", "三文鱼", "鳕鱼"}, 170, 0, 22, 8),
            new KeywordNutrition(new String[]{"虾"}, 99, 0.2, 24, 0.3),
            new KeywordNutrition(new String[]{"鸡蛋", "蛋"}, 143, 0.7, 12.6, 9.5),
            new KeywordNutrition(new String[]{"豆腐"}, 81, 1.9, 8.1, 4.2),
            new KeywordNutrition(new String[]{"牛奶"}, 54, 5, 3.4, 3.2),
            new KeywordNutrition(new String[]{"酸奶"}, 72, 9, 2.5, 3.1),
            new KeywordNutrition(new String[]{"豆浆"}, 31, 1.7, 3, 1.6),
            new KeywordNutrition(new String[]{"西兰花"}, 34, 6.6, 2.8, 0.4),
            new KeywordNutrition(new String[]{"菠菜"}, 23, 3.6, 2.9, 0.4),
            new KeywordNutrition(new String[]{"黄瓜"}, 16, 3.6, 0.7, 0.1),
            new KeywordNutrition(new String[]{"番茄", "西红柿"}, 18, 3.9, 0.9, 0.2),
            new KeywordNutrition(new String[]{"胡萝卜"}, 41, 9.6, 0.9, 0.2),
            new KeywordNutrition(new String[]{"苹果"}, 52, 13.8, 0.3, 0.2),
            new KeywordNutrition(new String[]{"香蕉"}, 89, 22.8, 1.1, 0.3),
            new KeywordNutrition(new String[]{"橙子", "橘子"}, 47, 11.8, 0.9, 0.1),
            new KeywordNutrition(new String[]{"花生", "坚果", "核桃"}, 567, 16.1, 25.8, 49.2),
            new KeywordNutrition(new String[]{"食用油", "橄榄油", "菜籽油", "花生油"}, 899, 0, 0, 99.9)
    );

    private static class IngredientCandidate {
        private final String displayName;
        private final String normalizedName;
        private final double grams;

        private IngredientCandidate(String displayName, String normalizedName, double grams) {
            this.displayName = displayName;
            this.normalizedName = normalizedName;
            this.grams = grams;
        }
    }

    private static class ShoppingAmount {
        private String displayName;
        private double grams;
    }

    private static class NutritionPer100 {
        private final double calories;
        private final double carbs;
        private final double protein;
        private final double fat;

        private NutritionPer100(double calories, double carbs, double protein, double fat) {
            this.calories = calories;
            this.carbs = carbs;
            this.protein = protein;
            this.fat = fat;
        }
    }

    private static class KeywordNutrition extends NutritionPer100 {
        private final String[] keywords;

        private KeywordNutrition(String[] keywords, double calories, double carbs, double protein, double fat) {
            super(calories, carbs, protein, fat);
            this.keywords = keywords;
        }
    }

    private static class AiNutritionResponse {
        public Double caloriesPer100g;
        public Double carbsPer100g;
        public Double proteinPer100g;
        public Double fatPer100g;
    }
}
