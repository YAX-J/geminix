package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.worklog.common.BusinessException;
import com.worklog.dto.LoginReq;
import com.worklog.dto.LoginVO;
import com.worklog.dto.RegisterReq;
import com.worklog.dto.UserVO;
import com.worklog.entity.User;
import com.worklog.mapper.UserMapper;
import com.worklog.service.AuthService;
import com.worklog.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO register(RegisterReq req) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (exists > 0) {
            throw new BusinessException(400, "用户名已被注册");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : req.getUsername());
        userMapper.insert(user);
        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginReq req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        return buildLoginVO(user);
    }

    @Override
    public UserVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已失效");
        }
        return UserVO.of(user.getId(), user.getUsername(), user.getNickname());
    }

    private LoginVO buildLoginVO(User user) {
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        return new LoginVO(token, UserVO.of(user.getId(), user.getUsername(), user.getNickname()));
    }
}
