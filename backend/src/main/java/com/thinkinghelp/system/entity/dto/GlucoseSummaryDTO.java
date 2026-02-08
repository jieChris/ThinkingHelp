package com.thinkinghelp.system.entity.dto;

import lombok.Data;

@Data
public class GlucoseSummaryDTO {
    private Double avgGlucose;
    private Double fastingAvg;
    private Double postMealAvg;
    private Double beforeSleepAvg;
    private Double randomAvg;
    private Double latestGlucose;
    private Integer highCount;
    private Integer lowCount;
    private Integer normalCount;
    private Integer totalCount;
}
