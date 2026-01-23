package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.HealthRecord;
import com.thinkinghelp.system.mapper.HealthRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/health/records")
@RequiredArgsConstructor
@Tag(name = "健康记录(综合)", description = "用户各指标综合记录")
public class HealthRecordController {

    private final HealthRecordMapper healthRecordMapper;

    @PostMapping
    @Operation(summary = "添加一条综合记录")
    public Result<String> addRecord(@RequestBody HealthRecord record) {
        if (record.getUserId() == null) {
            return Result.error("缺少用户ID");
        }
        if (record.getRecordedAt() == null) {
            record.setRecordedAt(LocalDateTime.now());
        }
        healthRecordMapper.insert(record);
        return Result.success("记录保存成功");
    }

    @GetMapping
    @Operation(summary = "获取综合记录列表")
    public Result<List<HealthRecord>> listRecords(@RequestParam Long userId,
                                                  @RequestParam(required = false) String start,
                                                  @RequestParam(required = false) String end) {
        LambdaQueryWrapper<HealthRecord> query = new LambdaQueryWrapper<>();
        query.eq(HealthRecord::getUserId, userId);
        if (start != null && !start.isEmpty()) {
            query.ge(HealthRecord::getRecordedAt, parseDateTime(start));
        }
        if (end != null && !end.isEmpty()) {
            query.le(HealthRecord::getRecordedAt, parseDateTime(end));
        }
        query.orderByDesc(HealthRecord::getRecordedAt);
        return Result.success(healthRecordMapper.selectList(query));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除记录")
    public Result<String> deleteRecord(@PathVariable Long id) {
        healthRecordMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新一条记录")
    public Result<HealthRecord> getLatestRecord(@RequestParam Long userId) {
        LambdaQueryWrapper<HealthRecord> query = new LambdaQueryWrapper<>();
        query.eq(HealthRecord::getUserId, userId);
        query.orderByDesc(HealthRecord::getRecordedAt);
        query.last("LIMIT 1");
        return Result.success(healthRecordMapper.selectOne(query));
    }

    private java.time.LocalDateTime parseDateTime(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return java.time.LocalDateTime.parse(value, formatter);
    }
}
