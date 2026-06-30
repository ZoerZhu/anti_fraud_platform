package com.anti.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除请求DTO
 */
@Data
public class BatchDeleteRequest {

    /**
     * 关卡ID列表
     */
    @NotEmpty(message = "请选择要删除的关卡")
    private List<Long> challengeIds;
}
