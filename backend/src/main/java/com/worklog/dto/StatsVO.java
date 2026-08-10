package com.worklog.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 统计相关 VO（概览 / 热力图 / 本周分布 / 高频标签）
 */
public class StatsVO {

    /** 数据概览：左栏统计 + 右栏解决率 */
    @Data
    public static class Overview {
        private long reportTotal;
        private long reportMonth;
        private long issueOpen;
        private long issueDone;
        private int solveRate;
    }

    /** 热力图：日期(yyyy-MM-dd) -> 当日记录数 */
    @Data
    public static class Heatmap {
        private String year;
        private Map<String, Integer> daily;
    }

    /** 本周日报分布：周一~周日 */
    @Data
    public static class Weekly {
        private List<DayCount> days;
    }

    @Data
    public static class DayCount {
        private String label;
        private int count;
    }

    /** 高频问题类型 TOP N */
    @Data
    public static class HotTag {
        private String tag;
        private long count;
    }
}
