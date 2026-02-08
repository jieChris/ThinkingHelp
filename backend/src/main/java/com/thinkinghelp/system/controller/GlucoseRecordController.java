package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.DietLog;
import com.thinkinghelp.system.entity.GlucoseRecord;
import com.thinkinghelp.system.entity.dto.GlucoseAnalysisDTO;
import com.thinkinghelp.system.entity.dto.GlucoseSummaryDTO;
import com.thinkinghelp.system.mapper.DietLogMapper;
import com.thinkinghelp.system.mapper.GlucoseRecordMapper;
import com.thinkinghelp.system.utils.DateTimeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/health/glucose-records")
@RequiredArgsConstructor
@Tag(name = "控糖管理", description = "血糖专项记录")
public class GlucoseRecordController {

    private final GlucoseRecordMapper glucoseRecordMapper;
    private final DietLogMapper dietLogMapper;

    @PostMapping
    @Operation(summary = "添加血糖记录")
    public Result<String> addRecord(@RequestBody GlucoseRecord record) {
        if (record.getUserId() == null) {
            return Result.error("缺少用户ID");
        }
        if (record.getGlucoseValue() == null || record.getGlucoseValue() <= 0) {
            return Result.error("请输入有效血糖值");
        }
        if (record.getMeasureType() == null || record.getMeasureType().isBlank()) {
            record.setMeasureType("FASTING");
        }
        if (record.getEventType() == null || record.getEventType().isBlank()) {
            record.setEventType(classifyEventType(record.getGlucoseValue(), record.getMeasureType()));
        }
        if (record.getRecordedAt() == null) {
            record.setRecordedAt(LocalDateTime.now());
        }
        record.setCreatedAt(LocalDateTime.now());
        glucoseRecordMapper.insert(record);
        return Result.success("记录保存成功");
    }

    @GetMapping
    @Operation(summary = "获取血糖记录列表")
    public Result<List<GlucoseRecord>> listRecords(@RequestParam Long userId,
                                                   @RequestParam(required = false) String measureType,
                                                   @RequestParam(required = false) String eventType,
                                                   @RequestParam(required = false) String start,
                                                   @RequestParam(required = false) String end) {
        LambdaQueryWrapper<GlucoseRecord> query = new LambdaQueryWrapper<>();
        query.eq(GlucoseRecord::getUserId, userId);
        if (measureType != null && !measureType.isBlank()) {
            query.eq(GlucoseRecord::getMeasureType, measureType.trim().toUpperCase());
        }
        if (eventType != null && !eventType.isBlank()) {
            query.eq(GlucoseRecord::getEventType, eventType.trim().toUpperCase());
        }
        if (start != null && !start.isBlank()) {
            query.ge(GlucoseRecord::getRecordedAt, parseDateTime(start));
        }
        if (end != null && !end.isBlank()) {
            query.le(GlucoseRecord::getRecordedAt, parseDateTime(end));
        }
        query.orderByDesc(GlucoseRecord::getRecordedAt);
        return Result.success(glucoseRecordMapper.selectList(query));
    }

    @GetMapping("/summary")
    @Operation(summary = "获取血糖统计")
    public Result<GlucoseSummaryDTO> summary(@RequestParam Long userId,
                                             @RequestParam(required = false) String start,
                                             @RequestParam(required = false) String end) {
        LambdaQueryWrapper<GlucoseRecord> query = new LambdaQueryWrapper<>();
        query.eq(GlucoseRecord::getUserId, userId);
        if (start != null && !start.isBlank()) {
            query.ge(GlucoseRecord::getRecordedAt, parseDateTime(start));
        }
        if (end != null && !end.isBlank()) {
            query.le(GlucoseRecord::getRecordedAt, parseDateTime(end));
        }
        query.orderByDesc(GlucoseRecord::getRecordedAt);
        List<GlucoseRecord> records = glucoseRecordMapper.selectList(query);
        GlucoseSummaryDTO dto = buildSummary(records);
        return Result.success(dto);
    }

