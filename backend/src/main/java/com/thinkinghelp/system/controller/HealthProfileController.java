package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.HealthProfile;
import com.thinkinghelp.system.entity.HealthProfileHistory;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.entity.dto.HealthProfileHistoryItem;
import com.thinkinghelp.system.entity.dto.HealthProfileDTO;
import com.thinkinghelp.system.mapper.HealthProfileHistoryMapper;
import com.thinkinghelp.system.mapper.HealthProfileMapper;
import com.thinkinghelp.system.mapper.UserMapper;
import com.thinkinghelp.system.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/health/profile")
@RequiredArgsConstructor
@Tag(name = "健康档案", description = "健康档案基础信息")
@Slf4j
public class HealthProfileController {

    private final HealthProfileMapper healthProfileMapper;
    private final HealthProfileHistoryMapper historyMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "获取健康档案")
    public Result<HealthProfileDTO> getProfile(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        HealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>().eq(HealthProfile::getUserId, userId));
        if (profile == null) {
            return Result.success(null);
        }
        return Result.success(toDto(profile));
    }

    @PostMapping
    @Operation(summary = "保存健康档案")
    public Result<String> saveProfile(@RequestHeader("Authorization") String token,
                                      @RequestBody HealthProfileDTO dto) {
        Long userId = getUserIdFromToken(token);
        HealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>().eq(HealthProfile::getUserId, userId));
        if (profile == null) {
            profile = new HealthProfile();
            profile.setUserId(userId);
        }

        profile.setName(dto.getName());
        profile.setGender(dto.getGender());
        profile.setAge(dto.getAge());
        profile.setHeight(dto.getHeight());
        profile.setWeight(dto.getWeight());
        profile.setBmi(dto.getBmi());
        profile.setReportDate(dto.getReportDate());
        profile.setBpSystolic(dto.getBpSystolic());
        profile.setBpDiastolic(dto.getBpDiastolic());
        profile.setFastingGlucose(dto.getFastingGlucose());
        profile.setHba1c(dto.getHba1c());
        profile.setTotalCholesterol(dto.getTotalCholesterol());
        profile.setTriglycerides(dto.getTriglycerides());
        profile.setHdl(dto.getHdl());
        profile.setLdl(dto.getLdl());
        profile.setUricAcid(dto.getUricAcid());
        profile.setAlt(dto.getAlt());
        profile.setAst(dto.getAst());
        profile.setCreatinine(dto.getCreatinine());
        profile.setBun(dto.getBun());
        profile.setOtherRestrictions(dto.getOtherRestrictions());
        profile.setUpdatedAt(LocalDateTime.now());

        try {
            profile.setDiseases(objectMapper.writeValueAsString(
                    dto.getDiseases() == null ? Collections.emptyList() : dto.getDiseases()));
            profile.setAllergies(objectMapper.writeValueAsString(
                    dto.getAllergies() == null ? Collections.emptyList() : dto.getAllergies()));
        } catch (Exception e) {
            return Result.error("档案保存失败：数据格式错误");
        }

        if (profile.getId() == null) {
            healthProfileMapper.insert(profile);
        } else {
            healthProfileMapper.updateById(profile);
        }
        saveHistory(userId, dto);
        return Result.success("档案保存成功");
    }

    @GetMapping("/history")
    @Operation(summary = "获取健康档案历史记录")
    public Result<List<HealthProfileHistoryItem>> listHistory(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        List<HealthProfileHistory> historyList = historyMapper.selectList(
                new LambdaQueryWrapper<HealthProfileHistory>()
                        .eq(HealthProfileHistory::getUserId, userId)
                        .orderByDesc(HealthProfileHistory::getCreatedAt));
        List<HealthProfileHistoryItem> items = new ArrayList<>();
        for (HealthProfileHistory history : historyList) {
            HealthProfileHistoryItem item = new HealthProfileHistoryItem();
            item.setId(history.getId());
            item.setReportDate(history.getReportDate());
            item.setCreatedAt(history.getCreatedAt());
            item.setSummary(buildSummary(history.getProfileJson()));
            items.add(item);
        }
        return Result.success(items);
    }

    @GetMapping("/history/{id}")
    @Operation(summary = "获取健康档案历史详情")
    public Result<HealthProfileDTO> getHistory(@RequestHeader("Authorization") String token,
                                               @PathVariable Long id) {
        Long userId = getUserIdFromToken(token);
        HealthProfileHistory history = historyMapper.selectById(id);
        if (history == null || !history.getUserId().equals(userId)) {
            return Result.error("记录不存在");
        }
        try {
            HealthProfileDTO dto = objectMapper.readValue(history.getProfileJson(), HealthProfileDTO.class);
            return Result.success(dto);
        } catch (Exception e) {
            return Result.error("解析失败");
        }
    }

    @PutMapping("/history/{id}")
    @Operation(summary = "更新健康档案历史记录")
    public Result<String> updateHistory(@RequestHeader("Authorization") String token,
                                        @PathVariable Long id,
                                        @RequestBody HealthProfileDTO dto) {
        Long userId = getUserIdFromToken(token);
        HealthProfileHistory history = historyMapper.selectById(id);
        if (history == null || !history.getUserId().equals(userId)) {
            return Result.error("记录不存在");
        }
        try {
            history.setProfileJson(objectMapper.writeValueAsString(dto));
            history.setReportDate(dto.getReportDate());
            history.setUpdatedAt(LocalDateTime.now());
            historyMapper.updateById(history);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error("更新失败");
        }
    }

    private HealthProfileDTO toDto(HealthProfile profile) {
        HealthProfileDTO dto = new HealthProfileDTO();
        dto.setName(profile.getName());
        dto.setGender(profile.getGender());
        dto.setAge(profile.getAge());
        dto.setHeight(profile.getHeight());
        dto.setWeight(profile.getWeight());
        dto.setBmi(profile.getBmi());
        dto.setReportDate(profile.getReportDate());
        dto.setBpSystolic(profile.getBpSystolic());
        dto.setBpDiastolic(profile.getBpDiastolic());
        dto.setFastingGlucose(profile.getFastingGlucose());
        dto.setHba1c(profile.getHba1c());
        dto.setTotalCholesterol(profile.getTotalCholesterol());
        dto.setTriglycerides(profile.getTriglycerides());
        dto.setHdl(profile.getHdl());
        dto.setLdl(profile.getLdl());
        dto.setUricAcid(profile.getUricAcid());
        dto.setAlt(profile.getAlt());
        dto.setAst(profile.getAst());
        dto.setCreatinine(profile.getCreatinine());
        dto.setBun(profile.getBun());
        dto.setOtherRestrictions(profile.getOtherRestrictions());

        try {
            dto.setDiseases(profile.getDiseases() == null
                    ? Collections.emptyList()
                    : objectMapper.readValue(profile.getDiseases(), new TypeReference<>() {}));
            dto.setAllergies(profile.getAllergies() == null
                    ? Collections.emptyList()
                    : objectMapper.readValue(profile.getAllergies(), new TypeReference<>() {}));
        } catch (Exception e) {
            dto.setDiseases(Collections.emptyList());
            dto.setAllergies(Collections.emptyList());
        }
        return dto;
    }

    private void saveHistory(Long userId, HealthProfileDTO dto) {
        try {
            HealthProfileHistory history = new HealthProfileHistory();
            history.setUserId(userId);
            history.setReportDate(dto.getReportDate());
            history.setProfileJson(objectMapper.writeValueAsString(dto));
            history.setCreatedAt(LocalDateTime.now());
            history.setUpdatedAt(LocalDateTime.now());
            historyMapper.insert(history);
        } catch (Exception e) {
            log.warn("Failed to save health profile history", e);
        }
    }

    private String buildSummary(String profileJson) {
        if (profileJson == null || profileJson.isBlank()) {
            return "";
        }
        try {
            HealthProfileDTO dto = objectMapper.readValue(profileJson, HealthProfileDTO.class);
            List<String> parts = new ArrayList<>();
            if (dto.getDiseases() != null && !dto.getDiseases().isEmpty()) {
                parts.add("慢病:" + String.join("、", dto.getDiseases()));
            }
            if (dto.getAllergies() != null && !dto.getAllergies().isEmpty()) {
                parts.add("过敏:" + String.join("、", dto.getAllergies()));
            }
            if (dto.getOtherRestrictions() != null && !dto.getOtherRestrictions().isBlank()) {
                parts.add("忌口:" + dto.getOtherRestrictions());
            }
            return String.join(" | ", parts);
        } catch (Exception e) {
            return "";
        }
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
