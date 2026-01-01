package com.thinkinghelp.system.entity.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username; // 账号
    private String password; // 密码
    private String nickname; // 用户名
    private Integer gender; // 0:女, 1:男
}
