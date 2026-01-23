package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.DietLog;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.mapper.DietLogMapper;
import com.thinkinghelp.system.mapper.UserMapper;
import com.thinkinghelp.system.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/diet/logs")
@RequiredArgsConstructor
@Tag(name = "饮食记录", description = "用户饮食记录")
public class DietLogController {

    private final DietLogMapper dietLogMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

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
    public Result<List<DietLog>> listLogs(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        LambdaQueryWrapper<DietLog> query = new LambdaQueryWrapper<>();
        query.eq(DietLog::getUserId, userId);
        query.orderByDesc(DietLog::getRecordedAt);
        return Result.success(dietLogMapper.selectList(query));
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
