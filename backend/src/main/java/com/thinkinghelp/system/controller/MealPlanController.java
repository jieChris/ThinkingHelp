package com.thinkinghelp.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.MealPlan;
import com.thinkinghelp.system.entity.MealPlanFeedback;
import com.thinkinghelp.system.entity.MealPlanTask;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.entity.dto.MealPlanDTO;
import com.thinkinghelp.system.entity.dto.MealPlanFeedbackRequest;
import com.thinkinghelp.system.entity.dto.MealPlanFeedbackSummaryDTO;
import com.thinkinghelp.system.entity.dto.MealPlanImportResponse;
import com.thinkinghelp.system.entity.dto.MealPlanPageResultDTO;
import com.thinkinghelp.system.entity.dto.MealPlanSaveRequest;
import com.thinkinghelp.system.entity.dto.MealPlanTaskStatus;
import com.thinkinghelp.system.mapper.MealPlanFeedbackMapper;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequiredArgsConstructor
@Tag(name = "食谱生成", description = "智能食谱生成与导出")
public class MealPlanController {

    private final ExportService exportService;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final MealPlanMapper mealPlanMapper;
    private final MealPlanTaskMapper mealPlanTaskMapper;
    private final MealPlanFeedbackMapper mealPlanFeedbackMapper;
    private final ObjectMapper objectMapper;
    private final Executor mealPlanExecutor;

    @GetMapping("/api/meal-plan/weekly")
    @Operation(summary = "生成一周食谱")
    public Result<MealPlanDTO> generateWeekly(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        MealPlanDTO plan = exportService.generateWeeklyMealPlan(userId);
        persistGeneratedPlan(userId, "WEEK", plan);
        return Result.success(plan);
    }

