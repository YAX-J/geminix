package com.worklog.controller;

import com.worklog.common.R;
import com.worklog.config.RequireRole;
import com.worklog.dto.ImportReq;
import com.worklog.dto.ImportResultVO;
import com.worklog.service.ImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Markdown 批量导入（Obsidian / workbuddy memory 等本地 Markdown 目录 → 日报）
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    /**
     * 批量导入 Markdown 文件为日报
     * 请求体：{ "files": [ { "name": "2026-08-20.md", "content": "# ..." } ] }
     */
    @PostMapping("/markdown")
    @RequireRole({"ADMIN", "AUTHOR"})
    public R<ImportResultVO> importMarkdown(@Valid @RequestBody ImportReq req) {
        return R.ok(importService.importMarkdown(req));
    }
}
