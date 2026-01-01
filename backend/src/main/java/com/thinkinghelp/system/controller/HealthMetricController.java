package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.HealthMetric;
import com.thinkinghelp.system.mapper.HealthMetricMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/health/metrics")
@RequiredArgsConstructor
@Tag(name = "健康数据", description = "用户健康数据记录")
public class HealthMetricController {

    private final HealthMetricMapper healthMetricMapper;

    @PostMapping
    @Operation(summary = "添加健康记录")
    public Result<String> addMetric(@RequestBody HealthMetric metric) {
        if (metric.getUserId() == null) {
            return Result.error("缺少用户ID");
        }
        if (metric.getRecordedAt() == null) {
            metric.setRecordedAt(LocalDateTime.now());
        }
        healthMetricMapper.insert(metric);
        return Result.success("记录成功");
    }

    @PostMapping("/batch")
    @Operation(summary = "批量添加健康记录")
    public Result<String> addMetricsBatch(@RequestBody List<HealthMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return Result.error("记录列表为空");
        }
        for (HealthMetric metric : metrics) {
            if (metric.getUserId() != null) {
                if (metric.getRecordedAt() == null)
                    metric.setRecordedAt(LocalDateTime.now());
                healthMetricMapper.insert(metric);
            }
        }
        return Result.success("批量记录成功");
    }

    @GetMapping
    @Operation(summary = "获取健康记录列表")
    public Result<List<HealthMetric>> listMetrics(@RequestParam Long userId,
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<HealthMetric> query = new LambdaQueryWrapper<>();
        query.eq(HealthMetric::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            query.eq(HealthMetric::getMetricType, type);
        }
        query.orderByDesc(HealthMetric::getRecordedAt);
        return Result.success(healthMetricMapper.selectList(query));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除健康记录")
    public Result<String> deleteMetric(@PathVariable Long id) {
        healthMetricMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
