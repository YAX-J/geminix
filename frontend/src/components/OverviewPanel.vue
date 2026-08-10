<template>
  <aside class="panel-right">
    <div class="ov-card">
      <div class="ov-title">✅ 问题解决率</div>
      <div class="ring-box">
        <div class="ring" :style="ringStyle"><span>{{ issueStore.solveRate }}%</span></div>
        <div class="ring-legend">
          待解决 <b>{{ issueStore.openCount }}</b> 个<br />
          已解决 <b>{{ issueStore.doneCount }}</b> 个
        </div>
      </div>
    </div>

    <div class="ov-card">
      <div class="ov-title">📊 本周日报分布</div>
      <div class="week-bars">
        <div v-for="b in weekBars" :key="b.label" class="bar-wrap">
          <div class="bar" :style="{ height: b.pct + '%' }"><span class="bar-tip">{{ b.count }}</span></div>
          <span class="bar-label">{{ b.label }}</span>
        </div>
      </div>
    </div>

    <div class="ov-card">
      <div class="ov-title">🍩 问题类型分布</div>
      <div ref="distEl" class="chart-box"></div>
    </div>

    <div class="ov-card">
      <div class="ov-title">📈 问题趋势（近 14 天）</div>
      <div ref="trendEl" class="chart-box tall"></div>
    </div>

    <div class="ov-card">
      <div class="ov-title">🔥 高频问题类型 TOP3</div>
      <div class="hot-list">
        <div v-for="(t, i) in hotTags" :key="t.tag" class="hot-item">
          <span class="hot-rank">{{ i + 1 }}</span>
          <span class="tag" :class="tagClassMap[t.tag] || 'tag-backend'">{{ t.tag }}</span>
          <div class="bar-mini"><i :style="{ width: t.pct + '%' }"></i></div>
          <span class="n">{{ t.count }} 个</span>
        </div>
        <div v-if="!hotTags.length" class="no-data">暂无数据</div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, watch, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useReportStore } from '@/stores/reports'
import { useIssueStore } from '@/stores/issues'
import { useTagStore } from '@/stores/tags'
import { getIssueDist, getIssueTrend } from '@/api/stats'
import { tagClassMap } from '@/mock/demoData'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

const reportStore = useReportStore()
const issueStore = useIssueStore()
const tagStore = useTagStore()

const distEl = ref(null)
const trendEl = ref(null)
let distChart = null
let trendChart = null

const ringStyle = computed(() => ({
  background: `conic-gradient(var(--solution) 0 ${issueStore.solveRate}%, #e2e8f0 ${issueStore.solveRate}% 100%)`
}))

/* 本周（周一~周日）日报分布 */
const weekBars = computed(() => {
  const now = new Date()
  const monday = new Date(now)
  monday.setDate(now.getDate() - ((now.getDay() + 6) % 7))
  const days = []
  const labels = ['一', '二', '三', '四', '五', '六', '日']
  const counts = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    counts.push(reportStore.items.filter((r) => r.date === key).length)
  }
  const max = Math.max(...counts, 1)
  for (let i = 0; i < 7; i++) {
    days.push({ label: labels[i], count: counts[i], pct: (counts[i] / max) * 100 })
  }
  return days
})

/* 高频问题类型 */
const hotTags = computed(() => {
  const map = {}
  issueStore.items.forEach((i) => {
    map[i.tag] = (map[i.tag] || 0) + 1
  })
  const arr = Object.entries(map).map(([tag, count]) => ({ tag, count })).sort((a, b) => b.count - a.count).slice(0, 3)
  const max = arr.length ? arr[0].count : 1
  return arr.map((t) => ({ ...t, pct: (t.count / max) * 100 }))
})

/* 图表初始化 */
function initCharts() {
  if (distEl.value && !distChart) {
    distChart = echarts.init(distEl.value)
  }
  if (trendEl.value && !trendChart) {
    trendChart = echarts.init(trendEl.value)
  }
}

