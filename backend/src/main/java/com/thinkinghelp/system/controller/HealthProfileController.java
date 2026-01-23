package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.HealthProfile;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.entity.dto.HealthProfileDTO;
import com.thinkinghelp.system.mapper.HealthProfileMapper;
import com.thinkinghelp.system.mapper.UserMapper;
import com.thinkinghelp.system.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@RequestMapping("/api/health/profile")
@RequiredArgsConstructor
@Tag(name = "健康档案", description = "健康档案基础信息")
public class HealthProfileController {

    private final HealthProfileMapper healthProfileMapper;
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
        return Result.success("档案保存成功");
    }

    private HealthProfileDTO toDto(HealthProfile profile) {
        HealthProfileDTO dto = new HealthProfileDTO();
        dto.setName(profile.getName());
        dto.setGender(profile.getGender());
        dto.setAge(profile.getAge());
        dto.setHeight(profile.getHeight());
        dto.setWeight(profile.getWeight());
        dto.setBmi(profile.getBmi());
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

    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = jwtUtils.extractUsername(token);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user.getId();
    }
}
