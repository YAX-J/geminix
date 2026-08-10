package com.worklog.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 问题 Excel 导出行模型
 */
@Data
public class IssueExcelRow {

    @ExcelProperty("问题标题")
    private String title;

    @ExcelProperty("问题描述")
    private String description;

    @ExcelProperty("解决方案")
    private String solution;

    @ExcelProperty("标签")
    private String tag;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("关联日报日期")
    private String reportDate;

    @ExcelProperty("创建时间")
    private String createdAt;
}
