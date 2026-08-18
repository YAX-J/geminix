package com.worklog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.worklog.common.R;
import com.worklog.config.RequireRole;
import com.worklog.dto.ReportReq;
import com.worklog.entity.Report;
import com.worklog.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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
    @RequireRole({"ADMIN", "AUTHOR"})
    public R<Report> add(@Valid @RequestBody ReportReq req) {
        return R.ok(reportService.addReport(req));
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "AUTHOR"})
    public R<Report> update(@PathVariable Long id, @Valid @RequestBody ReportReq req) {
        return R.ok(reportService.updateReport(id, req));
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "AUTHOR"})
    public R<Void> delete(@PathVariable Long id) {
        reportService.deleteReport(id);
        return R.ok();
    }

    /** 周报导出：返回 Markdown 文件（该日期所在周），如 /api/reports/export/weekly?date=2026-08-10 */
    @GetMapping("/export/weekly")
    public ResponseEntity<byte[]> exportWeekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String md = reportService.exportWeekly(date);
        String filename = "worklog-week-" + date + ".md";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .body(md.getBytes(StandardCharsets.UTF_8));
    }

    /** 月报导出：返回 Markdown 文件（该日期所在自然月），如 /api/reports/export/monthly?date=2026-08-10 */
    @GetMapping("/export/monthly")
    public ResponseEntity<byte[]> exportMonthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String md = reportService.exportMonthly(date);
        String filename = "worklog-month-" + date + ".md";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .body(md.getBytes(StandardCharsets.UTF_8));
    }

    /** 日报 Excel 导出：/api/reports/export/excel */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] bytes = reportService.exportExcel();
        String filename = "worklog-reports.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
