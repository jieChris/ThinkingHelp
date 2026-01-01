package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.entity.dto.AuthRequest;
import com.thinkinghelp.system.mapper.UserMapper;
import com.thinkinghelp.system.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证中心", description = "用户注册与登录接口")
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@RequestBody AuthRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return Result.error("用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        return Result.success(data);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@RequestBody com.thinkinghelp.system.entity.dto.RegisterRequest request) {
        // 0. Account Format Validation (User Request: 7-12 digits)
        if (!request.getUsername().matches("^\\d{7,12}$")) {
            return Result.error("账号格式错误：必须为 7-12 位纯数字");
        }

        // 1. Check if account exists
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()))) {
            return Result.error("该账号已存在，请更换账号");
        }

        // 2. Password strength validation (At least 8 chars, letters and numbers)
        // Adjust regex as needed. ^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$
        if (!request.getPassword().matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$")) {
            return Result.error("密码强度不足：需包含字母和数字，且长度至少8位");
        }

        User user = new User();
        user.setUsername(request.getUsername()); // Account
        user.setNickname(request.getNickname()); // Display Name
        user.setGender(request.getGender());
        user.setRole("USER"); // Default Role
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setMemberLevel(0); // Default Not VIP

        userMapper.insert(user);
        return Result.success("注册成功");
    }
}
