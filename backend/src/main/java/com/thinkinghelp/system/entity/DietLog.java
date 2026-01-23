package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("diet_logs")
public class DietLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String mealType;
    private Integer foodId;
    private String unit;
    private Double count;
    private LocalDateTime recordedAt;
}
