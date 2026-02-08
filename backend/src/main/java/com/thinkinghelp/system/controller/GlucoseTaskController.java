package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.GlucoseFollowupTask;
import com.thinkinghelp.system.entity.GlucoseRecord;
import com.thinkinghelp.system.mapper.GlucoseFollowupTaskMapper;
import com.thinkinghelp.system.mapper.GlucoseRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/health/glucose-tasks")
@RequiredArgsConstructor
@Tag(name = "控糖随访任务", description = "控糖提醒任务管理")
public class GlucoseTaskController {

    private final GlucoseFollowupTaskMapper taskMapper;
    private final GlucoseRecordMapper glucoseRecordMapper;

    @GetMapping
    @Operation(summary = "获取任务列表")
    public Result<List<GlucoseFollowupTask>> listTasks(@RequestParam Long userId,
                                                       @RequestParam(required = false) String status) {
        LambdaQueryWrapper<GlucoseFollowupTask> query = new LambdaQueryWrapper<>();
        query.eq(GlucoseFollowupTask::getUserId, userId);
        if (status != null && !status.isBlank()) {
            query.eq(GlucoseFollowupTask::getStatus, status.trim().toUpperCase());
        }
        query.orderByAsc(GlucoseFollowupTask::getStatus)
                .orderByAsc(GlucoseFollowupTask::getDueAt)
                .orderByDesc(GlucoseFollowupTask::getCreatedAt);
        return Result.success(taskMapper.selectList(query));
    }

    @PostMapping
    @Operation(summary = "新增任务")
    public Result<String> createTask(@RequestBody GlucoseFollowupTask task) {
        if (task.getUserId() == null) {
            return Result.error("缺少用户ID");
        }
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            return Result.error("任务标题不能为空");
        }
        if (task.getStatus() == null || task.getStatus().isBlank()) {
            task.setStatus("PENDING");
        }
        if (task.getDueAt() == null) {
            task.setDueAt(LocalDateTime.now().plusDays(1));
        }
        if (task.getTaskType() == null || task.getTaskType().isBlank()) {
            task.setTaskType("FOLLOWUP");
        }
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return Result.success("创建成功");
    }

    @PostMapping("/auto-generate")
    @Operation(summary = "自动生成控糖随访任务")
    public Result<List<GlucoseFollowupTask>> autoGenerate(@RequestParam Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(7);
        List<GlucoseRecord> records = glucoseRecordMapper.selectList(
                new LambdaQueryWrapper<GlucoseRecord>()
                        .eq(GlucoseRecord::getUserId, userId)
                        .ge(GlucoseRecord::getRecordedAt, since)
                        .orderByDesc(GlucoseRecord::getRecordedAt)
        );

        int highCount = 0;
        int lowCount = 0;
        GlucoseRecord latest = records.isEmpty() ? null : records.get(0);
        for (GlucoseRecord record : records) {
            if (record.getGlucoseValue() == null) continue;
            String eventType = resolveEventType(record);
            if ("HIGH".equals(eventType)) highCount++;
            if ("LOW".equals(eventType)) lowCount++;
        }

        List<GlucoseFollowupTask> created = new ArrayList<>();
        if (latest != null && "HIGH".equals(resolveEventType(latest))) {
            maybeCreateTask(created, userId, "RETEST",
                    "24小时内复测空腹血糖",
                    "最近一次血糖偏高，建议次日空腹复测并记录。",
                    now.plusDays(1));
        }
        if (highCount >= 3) {
            maybeCreateTask(created, userId, "DIET_REVIEW",
                    "3天内复盘高碳水餐次",
                    "近7天高血糖次数较多，建议回顾主食份量与进餐结构。",
                    now.plusDays(3));
        }
        if (lowCount >= 1) {
            maybeCreateTask(created, userId, "LOW_ALERT",
                    "检查低血糖诱因并准备应急糖",
                    "近7天出现低血糖，建议补充应急处理方案并记录诱因。",
                    now.plusDays(1));
        }

        return Result.success(created);
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "完成任务")
    public Result<String> completeTask(@PathVariable Long id) {
        GlucoseFollowupTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        task.setStatus("DONE");
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return Result.success("已完成");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    public Result<String> deleteTask(@PathVariable Long id) {
        taskMapper.deleteById(id);
        return Result.success("删除成功");
    }

    private void maybeCreateTask(List<GlucoseFollowupTask> created,
                                 Long userId,
                                 String taskType,
                                 String title,
                                 String note,
                                 LocalDateTime dueAt) {
        Long exists = taskMapper.selectCount(new LambdaQueryWrapper<GlucoseFollowupTask>()
                .eq(GlucoseFollowupTask::getUserId, userId)
                .eq(GlucoseFollowupTask::getTaskType, taskType)
                .eq(GlucoseFollowupTask::getStatus, "PENDING"));
        if (exists != null && exists > 0) {
            return;
        }
        GlucoseFollowupTask task = new GlucoseFollowupTask();
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setTitle(title);
        task.setNote(note);
        task.setStatus("PENDING");
        task.setDueAt(dueAt);
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        created.add(task);
    }

    private String resolveEventType(GlucoseRecord record) {
        if (record.getEventType() != null && !record.getEventType().isBlank()) {
            return record.getEventType().trim().toUpperCase();
        }
        if (record.getGlucoseValue() == null) {
            return "NORMAL";
        }
        String measureType = record.getMeasureType() == null ? "FASTING" : record.getMeasureType().toUpperCase();
        if ("POST_MEAL_2H".equals(measureType)) {
            if (record.getGlucoseValue() < 3.9) return "LOW";
            if (record.getGlucoseValue() >= 11.1) return "HIGH";
            return "NORMAL";
        }
        if (record.getGlucoseValue() < 3.9) return "LOW";
        if (record.getGlucoseValue() >= 7.0) return "HIGH";
        return "NORMAL";
    }
}
