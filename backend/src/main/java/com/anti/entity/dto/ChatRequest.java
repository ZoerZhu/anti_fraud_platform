package com.anti.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天请求DTO
 */
@Data
public class ChatRequest {

    /**
     * 问题内容
     */
    @NotBlank(message = "问题内容不能为空")
    @Size(max = 2000, message = "问题内容不能超过2000个字符")
    private String question;

    /**
     * 会话ID(可选，传入则继续该会话)
     */
    @Size(max = 50, message = "会话ID不能超过50个字符")
    @Pattern(regexp = "^$|^session_\\d+_\\d+$", message = "会话ID格式不正确")
    private String sessionId;
}
