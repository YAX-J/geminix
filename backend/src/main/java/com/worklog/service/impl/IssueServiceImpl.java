package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.worklog.common.BusinessException;
import com.worklog.dto.IssueReq;
import com.worklog.entity.Issue;
import com.worklog.mapper.IssueMapper;
import com.worklog.service.IssueService;
import com.worklog.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final IssueMapper issueMapper;
    private final StatsService statsService;

    private static final List<String> VALID_STATUS = List.of("open", "done");

    @Override
    public Page<Issue> getIssues(String status, String keyword, String tag, long page, long size) {
        LambdaQueryWrapper<Issue> qw = new LambdaQueryWrapper<>();
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
        qw.orderByDesc(Issue::getCreatedAt).orderByDesc(Issue::getId);
        return issueMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public Issue getIssue(Long id) {
        Issue issue = issueMapper.selectById(id);
        if (issue == null) {
            throw new BusinessException(404, "问题不存在");
        }
        return issue;
    }

    @Override
    public Issue addIssue(IssueReq req) {
        Issue issue = new Issue();
        issue.setTitle(req.getTitle());
        issue.setDescription(req.getDescription());
        issue.setSolution(normalize(req.getSolution()));
        issue.setTag(StringUtils.hasText(req.getTag()) ? req.getTag() : "后端");
        issue.setReportDate(req.getReportDate());
        // 填写了解决方案且未指定状态 -> 自动已解决
        if (StringUtils.hasText(issue.getSolution())) {
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
        exist.setSolution(normalize(req.getSolution()));
        if (StringUtils.hasText(req.getTag())) {
            exist.setTag(req.getTag());
        }
        exist.setReportDate(req.getReportDate());
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
    public void deleteIssue(Long id) {
        getIssue(id);
        issueMapper.deleteById(id);
        statsService.evictStats();
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
