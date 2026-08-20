package com.worklog.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 导入结果：逐文件明细 + 汇总统计
 */
@Data
public class ImportResultVO {

    private int total;
    private int imported;
    private int skipped;
    private List<Row> rows = new ArrayList<>();

    @Data
    public static class Row {
        /** 原文件名 */
        private String fileName;
        /** 识别到的日期（非日期文件为空） */
        private String date;
        /** 导入后的日报标题 */
        private String title;
        /** imported / skipped_duplicate / skipped_non_date / failed */
        private String status;
        /** 说明（失败原因 / 跳过原因） */
        private String reason;
    }
}
