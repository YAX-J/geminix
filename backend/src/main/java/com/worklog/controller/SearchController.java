package com.worklog.controller;

import com.worklog.common.R;
import com.worklog.dto.SearchResultVO;
import com.worklog.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局搜索接口：跨日报与问题联合检索
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public R<SearchResultVO> search(@RequestParam String keyword) {
        return R.ok(searchService.search(keyword));
    }
}
