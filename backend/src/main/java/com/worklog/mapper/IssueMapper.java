package com.worklog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.worklog.entity.Issue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IssueMapper extends BaseMapper<Issue> {
}
