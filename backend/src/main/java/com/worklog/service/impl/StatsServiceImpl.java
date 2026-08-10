package com.worklog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 统计服务：查询结果缓存于 Redis，写操作由 Report/Issue 服务触发失效
 *
 * 缓存键设计：
 *   worklog:stats:overview        概览统计      TTL 5min
 *   worklog:stats:heatmap:{year}  年度热力图    TTL 12h
 *   worklog:stats:weekly          本周分布      TTL 1h
 *   worklog:stats:hot-tags        高频标签      TTL 1h
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final String KEY_OVERVIEW = "worklog:stats:overview";
    private static final String KEY_HEATMAP_PREFIX = "worklog:stats:heatmap:";
    private static final String KEY_WEEKLY = "worklog:stats:weekly";
    private static final String KEY_HOT_TAGS = "worklog:stats:hot-tags";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportMapper reportMapper;
    private final IssueMapper issueMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public StatsVO.Overview overview() {
        String cached = redisTemplate.opsForValue().get(KEY_OVERVIEW);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, StatsVO.Overview.class);
            } catch (JsonProcessingException e) {
                log.warn("overview 缓存反序列化失败，重新查询: {}", e.getMessage());
            }
        }
        StatsVO.Overview vo = buildOverview();
        try {
            redisTemplate.opsForValue().set(KEY_OVERVIEW, objectMapper.writeValueAsString(vo), 5, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("overview 缓存序列化失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public StatsVO.Heatmap heatmap(int year) {
        String key = KEY_HEATMAP_PREFIX + year;
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
        String cached = redisTemplate.opsForValue().get(KEY_WEEKLY);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, StatsVO.Weekly.class);
            } catch (JsonProcessingException e) {
                log.warn("weekly 缓存反序列化失败，重新查询: {}", e.getMessage());
            }
        }
        StatsVO.Weekly vo = buildWeekly();
        try {
            redisTemplate.opsForValue().set(KEY_WEEKLY, objectMapper.writeValueAsString(vo), 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("weekly 缓存序列化失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public List<StatsVO.HotTag> hotTags(int topN) {
        String cached = redisTemplate.opsForValue().get(KEY_HOT_TAGS);
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
            redisTemplate.opsForValue().set(KEY_HOT_TAGS, objectMapper.writeValueAsString(vo), 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("hot-tags 缓存序列化失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public void evictStats() {
        Set<String> keys = redisTemplate.keys(KEY_OVERVIEW);
        keys.addAll(redisTemplate.keys(KEY_HEATMAP_PREFIX + "*"));
        keys.add(KEY_WEEKLY);
        keys.add(KEY_HOT_TAGS);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /* ---------- 构建逻辑 ---------- */

    private StatsVO.Overview buildOverview() {
        StatsVO.Overview vo = new StatsVO.Overview();
        vo.setReportTotal(reportMapper.selectCount(null));
        LocalDate now = LocalDate.now();
        vo.setReportMonth(reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .ge(Report::getReportDate, now.withDayOfMonth(1))
                .le(Report::getReportDate, now)));
        long open = issueMapper.selectCount(new LambdaQueryWrapper<Issue>().eq(Issue::getStatus, "open"));
        long done = issueMapper.selectCount(new LambdaQueryWrapper<Issue>().eq(Issue::getStatus, "done"));
        vo.setIssueOpen(open);
        vo.setIssueDone(done);
        long total = open + done;
        vo.setSolveRate(total == 0 ? 0 : (int) Math.round(done * 100.0 / total));
        return vo;
    }

    private StatsVO.Heatmap buildHeatmap(int year) {
        QueryWrapper<Report> qw = new QueryWrapper<>();
        qw.select("report_date", "COUNT(*) AS cnt")
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
}
