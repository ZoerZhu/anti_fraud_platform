package com.anti.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 提交闯关答案请求DTO
 */
@Data
public class SubmitChallengeRequest {

    /**
     * 关卡ID
     */
    @NotNull(message = "关卡ID不能为空")
    @Positive(message = "关卡ID必须大于0")
    private Long challengeId;

    /**
     * 题目答案Map(题目ID -> 选择的选项索引列表)
     */
    @NotEmpty(message = "答案不能为空")
    private Map<String, List<Integer>> answers;

    /**
     * 开始时间戳
     */
    private Long startTime;
}
