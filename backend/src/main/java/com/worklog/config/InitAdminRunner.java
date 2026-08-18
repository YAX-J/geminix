package com.worklog.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.worklog.entity.User;
import com.worklog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动初始化默认管理员账号 admin / admin123
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitAdminRunner implements CommandLineRunner {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (existing != null) {
            // 存量 admin 若缺角色（升级场景），补设 ADMIN
            if (existing.getRole() == null || existing.getRole().isEmpty()) {
                existing.setRole("ADMIN");
                userMapper.updateById(existing);
                log.info("已为存量 admin 账号补设 ADMIN 角色");
            }
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("管理员");
        admin.setRole("ADMIN");
        userMapper.insert(admin);
        log.info("已创建默认账号 admin / admin123（ADMIN）");
    }
}
