package com.worklog.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 日报 Excel 导出行模型
 */
@Data
public class ReportExcelRow {

    @ExcelProperty("日期")
    private String date;

    @ExcelProperty("星期")
    private String weekday;

    @ExcelProperty("时间段")
    private String timeRange;

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("工作内容")
    private String tasks;

    @ExcelProperty("标签")
    private String tags;
}
