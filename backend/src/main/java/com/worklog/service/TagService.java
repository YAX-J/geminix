package com.worklog.service;

import com.worklog.entity.Tag;

import java.util.List;

public interface TagService {

    /** 全部标签（按 sort 升序） */
    List<Tag> listTags();

    Tag addTag(String name, String color);

    Tag updateTag(Long id, String name, String color, Integer sort);

    void deleteTag(Long id);
}
