package com.anti.entity.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建关联规则请求DTO
 */
@Data
public class CreateAssociationRuleRequest {

    /**
     * 触发标签
     */
    @NotBlank(message = "触发标签不能为空")
    private String triggerTag;

    /**
     * 预测标签数组(JSON字符串)
     */
    @NotBlank(message = "预测标签不能为空")
    private String predictedTags;

    /**
     * 置信度
     */
    @NotNull(message = "置信度不能为空")
    @DecimalMin(value = "0.0", message = "置信度不能小于0")
    @DecimalMax(value = "1.0", message = "置信度不能大于1")
    private BigDecimal confidence;

    /**
     * 规则描述
     */
    private String description;
}
