package com.worklog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签实体：独立管理，日报/问题不再硬编码五类
 */
@Data
@TableName("tag")
public class Tag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称（唯一） */
    private String name;

    /** 展示颜色（CSS 类名或色值，可空） */
    private String color;

    /** 排序号（越小越靠前） */
    private Integer sort;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
