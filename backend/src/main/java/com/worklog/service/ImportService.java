package com.worklog.service;

import com.worklog.dto.ImportReq;
import com.worklog.dto.ImportResultVO;

/**
 * Markdown 导入：将 YYYY-MM-DD.md 按日期解析为日报
 */
public interface ImportService {

    /**
     * 批量导入 Markdown 文件为日报
     * 规则：文件名含 yyyy-MM-dd → 日报日期；首个 # 行 → 标题；## 小节 → 工作内容
     * 去重：同一用户同一日期已存在则跳过；非日期命名文件跳过
     */
    ImportResultVO importMarkdown(ImportReq req);
}
