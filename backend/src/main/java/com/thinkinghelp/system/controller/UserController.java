package com.thinkinghelp.system.controller;

import com.thinkinghelp.system.common.Result;
import com.thinkinghelp.system.entity.User;
import com.thinkinghelp.system.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "个人中心", description = "用户个人信息管理")
public class UserController {

    private final UserMapper userMapper;

    @PutMapping("/profile")
    @Operation(summary = "更新个人信息")
    public Result<User> updateProfile(@RequestBody User user) {
        User existUser = userMapper.selectById(user.getId());
        if (existUser == null) {
            return Result.error("用户不存在");
        }

        // Update allowable fields
        if (user.getNickname() != null)
            existUser.setNickname(user.getNickname());
        if (user.getAvatar() != null)
            existUser.setAvatar(user.getAvatar());
        if (user.getGender() != null)
            existUser.setGender(user.getGender());

        userMapper.updateById(existUser);
        existUser.setPasswordHash(null); // Hide password
        return Result.success(existUser);
    }

    @PostMapping("/upload/avatar")
    @Operation(summary = "上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        if (file.getSize() > 1024 * 1024) {
            return Result.error("文件大小不能超过1MB");
        }

        try {
            // Create uploads directory if not exists
            String projectRoot = System.getProperty("user.dir");
            String uploadDir = projectRoot + File.separator + "uploads";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + suffix;

            // Save file
            File dest = new File(uploadDir + File.separator + fileName);
            file.transferTo(dest);

            // Return URL (Relative path handled by WebConfig)
            // Example: /uploads/uuid.png
            String avatarUrl = "/uploads/" + fileName;
            return Result.success(avatarUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}
