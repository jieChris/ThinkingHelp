package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username; // 账号
    private String passwordHash;
    private String nickname; // 用户名(昵称)
    private Integer gender; // 0:女, 1:男, 2:未知
    private String avatar; // 头像URL
    private String role; // 角色 (USER/ADMIN)
    private Integer memberLevel; // 0: 普通用户, 1: VIP
    private LocalDateTime createdAt;
}
