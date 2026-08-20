package com.worklog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 日报新增/更新请求
 */
@Data
public class ReportReq {

    @NotNull(message = "日期不能为空")
    private LocalDate date;

    private String timeRange;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotEmpty(message = "工作内容不能为空")
    private List<String> tasks;

    private List<String> tags;

    /** 所属项目（可空 = 未分类） */
    private String project;
}
