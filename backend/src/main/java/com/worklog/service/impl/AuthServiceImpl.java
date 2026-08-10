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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 登录失败计数缓存 key */
    private static final String LOGIN_FAIL_KEY = "worklog:login:fail:";
    /** 允许的最大失败次数 */
    private static final int MAX_FAIL_TIMES = 5;
    /** 锁定时间（分钟） */
    private static final long LOCK_MINUTES = 10;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
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
        String failKey = LOGIN_FAIL_KEY + req.getUsername();
        // 防爆破：失败次数达上限则锁定（提示剩余倒计时）
        Long failCount = readFailCount(failKey);
        if (failCount != null && failCount >= MAX_FAIL_TIMES) {
            long remainSeconds = getRemainSeconds(failKey);
            throw new BusinessException(429, "失败次数过多已锁定，请 " + formatRemain(remainSeconds) + " 后再试");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                redisTemplate.expire(failKey, LOCK_MINUTES, TimeUnit.MINUTES);
            }
            long current = count == null ? 0 : count;
            if (current >= MAX_FAIL_TIMES) {
                throw new BusinessException(429, "失败次数过多已锁定，请 " + LOCK_MINUTES + " 分钟后再试");
            }
            int remain = Math.max(0, MAX_FAIL_TIMES - (int) current);
            throw new BusinessException(400, "用户名或密码错误，剩余尝试 " + remain + " 次");
        }
        // 登录成功清除失败计数
        redisTemplate.delete(failKey);
        return buildLoginVO(user);
    }

    /** 读取失败计数（容错：Redis 脏数据按 0 处理） */
    private Long readFailCount(String key) {
        String v = redisTemplate.opsForValue().get(key);
        if (v == null) {
            return null;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 剩余锁定秒数（键不存在返回 0） */
    private long getRemainSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? 0 : ttl;
    }

    private String formatRemain(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        if (m <= 0) {
            return s + " 秒";
        }
        if (s == 0) {
            return m + " 分钟";
        }
        return m + " 分 " + s + " 秒";
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
