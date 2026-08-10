package com.worklog.controller;

import com.worklog.common.R;
import com.worklog.dto.StatsVO;
import com.worklog.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统计接口：结果均经 Redis 缓存，写操作自动失效
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 数据概览：累计/本月日报、待解决/已解决问题、解决率 */
    @GetMapping("/overview")
    public R<StatsVO.Overview> overview() {
        return R.ok(statsService.overview());
    }

    /** 热力图：某年每天日报条数，如 /api/stats/heatmap?year=2026 */
    @GetMapping("/heatmap")
    public R<StatsVO.Heatmap> heatmap(@RequestParam(defaultValue = "2026") int year) {
        return R.ok(statsService.heatmap(year));
    }

    /** 本周日报分布（周一~周日） */
    @GetMapping("/weekly")
    public R<StatsVO.Weekly> weekly() {
        return R.ok(statsService.weekly());
    }

    /** 高频问题类型 TOP N */
    @GetMapping("/hot-tags")
    public R<List<StatsVO.HotTag>> hotTags(@RequestParam(defaultValue = "3") int topN) {
        return R.ok(statsService.hotTags(topN));
    }
}
