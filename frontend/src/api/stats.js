import request from './request'

// 统计接口（概览/热力图/周分布/高频标签/问题分布与趋势/连续打卡）
export function getStatsOverview() {
  return request.get('/stats/overview')
}

export function getStatsHeatmap(year) {
  return request.get('/stats/heatmap', { params: { year } })
}

export function getStatsWeekly() {
  return request.get('/stats/weekly')
}

export function getStatsHotTags(topN = 3) {
  return request.get('/stats/hot-tags', { params: { topN } })
}

/** 问题类型全量分布（饼图） */
export function getIssueDist() {
  return request.get('/stats/issue-dist')
}

/** 近 N 天问题新增/解决趋势（折线图） */
export function getIssueTrend(days = 14) {
  return request.get('/stats/issue-trend', { params: { days } })
}

/** 连续打卡天数 */
export function getStreak() {
  return request.get('/stats/streak')
}
