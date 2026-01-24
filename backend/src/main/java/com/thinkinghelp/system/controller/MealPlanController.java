package com.thinkinghelp.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.MealPlan;
import com.thinkinghelp.system.entity.MealPlanTask;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.entity.dto.MealPlanDTO;
import com.thinkinghelp.system.entity.dto.MealPlanImportResponse;
import com.thinkinghelp.system.entity.dto.MealPlanSaveRequest;
import com.thinkinghelp.system.entity.dto.MealPlanTaskStatus;
import com.thinkinghelp.system.mapper.MealPlanMapper;
import com.thinkinghelp.system.mapper.MealPlanTaskMapper;
import com.thinkinghelp.system.mapper.UserMapper;
import com.thinkinghelp.system.service.ExportService;
import com.thinkinghelp.system.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequiredArgsConstructor
@Tag(name = "食谱生成", description = "AI 食谱生成与导出")
public class MealPlanController {

    private final ExportService exportService;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final MealPlanMapper mealPlanMapper;
    private final MealPlanTaskMapper mealPlanTaskMapper;
    private final ObjectMapper objectMapper;
    private final Executor mealPlanExecutor;

    @GetMapping("/api/meal-plan/weekly")
    @Operation(summary = "生成一周食谱")
    public Result<MealPlanDTO> generateWeekly(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        MealPlanDTO plan = exportService.generateWeeklyMealPlan(userId);
        return Result.success(plan);
    }

    @GetMapping("/api/meal-plan/generate")
    @Operation(summary = "生成指定周期食谱")
    public Result<MealPlanDTO> generateByRange(@RequestHeader("Authorization") String token,
                                               @RequestParam(defaultValue = "WEEK") String range,
                                               @RequestParam(required = false) String requirements) {
        Long userId = getUserIdFromToken(token);
        MealPlanDTO plan = exportService.generateMealPlan(userId, range, requirements);
        return Result.success(plan);
    }

    @PostMapping("/api/meal-plan/generate-async")
    @Operation(summary = "异步生成食谱")
    public Result<Map<String, Object>> generateAsync(@RequestHeader("Authorization") String token,
                                                     @RequestParam(defaultValue = "WEEK") String range,
                                                     @RequestParam(required = false) String requirements) {
        Long userId = getUserIdFromToken(token);
        MealPlanTask task = new MealPlanTask();
        task.setUserId(userId);
        task.setRangeType(range);
        task.setRequirements(requirements);
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        mealPlanTaskMapper.insert(task);

        Long taskId = task.getId();
        CompletableFuture.runAsync(() -> runTask(taskId, userId, range, requirements), mealPlanExecutor);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        return Result.success(data);
    }

    @GetMapping("/api/meal-plan/task/{id}")
    @Operation(summary = "获取食谱生成任务状态")
    public Result<MealPlanTaskStatus> getTaskStatus(@RequestHeader("Authorization") String token,
                                                    @PathVariable Long id) {
        Long userId = getUserIdFromToken(token);
        MealPlanTask task = mealPlanTaskMapper.selectById(id);
        if (task == null || !task.getUserId().equals(userId)) {
            return Result.error("任务不存在");
        }
        MealPlanTaskStatus status = new MealPlanTaskStatus();
        status.setId(task.getId());
        status.setStatus(task.getStatus());
        status.setRangeType(task.getRangeType());
        status.setErrorMessage(task.getErrorMessage());
        if ("SUCCESS".equalsIgnoreCase(task.getStatus()) && task.getPlanJson() != null) {
            try {
                status.setPlan(objectMapper.readValue(task.getPlanJson(), MealPlanDTO.class));
            } catch (Exception ignored) {
            }
        }
        return Result.success(status);
    }

    @PostMapping("/api/meal-plan")
    @Operation(summary = "保存食谱")
    public Result<String> savePlan(@RequestHeader("Authorization") String token,
                                   @RequestBody MealPlanSaveRequest request) {
        if (request.getPlan() == null) {
            return Result.error("食谱内容不能为空");
        }
        Long userId = getUserIdFromToken(token);
        try {
            MealPlan plan = new MealPlan();
            plan.setUserId(userId);
            plan.setRangeType(request.getRangeType() == null ? "WEEK" : request.getRangeType());
            plan.setTitle(request.getPlan().getTitle());
            plan.setAdvice(request.getPlan().getAdvice());
            plan.setPlanJson(objectMapper.writeValueAsString(request.getPlan()));
            plan.setCreatedAt(LocalDateTime.now());
            mealPlanMapper.insert(plan);
            return Result.success("保存成功");
        } catch (Exception e) {
            return Result.error("保存失败");
        }
    }

