package com.worklog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 问题实体：独立模块，可单独记录、不依赖日报
 */
@Data
@TableName("issue")
public class Issue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    /** 解决方案：为空表示待解决 */
    private String solution;

    private String tag;

    /** open 待解决 / done 已解决 */
    private String status;

    /** 关联日报日期：可空 = 独立问题 */
    private LocalDate reportDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
