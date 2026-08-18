package com.worklog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.worklog.common.R;
import com.worklog.config.RequireRole;
import com.worklog.dto.IssueReq;
import com.worklog.entity.Issue;
import com.worklog.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    /** 问题列表：支持 status / keyword / tag 过滤 */
    @GetMapping
    public R<Page<Issue>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(issueService.getIssues(status, keyword, tag, page, size));
    }

    @GetMapping("/{id}")
    public R<Issue> detail(@PathVariable Long id) {
        return R.ok(issueService.getIssue(id));
    }

    @PostMapping
    @RequireRole({"ADMIN", "AUTHOR"})
    public R<Issue> add(@Valid @RequestBody IssueReq req) {
        return R.ok(issueService.addIssue(req));
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "AUTHOR"})
    public R<Issue> update(@PathVariable Long id, @Valid @RequestBody IssueReq req) {
        return R.ok(issueService.updateIssue(id, req));
    }

    /** 状态切换（标记已解决 / 重新打开），不传 status 时自动取反 */
    @PatchMapping("/{id}/status")
    @RequireRole({"ADMIN", "AUTHOR"})
    public R<Issue> toggleStatus(@PathVariable Long id, @RequestParam(required = false) String status) {
        return R.ok(issueService.toggleStatus(id, status));
    }

    /** 收藏切换（常见问题置顶） */
    @PatchMapping("/{id}/favorite")
    @RequireRole({"ADMIN", "AUTHOR"})
    public R<Issue> toggleFavorite(@PathVariable Long id) {
        return R.ok(issueService.toggleFavorite(id));
    }

    /** 问题 Excel 导出：/api/issues/export/excel */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] bytes = issueService.exportExcel();
        String filename = "worklog-issues.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "AUTHOR"})
    public R<Void> delete(@PathVariable Long id) {
        issueService.deleteIssue(id);
        return R.ok();
    }
}
