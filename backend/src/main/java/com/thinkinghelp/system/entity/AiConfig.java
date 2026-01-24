package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_configs")
public class AiConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String apiKey;
    private String baseUrl;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
