package com.anti.entity.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class ScoreChangeRequest {

    @NotNull(message = "目标用户ID不能为空")
    private Long userId;

    @NotNull(message = "积分数量不能为空")
    @Min(value = 1, message = "积分数量必须大于0")
    @Max(value = 1000, message = "单次积分调整不能超过1000")
    private Integer score;

    private String reason;
}
