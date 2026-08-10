package com.worklog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 问题新增/更新请求
 */
@Data
public class IssueReq {

    @NotBlank(message = "问题标题不能为空")
    private String title;

    @NotBlank(message = "问题描述不能为空")
    private String description;

    /** 解决方案：为空视为待解决 */
    private String solution;

    private String tag;

    /** open 待解决 / done 已解决 */
    private String status;

    /** 关联日报日期：可空 = 独立问题 */
    private LocalDate reportDate;
}
