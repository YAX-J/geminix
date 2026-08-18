package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worklog.common.UserContext;
import com.worklog.dto.StatsVO;
import com.worklog.entity.Issue;
import com.worklog.entity.Report;
import com.worklog.mapper.IssueMapper;
import com.worklog.mapper.ReportMapper;
import com.worklog.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 统计服务：查询结果缓存于 Redis，写操作由 Report/Issue 服务触发失效。
 * 多用户改造后：所有查询按 user_id 隔离，缓存键带 userId 后缀防止串数据。
 *
 * 缓存键设计（{uid} = 当前用户 id）：
 *   worklog:stats:overview:{uid}          概览统计      TTL 5min
 *   worklog:stats:heatmap:{year}:{uid}    年度热力图    TTL 12h
 *   worklog:stats:weekly:{uid}            本周分布      TTL 1h
 *   worklog:stats:hot-tags:{uid}          高频标签      TTL 1h
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final String KEY_OVERVIEW = "worklog:stats:overview";
    private static final String KEY_HEATMAP_PREFIX = "worklog:stats:heatmap:";
    private static final String KEY_WEEKLY = "worklog:stats:weekly";
    private static final String KEY_HOT_TAGS = "worklog:stats:hot-tags";
    private static final String KEY_ISSUE_DIST = "worklog:stats:issue-dist";
    private static final String KEY_ISSUE_TREND_PREFIX = "worklog:stats:issue-trend:";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportMapper reportMapper;
    private final IssueMapper issueMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public StatsVO.Overview overview() {
        String key = KEY_OVERVIEW + ":" + uid();
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, StatsVO.Overview.class);
            } catch (JsonProcessingException e) {
                log.warn("overview 缓存反序列化失败，重新查询: {}", e.getMessage());
            }
        }
        StatsVO.Overview vo = buildOverview();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), 5, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("overview 缓存序列化失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public StatsVO.Heatmap heatmap(int year) {
        String key = KEY_HEATMAP_PREFIX + year + ":" + uid();
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, StatsVO.Heatmap.class);
            } catch (JsonProcessingException e) {
                log.warn("heatmap 缓存反序列化失败，重新查询: {}", e.getMessage());
            }
        }
        StatsVO.Heatmap vo = buildHeatmap(year);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), 12, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("heatmap 缓存序列化失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public StatsVO.Weekly weekly() {
        String key = KEY_WEEKLY + ":" + uid();
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, StatsVO.Weekly.class);
            } catch (JsonProcessingException e) {
                log.warn("weekly 缓存反序列化失败，重新查询: {}", e.getMessage());
            }
        }
        StatsVO.Weekly vo = buildWeekly();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("weekly 缓存序列化失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public List<StatsVO.HotTag> hotTags(int topN) {
        String key = KEY_HOT_TAGS + ":" + uid();
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, StatsVO.HotTag.class));
            } catch (JsonProcessingException e) {
                log.warn("hot-tags 缓存反序列化失败，重新查询: {}", e.getMessage());
            }
        }
        List<StatsVO.HotTag> vo = buildHotTags(topN);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("hot-tags 缓存序列化失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public void evictStats() {
        Long u = UserContext.get();
        Set<String> keys = new HashSet<>();
        String suffix = u == null ? "" : ":" + u;
        keys.add(KEY_OVERVIEW + suffix);
        keys.add(KEY_WEEKLY + suffix);
        keys.add(KEY_HOT_TAGS + suffix);
        keys.add(KEY_ISSUE_DIST + suffix);
        keys.addAll(redisTemplate.keys(KEY_HEATMAP_PREFIX + "*" + suffix));
        keys.addAll(redisTemplate.keys(KEY_ISSUE_TREND_PREFIX + "*" + suffix));
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public List<StatsVO.HotTag> issueDist() {
        String key = KEY_ISSUE_DIST + ":" + uid();
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, StatsVO.HotTag.class));
            } catch (JsonProcessingException e) {
                log.warn("issue-dist 缓存反序列化失败，重新查询: {}", e.getMessage());
            }
        }
        QueryWrapper<Issue> qw = new QueryWrapper<>();
        qw.select("tag", "COUNT(*) AS cnt")
          .eq("user_id", uid())
          .groupBy("tag")
          .orderByDesc("cnt");
        List<Map<String, Object>> rows = issueMapper.selectMaps(qw);
        List<StatsVO.HotTag> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            StatsVO.HotTag ht = new StatsVO.HotTag();
            ht.setTag(String.valueOf(row.get("tag")));
            ht.setCount(((Number) row.get("cnt")).longValue());
            list.add(ht);
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(list), 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("issue-dist 缓存序列化失败: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public StatsVO.IssueTrend issueTrend(int days) {
        int n = Math.min(Math.max(days, 7), 90);
        String key = KEY_ISSUE_TREND_PREFIX + n + ":" + uid();
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, StatsVO.IssueTrend.class);
            } catch (JsonProcessingException e) {
                log.warn("issue-trend 缓存反序列化失败，重新查询: {}", e.getMessage());
            }
        }
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(n - 1L);

        // 每日新增（按 created_at 天）
        Map<String, Long> createdMap = countByDay(issueMapper, "created_at", start, null);
        // 每日解决（近似：status=done 且 updated_at 落在当天）
        Map<String, Long> solvedMap = countByDay(issueMapper, "updated_at", start, "done");

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> labels = new ArrayList<>();
        List<Long> created = new ArrayList<>();
        List<Long> solved = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDate d = start.plusDays(i);
            String fmt = d.format(DATE_FMT);
            labels.add(d.format(labelFmt));
            created.add(createdMap.getOrDefault(fmt, 0L));
            solved.add(solvedMap.getOrDefault(fmt, 0L));
        }
        StatsVO.IssueTrend vo = new StatsVO.IssueTrend();
        vo.setLabels(labels);
        vo.setCreated(created);
        vo.setSolved(solved);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("issue-trend 缓存序列化失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public int streak() {
        List<Report> reports = reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .select(Report::getReportDate)
                .eq(Report::getUserId, uid()));
        Set<String> dates = new HashSet<>();
        for (Report r : reports) {
            if (r.getReportDate() != null) {
                dates.add(r.getReportDate().format(DATE_FMT));
            }
        }
        LocalDate today = LocalDate.now();
        LocalDate cursor = dates.contains(today.format(DATE_FMT)) ? today : today.minusDays(1);
        int streak = 0;
        while (dates.contains(cursor.format(DATE_FMT))) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /**
     * 按天统计 issue 数量。
     *
     * @param column  分组列（created_at / updated_at）
     * @param from    起始日期（含）
     * @param status  限定状态（可空）
     */
    private Map<String, Long> countByDay(IssueMapper mapper,
                                         String column, LocalDate from, String status) {
        QueryWrapper<Issue> qw = new QueryWrapper<>();
        qw.select("DATE(" + column + ") AS d", "COUNT(*) AS cnt")
          .eq("user_id", uid())
          .ge(column, from.atStartOfDay())
          .groupBy("DATE(" + column + ")");
        if (StringUtils.hasText(status)) {
            qw.eq("status", status);
        }
        List<Map<String, Object>> rows = mapper.selectMaps(qw);
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object d = row.get("d");
            if (d == null) continue;
            String fmt = d instanceof java.sql.Date
                    ? ((java.sql.Date) d).toLocalDate().format(DATE_FMT)
                    : String.valueOf(d);
            map.put(fmt, ((Number) row.get("cnt")).longValue());
        }
        return map;
    }

    /* ---------- 构建逻辑 ---------- */

    private StatsVO.Overview buildOverview() {
        StatsVO.Overview vo = new StatsVO.Overview();
        vo.setReportTotal(reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, uid())));
        LocalDate now = LocalDate.now();
        vo.setReportMonth(reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, uid())
                .ge(Report::getReportDate, now.withDayOfMonth(1))
                .le(Report::getReportDate, now)));
        long open = issueMapper.selectCount(new LambdaQueryWrapper<Issue>()
                .eq(Issue::getUserId, uid()).eq(Issue::getStatus, "open"));
        long done = issueMapper.selectCount(new LambdaQueryWrapper<Issue>()
                .eq(Issue::getUserId, uid()).eq(Issue::getStatus, "done"));
        vo.setIssueOpen(open);
        vo.setIssueDone(done);
        long total = open + done;
        vo.setSolveRate(total == 0 ? 0 : (int) Math.round(done * 100.0 / total));
        return vo;
    }

    private StatsVO.Heatmap buildHeatmap(int year) {
        QueryWrapper<Report> qw = new QueryWrapper<>();
        qw.select("report_date", "COUNT(*) AS cnt")
          .eq("user_id", uid())
          .apply("YEAR(report_date) = {0}", year)
          .groupBy("report_date");
        List<Map<String, Object>> rows = reportMapper.selectMaps(qw);
        Map<String, Integer> daily = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object date = row.get("report_date");
            if (date == null) continue;
            String key = date instanceof java.sql.Date
                    ? ((java.sql.Date) date).toLocalDate().format(DATE_FMT)
                    : String.valueOf(date);
            daily.put(key, ((Number) row.get("cnt")).intValue());
        }
        StatsVO.Heatmap vo = new StatsVO.Heatmap();
        vo.setYear(String.valueOf(year));
        vo.setDaily(daily);
        return vo;
    }

    private StatsVO.Weekly buildWeekly() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays((today.getDayOfWeek().getValue() + 6) % 7);
        QueryWrapper<Report> qw = new QueryWrapper<>();
        qw.select("report_date", "COUNT(*) AS cnt")
          .eq("user_id", uid())
          .between("report_date", monday, monday.plusDays(6))
          .groupBy("report_date");
        List<Map<String, Object>> rows = reportMapper.selectMaps(qw);
        Map<String, Integer> countByDate = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object date = row.get("report_date");
            if (date == null) continue;
            String key = date instanceof java.sql.Date
                    ? ((java.sql.Date) date).toLocalDate().format(DATE_FMT)
                    : String.valueOf(date);
            countByDate.put(key, ((Number) row.get("cnt")).intValue());
        }
        String[] labels = {"一", "二", "三", "四", "五", "六", "日"};
        List<StatsVO.DayCount> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            StatsVO.DayCount dc = new StatsVO.DayCount();
            dc.setLabel(labels[i]);
            dc.setCount(countByDate.getOrDefault(monday.plusDays(i).format(DATE_FMT), 0));
            days.add(dc);
        }
        StatsVO.Weekly vo = new StatsVO.Weekly();
        vo.setDays(days);
        return vo;
    }

    private List<StatsVO.HotTag> buildHotTags(int topN) {
        QueryWrapper<Issue> qw = new QueryWrapper<>();
        qw.select("tag", "COUNT(*) AS cnt")
          .eq("user_id", uid())
          .groupBy("tag")
          .orderByDesc("cnt")
          .last("LIMIT " + topN);
        List<Map<String, Object>> rows = issueMapper.selectMaps(qw);
        List<StatsVO.HotTag> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            StatsVO.HotTag ht = new StatsVO.HotTag();
            ht.setTag(String.valueOf(row.get("tag")));
            ht.setCount(((Number) row.get("cnt")).longValue());
            list.add(ht);
        }
        return list;
    }

    /** 当前用户 id（字符串形式，用于缓存键与 SQL 过滤） */
    private String uid() {
        return String.valueOf(UserContext.require());
    }
}
