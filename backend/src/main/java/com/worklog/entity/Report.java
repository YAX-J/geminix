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
 * 日报实体：只承载工作内容，与问题解耦
 */
@Data
@TableName(value = "report", autoResultMap = true)
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户（数据隔离） */
    private Long userId;

    private LocalDate reportDate;

    /** 星期（冗余存储，查询展示用） */
    private String weekday;

    /** 时间段，如 09:30 - 18:20 */
    private String timeRange;

    private String title;

    /** 工作内容列表（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tasks;

    /** 标签列表（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 所属项目（可空 = 未分类，用于多项目日志归类） */
    private String project;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
