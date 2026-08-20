package com.worklog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.worklog.dto.IssueReq;
import com.worklog.entity.Issue;

public interface IssueService {

    /** 分页查询：支持按状态 / 关键词 / 标签 / 项目过滤 */
    Page<Issue> getIssues(String status, String keyword, String tag, String project, long page, long size);

    Issue getIssue(Long id);

    Issue addIssue(IssueReq req);

    Issue updateIssue(Long id, IssueReq req);

    /** 仅切换状态 open <-> done */
    Issue toggleStatus(Long id, String status);

    /** 切换收藏状态（常见问题置顶） */
    Issue toggleFavorite(Long id);

    void deleteIssue(Long id);

    /** 全部问题导出 Excel（xlsx 字节流） */
    byte[] exportExcel();
}
