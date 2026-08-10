package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.worklog.common.BusinessException;
import com.worklog.dto.ReportReq;
import com.worklog.entity.Report;
import com.worklog.mapper.ReportMapper;
import com.worklog.service.ReportService;
import com.worklog.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final StatsService statsService;

    private static final String[] WEEK_CN = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};

    @Override
    public Page<Report> getReports(LocalDate date, String keyword, String tag, long page, long size) {
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<>();
        if (date != null) {
            qw.eq(Report::getReportDate, date);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            qw.and(w -> w.like(Report::getTitle, kw)
                    .or().like(Report::getTasks, kw)
                    .or().like(Report::getTags, kw));
        }
        if (StringUtils.hasText(tag)) {
            qw.like(Report::getTags, "\"" + tag.trim() + "\"");
        }
        qw.orderByDesc(Report::getReportDate).orderByDesc(Report::getId);
        return reportMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public Report getReport(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(404, "日报不存在");
        }
        return report;
    }

    @Override
    public Report addReport(ReportReq req) {
        Report report = new Report();
        copyProps(report, req);
        reportMapper.insert(report);
        statsService.evictStats();
        return report;
    }

    @Override
    public Report updateReport(Long id, ReportReq req) {
        Report exist = getReport(id);
        copyProps(exist, req);
        reportMapper.updateById(exist);
        statsService.evictStats();
        return exist;
    }

    @Override
    public void deleteReport(Long id) {
        getReport(id);
        reportMapper.deleteById(id);
        statsService.evictStats();
    }

    private void copyProps(Report report, ReportReq req) {
        report.setReportDate(req.getDate());
        report.setWeekday(weekCn(req.getDate().getDayOfWeek()));
        report.setTimeRange(StringUtils.hasText(req.getTimeRange()) ? req.getTimeRange() : "");
        report.setTitle(req.getTitle());
        report.setTasks(req.getTasks());
        report.setTags(req.getTags() == null || req.getTags().isEmpty() ? List.of("后端") : req.getTags());
    }

    private String weekCn(DayOfWeek dow) {
        return WEEK_CN[dow.getValue()];
    }
}
