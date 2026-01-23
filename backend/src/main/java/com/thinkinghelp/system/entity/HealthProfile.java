package com.thinkinghelp.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_profiles")
public class HealthProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;

    private String name;
    private String gender;
    private Integer age;
    private Double height;
    private Double weight;
    private String bmi;

    private String diseases;
    private String allergies;
    private String otherRestrictions;

    private LocalDateTime updatedAt;
}
