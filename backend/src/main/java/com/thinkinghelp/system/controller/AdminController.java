package com.thinkinghelp.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "后台管理", description = "管理员专属接口")
public class AdminController {

    private final UserMapper userMapper;

    @GetMapping("/users")
    @Operation(summary = "获取用户列表")
    public Result<IPage<User>> listUsers(@RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<User> userPage = new Page<>(page, size);
        IPage<User> result = userMapper.selectPage(userPage, null);
        // Hide sensitive info like passwordHash in response if needed,
        // but for admin view it might be fine or we should sanitize.
        // Let's sanitize lightly by loop or DTO. For demo, raw entity is okay but
        // better to clear password.
        result.getRecords().forEach(u -> u.setPasswordHash(null));
        return Result.success(result);
    }

    @PutMapping("/users/{id}/member-level")
    @Operation(summary = "更新用户会员等级")
    public Result<String> updateUserLevel(@PathVariable Long id, @RequestParam Integer level) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setMemberLevel(level);
        userMapper.updateById(user);
        return Result.success("更新成功");
    }
}
