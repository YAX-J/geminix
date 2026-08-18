package com.worklog.service;

import com.worklog.dto.SearchResultVO;

/**
 * 全局搜索：跨日报与问题联合检索（按当前用户隔离）
 */
public interface SearchService {

    SearchResultVO search(String keyword);
}
