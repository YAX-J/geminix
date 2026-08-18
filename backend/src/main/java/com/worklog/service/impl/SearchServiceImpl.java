package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.worklog.common.UserContext;
import com.worklog.dto.SearchResultVO;
import com.worklog.entity.Issue;
import com.worklog.entity.Report;
import com.worklog.mapper.IssueMapper;
import com.worklog.mapper.ReportMapper;
import com.worklog.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final int MAX_EACH = 20;

    private final ReportMapper reportMapper;
    private final IssueMapper issueMapper;

    @Override
    public SearchResultVO search(String keyword) {
        Long uid = UserContext.require();
        String kw = keyword == null ? "" : keyword.trim();
        if (!StringUtils.hasText(kw)) {
            return new SearchResultVO(List.of(), List.of());
        }

        List<Report> reports = reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, uid)
                .and(w -> w.like(Report::getTitle, kw)
                        .or().like(Report::getTasks, kw)
                        .or().like(Report::getTags, kw))
                .orderByDesc(Report::getReportDate)
                .last("LIMIT " + MAX_EACH));

        List<Issue> issues = issueMapper.selectList(new LambdaQueryWrapper<Issue>()
                .eq(Issue::getUserId, uid)
                .and(w -> w.like(Issue::getTitle, kw)
                        .or().like(Issue::getDescription, kw)
                        .or().like(Issue::getSolution, kw))
                .orderByDesc(Issue::getCreatedAt)
                .last("LIMIT " + MAX_EACH));

        return new SearchResultVO(reports, issues);
    }
}
