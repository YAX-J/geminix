package com.worklog.dto;

import com.worklog.entity.Issue;
import com.worklog.entity.Report;
import lombok.Data;

import java.util.List;

/**
 * 全局搜索结果：日报 + 问题 两组结果
 */
@Data
public class SearchResultVO {

    private List<Report> reports;
    private List<Issue> issues;

    public SearchResultVO(List<Report> reports, List<Issue> issues) {
        this.reports = reports;
        this.issues = issues;
    }
}
