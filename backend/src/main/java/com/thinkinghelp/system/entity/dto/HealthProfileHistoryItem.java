package com.thinkinghelp.system.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HealthProfileHistoryItem {
    private Long id;
    private String reportDate;
    private String summary;
    private LocalDateTime createdAt;
}
