package com.worklog.service;

import com.worklog.dto.UserVO;

import java.util.List;

/**
 * 用户管理服务（ADMIN 专用）
 */
public interface UserService {

    /** 用户列表 */
    List<UserVO> listUsers();

    /** 修改用户角色（ADMIN / AUTHOR / READER） */
    void updateRole(Long id, String role);

    /** 重置用户密码 */
    void resetPassword(Long id, String password);
}