    @GetMapping("/analysis")
    @Operation(summary = "获取控糖分析与饮食关联")
    public Result<GlucoseAnalysisDTO> analysis(@RequestParam Long userId,
                                               @RequestParam(required = false) String start,
                                               @RequestParam(required = false) String end) {
        LambdaQueryWrapper<GlucoseRecord> glucoseQuery = new LambdaQueryWrapper<>();
        glucoseQuery.eq(GlucoseRecord::getUserId, userId);
        if (start != null && !start.isBlank()) {
            glucoseQuery.ge(GlucoseRecord::getRecordedAt, parseDateTime(start));
        }
        if (end != null && !end.isBlank()) {
            glucoseQuery.le(GlucoseRecord::getRecordedAt, parseDateTime(end));
        }
        glucoseQuery.orderByDesc(GlucoseRecord::getRecordedAt);
        List<GlucoseRecord> glucoseRecords = glucoseRecordMapper.selectList(glucoseQuery);

        LambdaQueryWrapper<DietLog> mealQuery = new LambdaQueryWrapper<>();
        mealQuery.eq(DietLog::getUserId, userId);
        if (start != null && !start.isBlank()) {
            mealQuery.ge(DietLog::getRecordedAt, parseDateTime(start));
        }
        if (end != null && !end.isBlank()) {
            mealQuery.le(DietLog::getRecordedAt, parseDateTime(end));
        }
        mealQuery.orderByDesc(DietLog::getRecordedAt);
        List<DietLog> mealLogs = dietLogMapper.selectList(mealQuery);

        return Result.success(buildAnalysis(glucoseRecords, mealLogs));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除血糖记录")
    public Result<String> deleteRecord(@PathVariable Long id) {
        glucoseRecordMapper.deleteById(id);
        return Result.success("删除成功");
    }

    private GlucoseSummaryDTO buildSummary(List<GlucoseRecord> records) {
        GlucoseSummaryDTO dto = new GlucoseSummaryDTO();
        if (records == null || records.isEmpty()) {
            dto.setHighCount(0);
            dto.setLowCount(0);
            dto.setNormalCount(0);
            dto.setTotalCount(0);
            return dto;
        }
        double sum = 0;
        int count = 0;
        double fastingSum = 0;
        int fastingCount = 0;
        double postMealSum = 0;
        int postMealCount = 0;
        double beforeSleepSum = 0;
        int beforeSleepCount = 0;
        double randomSum = 0;
        int randomCount = 0;
        int highCount = 0;
        int lowCount = 0;
        int normalCount = 0;
        for (GlucoseRecord record : records) {
            if (record.getGlucoseValue() == null) {
                continue;
            }
            double value = record.getGlucoseValue();
            sum += value;
            count++;
            String measureType = record.getMeasureType() == null ? "" : record.getMeasureType().toUpperCase();
            if ("FASTING".equals(measureType)) {
                fastingSum += value;
                fastingCount++;
            } else if ("POST_MEAL_2H".equals(measureType)) {
                postMealSum += value;
                postMealCount++;
            } else if ("BEFORE_SLEEP".equals(measureType)) {
                beforeSleepSum += value;
                beforeSleepCount++;
            } else if ("RANDOM".equals(measureType)) {
                randomSum += value;
                randomCount++;
            }
            String eventType = (record.getEventType() == null || record.getEventType().isBlank())
                    ? classifyEventType(value, measureType)
                    : record.getEventType().toUpperCase();
            if ("HIGH".equals(eventType)) {
                highCount++;
            } else if ("LOW".equals(eventType)) {
                lowCount++;
            } else {
                normalCount++;
            }
        }
        dto.setAvgGlucose(avg(sum, count));
        dto.setFastingAvg(avg(fastingSum, fastingCount));
        dto.setPostMealAvg(avg(postMealSum, postMealCount));
        dto.setBeforeSleepAvg(avg(beforeSleepSum, beforeSleepCount));
        dto.setRandomAvg(avg(randomSum, randomCount));
        dto.setLatestGlucose(records.get(0).getGlucoseValue());
        dto.setHighCount(highCount);
        dto.setLowCount(lowCount);
        dto.setNormalCount(normalCount);
        dto.setTotalCount(count);
        return dto;
    }

    private Double avg(double sum, int count) {
        if (count <= 0) return null;
        return Math.round((sum / count) * 100.0) / 100.0;
    }

    private GlucoseAnalysisDTO buildAnalysis(List<GlucoseRecord> glucoseRecords, List<DietLog> mealLogs) {
        GlucoseAnalysisDTO dto = new GlucoseAnalysisDTO();
        if (glucoseRecords == null || glucoseRecords.isEmpty()) {
            dto.setRiskLevel("LOW");
            dto.getAlerts().add("暂无血糖数据，建议先连续记录3-7天。");
            return dto;
        }

        int highCount = 0;
        int lowCount = 0;
        int total = 0;
        for (GlucoseRecord record : glucoseRecords) {
            if (record.getGlucoseValue() == null) continue;
            total++;
            String eventType = normalizeEventType(record.getEventType(), record.getGlucoseValue(), record.getMeasureType());
            if ("HIGH".equals(eventType)) {
                highCount++;
            } else if ("LOW".equals(eventType)) {
                lowCount++;
            }
        }

        double highRatio = total == 0 ? 0 : (double) highCount / total;
        String riskLevel;
        if (highRatio >= 0.4 || highCount >= 5 || lowCount >= 2) {
            riskLevel = "HIGH";
        } else if (highRatio >= 0.2 || highCount >= 2 || lowCount >= 1) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }
        dto.setRiskLevel(riskLevel);

        GlucoseRecord latest = glucoseRecords.get(0);
        if (latest.getGlucoseValue() != null) {
            String latestEvent = normalizeEventType(latest.getEventType(), latest.getGlucoseValue(), latest.getMeasureType());
            if ("HIGH".equals(latestEvent)) {
                dto.getAlerts().add("最近一次血糖偏高，建议复测并减少高GI主食。");
            } else if ("LOW".equals(latestEvent)) {
                dto.getAlerts().add("最近一次血糖偏低，注意及时补充碳水并观察症状。");
            }
        }
        if (highCount > 0) {
            dto.getAlerts().add("统计期内高血糖次数：" + highCount + " 次。");
        }
        if (lowCount > 0) {
            dto.getAlerts().add("统计期内低血糖次数：" + lowCount + " 次。");
        }

        List<GlucoseAnalysisDTO.MealImpactItem> impacts = new ArrayList<>();
        for (GlucoseRecord glucoseRecord : glucoseRecords) {
            if (!"POST_MEAL_2H".equalsIgnoreCase(glucoseRecord.getMeasureType())) {
                continue;
            }
            if (glucoseRecord.getGlucoseValue() == null || glucoseRecord.getGlucoseValue() < 11.1) {
                continue;
            }
            DietLog matched = findClosestPreviousMeal(glucoseRecord, mealLogs);
            GlucoseAnalysisDTO.MealImpactItem item = new GlucoseAnalysisDTO.MealImpactItem();
            item.setGlucoseTime(formatDateTime(glucoseRecord.getRecordedAt()));
            item.setGlucoseValue(glucoseRecord.getGlucoseValue());
            item.setMeasureType("餐后2小时");
            if (matched != null) {
                item.setMealTime(formatDateTime(matched.getRecordedAt()));
                item.setMealType(mealTypeLabel(matched.getMealType()));
                item.setMealName(matched.getFoodName());
                item.setMealCalories(matched.getCalories());
                item.setMealCarbs(matched.getCarbsGrams());
                item.setSuggestion(buildMealSuggestion(matched, glucoseRecord));
            } else {
                item.setSuggestion("该条高血糖未匹配到2-4小时内饮食记录，建议补齐餐次记录。");
            }
            impacts.add(item);
            if (impacts.size() >= 10) {
                break;
            }
        }
        dto.setMealImpacts(impacts);
        if (!impacts.isEmpty()) {
            dto.getAlerts().add("检测到餐后高血糖与饮食记录关联，建议优先调整对应餐次。");
        }
        return dto;
    }

