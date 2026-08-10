package com.worklog.service;

import com.worklog.dto.StatsVO;

import java.util.List;

public interface StatsService {

    /** 数据概览（Redis 缓存，写操作后失效） */
    StatsVO.Overview overview();

    /** 热力图：某年每天的日报条数（Redis 缓存） */
    StatsVO.Heatmap heatmap(int year);

    /** 本周日报分布（周一~周日） */
    StatsVO.Weekly weekly();

    /** 高频问题类型 TOP N */
    List<StatsVO.HotTag> hotTags(int topN);

    /** 问题类型全量分布（饼图数据） */
    List<StatsVO.HotTag> issueDist();

    /** 近 N 天问题新增/解决趋势（折线图数据） */
    StatsVO.IssueTrend issueTrend(int days);

    /** 连续打卡天数（streak）：今天已写从今天起算，未写则从昨天起算 */
    int streak();

    /** 清除全部统计缓存（日报/问题写操作时调用） */
    void evictStats();
}