    @GetMapping("/api/meal-plan/generate")
    @Operation(summary = "生成指定周期食谱")
    public Result<MealPlanDTO> generateByRange(@RequestHeader("Authorization") String token,
                                               @RequestParam(defaultValue = "WEEK") String range,
                                               @RequestParam(required = false) String requirements) {
        Long userId = getUserIdFromToken(token);
        MealPlanDTO plan = exportService.generateMealPlan(userId, range, requirements);
        persistGeneratedPlan(userId, range, plan);
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

    @GetMapping("/api/meal-plan/requirements/history")
    @Operation(summary = "获取历史生成需求（去重）")
    public Result<List<String>> getRequirementHistory(@RequestHeader("Authorization") String token,
                                                      @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = getUserIdFromToken(token);
        int safeLimit = limit == null ? 10 : Math.max(1, Math.min(limit, 30));

        List<MealPlanTask> tasks = mealPlanTaskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MealPlanTask>()
                        .eq(MealPlanTask::getUserId, userId)
                        .isNotNull(MealPlanTask::getRequirements)
                        .ne(MealPlanTask::getRequirements, "")
                        .orderByDesc(MealPlanTask::getCreatedAt)
                        .last("LIMIT 100"));

        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (MealPlanTask task : tasks) {
            if (task == null || task.getRequirements() == null) continue;
            String text = task.getRequirements().trim();
            if (text.isEmpty()) continue;
            unique.add(text);
            if (unique.size() >= safeLimit) {
                break;
            }
        }
        return Result.success(new ArrayList<>(unique));
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
        List<MealPlan> result = plans.stream().peek(this::attachAdjustmentSummary).peek(p -> p.setPlanJson(null)).collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/api/meal-plan/page")
    @Operation(summary = "按条件分页获取已保存食谱")
    public Result<MealPlanPageResultDTO> pagePlans(@RequestHeader("Authorization") String token,
                                                   @RequestParam(defaultValue = "ALL") String status,
                                                   @RequestParam(defaultValue = "ALL") String time,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "DESC") String sort,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = getUserIdFromToken(token);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 50));
        String normalizedStatus = normalizePlanStatus(status);
        String normalizedTime = normalizePlanTime(time);
        String normalizedSort = normalizePlanSort(sort);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MealPlan> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MealPlan>()
                        .eq(MealPlan::getUserId, userId);
        applyTimeFilter(wrapper, normalizedTime);
        if ("ASC".equals(normalizedSort)) {
            wrapper.orderByAsc(MealPlan::getCreatedAt);
        } else {
            wrapper.orderByDesc(MealPlan::getCreatedAt);
        }

        List<MealPlan> plans = mealPlanMapper.selectList(wrapper);
        List<MealPlan> filtered = plans.stream().peek(this::attachAdjustmentSummary).filter(item -> {
            if (!matchPlanStatus(item, normalizedStatus)) {
                return false;
            }
            return matchPlanKeyword(item, normalizedKeyword);
        }).peek(p -> p.setPlanJson(null)).collect(Collectors.toList());

        long total = filtered.size();
        int from = (safePage - 1) * safePageSize;
        int to = Math.min(from + safePageSize, filtered.size());
        List<MealPlan> pageRecords = from >= filtered.size()
                ? Collections.emptyList()
                : new ArrayList<>(filtered.subList(from, to));

        MealPlanPageResultDTO result = new MealPlanPageResultDTO();
        result.setRecords(pageRecords);
        result.setTotal(total);
        result.setPage(safePage);
        result.setPageSize(safePageSize);
        return Result.success(result);
    }

    @PostMapping("/api/meal-plan/feedback")
    @Operation(summary = "记录食谱反馈")
    public Result<String> saveFeedback(@RequestHeader("Authorization") String token,
                                       @RequestBody MealPlanFeedbackRequest request) {
        Long userId = getUserIdFromToken(token);
        String mealType = normalizeFeedbackMealType(request.getMealType());
        String direction = normalizeFeedbackDirection(request.getDirection());
        if (mealType == null || direction == null) {
            return Result.error("反馈参数无效");
        }
        MealPlanFeedback feedback = new MealPlanFeedback();
        feedback.setUserId(userId);
        feedback.setMealType(mealType);
        feedback.setDirection(direction);
        feedback.setNote(request.getNote());
        feedback.setCreatedAt(LocalDateTime.now());
        mealPlanFeedbackMapper.insert(feedback);
        return Result.success("反馈已记录");
    }

    @GetMapping("/api/meal-plan/feedback/summary")
    @Operation(summary = "获取食谱反馈汇总（用于生成前预览）")
    public Result<MealPlanFeedbackSummaryDTO> feedbackSummary(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<MealPlanFeedback> feedbacks = mealPlanFeedbackMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MealPlanFeedback>()
                        .eq(MealPlanFeedback::getUserId, userId)
                        .ge(MealPlanFeedback::getCreatedAt, since)
                        .orderByDesc(MealPlanFeedback::getCreatedAt)
        );
        MealPlanFeedbackSummaryDTO summary = buildFeedbackSummary(feedbacks);
        return Result.success(summary);
    }

    @PostMapping("/api/meal-plan/import")
    @Operation(summary = "导入食谱（表格）")
    public Result<MealPlanImportResponse> importPlan(@RequestHeader("Authorization") String token,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam(required = false) String title,
                                                     @RequestParam(required = false) String rangeType) {
        if (file == null || file.isEmpty()) {
            return Result.error("请上传表格文件");
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
            exportService.enrichMealPlan(dto);
            return Result.success(dto);
        } catch (Exception e) {
            return Result.error("解析失败");
        }
    }

    @DeleteMapping("/api/meal-plan/{id}")
    @Operation(summary = "删除已保存食谱")
    public Result<String> deletePlan(@RequestHeader("Authorization") String token,
                                     @PathVariable Long id) {
        Long userId = getUserIdFromToken(token);
        MealPlan plan = mealPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return Result.error("食谱不存在");
        }
        int affected = mealPlanMapper.deleteById(id);
        if (affected > 0) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    @GetMapping("/api/export/pdf")
    @Operation(summary = "导出食谱文档")
    public void exportPdf(@RequestHeader(value = "Authorization", required = false) String token,
                          @RequestParam(required = false) Long planId,
                          @RequestParam(required = false, defaultValue = "WEEK") String range,
                          HttpServletResponse response) throws IOException {
        try {
            Long userId = tryGetUserIdFromToken(token);
            if (userId == null) {
                response.sendError(401, "未登录或登录已过期");
                return;
            }
            MealPlanDTO plan;
            if (planId != null) {
                MealPlan saved = mealPlanMapper.selectById(planId);
                if (saved == null || !saved.getUserId().equals(userId)) {
                    response.sendError(404, "食谱不存在");
                    return;
                }
                try {
                    plan = objectMapper.readValue(saved.getPlanJson(), MealPlanDTO.class);
                    exportService.enrichMealPlan(plan);
                } catch (Exception e) {
                    response.sendError(500, "保存食谱解析失败");
                    return;
                }
            } else {
                plan = exportService.generateMealPlan(userId, range);
            }

            response.setContentType("application/pdf");
            String fileName = URLEncoder.encode("食谱.pdf", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            exportService.exportMealPlanToPdf(plan, response.getOutputStream());
        } catch (Exception e) {
            if (!response.isCommitted()) {
                response.reset();
                response.sendError(500, "导出失败，请稍后重试");
            }
        }
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

    private Long tryGetUserIdFromToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }
            return getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeFeedbackMealType(String mealType) {
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

    private String normalizeFeedbackDirection(String direction) {
        if (direction == null) return null;
        String value = direction.trim().toUpperCase();
        if ("MORE".equals(value) || "LESS".equals(value)) {
            return value;
        }
        return null;
    }

    private String normalizePlanStatus(String status) {
        if (status == null) return "ALL";
        String value = status.trim().toUpperCase();
        if ("ADJUSTED".equals(value) || "PLAIN".equals(value)) {
            return value;
        }
        return "ALL";
    }

    private String normalizePlanTime(String time) {
        if (time == null) return "ALL";
        String value = time.trim().toUpperCase();
        if ("WEEK".equals(value) || "DAYS_30".equals(value)) {
            return value;
        }
        return "ALL";
    }

    private String normalizePlanSort(String sort) {
        if (sort == null) return "DESC";
        String value = sort.trim().toUpperCase();
        if ("ASC".equals(value)) {
            return "ASC";
        }
        return "DESC";
    }

    private void applyTimeFilter(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MealPlan> wrapper,
                                 String normalizedTime) {
        if ("WEEK".equals(normalizedTime)) {
            LocalDateTime now = LocalDateTime.now();
            int dayOffset = now.getDayOfWeek().getValue() - 1;
            LocalDateTime weekStart = now.toLocalDate().minusDays(dayOffset).atStartOfDay();
            wrapper.ge(MealPlan::getCreatedAt, weekStart);
            return;
        }
        if ("DAYS_30".equals(normalizedTime)) {
            wrapper.ge(MealPlan::getCreatedAt, LocalDateTime.now().minusDays(30));
        }
    }

    private boolean matchPlanStatus(MealPlan plan, String normalizedStatus) {
        if ("ADJUSTED".equals(normalizedStatus)) {
            return Boolean.TRUE.equals(plan.getHasAdjustment());
        }
        if ("PLAIN".equals(normalizedStatus)) {
            return !Boolean.TRUE.equals(plan.getHasAdjustment());
        }
        return true;
    }

    private boolean matchPlanKeyword(MealPlan plan, String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return true;
        }
        String title = plan.getTitle() == null ? "" : plan.getTitle().toLowerCase();
        String summary = plan.getAdjustmentSummary() == null ? "" : plan.getAdjustmentSummary().toLowerCase();
        return title.contains(normalizedKeyword) || summary.contains(normalizedKeyword);
    }

    private void attachAdjustmentSummary(MealPlan mealPlan) {
        mealPlan.setHasAdjustment(false);
        mealPlan.setAdjustmentSummary("");
        if (mealPlan.getPlanJson() == null || mealPlan.getPlanJson().isBlank()) {
            return;
        }
        try {
            MealPlanDTO dto = objectMapper.readValue(mealPlan.getPlanJson(), MealPlanDTO.class);
            List<String> notes = dto.getAdjustmentNotes();
            if (notes == null || notes.isEmpty()) {
                return;
            }
            mealPlan.setHasAdjustment(true);
            if (notes.size() <= 2) {
                mealPlan.setAdjustmentSummary(String.join("；", notes));
            } else {
                mealPlan.setAdjustmentSummary(String.join("；", notes.subList(0, 2)) + " 等" + notes.size() + "项");
            }
        } catch (Exception ignored) {
        }
    }

    private MealPlanFeedbackSummaryDTO buildFeedbackSummary(List<MealPlanFeedback> feedbacks) {
        MealPlanFeedbackSummaryDTO summary = new MealPlanFeedbackSummaryDTO();
        if (feedbacks == null || feedbacks.isEmpty()) {
            summary.setRecentFeedbackCount(0);
            summary.setFeedbackText("");
            return summary;
        }
        summary.setRecentFeedbackCount(feedbacks.size());
        Map<String, int[]> stats = new HashMap<>();
        for (MealPlanFeedback item : feedbacks) {
            String mealType = normalizeFeedbackMealType(item.getMealType());
            if (mealType == null) continue;
            int[] count = stats.computeIfAbsent(mealType, key -> new int[2]);
            if ("MORE".equalsIgnoreCase(item.getDirection())) {
                count[0]++;
            } else if ("LESS".equalsIgnoreCase(item.getDirection())) {
                count[1]++;
            }
        }
        if (stats.isEmpty()) {
            summary.setFeedbackText("");
            return summary;
        }
        List<String> parts = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        List<String> order = Arrays.asList("BREAKFAST", "LUNCH", "DINNER", "SNACK");
        for (String meal : order) {
            int[] count = stats.get(meal);
            if (count == null) continue;
            String label = feedbackMealLabel(meal);
            int diff = count[0] - count[1];
            if (diff > 0) {
                parts.add(label + "量偏少");
                int percent = diff >= 3 ? 15 : 10;
                notes.add(label + "预计上调约" + percent + "%");
            } else if (diff < 0) {
                parts.add(label + "量偏多");
                int percent = Math.abs(diff) >= 3 ? 15 : 10;
                notes.add(label + "预计下调约" + percent + "%");
            }
        }
        summary.setFeedbackText(String.join("；", parts));
        summary.setNotes(notes);
        return summary;
    }

    private String feedbackMealLabel(String mealType) {
        if (mealType == null) return "餐次";
        switch (mealType.toUpperCase()) {
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

    private void runTask(Long taskId, Long userId, String range, String requirements) {
        updateTask(taskId, "RUNNING", null, null);
        try {
            MealPlanDTO plan = exportService.generateMealPlan(userId, range, requirements);
            persistGeneratedPlan(userId, range, plan);
            String planJson = objectMapper.writeValueAsString(plan);
            updateTask(taskId, "SUCCESS", planJson, null);
        } catch (Exception e) {
            updateTask(taskId, "FAILED", null, e.getMessage());
        }
    }

    private void persistGeneratedPlan(Long userId, String range, MealPlanDTO plan) {
        if (userId == null || plan == null) {
            throw new IllegalArgumentException("食谱保存参数缺失");
        }
        try {
            MealPlan entity = new MealPlan();
            entity.setUserId(userId);
            entity.setRangeType(range == null || range.isBlank() ? "WEEK" : range.toUpperCase(Locale.ROOT));
            String title = plan.getTitle();
            if (title == null || title.isBlank()) {
                title = "智能生成食谱";
                plan.setTitle(title);
            }
            entity.setTitle(title);
            entity.setAdvice(plan.getAdvice());
            entity.setPlanJson(objectMapper.writeValueAsString(plan));
            entity.setCreatedAt(LocalDateTime.now());
            mealPlanMapper.insert(entity);
        } catch (Exception e) {
            throw new RuntimeException("自动保存食谱失败: " + e.getMessage(), e);
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
            if (isScheduleHeaderLine(line) || isShoppingHeaderLine(line)) {
                continue;
            }
            if (line.startsWith("Expert Advice") || line.startsWith("营养建议")) {
                int idx = line.indexOf(':');
                if (idx < 0) {
                    idx = line.indexOf('：');
                }
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
            if (isScheduleHeaderLine(line)) {
                scheduleStart = i + 1;
            } else if (isShoppingHeaderLine(line)) {
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
            if (isShoppingHeaderLine(line)) {
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
                || "日期".equals(line)
                || "早餐".equals(line)
                || "午餐".equals(line)
                || "晚餐".equals(line)
                || isScheduleHeaderLine(line)
                || isShoppingHeaderLine(line);
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

    private boolean isScheduleHeaderLine(String line) {
        return "weekly schedule".equalsIgnoreCase(line) || "每周食谱安排".equals(line);
    }

    private boolean isShoppingHeaderLine(String line) {
        return "shopping list".equalsIgnoreCase(line) || "每日采购清单".equals(line);
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
