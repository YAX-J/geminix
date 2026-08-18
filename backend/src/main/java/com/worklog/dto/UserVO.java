package com.worklog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息（脱敏，不含密码）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;

    public static UserVO of(Long id, String username, String nickname, String role) {
        UserVO vo = new UserVO();
        vo.setId(id);
        vo.setUsername(username);
        vo.setNickname(nickname);
        vo.setRole(role);
        return vo;
    }
}
