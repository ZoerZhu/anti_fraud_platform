package com.anti.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/**
 * 用户反馈请求DTO
 */
@Data
public class FeedbackRequest {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    @Size(max = 50, message = "会话ID不能超过50个字符")
    @Pattern(regexp = "^session_\\d+_\\d+$", message = "会话ID格式不正确")
    private String sessionId;

    /**
     * 反馈:1满意,-1不满意
     */
    @NotNull(message = "反馈值不能为空")
    @Min(value = -1, message = "反馈值只能为-1或1")
    @Max(value = 1, message = "反馈值只能为-1或1")
    private Integer feedback;

    @AssertTrue(message = "反馈值只能为-1或1")
    public boolean isFeedbackValid() {
        return feedback == null || feedback == -1 || feedback == 1;
    }
}
