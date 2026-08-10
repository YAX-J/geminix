package com.worklog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.worklog.common.R;
import com.worklog.dto.ReportReq;
import com.worklog.entity.Report;
import com.worklog.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 日报列表：支持 date / keyword / tag 过滤，默认分页 page=1 size=20 */
    @GetMapping
    public R<Page<Report>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(reportService.getReports(date, keyword, tag, page, size));
    }

    @GetMapping("/{id}")
    public R<Report> detail(@PathVariable Long id) {
        return R.ok(reportService.getReport(id));
    }

    @PostMapping
    public R<Report> add(@Valid @RequestBody ReportReq req) {
        return R.ok(reportService.addReport(req));
    }

    @PutMapping("/{id}")
    public R<Report> update(@PathVariable Long id, @Valid @RequestBody ReportReq req) {
        return R.ok(reportService.updateReport(id, req));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        reportService.deleteReport(id);
        return R.ok();
    }
}
