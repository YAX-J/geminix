package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.worklog.common.BusinessException;
import com.worklog.common.UserContext;
import com.worklog.dto.UserVO;
import com.worklog.entity.User;
import com.worklog.mapper.UserMapper;
import com.worklog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final List<String> VALID_ROLES = List.of("ADMIN", "AUTHOR", "READER");

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public List<UserVO> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getId))
                .stream()
                .map(u -> {
                    UserVO vo = UserVO.of(u.getId(), u.getUsername(), u.getNickname(), u.getRole());
                    vo.setCreatedAt(u.getCreatedAt());
                    return vo;
                })
                .toList();
    }

    @Override
    public void updateRole(Long id, String role) {
        if (!StringUtils.hasText(role) || !VALID_ROLES.contains(role)) {
            throw new BusinessException(400, "非法角色，仅支持 ADMIN/AUTHOR/READER");
        }
        User user = requireUser(id);
        // 保护：不允许把最后一个 ADMIN 降级
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(role)) {
            long adminCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "ADMIN"));
            if (adminCount <= 1) {
                throw new BusinessException(400, "至少保留一个管理员");
            }
        }
        user.setRole(role);
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id, String password) {
        if (!StringUtils.hasText(password) || password.length() < 6 || password.length() > 32) {
            throw new BusinessException(400, "密码长度须为 6-32 位");
        }
        User user = requireUser(id);
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}
