package com.anti.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量更新状态请求DTO
 */
@Data
public class BatchStatusRequest {

    /**
     * 关卡ID列表
     */
    @NotEmpty(message = "请选择要操作的关卡")
    private List<Long> challengeIds;

    /**
     * 状态: 0-禁用, 1-启用
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}
