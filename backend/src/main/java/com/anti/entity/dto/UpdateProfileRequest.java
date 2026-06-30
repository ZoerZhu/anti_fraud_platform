package com.anti.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 20, message = "年级不能超过20个字符")
    private String grade;

    @Size(max = 100, message = "专业不能超过100个字符")
    private String major;

    @Min(value = 0, message = "知识水平不能小于0")
    @Max(value = 100, message = "知识水平不能大于100")
    private Integer knowledgeLevel;

    @Size(max = 500, message = "薄弱点不能超过500个字符")
    private String weakPoints;

    @Size(max = 500, message = "兴趣标签不能超过500个字符")
    private String interestTags;
}
