package com.worklog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterReq {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "用户名须为 3-20 位字母/数字/下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为 6-32 位")
    private String password;

    @Size(max = 20, message = "昵称最长 20 字")
    private String nickname;

    /** 注册角色：AUTHOR（默认，可写）/ READER（只读）；ADMIN 不可通过注册获得 */
    @Pattern(regexp = "^(AUTHOR|READER)$", message = "角色仅支持 AUTHOR 或 READER")
    private String role;
}
