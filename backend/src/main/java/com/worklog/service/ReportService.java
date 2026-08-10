package com.worklog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.worklog.dto.ReportReq;
import com.worklog.entity.Report;

import java.time.LocalDate;

public interface ReportService {

    /** 分页查询：支持按日期 / 关键词 / 标签过滤，按日期倒序 */
    Page<Report> getReports(LocalDate date, String keyword, String tag, long page, long size);

    Report getReport(Long id);

    Report addReport(ReportReq req);

    Report updateReport(Long id, ReportReq req);

    void deleteReport(Long id);

    /** 生成指定日期所在周的周报（Markdown 文本） */
    String exportWeekly(java.time.LocalDate date);

    /** 生成指定日期所在自然月的月报（Markdown 文本） */
    String exportMonthly(java.time.LocalDate date);

    /** 全部日报导出 Excel（xlsx 字节流） */
    byte[] exportExcel();
}
