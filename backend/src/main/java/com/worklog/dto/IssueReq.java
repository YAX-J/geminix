package com.worklog.dto;

import com.worklog.entity.Issue.IssueSolution;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 问题新增/更新请求
 */
@Data
public class IssueReq {

    @NotBlank(message = "问题标题不能为空")
    private String title;

    @NotBlank(message = "问题描述不能为空")
    private String description;

    /** 解决方案：为空视为待解决（兼容旧字段，新增多方案后此字段自动取最佳方案） */
    private String solution;

    private String tag;

    /** open 待解决 / done 已解决 */
    private String status;

    /** 关联日报日期：可空 = 独立问题 */
    private LocalDate reportDate;

    /** 多个解决方案（可空）；元素 content 必填，best 标记最佳方案 */
    private List<IssueSolution> solutions;

    /** 是否收藏（常见问题置顶） */
    private Boolean favorite;
}