    private DietLog findClosestPreviousMeal(GlucoseRecord glucoseRecord, List<DietLog> mealLogs) {
        if (glucoseRecord == null || glucoseRecord.getRecordedAt() == null || mealLogs == null || mealLogs.isEmpty()) {
            return null;
        }
        DietLog best = null;
        long bestMinutes = Long.MAX_VALUE;
        for (DietLog meal : mealLogs) {
            if (meal.getRecordedAt() == null) continue;
            if (meal.getRecordedAt().isAfter(glucoseRecord.getRecordedAt())) {
                continue;
            }
            long diff = java.time.Duration.between(meal.getRecordedAt(), glucoseRecord.getRecordedAt()).toMinutes();
            if (diff < 0 || diff > 240) {
                continue;
            }
            if (diff < bestMinutes) {
                bestMinutes = diff;
                best = meal;
            }
        }
        return best;
    }

    private String buildMealSuggestion(DietLog meal, GlucoseRecord glucoseRecord) {
        Double carbs = meal.getCarbsGrams();
        Double calories = meal.getCalories();
        if (carbs != null && carbs > 60) {
            return "该餐碳水偏高，建议减少精制主食并增加蔬菜蛋白比例。";
        }
        if (calories != null && calories > 800) {
            return "该餐总热量偏高，建议控制份量并分散到加餐。";
        }
        if (glucoseRecord.getGlucoseValue() != null && glucoseRecord.getGlucoseValue() >= 13.0) {
            return "餐后血糖明显偏高，建议优先替换高GI食物并复测。";
        }
        return "建议控制该餐主食比例，并优先选择低GI碳水。";
    }

    private String normalizeEventType(String eventType, Double glucoseValue, String measureType) {
        if (eventType != null && !eventType.isBlank()) {
            return eventType.trim().toUpperCase(Locale.ROOT);
        }
        return classifyEventType(glucoseValue, measureType);
    }

    private String formatDateTime(LocalDateTime time) {
        if (time == null) return "";
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String mealTypeLabel(String mealType) {
        if (mealType == null) return "";
        switch (mealType.toUpperCase(Locale.ROOT)) {
            case "BREAKFAST":
                return "早餐";
            case "LUNCH":
                return "午餐";
            case "DINNER":
                return "晚餐";
            case "SNACK":
                return "加餐";
            default:
                return mealType;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        return DateTimeUtils.parseFlexibleDateTime(value);
    }

    private String classifyEventType(Double glucoseValue, String measureTypeRaw) {
        if (glucoseValue == null) return "NORMAL";
        String measureType = measureTypeRaw == null ? "FASTING" : measureTypeRaw.toUpperCase();
        if ("POST_MEAL_2H".equals(measureType)) {
            if (glucoseValue < 3.9) return "LOW";
            if (glucoseValue >= 11.1) return "HIGH";
            return "NORMAL";
        }
        if ("BEFORE_SLEEP".equals(measureType) || "RANDOM".equals(measureType)) {
            if (glucoseValue < 3.9) return "LOW";
            if (glucoseValue >= 10.0) return "HIGH";
            return "NORMAL";
        }
        if (glucoseValue < 3.9) return "LOW";
        if (glucoseValue >= 7.0) return "HIGH";
        return "NORMAL";
    }
}
