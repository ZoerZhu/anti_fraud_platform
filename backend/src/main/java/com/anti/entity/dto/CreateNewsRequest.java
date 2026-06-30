package com.anti.entity.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class CreateNewsRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 120, message = "标题不能超过120个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 255, message = "摘要不能超过255个字符")
    private String summary;

    @Size(max = 255, message = "封面图地址不能超过255个字符")
    private String coverImage;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Size(max = 30, message = "资讯类型不能超过30个字符")
    @Pattern(regexp = "news|warning|policy", message = "资讯类型只能为news、warning或policy")
    private String newsType;

    @Min(value = 0, message = "置顶状态只能为0或1")
    @Max(value = 1, message = "置顶状态只能为0或1")
    private Integer isTop;

    @Min(value = 0, message = "必读状态只能为0或1")
    @Max(value = 1, message = "必读状态只能为0或1")
    private Integer isMandatory;
}
