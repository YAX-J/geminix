package com.worklog.dto;

import lombok.Data;

/**
 * 日报提醒状态：今日是否已写日报 / 是否触发提醒 / 连续打卡天数
 */
@Data
public class ReminderVO {

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 今日是否已写日报 */
    private boolean written;

    /** 是否触发写日报提醒 */
    private boolean remind;

    /** 连续打卡天数（含今天，若今天未写则从昨天起算） */
    private int streak;
}
