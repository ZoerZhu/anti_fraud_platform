package com.anti.entity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private Long id;

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;

    @Size(max = 500, message = "头像地址不能超过500个字符")
    private String avatar;

    @Size(max = 20, message = "年级不能超过20个字符")
    private String grade;

    @Size(max = 100, message = "专业不能超过100个字符")
    private String major;
}
