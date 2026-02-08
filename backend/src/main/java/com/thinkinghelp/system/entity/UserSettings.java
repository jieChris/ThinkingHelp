package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("user_settings")
public class UserSettings {
    @TableId
    private Long userId;

    // 0:标准, 1:大, 2:特大
    private Integer fontSize;

    // 'light', 'dark'
    private String theme;

    // 'strict', 'gentle'
    private String aiPersona;

    private Boolean notificationEnabled;
    
    // 首页可展示卡片配置(JSON数组字符串)
    private String dashboardCards;

    private LocalDateTime updatedAt;
}
