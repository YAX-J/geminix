package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.worklog.common.BusinessException;
import com.worklog.entity.Tag;
import com.worklog.mapper.TagMapper;
import com.worklog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public List<Tag> listTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .orderByAsc(Tag::getSort).orderByAsc(Tag::getId));
    }

    @Override
    public Tag addTag(String name, String color) {
        String n = requireName(name);
        checkNameExists(n, null);
        Tag tag = new Tag();
        tag.setName(n);
        tag.setColor(StringUtils.hasText(color) ? color.trim() : null);
        // 新标签排末尾
        Integer maxSort = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                        .select(Tag::getSort).orderByDesc(Tag::getSort).last("LIMIT 1"))
                .stream().findFirst().map(Tag::getSort).orElse(0);
        tag.setSort(maxSort == null ? 0 : maxSort + 1);
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    public Tag updateTag(Long id, String name, String color, Integer sort) {
        Tag exist = tagMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "标签不存在");
        }
        if (name != null) {
            String n = requireName(name);
            checkNameExists(n, id);
            exist.setName(n);
        }
        if (color != null) {
            exist.setColor(StringUtils.hasText(color) ? color.trim() : null);
        }
        if (sort != null) {
            exist.setSort(sort);
        }
        tagMapper.updateById(exist);
        return exist;
    }

    @Override
    public void deleteTag(Long id) {
        if (tagMapper.selectById(id) == null) {
            throw new BusinessException(404, "标签不存在");
        }
        tagMapper.deleteById(id);
    }

    private String requireName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(400, "标签名称不能为空");
        }
        return name.trim();
    }

    private void checkNameExists(String name, Long excludeId) {
        Long count = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, name)
                .ne(excludeId != null, Tag::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(400, "标签「" + name + "」已存在");
        }
    }
}
