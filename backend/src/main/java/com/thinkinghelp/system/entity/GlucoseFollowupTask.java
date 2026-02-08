package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("glucose_followup_tasks")
public class GlucoseFollowupTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String taskType;
    private String title;
    private String note;
    private String status;
    private LocalDateTime dueAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
