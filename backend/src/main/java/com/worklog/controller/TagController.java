package com.worklog.controller;

import com.worklog.common.R;
import com.worklog.config.RequireRole;
import com.worklog.entity.Tag;
import com.worklog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签管理接口（全局共享字典：查看所有人可，管理仅 ADMIN）
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /** 全部标签（按 sort 升序） */
    @GetMapping
    public R<List<Tag>> list() {
        return R.ok(tagService.listTags());
    }

    @PostMapping
    @RequireRole("ADMIN")
    public R<Tag> add(@RequestParam String name, @RequestParam(required = false) String color) {
        return R.ok(tagService.addTag(name, color));
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public R<Tag> update(@PathVariable Long id,
                         @RequestParam(required = false) String name,
                         @RequestParam(required = false) String color,
                         @RequestParam(required = false) Integer sort) {
        return R.ok(tagService.updateTag(id, name, color, sort));
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public R<Void> delete(@PathVariable Long id) {
        tagService.deleteTag(id);
        return R.ok();
    }
}
