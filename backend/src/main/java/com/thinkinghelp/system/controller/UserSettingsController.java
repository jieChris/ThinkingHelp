package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.entity.UserSettings;
import com.thinkinghelp.system.mapper.UserMapper;
import com.thinkinghelp.system.mapper.UserSettingsMapper;
import com.thinkinghelp.system.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户设置", description = "用户偏好设置与数据管理")
public class UserSettingsController {

    private final UserSettingsMapper settingsMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @GetMapping("/settings")
    @Operation(summary = "获取用户设置")
    public Result<UserSettings> getSettings(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        UserSettings settings = settingsMapper.selectById(userId);
        if (settings == null) {
            // Return default settings if not exists
            settings = new UserSettings()
                    .setUserId(userId)
                    .setFontSize(0)
                    .setTheme("light")
                    .setAiPersona("gentle")
                    .setNotificationEnabled(true);
            settingsMapper.insert(settings);
        }
        return Result.success(settings);
    }

    @PutMapping("/settings")
    @Operation(summary = "更新用户设置")
    public Result<String> updateSettings(@RequestHeader("Authorization") String token,
            @RequestBody UserSettings settings) {
        Long userId = getUserIdFromToken(token);
        settings.setUserId(userId);

        if (settingsMapper.exists(new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, userId))) {
            settingsMapper.updateById(settings);
        } else {
            settingsMapper.insert(settings);
        }
        return Result.success("设置更新成功");
    }

    @PostMapping("/export-data")
    @Operation(summary = "导出用户数据")
    public void exportData(@RequestHeader("Authorization") String token, HttpServletResponse response)
            throws IOException {
        Long userId = getUserIdFromToken(token);
        // Mock export data
        String data = "User Export Data for User ID: " + userId + "\nTimestamp: " + System.currentTimeMillis();

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=\"user_data.txt\"");
        response.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
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
