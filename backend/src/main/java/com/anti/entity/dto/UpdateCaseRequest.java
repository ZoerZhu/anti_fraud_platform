package com.anti.entity.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新案例请求DTO
 */
@Data
public class UpdateCaseRequest {

    /**
     * 案例标题
     */
    @Size(max = 120, message = "案例标题不能超过120个字符")
    private String title;

    /**
     * 案例类型
     */
    @Size(max = 50, message = "案例类型不能超过50个字符")
    private String caseType;

    /**
     * 案例详情内容
     */
    private String content;

    /**
     * 诈骗剧本结构(JSON)
     */
    private String scripts;

    /**
     * 目标年级数组
     */
    private List<String> targetGrades;

    /**
     * 目标专业数组
     */
    private List<String> targetMajors;

    /**
     * 难度等级(1-5)
     */
    @Min(value = 1, message = "难度等级不能小于1")
    @Max(value = 5, message = "难度等级不能大于5")
    private Integer difficultyLevel;

    /**
     * 风险评分(0-10)
     */
    @DecimalMin(value = "0.0", message = "风险评分不能小于0")
    @DecimalMax(value = "10.0", message = "风险评分不能大于10")
    private java.math.BigDecimal riskScore;

    /**
     * 标签ID数组
     */
    private List<Long> tagIds;

    /**
     * 状态:0禁用1启用
     */
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;

    /**
     * 是否精选:0否1是
     */
    @Min(value = 0, message = "是否精选只能为0或1")
    @Max(value = 1, message = "是否精选只能为0或1")
    private Integer isFeatured;
}
