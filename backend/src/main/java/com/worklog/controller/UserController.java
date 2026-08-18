package com.worklog.controller;

import com.worklog.common.R;
import com.worklog.config.RequireRole;
import com.worklog.dto.UserVO;
import com.worklog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口（ADMIN 专用）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class UserController {

    private final UserService userService;

    /** 用户列表 */
    @GetMapping
    public R<List<UserVO>> list() {
        return R.ok(userService.listUsers());
    }

    /** 修改用户角色 */
    @PutMapping("/{id}/role")
    public R<Void> updateRole(@PathVariable Long id, @RequestParam String role) {
        userService.updateRole(id, role);
        return R.ok();
    }

    /** 重置用户密码 */
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return R.ok();
    }
}
