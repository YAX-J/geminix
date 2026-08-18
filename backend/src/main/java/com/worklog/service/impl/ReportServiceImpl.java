package com.worklog.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.worklog.common.BusinessException;
import com.worklog.common.UserContext;
import com.worklog.dto.ReportExcelRow;
import com.worklog.dto.ReportReq;
import com.worklog.entity.Issue;
import com.worklog.entity.Report;
import com.worklog.mapper.IssueMapper;
import com.worklog.mapper.ReportMapper;
import com.worklog.service.ReportService;
import com.worklog.service.ReminderService;
import com.worklog.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final IssueMapper issueMapper;
    private final StatsService statsService;
    private final ReminderService reminderService;

    private static final String[] WEEK_CN = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};

    @Override
    public Page<Report> getReports(LocalDate date, String keyword, String tag, long page, long size) {
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<>();
        qw.eq(Report::getUserId, UserContext.require());
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
        Report report = reportMapper.selectOne(new LambdaQueryWrapper<Report>()
                .eq(Report::getId, id)
                .eq(Report::getUserId, UserContext.require()));
        if (report == null) {
            throw new BusinessException(404, "日报不存在");
        }
        return report;
    }

    @Override
    public Report addReport(ReportReq req) {
        Report report = new Report();
        report.setUserId(UserContext.require());
        copyProps(report, req);
        reportMapper.insert(report);
        statsService.evictStats();
        reminderService.clearToday(req.getDate().toString());
        return report;
    }

    @Override
    public Report updateReport(Long id, ReportReq req) {
        Report exist = getReport(id);
        copyProps(exist, req);
        reportMapper.updateById(exist);
        statsService.evictStats();
        reminderService.clearToday(req.getDate().toString());
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

    @Override
    public String exportWeekly(LocalDate date) {
        LocalDate monday = date.minusDays((date.getDayOfWeek().getValue() + 6) % 7);
        LocalDate sunday = monday.plusDays(6);

        List<Report> reports = reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, UserContext.require())
                .between(Report::getReportDate, monday, sunday)
                .orderByAsc(Report::getReportDate));

        LocalDateTime weekStart = monday.atStartOfDay();
        LocalDateTime weekEnd = sunday.plusDays(1).atStartOfDay();
        List<Issue> issues = issueMapper.selectList(new LambdaQueryWrapper<Issue>()
                .eq(Issue::getUserId, UserContext.require())
                .ge(Issue::getCreatedAt, weekStart)
                .lt(Issue::getCreatedAt, weekEnd)
                .orderByAsc(Issue::getCreatedAt));

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder();
        sb.append("# 工作周报 ").append(monday.format(df)).append(" ~ ").append(sunday.format(df)).append("\n\n");

        // 按日期分组日报
        Map<LocalDate, List<Report>> byDate = new LinkedHashMap<>();
        for (Report r : reports) {
            byDate.computeIfAbsent(r.getReportDate(), k -> new java.util.ArrayList<>()).add(r);
        }
        sb.append("## 一、本周工作\n\n");
        if (byDate.isEmpty()) {
            sb.append("（本周暂无日报记录）\n\n");
        }
        for (Map.Entry<LocalDate, List<Report>> e : byDate.entrySet()) {
            LocalDate d = e.getKey();
            sb.append("### ").append(d.format(df)).append(" ").append(weekCn(d.getDayOfWeek())).append("\n\n");
            for (Report r : e.getValue()) {
                sb.append("- **").append(r.getTitle()).append("**");
                if (StringUtils.hasText(r.getTimeRange())) {
                    sb.append("（").append(r.getTimeRange()).append("）");
                }
                if (r.getTags() != null && !r.getTags().isEmpty()) {
                    sb.append(" `").append(String.join(" / ", r.getTags())).append("`");
                }
                sb.append("\n");
                if (r.getTasks() != null) {
                    for (String t : r.getTasks()) {
                        sb.append("  - ").append(t).append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        sb.append("## 二、本周问题与方案\n\n");
        if (issues.isEmpty()) {
            sb.append("（本周未记录问题）\n");
        }
        for (Issue i : issues) {
            String status = "done".equals(i.getStatus()) ? "✅ 已解决" : "⏳ 待解决";
            sb.append("- [").append(status).append("] **").append(i.getTitle()).append("**（")
                    .append(i.getTag() == null ? "未分类" : i.getTag()).append("）\n");
            if (StringUtils.hasText(i.getSolution())) {
                sb.append("  - 方案：").append(i.getSolution()).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String exportMonthly(LocalDate date) {
        YearMonth ym = YearMonth.from(date);
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();

        List<Report> reports = reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, UserContext.require())
                .between(Report::getReportDate, first, last)
                .orderByAsc(Report::getReportDate));
        List<Issue> issues = issueMapper.selectList(new LambdaQueryWrapper<Issue>()
                .eq(Issue::getUserId, UserContext.require())
                .ge(Issue::getCreatedAt, first.atStartOfDay())
                .lt(Issue::getCreatedAt, last.plusDays(1).atStartOfDay())
                .orderByAsc(Issue::getCreatedAt));

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder();
        sb.append("# 工作月报 ").append(ym.getYear()).append(" 年 ").append(ym.getMonthValue())
                .append(" 月\n\n");

        // 按日期分组日报
        Map<LocalDate, List<Report>> byDate = new LinkedHashMap<>();
        for (Report r : reports) {
            byDate.computeIfAbsent(r.getReportDate(), k -> new java.util.ArrayList<>()).add(r);
        }
        sb.append("## 一、本月工作\n\n");
        if (byDate.isEmpty()) {
            sb.append("（本月暂无日报记录）\n\n");
        }
        for (Map.Entry<LocalDate, List<Report>> e : byDate.entrySet()) {
            LocalDate d = e.getKey();
            sb.append("### ").append(d.format(df)).append(" ").append(weekCn(d.getDayOfWeek())).append("\n\n");
            for (Report r : e.getValue()) {
                sb.append("- **").append(r.getTitle()).append("**");
                if (StringUtils.hasText(r.getTimeRange())) {
                    sb.append("（").append(r.getTimeRange()).append("）");
                }
                if (r.getTags() != null && !r.getTags().isEmpty()) {
                    sb.append(" `").append(String.join(" / ", r.getTags())).append("`");
                }
                sb.append("\n");
                if (r.getTasks() != null) {
                    for (String t : r.getTasks()) {
                        sb.append("  - ").append(t).append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        sb.append("## 二、本月问题与方案\n\n");
        if (issues.isEmpty()) {
            sb.append("（本月未记录问题）\n");
        }
        for (Issue i : issues) {
            String status = "done".equals(i.getStatus()) ? "✅ 已解决" : "⏳ 待解决";
            sb.append("- [").append(status).append("] **").append(i.getTitle()).append("**（")
                    .append(i.getTag() == null ? "未分类" : i.getTag()).append("）\n");
            if (StringUtils.hasText(i.getSolution())) {
                sb.append("  - 方案：").append(i.getSolution()).append("\n");
            }
        }
        return sb.toString();
    }

    private String weekCn(DayOfWeek dow) {
        return WEEK_CN[dow.getValue()];
    }

    @Override
    public byte[] exportExcel() {
        List<Report> reports = reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, UserContext.require())
                .orderByDesc(Report::getReportDate).orderByDesc(Report::getId));
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<ReportExcelRow> rows = reports.stream().map(r -> {
            ReportExcelRow row = new ReportExcelRow();
            row.setDate(r.getReportDate() == null ? "" : r.getReportDate().format(df));
            row.setWeekday(r.getWeekday() == null ? "" : r.getWeekday());
            row.setTimeRange(r.getTimeRange() == null ? "" : r.getTimeRange());
            row.setTitle(r.getTitle() == null ? "" : r.getTitle());
            row.setTasks(r.getTasks() == null ? "" : String.join("\n", r.getTasks()));
            row.setTags(r.getTags() == null ? "" : String.join(" / ", r.getTags()));
            return row;
        }).toList();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, ReportExcelRow.class).sheet("日报").doWrite(rows);
        return out.toByteArray();
    }
}
