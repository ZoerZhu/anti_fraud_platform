package com.anti.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论请求DTO
 */
@Data
public class CreateCommentRequest {

    /**
     * 帖子ID
     */
    @NotNull(message = "帖子ID不能为空")
    @Positive(message = "帖子ID必须大于0")
    private Long postId;

    /**
     * 父评论ID(0表示一级评论)
     */
    @PositiveOrZero(message = "父评论ID不能小于0")
    private Long parentId;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容不能超过2000个字符")
    private String content;
}
