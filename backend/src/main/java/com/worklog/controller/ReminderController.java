package com.worklog.controller;

import com.worklog.common.R;
import com.worklog.dto.ReminderVO;
import com.worklog.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日报提醒接口
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    /** 今日日报提醒状态：是否已写 / 是否提醒 / 连续打卡天数 */
    @GetMapping("/today-status")
    public R<ReminderVO> todayStatus() {
        return R.ok(reminderService.todayStatus());
    }
}
