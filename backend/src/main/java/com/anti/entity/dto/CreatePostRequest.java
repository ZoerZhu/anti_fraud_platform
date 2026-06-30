package com.anti.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建帖子请求DTO
 */
@Data
public class CreatePostRequest {

    /**
     * 帖子标题
     */
    @NotBlank(message = "帖子标题不能为空")
    @Size(max = 100, message = "帖子标题不能超过100个字符")
    private String title;

    /**
     * 帖子内容
     */
    @NotBlank(message = "帖子内容不能为空")
    @Size(max = 10000, message = "帖子内容不能超过10000个字符")
    private String content;

    /**
     * 帖子类型: experience-经验分享, question-提问, discussion-讨论
     */
    @Size(max = 30, message = "帖子类型不能超过30个字符")
    @Pattern(regexp = "^$|experience|question|discussion", message = "帖子类型只能是experience、question或discussion")
    private String postType;

    /**
     * 标签ID数组
     */
    private List<Long> tagIds;
}
