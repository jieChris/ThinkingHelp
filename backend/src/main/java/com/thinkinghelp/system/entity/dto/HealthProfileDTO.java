package com.thinkinghelp.system.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class HealthProfileDTO {
    private String name;
    private String gender;
    private Integer age;
    private Double height;
    private Double weight;
    private String bmi;
    private List<String> diseases;
    private List<String> allergies;
    private String otherRestrictions;
}
