package com.anti.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 情景模拟决策请求DTO
 */
@Data
public class ScenarioDecisionRequest {

    /**
     * 关卡ID
     */
    @NotNull(message = "关卡ID不能为空")
    @Positive(message = "关卡ID必须大于0")
    private Long challengeId;

    /**
     * 当前节点ID
     */
    @NotBlank(message = "当前节点不能为空")
    private String currentNode;

    /**
     * 选择的边ID
     */
    @NotBlank(message = "选择项不能为空")
    private String selectedEdgeId;

    /**
     * 开始时间戳
     */
    private Long startTime;
}
