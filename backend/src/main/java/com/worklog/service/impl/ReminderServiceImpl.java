package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.worklog.dto.ReminderVO;
import com.worklog.entity.Report;
import com.worklog.mapper.ReportMapper;
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

/**
 * 日报提醒服务：定时任务每天 18:00 检查今日未写日报的用户并写入 Redis 提醒标记；
 * 前端进入页面时实时查询今日状态展示提醒条。
 *
 * Redis 键：worklog:remind:{yyyy-MM-dd} = "1"（TTL 至当日 24:00）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private static final String KEY_REMIND_PREFIX = "worklog:remind:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportMapper reportMapper;
    private final StatsService statsService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public ReminderVO todayStatus() {
        LocalDate today = LocalDate.now();
        boolean written = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReportDate, today)) > 0;
        String remind = redisTemplate.opsForValue().get(key(today));
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
        long count = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReportDate, today));
        if (count > 0) {
            log.info("[remind] 今日已有日报，无需提醒");
            return;
        }
        String k = key(today);
        redisTemplate.opsForValue().set(k, "1", Duration.between(LocalDateTime.now(), today.atTime(LocalTime.MAX)));
        log.info("[remind] 今日未写日报，已写入提醒标记: {}", k);
    }

    @Override
    public void clearToday(String reportDate) {
        if (!StringUtils.hasText(reportDate)) {
            return;
        }
        String k = KEY_REMIND_PREFIX + reportDate;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(k))) {
            redisTemplate.delete(k);
        }
    }

    private String key(LocalDate date) {
        return KEY_REMIND_PREFIX + date.format(DATE_FMT);
    }
}