async function renderCharts() {
  await nextTick()
  initCharts()

  // 问题类型分布（饼图）
  const dist = USE_MOCK ? buildLocalDist() : await getIssueDist().catch(() => [])
  const palette = ['#6366f1', '#10b981', '#f59e0b', '#0ea5e9', '#ef4444', '#8b5cf6', '#14b8a6', '#f97316']
  if (distChart) {
    distChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 个 ({d}%)' },
      legend: {
        bottom: 0,
        icon: 'circle',
        itemWidth: 8,
        itemHeight: 8,
        textStyle: { fontSize: 11, color: '#64748b' }
      },
      series: [
        {
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 12, fontWeight: 700 } },
          data: dist.map((d, i) => ({
            name: d.tag,
            value: d.count,
            itemStyle: { color: palette[i % palette.length] }
          }))
        }
      ]
    })
  }

  // 问题趋势（折线图）
  const trend = USE_MOCK ? buildLocalTrend(14) : await getIssueTrend(14).catch(() => null)
  if (trendChart) {
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11, color: '#64748b' } },
      grid: { left: 34, right: 16, top: 20, bottom: 40 },
      xAxis: {
        type: 'category',
        data: trend?.labels || [],
        axisLabel: { fontSize: 10, color: '#94a3b8' },
        axisLine: { lineStyle: { color: '#e4e7ef' } }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { fontSize: 10, color: '#94a3b8' },
        splitLine: { lineStyle: { color: '#eef0f6' } }
      },
      series: [
        {
          name: '新增',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: trend?.created || [],
          lineStyle: { width: 2.5, color: '#6366f1' },
          itemStyle: { color: '#6366f1' },
          areaStyle: { color: 'rgba(99,102,241,0.12)' }
        },
        {
          name: '解决',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: trend?.solved || [],
          lineStyle: { width: 2.5, color: '#10b981' },
          itemStyle: { color: '#10b981' },
          areaStyle: { color: 'rgba(16,185,129,0.10)' }
        }
      ]
    })
  }
}

function buildLocalDist() {
  const map = {}
  issueStore.items.forEach((i) => {
    map[i.tag] = (map[i.tag] || 0) + 1
  })
  return Object.entries(map)
    .map(([tag, count]) => ({ tag, count }))
    .sort((a, b) => b.count - a.count)
}

function buildLocalTrend(days) {
  const labels = []
  const created = []
  const solved = []
  const createdMap = {}
  const solvedMap = {}
  issueStore.items.forEach((i) => {
    const d = (i.createdAt || '').slice(0, 10)
    if (d) createdMap[d] = (createdMap[d] || 0) + 1
    if (i.status === 'done') {
      const u = (i.updatedAt || '').slice(0, 10)
      if (u) solvedMap[u] = (solvedMap[u] || 0) + 1
    }
  })
  const today = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    labels.push(`${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
    created.push(createdMap[key] || 0)
    solved.push(solvedMap[key] || 0)
  }
  return { labels, created, solved }
}

function resizeCharts() {
  distChart?.resize()
  trendChart?.resize()
}

onMounted(async () => {
  await Promise.all([reportStore.load(), issueStore.load(), tagStore.load()])
  renderCharts()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  distChart?.dispose()
  trendChart?.dispose()
  distChart = null
  trendChart = null
})

// 数据变化时刷新图表
watch(
  () => issueStore.items.length,
  () => renderCharts()
)
</script>

<style scoped>
.panel-right {
  background: var(--card);
  border-left: 1px solid var(--border);
  overflow-y: auto;
  padding: 16px;
}
.ov-card {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 12px;
}
.ov-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-2);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.ring-box {
  display: flex;
  align-items: center;
  gap: 18px;
}
.ring {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  flex-shrink: 0;
  position: relative;
  display: grid;
  place-items: center;
}
.ring::after {
  content: '';
  width: 66px;
  height: 66px;
  border-radius: 50%;
  background: var(--bg);
}
.ring span {
  position: absolute;
  font-size: 16px;
  font-weight: 800;
  color: var(--solution-deep);
}
.ring-legend {
  font-size: 12.5px;
  color: var(--text-2);
  line-height: 2;
}
.ring-legend b {
  color: var(--text);
}
.week-bars {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 84px;
  padding-top: 4px;
}
.bar-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}
.bar {
  width: 100%;
  max-width: 26px;
  background: var(--primary-bg);
  border-radius: 5px 5px 3px 3px;
  position: relative;
  transition: background 0.2s;
}
.bar:hover {
  background: var(--primary);
}
.bar .bar-tip {
  position: absolute;
  top: -22px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10.5px;
  color: var(--text-2);
  font-weight: 700;
}
.bar-label {
  font-size: 10.5px;
  color: var(--text-3);
}
.chart-box {
  height: 150px;
}
.chart-box.tall {
  height: 180px;
}
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.hot-item {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12.5px;
}
.hot-rank {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid var(--border);
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 800;
  color: var(--primary);
  flex-shrink: 0;
}
.hot-item .tag {
  font-size: 10.5px;
  flex-shrink: 0;
}
.bar-mini {
  flex: 1;
  height: 7px;
  background: var(--primary-bg);
  border-radius: 4px;
  overflow: hidden;
}
.bar-mini i {
  display: block;
  height: 100%;
  background: var(--primary);
  border-radius: 4px;
}
.hot-item .n {
  font-size: 11px;
  color: var(--text-3);
  flex-shrink: 0;
}
.no-data {
  color: var(--text-3);
  font-size: 12px;
}
</style>
