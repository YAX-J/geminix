package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.worklog.common.UserContext;
import com.worklog.dto.ImportReq;
import com.worklog.dto.ImportResultVO;
import com.worklog.entity.Issue;
import com.worklog.entity.Report;
import com.worklog.mapper.IssueMapper;
import com.worklog.mapper.ReportMapper;
import com.worklog.service.ImportService;
import com.worklog.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private final ReportMapper reportMapper;
    private final IssueMapper issueMapper;
    private final StatsService statsService;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern SECTION_PATTERN = Pattern.compile("(?m)^##\\s+");
    private static final String[] WEEK_CN = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};

    @Override
    public ImportResultVO importMarkdown(ImportReq req) {
        Long userId = UserContext.require();
        String project = req.getProject();
        List<String> tags = req.getTags() == null || req.getTags().isEmpty() ? List.of("导入") : req.getTags();
        boolean nonDateAsIssue = "issue".equals(req.getNonDateMode());

        ImportResultVO result = new ImportResultVO();
        result.setTotal(req.getFiles().size());

        for (ImportReq.ImportFile f : req.getFiles()) {
            String fileName = f.getName() == null ? "" : f.getName();
            String content = f.getContent() == null ? "" : f.getContent();
            try {
                parseAndInsert(userId, fileName, content, project, tags, nonDateAsIssue, result);
            } catch (Exception e) {
                log.warn("导入失败 file={} err={}", fileName, e.getMessage());
                ImportResultVO.Row row = row(fileName, "", "", "failed", e.getMessage());
                result.getRows().add(row);
            }
        }

        int skipped = result.getRows().stream()
                .filter(r -> r.getStatus().startsWith("skipped")).toList().size();
        result.setSkipped(skipped);
        result.setImported(result.getTotal() - skipped - (int) result.getRows().stream()
                .filter(r -> "failed".equals(r.getStatus())).count());

        if (result.getImported() > 0) {
            // 热力图/统计图表基于缓存，导入后失效重建
            statsService.evictStats();
        }
        return result;
    }

    private void parseAndInsert(Long userId, String fileName, String content,
                                String project, List<String> tags, boolean nonDateAsIssue,
                                ImportResultVO result) {
        // 1. 从文件名提取日期
        Matcher m = DATE_PATTERN.matcher(fileName);
        if (!m.find()) {
            if (nonDateAsIssue) {
                importAsIssue(userId, fileName, content, project, tags, result);
            } else {
                result.getRows().add(row(fileName, "", "", "skipped_non_date", "文件名不含日期，非日报文件"));
            }
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(m.group(1), DF);
        } catch (DateTimeParseException e) {
            result.getRows().add(row(fileName, m.group(1), "", "failed", "日期格式非法"));
            return;
        }

        // 2. 去重：同用户同日期已存在则跳过
        Long exist = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, userId)
                .eq(Report::getReportDate, date));
        if (exist != null && exist > 0) {
            result.getRows().add(row(fileName, date.toString(), "", "skipped_duplicate", "该日期日报已存在"));
            return;
        }

        // 3. 解析标题（首个 # 行）
        String title = extractTitle(content, date.toString());

        // 4. 解析工作内容（## 小节 → 每条 task）
        List<String> tasks = extractTasks(content, title);

        // 5. 落库
        Report report = new Report();
        report.setUserId(userId);
        report.setReportDate(date);
        report.setWeekday(weekCn(date.getDayOfWeek()));
        report.setTimeRange("");
        report.setTitle(title);
        report.setTasks(tasks);
        report.setTags(tags);
        report.setProject(project);
        reportMapper.insert(report);

        result.getRows().add(row(fileName, date.toString(), title, "imported", "导入 " + tasks.size() + " 条工作内容"));
    }

    /** 非日期文件：导入为问题（open 待解决），标题取首个 # 行或文件名 */
    private void importAsIssue(Long userId, String fileName, String content,
                               String project, List<String> tags, ImportResultVO result) {
        String base = fileName.replaceFirst("(?i)\\.(md|markdown|txt)$", "");
        String title = extractTitle(content, base);
        if (title.length() > 120) {
            title = title.substring(0, 120);
        }
        Long exist = issueMapper.selectCount(new LambdaQueryWrapper<Issue>()
                .eq(Issue::getUserId, userId)
                .eq(Issue::getTitle, title));
        if (exist != null && exist > 0) {
            result.getRows().add(row(fileName, "", title, "skipped_duplicate", "同名问题已存在"));
            return;
        }
        Issue issue = new Issue();
        issue.setUserId(userId);
        issue.setTitle(title);
        issue.setDescription(content);
        issue.setTag(tags.get(0));
        issue.setProject(project);
        issue.setStatus("open");
        issueMapper.insert(issue);
        result.getRows().add(row(fileName, "", title, "imported", "非日期文件，导入为问题"));
    }

    /** 标题：首个 "# " 行内容；无则用 fallback */
    private String extractTitle(String content, String fallback) {
        for (String line : content.split("\n")) {
            String t = line.trim();
            if (t.startsWith("# ") && t.length() > 2) {
                String title = t.substring(2).trim();
                if (title.length() > 120) {
                    title = title.substring(0, 120);
                }
                return title;
            }
        }
        return fallback;
    }

    /** 工作内容：按 "## " 小节拆分，每节一条 task（小节标题 + 其下 Markdown 原文）；无小节则整篇作为一条 */
    private List<String> extractTasks(String content, String title) {
        List<String> tasks = new ArrayList<>();
        String[] sections = SECTION_PATTERN.split(content);
        // sections[0] 为标题行之前的内容（通常为空），从 1 开始取小节
        for (int i = 1; i < sections.length; i++) {
            String sec = sections[i].trim();
            if (sec.isEmpty()) {
                continue;
            }
            String[] lines = sec.split("\n", 2);
            String heading = lines[0].trim();
            String body = lines.length > 1 ? lines[1].trim() : "";
            tasks.add(body.isEmpty() ? heading : heading + "\n" + body);
        }
        if (tasks.isEmpty()) {
            // 无 ## 小节：去掉标题行后整篇作为一条
            String body = content.trim();
            if (StringUtils.hasText(title)) {
                body = body.replaceFirst("(?m)^#\\s+.*$", "").trim();
            }
            if (!body.isEmpty()) {
                tasks.add(body);
            }
        }
        if (tasks.isEmpty()) {
            tasks.add(title);
        }
        return tasks;
    }

    private String weekCn(DayOfWeek dow) {
        return WEEK_CN[dow.getValue()];
    }

    private ImportResultVO.Row row(String fileName, String date, String title, String status, String reason) {
        ImportResultVO.Row r = new ImportResultVO.Row();
        r.setFileName(fileName);
        r.setDate(date);
        r.setTitle(title);
        r.setStatus(status);
        r.setReason(reason);
        return r;
    }
}
