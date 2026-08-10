package com.worklog.controller;

import com.worklog.common.R;
import com.worklog.dto.LoginReq;
import com.worklog.dto.LoginVO;
import com.worklog.dto.RegisterReq;
import com.worklog.dto.UserVO;
import com.worklog.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 注册（成功即返回 token，自动登录） */
    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterReq req) {
        return R.ok(authService.register(req));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginReq req) {
        return R.ok(authService.login(req));
    }

    /** 当前登录用户信息（由 JWT 拦截器解析 token 后注入 userId） */
    @GetMapping("/me")
    public R<UserVO> me(@RequestAttribute("userId") Long userId) {
        return R.ok(authService.me(userId));
    }
}
