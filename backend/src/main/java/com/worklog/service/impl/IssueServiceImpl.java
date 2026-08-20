package com.worklog.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.worklog.common.BusinessException;
import com.worklog.common.UserContext;
import com.worklog.dto.IssueExcelRow;
import com.worklog.dto.IssueReq;
import com.worklog.entity.Issue;
import com.worklog.entity.Issue.IssueSolution;
import com.worklog.mapper.IssueMapper;
import com.worklog.service.IssueService;
import com.worklog.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final IssueMapper issueMapper;
    private final StatsService statsService;

    private static final List<String> VALID_STATUS = List.of("open", "done");

    @Override
    public Page<Issue> getIssues(String status, String keyword, String tag, String project, long page, long size) {
        LambdaQueryWrapper<Issue> qw = new LambdaQueryWrapper<>();
        qw.eq(Issue::getUserId, UserContext.require());
        if (StringUtils.hasText(status)) {
            qw.eq(Issue::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            qw.and(w -> w.like(Issue::getTitle, kw)
                    .or().like(Issue::getDescription, kw)
                    .or().like(Issue::getSolution, kw));
        }
        if (StringUtils.hasText(tag)) {
            qw.eq(Issue::getTag, tag.trim());
        }
        if (StringUtils.hasText(project)) {
            qw.eq(Issue::getProject, project.trim());
        }
        // 常见问题（收藏）置顶，其次最新创建
        qw.orderByDesc(Issue::getFavorite).orderByDesc(Issue::getCreatedAt).orderByDesc(Issue::getId);
        return issueMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public Issue getIssue(Long id) {
        Issue issue = issueMapper.selectOne(new LambdaQueryWrapper<Issue>()
                .eq(Issue::getId, id)
                .eq(Issue::getUserId, UserContext.require()));
        if (issue == null) {
            throw new BusinessException(404, "问题不存在");
        }
        return issue;
    }

    @Override
    public Issue addIssue(IssueReq req) {
        Issue issue = new Issue();
        issue.setUserId(UserContext.require());
        issue.setTitle(req.getTitle());
        issue.setDescription(req.getDescription());
        issue.setTag(StringUtils.hasText(req.getTag()) ? req.getTag() : "后端");
        issue.setReportDate(req.getReportDate());
        issue.setProject(req.getProject());
        issue.setFavorite(Boolean.TRUE.equals(req.getFavorite()));
        applySolutions(issue, req);
        // 有方案（solution 或 solutions 非空）且未指定状态 -> 自动已解决
        if (StringUtils.hasText(issue.getSolution()) || hasAnySolution(issue.getSolutions())) {
            issue.setStatus("done");
        } else {
            issue.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "open");
        }
        validateStatus(issue.getStatus());
        issueMapper.insert(issue);
        statsService.evictStats();
        return issue;
    }

    @Override
    public Issue updateIssue(Long id, IssueReq req) {
        Issue exist = getIssue(id);
        exist.setTitle(req.getTitle());
        exist.setDescription(req.getDescription());
        if (StringUtils.hasText(req.getTag())) {
            exist.setTag(req.getTag());
        }
        exist.setReportDate(req.getReportDate());
        exist.setProject(req.getProject());
        if (req.getFavorite() != null) {
            exist.setFavorite(req.getFavorite());
        }
        applySolutions(exist, req);
        if (StringUtils.hasText(req.getStatus())) {
            validateStatus(req.getStatus());
            exist.setStatus(req.getStatus());
        }
        issueMapper.updateById(exist);
        statsService.evictStats();
        return exist;
    }

    @Override
    public Issue toggleStatus(Long id, String status) {
        Issue exist = getIssue(id);
        String target = StringUtils.hasText(status) ? status
                : ("done".equals(exist.getStatus()) ? "open" : "done");
        validateStatus(target);
        exist.setStatus(target);
        issueMapper.updateById(exist);
        statsService.evictStats();
        return exist;
    }

    @Override
    public Issue toggleFavorite(Long id) {
        Issue exist = getIssue(id);
        exist.setFavorite(!Boolean.TRUE.equals(exist.getFavorite()));
        issueMapper.updateById(exist);
        return exist;
    }

    @Override
    public void deleteIssue(Long id) {
        getIssue(id);
        issueMapper.deleteById(id);
        statsService.evictStats();
    }

    @Override
    public byte[] exportExcel() {
        List<Issue> issues = issueMapper.selectList(new LambdaQueryWrapper<Issue>()
                .eq(Issue::getUserId, UserContext.require())
                .orderByDesc(Issue::getCreatedAt).orderByDesc(Issue::getId));
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<IssueExcelRow> rows = issues.stream().map(i -> {
            IssueExcelRow row = new IssueExcelRow();
            row.setTitle(i.getTitle() == null ? "" : i.getTitle());
            row.setDescription(i.getDescription() == null ? "" : i.getDescription());
            row.setSolution(i.getSolution() == null ? "" : i.getSolution());
            row.setTag(i.getTag() == null ? "" : i.getTag());
            row.setStatus("done".equals(i.getStatus()) ? "已解决" : "待解决");
            row.setReportDate(i.getReportDate() == null ? "" : i.getReportDate().toString());
            row.setCreatedAt(i.getCreatedAt() == null ? "" : i.getCreatedAt().format(df));
            return row;
        }).toList();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, IssueExcelRow.class).sheet("问题").doWrite(rows);
        return out.toByteArray();
    }

    /**
     * 处理多方案：清洗非法项，best 方案摘要同步回 solution（兼容旧字段），
     * 请求未传 solutions 时保留原值（编辑场景）。
     */
    private void applySolutions(Issue issue, IssueReq req) {
        List<IssueSolution> reqSolutions = req.getSolutions();
        if (reqSolutions == null) {
            // 未传多方案：仅当显式传了 solution 时覆盖摘要
            if (req.getSolution() != null) {
                issue.setSolution(normalize(req.getSolution()));
            }
            return;
        }
        List<IssueSolution> cleaned = reqSolutions.stream()
                .filter(s -> s != null && StringUtils.hasText(s.getContent()))
                .map(s -> {
                    IssueSolution ns = new IssueSolution();
                    ns.setContent(s.getContent().trim());
                    ns.setBest(Boolean.TRUE.equals(s.getBest()));
                    return ns;
                })
                .toList();
        issue.setSolutions(cleaned.isEmpty() ? null : cleaned);
        // 最佳方案摘要回写 solution（无 best 则取第一个）
        if (cleaned.isEmpty()) {
            issue.setSolution("");
        } else {
            IssueSolution best = cleaned.stream().filter(s -> Boolean.TRUE.equals(s.getBest()))
                    .findFirst().orElse(cleaned.get(0));
            issue.setSolution(best.getContent());
        }
    }

    private boolean hasAnySolution(List<IssueSolution> solutions) {
        return solutions != null && !solutions.isEmpty();
    }

    private String normalize(String s) {
        return StringUtils.hasText(s) ? s.trim() : "";
    }

    private void validateStatus(String status) {
        if (!VALID_STATUS.contains(status)) {
            throw new BusinessException(400, "非法状态: " + status + "（仅支持 open/done）");
        }
    }
}
