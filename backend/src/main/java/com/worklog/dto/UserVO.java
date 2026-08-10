package com.worklog.dto;

import lombok.Data;

/**
 * 用户信息（脱敏，不含密码）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;

    public static UserVO of(Long id, String username, String nickname) {
        UserVO vo = new UserVO();
        vo.setId(id);
        vo.setUsername(username);
        vo.setNickname(nickname);
        return vo;
    }
}