    @GetMapping("/api/meal-plan")
    @Operation(summary = "获取已保存食谱列表")
    public Result<List<MealPlan>> listPlans(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        List<MealPlan> plans = mealPlanMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MealPlan>()
                        .eq(MealPlan::getUserId, userId)
                        .orderByDesc(MealPlan::getCreatedAt));
        return Result.success(plans.stream().peek(p -> p.setPlanJson(null)).collect(Collectors.toList()));
    }

    @PostMapping("/api/meal-plan/import")
    @Operation(summary = "导入食谱（Excel）")
    public Result<MealPlanImportResponse> importPlan(@RequestHeader("Authorization") String token,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam(required = false) String title,
                                                     @RequestParam(required = false) String rangeType) {
        if (file == null || file.isEmpty()) {
            return Result.error("请上传 Excel 文件");
        }
        Long userId = getUserIdFromToken(token);
        try {
            String filename = file.getOriginalFilename();
            boolean isPdf = filename != null && filename.toLowerCase().endsWith(".pdf");
            ParsedPlan parsed = isPdf ? parsePdf(file) : parseExcel(file);
            if (parsed.plan.getWeeklyPlan() == null || parsed.plan.getWeeklyPlan().isEmpty()) {
                return Result.error("未识别到有效食谱内容");
            }
            String finalTitle = (title != null && !title.isBlank())
                    ? title
                    : (parsed.title != null && !parsed.title.isBlank() ? parsed.title : inferTitle(filename));
            parsed.plan.setTitle(finalTitle);
            if (parsed.advice != null && !parsed.advice.isBlank()) {
                parsed.plan.setAdvice(parsed.advice);
            } else {
                parsed.plan.setAdvice("导入食谱，请结合自身情况参考执行。");
            }

            String finalRange = resolveRangeType(rangeType, parsed.dayCount, parsed.rowCount);

            MealPlan plan = new MealPlan();
            plan.setUserId(userId);
            plan.setRangeType(finalRange);
            plan.setTitle(parsed.plan.getTitle());
            plan.setAdvice(parsed.plan.getAdvice());
            plan.setPlanJson(objectMapper.writeValueAsString(parsed.plan));
            plan.setCreatedAt(LocalDateTime.now());
            mealPlanMapper.insert(plan);

            MealPlanImportResponse response = new MealPlanImportResponse();
            response.setPlanId(plan.getId());
            response.setRangeType(finalRange);
            response.setPlan(parsed.plan);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/api/meal-plan/{id}")
    @Operation(summary = "获取食谱详情")
    public Result<MealPlanDTO> getPlan(@RequestHeader("Authorization") String token,
                                       @PathVariable Long id) {
        Long userId = getUserIdFromToken(token);
        MealPlan plan = mealPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return Result.error("食谱不存在");
        }
        try {
            MealPlanDTO dto = objectMapper.readValue(plan.getPlanJson(), MealPlanDTO.class);
            return Result.success(dto);
        } catch (Exception e) {
            return Result.error("解析失败");
        }
    }

    @GetMapping("/api/export/pdf")
    @Operation(summary = "导出食谱 PDF")
    public void exportPdf(@RequestHeader("Authorization") String token,
                          @RequestParam(required = false) Long planId,
                          @RequestParam(required = false, defaultValue = "WEEK") String range,
                          HttpServletResponse response) throws IOException {
        Long userId = getUserIdFromToken(token);
        MealPlanDTO plan;
        if (planId != null) {
            MealPlan saved = mealPlanMapper.selectById(planId);
            if (saved == null || !saved.getUserId().equals(userId)) {
                response.setStatus(404);
                return;
            }
            try {
                plan = objectMapper.readValue(saved.getPlanJson(), MealPlanDTO.class);
            } catch (Exception e) {
                response.setStatus(500);
                return;
            }
        } else {
            plan = exportService.generateMealPlan(userId, range);
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"meal_plan.pdf\"");
        exportService.exportMealPlanToPdf(plan, response.getOutputStream());
    }

    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = jwtUtils.extractUsername(token);
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
        return user.getId();
    }

    private void runTask(Long taskId, Long userId, String range, String requirements) {
        updateTask(taskId, "RUNNING", null, null);
        try {
            MealPlanDTO plan = exportService.generateMealPlan(userId, range, requirements);
            String planJson = objectMapper.writeValueAsString(plan);
            updateTask(taskId, "SUCCESS", planJson, null);
        } catch (Exception e) {
            updateTask(taskId, "FAILED", null, e.getMessage());
        }
    }

    private void updateTask(Long taskId, String status, String planJson, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MealPlanTask> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        wrapper.eq(MealPlanTask::getId, taskId)
                .set(MealPlanTask::getStatus, status)
                .set(MealPlanTask::getUpdatedAt, now);
        if (planJson != null) {
            wrapper.set(MealPlanTask::getPlanJson, planJson);
        }
        if (errorMessage != null) {
            wrapper.set(MealPlanTask::getErrorMessage, errorMessage);
        }
        mealPlanTaskMapper.update(null, wrapper);
    }

    private ParsedPlan parseExcel(MultipartFile file) throws IOException {
        ParsedPlan result = new ParsedPlan();
        Map<String, MealPlanDTO.DailyPlan> dayMap = new LinkedHashMap<>();
        int rowCount = 0;
        int autoDayIndex = 1;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IOException("Excel 为空");
            }
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String day = getCellString(row.getCell(0), formatter);
                String meal = getCellString(row.getCell(1), formatter);
                String foods = getCellString(row.getCell(2), formatter);
                String calories = getCellString(row.getCell(3), formatter);

                if (foods.isBlank() || meal.isBlank()) {
                    continue;
                }
                String normalizedMeal = normalizeMealType(meal);
                if (normalizedMeal == null) {
                    continue;
                }
                if (day.isBlank()) {
                    day = "第" + autoDayIndex + "天";
                    autoDayIndex++;
                }
                MealPlanDTO.DailyPlan dayPlan = dayMap.computeIfAbsent(day, key -> {
                    MealPlanDTO.DailyPlan plan = new MealPlanDTO.DailyPlan();
                    plan.setDay(key);
                    return plan;
                });

                MealPlanDTO.Meal mealPlan = new MealPlanDTO.Meal();
                mealPlan.setName(foods);
                mealPlan.setFoods(splitFoods(foods));
                mealPlan.setCalories(calories);

                if ("早餐".equals(normalizedMeal)) {
                    dayPlan.setBreakfast(mealPlan);
                } else if ("午餐".equals(normalizedMeal)) {
                    dayPlan.setLunch(mealPlan);
                } else if ("晚餐".equals(normalizedMeal)) {
                    dayPlan.setDinner(mealPlan);
                } else if ("加餐".equals(normalizedMeal)) {
                    dayPlan.setSnack(mealPlan);
                }
                rowCount++;
            }
        }

        MealPlanDTO plan = new MealPlanDTO();
        plan.setWeeklyPlan(new ArrayList<>(dayMap.values()));
        plan.setShoppingList(Collections.emptyMap());

        result.plan = plan;
        result.rowCount = rowCount;
        result.dayCount = dayMap.size();
        return result;
    }

    private ParsedPlan parsePdf(MultipartFile file) throws IOException {
        ParsedPlan result = new ParsedPlan();
        List<String> lines = new ArrayList<>();
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            for (String line : text.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        }

        String title = null;
        String advice = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.equalsIgnoreCase("Weekly Schedule") || line.equalsIgnoreCase("Shopping List")) {
                continue;
            }
            if (line.startsWith("Expert Advice")) {
                int idx = line.indexOf(':');
                if (idx >= 0 && idx + 1 < line.length()) {
                    advice = line.substring(idx + 1).trim();
                } else if (i + 1 < lines.size()) {
                    advice = lines.get(i + 1).trim();
                }
                continue;
            }
            if (title == null) {
                title = line;
            }
        }

        int scheduleStart = 0;
        int scheduleEnd = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.equalsIgnoreCase("Weekly Schedule")) {
                scheduleStart = i + 1;
            } else if (line.equalsIgnoreCase("Shopping List")) {
                scheduleEnd = i;
                break;
            }
        }

        Map<String, MealPlanDTO.DailyPlan> dayMap = new LinkedHashMap<>();
        MealPlanDTO.DailyPlan current = null;
        int mealIndex = 0;
        for (int i = scheduleStart; i < scheduleEnd; i++) {
            String line = lines.get(i);
            if (isHeaderLine(line)) {
                continue;
            }
            if (isDayLabel(line)) {
                current = dayMap.computeIfAbsent(line, key -> {
                    MealPlanDTO.DailyPlan plan = new MealPlanDTO.DailyPlan();
                    plan.setDay(key);
                    return plan;
                });
                mealIndex = 0;
                continue;
            }
            if (current == null || isCalorieLine(line)) {
                continue;
            }
            if (mealIndex > 2) {
                continue;
            }
            String calories = null;
            if (i + 1 < scheduleEnd && isCalorieLine(lines.get(i + 1))) {
                calories = lines.get(i + 1);
                i++;
            }
            MealPlanDTO.Meal meal = new MealPlanDTO.Meal();
            meal.setName(line);
            meal.setFoods(splitFoods(line));
            meal.setCalories(calories);
            if (mealIndex == 0) {
                current.setBreakfast(meal);
            } else if (mealIndex == 1) {
                current.setLunch(meal);
            } else if (mealIndex == 2) {
                current.setDinner(meal);
            }
            mealIndex++;
        }

        Map<String, String> shoppingList = new LinkedHashMap<>();
        boolean inShopping = false;
        for (String line : lines) {
            if (line.equalsIgnoreCase("Shopping List")) {
                inShopping = true;
                continue;
            }
            if (!inShopping) continue;
            String[] parts = line.split("[:：]", 2);
            if (parts.length == 2) {
                shoppingList.put(parts[0].trim(), parts[1].trim());
            }
        }

        MealPlanDTO plan = new MealPlanDTO();
        plan.setWeeklyPlan(new ArrayList<>(dayMap.values()));
        plan.setShoppingList(shoppingList);

        result.plan = plan;
        result.rowCount = dayMap.size();
        result.dayCount = dayMap.size();
        result.title = title;
        result.advice = advice;
        return result;
    }

    private String getCellString(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private String normalizeMealType(String raw) {
        String value = raw.trim();
        if (value.isBlank()) return null;
        if (value.contains("早")) return "早餐";
        if (value.contains("午")) return "午餐";
        if (value.contains("晚")) return "晚餐";
        if (value.contains("加")) return "加餐";
        return null;
    }

    private boolean isHeaderLine(String line) {
        String lower = line.toLowerCase();
        if (lower.contains("day") && lower.contains("breakfast")) return true;
        return "day".equalsIgnoreCase(line)
                || "breakfast".equalsIgnoreCase(line)
                || "lunch".equalsIgnoreCase(line)
                || "dinner".equalsIgnoreCase(line)
                || "weekly schedule".equalsIgnoreCase(line)
                || "shopping list".equalsIgnoreCase(line);
    }

    private boolean isCalorieLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("kcal") || lower.contains("千卡");
    }

    private boolean isDayLabel(String line) {
        if (line.matches("^(周|星期)[一二三四五六日天]$")) {
            return true;
        }
        if (line.matches("^第\\d+天$")) {
            return true;
        }
        return line.matches("(?i)^day\\s*\\d+$");
    }

    private List<String> splitFoods(String foods) {
        if (foods == null || foods.isBlank()) return Collections.emptyList();
        String[] parts = foods.split("[、,;|/]");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private String inferTitle(String filename) {
        if (filename == null || filename.isBlank()) {
            return "导入食谱";
        }
        int idx = filename.lastIndexOf('.');
        if (idx > 0) {
            return filename.substring(0, idx);
        }
        return filename;
    }

    private String resolveRangeType(String input, int dayCount, int rowCount) {
        if (input != null && !input.isBlank()) {
            return input.toUpperCase();
        }
        if (rowCount <= 1) {
            return "MEAL";
        }
        if (dayCount == 1) {
            return "DAY";
        }
        if (dayCount == 3) {
            return "THREE_DAYS";
        }
        if (dayCount >= 7) {
            return "WEEK";
        }
        return "DAY";
    }

    private static class ParsedPlan {
        private MealPlanDTO plan;
        private int rowCount;
        private int dayCount;
        private String title;
        private String advice;
    }
}
