package com.worklog.task;

import com.worklog.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：每天 18:00 提醒写日报
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportReminderTask {

    private final ReminderService reminderService;

    /** 每天 18:00 执行：今日未写日报则写入 Redis 提醒标记 */
    @Scheduled(cron = "0 0 18 * * ?")
    public void remindDaily() {
        log.info("[remind] 定时任务触发：检查今日日报");
        reminderService.remindIfNeeded();
    }
}
