package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.DietLog;
import com.thinkinghelp.system.entity.FoodNutritionCache;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.entity.dto.FoodCalorieEstimateRequest;
import com.thinkinghelp.system.entity.dto.FoodCalorieEstimateResponse;
import com.thinkinghelp.system.entity.dto.FoodMacroEstimateRequest;
import com.thinkinghelp.system.entity.dto.FoodMacroEstimateResponse;
import com.thinkinghelp.system.mapper.DietLogMapper;
import com.thinkinghelp.system.mapper.FoodNutritionCacheMapper;
import com.thinkinghelp.system.mapper.UserMapper;
import com.thinkinghelp.system.service.AIService;
import com.thinkinghelp.system.service.AiConfigKeys;
import com.thinkinghelp.system.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/diet/logs")
@RequiredArgsConstructor
@Tag(name = "饮食记录", description = "用户饮食记录")
public class DietLogController {

    private final DietLogMapper dietLogMapper;
    private final FoodNutritionCacheMapper foodNutritionCacheMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final AIService aiService;

    private static final Pattern FOOD_NAME_ALLOWED = Pattern.compile("^[\\p{IsHan}A-Za-z0-9\\s·.()（）+\\-/]{1,64}$");
    private static final int AI_ESTIMATE_LIMIT_PER_MINUTE = 30;
    private static final long AI_ESTIMATE_WINDOW_MS = 60_000L;
    private final Map<Long, Deque<Long>> aiEstimateWindow = new ConcurrentHashMap<>();

    @PostMapping
    @Operation(summary = "新增饮食记录")
    public Result<String> addLog(@RequestHeader("Authorization") String token, @RequestBody DietLog log) {
        Long userId = getUserIdFromToken(token);
        log.setUserId(userId);
        if (log.getRecordedAt() == null) {
            log.setRecordedAt(LocalDateTime.now());
        }
        dietLogMapper.insert(log);
        return Result.success("记录成功");
    }

