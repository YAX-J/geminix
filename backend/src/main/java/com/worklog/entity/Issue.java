package com.worklog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 问题实体：独立模块，可单独记录、不依赖日报
 */
@Data
@TableName(value = "issue", autoResultMap = true)
public class Issue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    /** 解决方案（最佳方案摘要，兼容旧数据；多方案见 solutions） */
    private String solution;

    private String tag;

    /** open 待解决 / done 已解决 */
    private String status;

    /** 关联日报日期：可空 = 独立问题 */
    private LocalDate reportDate;

    /** 多个解决方案（JSON 数组，元素 {content, best}），最佳方案标记 best=true */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<IssueSolution> solutions;

    /** 是否收藏（常见问题置顶） */
    private Boolean favorite;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 单条解决方案 */
    @Data
    public static class IssueSolution {
        /** 方案内容 */
        private String content;
        /** 是否最佳方案 */
        private Boolean best;
    }
}
