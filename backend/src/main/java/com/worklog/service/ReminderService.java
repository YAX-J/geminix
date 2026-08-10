package com.worklog.service;

import com.worklog.dto.ReminderVO;

public interface ReminderService {

    /** 今日日报提醒状态（written / remind / streak） */
    ReminderVO todayStatus();

    /** 定时任务调用：18:00 检查今日未写日报则写入 Redis 提醒标记 */
    void remindIfNeeded();

    /** 日报写入成功后清除当日提醒标记 */
    void clearToday(String reportDate);
}