    @GetMapping
    @Operation(summary = "获取饮食记录")
    public Result<List<DietLog>> listLogs(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        Long userId = getUserIdFromToken(token);
        LambdaQueryWrapper<DietLog> query = new LambdaQueryWrapper<>();
        query.eq(DietLog::getUserId, userId);
        if (start != null && !start.isEmpty()) {
            query.ge(DietLog::getRecordedAt, parseDateTime(start));
        }
        if (end != null && !end.isEmpty()) {
            query.le(DietLog::getRecordedAt, parseDateTime(end));
        }
        query.orderByDesc(DietLog::getRecordedAt);
        return Result.success(dietLogMapper.selectList(query));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新饮食记录")
    public Result<String> updateLog(@RequestHeader("Authorization") String token,
                                    @PathVariable Long id,
                                    @RequestBody DietLog payload) {
        Long userId = getUserIdFromToken(token);
        DietLog existing = dietLogMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.error("记录不存在");
        }
        if (payload.getMealType() != null) {
            existing.setMealType(payload.getMealType());
        }
        if (payload.getFoodName() != null) {
            existing.setFoodName(payload.getFoodName());
        }
        if (payload.getUnit() != null) {
            existing.setUnit(payload.getUnit());
        }
        if (payload.getWeightGrams() != null) {
            existing.setWeightGrams(payload.getWeightGrams());
        }
        if (payload.getCalories() != null) {
            existing.setCalories(payload.getCalories());
        }
        if (payload.getCaloriesSource() != null) {
            existing.setCaloriesSource(payload.getCaloriesSource());
        }
        if (payload.getCarbsGrams() != null) {
            existing.setCarbsGrams(payload.getCarbsGrams());
        }
        if (payload.getSugarGrams() != null) {
            existing.setSugarGrams(payload.getSugarGrams());
        }
        if (payload.getCarbsSource() != null) {
            existing.setCarbsSource(payload.getCarbsSource());
        }
        if (payload.getSugarSource() != null) {
            existing.setSugarSource(payload.getSugarSource());
        }
        dietLogMapper.updateById(existing);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除饮食记录")
    public Result<String> deleteLog(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        Long userId = getUserIdFromToken(token);
        DietLog existing = dietLogMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.error("记录不存在");
        }
        dietLogMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @PostMapping("/estimate-calories")
    @Operation(summary = "估算食物热量")
    public Result<FoodCalorieEstimateResponse> estimateCalories(
            @RequestHeader("Authorization") String token,
            @RequestBody FoodCalorieEstimateRequest request) {
        Long userId = getUserIdFromToken(token);
        String normalizedName = normalizeFoodName(request.getFoodName());
        if (normalizedName == null) {
            return Result.error("食物名称格式不合法");
        }
        int weightGrams = resolveWeight(request.getWeightGrams());
        if (weightGrams <= 0) {
            return Result.error("重量必须在1~5000克之间");
        }

        FoodNutritionCache cache = getCacheByName(normalizedName);
        if (cache != null && cache.getCaloriesPer100g() != null && cache.getCaloriesPer100g() > 0) {
            FoodCalorieEstimateResponse response = new FoodCalorieEstimateResponse();
            response.setCalories(round2(cache.getCaloriesPer100g() * weightGrams / 100.0));
            response.setNote("本地缓存估算");
            return Result.success(response);
        }

        if (!allowAiEstimate(userId)) {
            return Result.error("估算过于频繁，请稍后再试");
        }

        String displayName = sanitizeFoodDisplayName(request.getFoodName());
        String prompt = "请估算以下食物“每100克”的热量(kcal)，仅输出严格 JSON：{ \"caloriesPer100g\": number, \"note\": string }。\n" +
                "食物：" + displayName + "\n" +
                "要求：caloriesPer100g 为正数，note 简短。";
        try {
            FoodCaloriePer100Response ai = aiService.chat(prompt, FoodCaloriePer100Response.class, AiConfigKeys.MEAL_PLAN);
            if (ai == null || ai.getCaloriesPer100g() == null || ai.getCaloriesPer100g() <= 0) {
                return Result.error("AI估算失败");
            }
            upsertCache(normalizedName, displayName, ai.getCaloriesPer100g(), null, null, "ai");
            FoodCalorieEstimateResponse response = new FoodCalorieEstimateResponse();
            response.setCalories(round2(ai.getCaloriesPer100g() * weightGrams / 100.0));
            response.setNote("AI估算并已缓存，仅供参考");
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("AI估算失败");
        }
    }

    @PostMapping("/estimate-macros")
    @Operation(summary = "估算食物碳水/糖")
    public Result<FoodMacroEstimateResponse> estimateMacros(
            @RequestHeader("Authorization") String token,
            @RequestBody FoodMacroEstimateRequest request) {
        Long userId = getUserIdFromToken(token);
        String normalizedName = normalizeFoodName(request.getFoodName());
        if (normalizedName == null) {
            return Result.error("食物名称格式不合法");
        }
        int weightGrams = resolveWeight(request.getWeightGrams());
        if (weightGrams <= 0) {
            return Result.error("重量必须在1~5000克之间");
        }

        FoodNutritionCache cache = getCacheByName(normalizedName);
        if (cache != null && (hasPositive(cache.getCarbsPer100g()) || hasPositive(cache.getSugarPer100g()))) {
            FoodMacroEstimateResponse response = new FoodMacroEstimateResponse();
            if (hasPositive(cache.getCarbsPer100g())) {
                response.setCarbsGrams(round2(cache.getCarbsPer100g() * weightGrams / 100.0));
            }
            if (hasPositive(cache.getSugarPer100g())) {
                response.setSugarGrams(round2(cache.getSugarPer100g() * weightGrams / 100.0));
            }
            response.setNote("本地缓存估算");
            return Result.success(response);
        }

        if (!allowAiEstimate(userId)) {
            return Result.error("估算过于频繁，请稍后再试");
        }

        String displayName = sanitizeFoodDisplayName(request.getFoodName());
        String prompt = "请估算以下食物“每100克”的碳水和糖含量(克)，仅输出严格 JSON：{ \"carbsPer100g\": number, \"sugarPer100g\": number, \"note\": string }。\n" +
                "食物：" + displayName + "\n" +
                "要求：carbsPer100g/sugarPer100g 为非负数，note 简短。";
        try {
            FoodMacroPer100Response ai = aiService.chat(prompt, FoodMacroPer100Response.class, AiConfigKeys.MEAL_PLAN);
            if (ai == null || (!hasPositive(ai.getCarbsPer100g()) && !hasPositive(ai.getSugarPer100g()))) {
                return Result.error("AI估算失败");
            }
            upsertCache(normalizedName, displayName, null, ai.getCarbsPer100g(), ai.getSugarPer100g(), "ai");
            FoodMacroEstimateResponse response = new FoodMacroEstimateResponse();
            if (hasPositive(ai.getCarbsPer100g())) {
                response.setCarbsGrams(round2(ai.getCarbsPer100g() * weightGrams / 100.0));
            }
            if (hasPositive(ai.getSugarPer100g())) {
                response.setSugarGrams(round2(ai.getSugarPer100g() * weightGrams / 100.0));
            }
            response.setNote("AI估算并已缓存，仅供参考");
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("AI估算失败");
        }
    }

    private FoodNutritionCache getCacheByName(String normalizedName) {
        return foodNutritionCacheMapper.selectOne(
                new LambdaQueryWrapper<FoodNutritionCache>()
                        .eq(FoodNutritionCache::getNormalizedName, normalizedName));
    }

    private void upsertCache(String normalizedName,
                             String displayName,
                             Double caloriesPer100g,
                             Double carbsPer100g,
                             Double sugarPer100g,
                             String dataSource) {
        FoodNutritionCache existing = getCacheByName(normalizedName);
        if (existing == null) {
            FoodNutritionCache cache = new FoodNutritionCache();
            cache.setNormalizedName(normalizedName);
            cache.setDisplayName(displayName);
            cache.setCaloriesPer100g(normalizePositive(caloriesPer100g));
            cache.setCarbsPer100g(normalizeNonNegative(carbsPer100g));
            cache.setSugarPer100g(normalizeNonNegative(sugarPer100g));
            cache.setDataSource(dataSource);
            cache.setCreatedAt(LocalDateTime.now());
            cache.setUpdatedAt(LocalDateTime.now());
            foodNutritionCacheMapper.insert(cache);
            return;
        }
        if (displayName != null && !displayName.isBlank()) {
            existing.setDisplayName(displayName);
        }
        if (hasPositive(caloriesPer100g)) {
            existing.setCaloriesPer100g(caloriesPer100g);
        }
        if (carbsPer100g != null && carbsPer100g >= 0) {
            existing.setCarbsPer100g(carbsPer100g);
        }
        if (sugarPer100g != null && sugarPer100g >= 0) {
            existing.setSugarPer100g(sugarPer100g);
        }
        if (dataSource != null) {
            existing.setDataSource(dataSource);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        foodNutritionCacheMapper.updateById(existing);
    }

    private boolean allowAiEstimate(Long userId) {
        if (userId == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Deque<Long> queue = aiEstimateWindow.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > AI_ESTIMATE_WINDOW_MS) {
                queue.pollFirst();
            }
            if (queue.size() >= AI_ESTIMATE_LIMIT_PER_MINUTE) {
                return false;
            }
            queue.offerLast(now);
            return true;
        }
    }

    private String normalizeFoodName(String foodName) {
        if (foodName == null) {
            return null;
        }
        String cleaned = sanitizeFoodDisplayName(foodName);
        if (cleaned.isBlank()) {
            return null;
        }
        if (!FOOD_NAME_ALLOWED.matcher(cleaned).matches()) {
            return null;
        }
        return cleaned.toLowerCase(Locale.ROOT);
    }

    private String sanitizeFoodDisplayName(String foodName) {
        if (foodName == null) {
            return "";
        }
        String cleaned = foodName.replaceAll("[\\r\\n\\t]", " ").trim();
        cleaned = cleaned.replaceAll("\\s+", " ");
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64);
        }
        return cleaned;
    }

    private int resolveWeight(Double weightGrams) {
        if (weightGrams == null) {
            return 0;
        }
        if (weightGrams <= 0 || weightGrams > 5000) {
            return 0;
        }
        return (int) Math.round(weightGrams);
    }

    private Double normalizePositive(Double value) {
        if (value == null || value <= 0) {
            return null;
        }
        return round2(value);
    }

    private Double normalizeNonNegative(Double value) {
        if (value == null || value < 0) {
            return null;
        }
        return round2(value);
    }

    private boolean hasPositive(Double value) {
        return value != null && value > 0;
    }

    private Double round2(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private LocalDateTime parseDateTime(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(value, formatter);
    }

    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = jwtUtils.extractUsername(token);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user.getId();
    }

    private static class FoodCaloriePer100Response {
        private Double caloriesPer100g;
        private String note;

        public Double getCaloriesPer100g() {
            return caloriesPer100g;
        }

        public void setCaloriesPer100g(Double caloriesPer100g) {
            this.caloriesPer100g = caloriesPer100g;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    private static class FoodMacroPer100Response {
        private Double carbsPer100g;
        private Double sugarPer100g;
        private String note;

        public Double getCarbsPer100g() {
            return carbsPer100g;
        }

        public void setCarbsPer100g(Double carbsPer100g) {
            this.carbsPer100g = carbsPer100g;
        }

        public Double getSugarPer100g() {
            return sugarPer100g;
        }

        public void setSugarPer100g(Double sugarPer100g) {
            this.sugarPer100g = sugarPer100g;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
