package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.worklog.common.UserContext;
import com.worklog.dto.ReminderVO;
import com.worklog.entity.Report;
import com.worklog.entity.User;
import com.worklog.mapper.ReportMapper;
import com.worklog.mapper.UserMapper;
import com.worklog.service.ReminderService;
import com.worklog.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日报提醒服务：定时任务每天 18:00 遍历所有用户，检查今日未写日报的用户并写入 Redis 提醒标记；
 * 前端进入页面时实时查询当前用户今日状态展示提醒条。
 *
 * Redis 键：worklog:remind:{yyyy-MM-dd}:{userId} = "1"（TTL 至当日 24:00）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private static final String KEY_REMIND_PREFIX = "worklog:remind:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final StatsService statsService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public ReminderVO todayStatus() {
        Long uid = UserContext.require();
        LocalDate today = LocalDate.now();
        boolean written = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, uid)
                .eq(Report::getReportDate, today)) > 0;
        String remind = redisTemplate.opsForValue().get(key(today, uid));
        ReminderVO vo = new ReminderVO();
        vo.setDate(today.format(DATE_FMT));
        vo.setWritten(written);
        vo.setRemind(!written && "1".equals(remind));
        vo.setStreak(statsService.streak());
        return vo;
    }

    @Override
    public void remindIfNeeded() {
        LocalDate today = LocalDate.now();
        List<User> users = userMapper.selectList(null);
        if (users.isEmpty()) {
            log.info("[remind] 无用户，跳过提醒检查");
            return;
        }
        int reminded = 0;
        for (User user : users) {
            long count = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                    .eq(Report::getUserId, user.getId())
                    .eq(Report::getReportDate, today));
            if (count > 0) {
                continue;
            }
            redisTemplate.opsForValue().set(key(today, user.getId()), "1",
                    Duration.between(LocalDateTime.now(), today.atTime(LocalTime.MAX)));
            reminded++;
        }
        log.info("[remind] 今日未写日报的用户数: {}", reminded);
    }

    @Override
    public void clearToday(String reportDate) {
        if (!StringUtils.hasText(reportDate)) {
            return;
        }
        Long uid = UserContext.get();
        String k = uid == null
                ? KEY_REMIND_PREFIX + reportDate
                : key(LocalDate.parse(reportDate), uid);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(k))) {
            redisTemplate.delete(k);
        }
    }

    private String key(LocalDate date, Long userId) {
        return KEY_REMIND_PREFIX + date.format(DATE_FMT) + ":" + userId;
    }
}
