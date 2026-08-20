package com.worklog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Markdown 批量导入请求：前端读取本地 .md 文件内容后整体提交
 */
@Data
public class ImportReq {

    @NotEmpty(message = "文件列表不能为空")
    @Valid
    private List<ImportFile> files;

    /** 所属项目（可选，导入日报/问题时统一归类） */
    private String project;

    /** 标签（可选，默认 ["导入"]） */
    private List<String> tags;

    /** 非日期文件处理方式：skip 跳过（默认） / issue 导入为问题 */
    private String nonDateMode;

    @Data
    public static class ImportFile {
        @NotEmpty(message = "文件名不能为空")
        private String name;

        private String content;
    }
}
