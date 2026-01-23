package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_profile_history")
public class HealthProfileHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String reportDate;
    private String profileJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
