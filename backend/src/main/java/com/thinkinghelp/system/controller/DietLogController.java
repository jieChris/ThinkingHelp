package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.DietLog;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.entity.dto.FoodCalorieEstimateRequest;
import com.thinkinghelp.system.entity.dto.FoodCalorieEstimateResponse;
import com.thinkinghelp.system.entity.dto.FoodMacroEstimateRequest;
import com.thinkinghelp.system.entity.dto.FoodMacroEstimateResponse;
import com.thinkinghelp.system.mapper.DietLogMapper;
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
import java.util.List;

@RestController
@RequestMapping("/api/diet/logs")
@RequiredArgsConstructor
@Tag(name = "饮食记录", description = "用户饮食记录")
public class DietLogController {

    private final DietLogMapper dietLogMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final AIService aiService;

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
        if (request.getFoodName() == null || request.getFoodName().isBlank()) {
            return Result.error("食物名称不能为空");
        }
        if (request.getWeightGrams() == null || request.getWeightGrams() <= 0) {
            return Result.error("重量必须大于0");
        }

        String prompt = "请估算以下食物的热量(kcal)，仅输出严格 JSON：{ \"calories\": number, \"note\": string }。\n" +
                "食物：" + request.getFoodName() + "\n" +
                "重量：" + request.getWeightGrams() + " 克\n" +
                "要求：calories 为该份量的总热量，note 简短说明(如: AI估算，仅供参考)。";

        try {
            FoodCalorieEstimateResponse response = aiService.chat(prompt, FoodCalorieEstimateResponse.class, AiConfigKeys.MEAL_PLAN);
            if (response.getCalories() == null) {
                return Result.error("AI估算失败");
            }
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
        if (request.getFoodName() == null || request.getFoodName().isBlank()) {
            return Result.error("食物名称不能为空");
        }
        if (request.getWeightGrams() == null || request.getWeightGrams() <= 0) {
            return Result.error("重量必须大于0");
        }

        String prompt = "请估算以下食物的碳水和糖含量(克)，仅输出严格 JSON：{ \"carbsGrams\": number, \"sugarGrams\": number, \"note\": string }。\n" +
                "食物：" + request.getFoodName() + "\n" +
                "重量：" + request.getWeightGrams() + " 克\n" +
                "要求：carbsGrams/sugarGrams 为该份量总克数，note 简短说明(如: AI估算，仅供参考)。";

        try {
            FoodMacroEstimateResponse response = aiService.chat(prompt, FoodMacroEstimateResponse.class, AiConfigKeys.MEAL_PLAN);
            if (response.getCarbsGrams() == null && response.getSugarGrams() == null) {
                return Result.error("AI估算失败");
            }
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("AI估算失败");
        }
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
}
