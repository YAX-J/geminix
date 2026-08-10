<template>
  <aside class="panel-left">
    <div class="panel-title">工作热力图 <span class="link" @click="toast('可切换年度视图')">2026 年 ›</span></div>
    <div class="hm-wrap">
      <div v-for="(col, ci) in columns" :key="ci" class="hm-col">
        <span class="hm-month">{{ col.label }}</span>
        <div
          v-for="(cell, di) in col.cells"
          :key="di"
          class="hm-cell"
          :class="[cell.empty ? 'empty' : 'l' + cell.level, cell.isToday ? 'today' : '']"
          :title="cell.empty ? '' : `${cell.label}${cell.hasReport ? '（' + cell.reportCount + ' 条日报）' : '（无日报）'}`"
          @click="onCellClick(cell)"
        ></div>
      </div>
    </div>
    <div class="hm-legend">
      少 <span class="hm-cell"></span><span class="hm-cell l1"></span><span class="hm-cell l2"></span
      ><span class="hm-cell l3"></span><span class="hm-cell l4"></span> 多
    </div>

    <div class="divider"></div>
    <div class="panel-title">数据概览</div>
    <div class="stats-grid">
      <div class="stat-card"><div class="stat-num primary">{{ reportStore.total }}</div><div class="stat-label">累计日报</div></div>
      <div class="stat-card"><div class="stat-num amber">{{ reportStore.monthCount }}</div><div class="stat-label">本月日报</div></div>
      <div class="stat-card"><div class="stat-num">{{ issueStore.openCount }}</div><div class="stat-label">待解决问题</div></div>
      <div class="stat-card"><div class="stat-num green">{{ issueStore.doneCount }}</div><div class="stat-label">已解决问题</div></div>
    </div>

    <div class="divider"></div>
    <div class="panel-title">日报标签筛选</div>
    <div class="tag-list">
      <span
        v-for="f in filters"
        :key="f.value"
        class="tag-chip"
        :class="{ active: ui.reportFilter === f.value }"
        @click="ui.reportFilter = f.value"
      >{{ f.label }}</span>
    </div>
  </aside>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useReportStore } from '@/stores/reports'
import { useIssueStore } from '@/stores/issues'
import { useUiStore } from '@/stores/ui'
import { toast } from '@/utils/toast'
import { weekCN } from '@/mock/demoData'

const router = useRouter()
const reportStore = useReportStore()
const issueStore = useIssueStore()
const ui = useUiStore()

const filters = [
  { label: '全部', value: 'all' },
  { label: '后端', value: '后端' },
  { label: '前端', value: '前端' },
  { label: '数据库', value: '数据库' },
  { label: 'Redis', value: 'Redis' },
  { label: '运维', value: '运维' }
]

function fmt(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/* 演示用伪随机热度（正式版由后端 /api/stats/heatmap 提供） */
function levelOf(key, day) {
  const rep = reportStore.byDate(key)
  if (rep) return Math.min(4, 1 + rep.tasks.length)
  const seed = day.getFullYear() * 10000 + (day.getMonth() + 1) * 100 + day.getDate()
  const r = ((seed * 9301 + 49297) % 233280) / 233280
  return r > 0.9 ? 2 : r > 0.65 ? 1 : 0
}

const columns = computed(() => {
  const today = new Date()
  const cols = []
  let prevMonth = -1
  for (let w = 14; w >= 0; w--) {
    const weekStart = new Date(today)
    weekStart.setDate(today.getDate() - today.getDay() - w * 7)
    const cells = []
    for (let d = 0; d < 7; d++) {
      const day = new Date(weekStart)
      day.setDate(weekStart.getDate() + d)
      if (day > today) {
        cells.push({ empty: true })
        continue
      }
      const key = fmt(day)
      const rep = reportStore.byDate(key)
      cells.push({
        empty: false,
        date: key,
        day,
        level: levelOf(key, day),
        isToday: key === fmt(today),
        hasReport: !!rep,
        reportCount: rep ? rep.tasks.length : 0,
        label: `${day.getMonth() + 1}-${day.getDate()} 周${weekCN[day.getDay()]}`
      })
    }
    const first = cells.find((c) => !c.empty)
    const label = first && first.day.getMonth() !== prevMonth ? `${first.day.getMonth() + 1}月` : ''
    if (first) prevMonth = first.day.getMonth()
    cols.push({ label, cells })
  }
  return cols
})

function onCellClick(cell) {
  if (cell.empty) return
  if (!cell.hasReport) {
    toast('该日期暂无日报')
    return
  }
  ui.heatmapTarget = cell.date
  if (router.currentRoute.value.name !== 'reports') {
    router.push('/')
  }
}

onMounted(async () => {
  await Promise.all([reportStore.load(), issueStore.load()])
})
</script>

<style scoped>
.panel-left {
  background: var(--card);
  border-right: 1px solid var(--border);
  overflow-y: auto;
  padding: 18px 16px 24px;
}
.panel-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-2);
  letter-spacing: 1px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.panel-title .link {
  color: var(--primary);
  font-weight: 600;
  cursor: pointer;
}
.panel-title .link:hover {
  text-decoration: underline;
}

.hm-wrap {
  display: flex;
  gap: 4px;
}
.hm-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}
.hm-month {
  font-size: 10.5px;
  color: var(--text-3);
  font-weight: 600;
  height: 14px;
}
.hm-cell {
  width: 13px;
  height: 13px;
  border-radius: 3px;
  background: #ebedf2;
  cursor: pointer;
  transition: transform 0.12s;
}
.hm-cell.empty {
  cursor: default;
}
.hm-cell:not(.empty):hover {
  transform: scale(1.25);
  outline: 1px solid var(--primary);
}
.hm-cell.l1 {
  background: #c7d2fe;
}
.hm-cell.l2 {
  background: #818cf8;
}
.hm-cell.l3 {
  background: #4f46e5;
}
.hm-cell.l4 {
  background: #3730a3;
}
.hm-cell.today {
  outline: 2px solid var(--problem);
  outline-offset: 1px;
}
.hm-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  font-size: 11px;
  color: var(--text-3);
  justify-content: flex-end;
}
.hm-legend .hm-cell {
  width: 11px;
  height: 11px;
  cursor: default;
}

.divider {
  height: 1px;
  background: var(--border);
  margin: 18px 0;
}
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.stat-card {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
}
.stat-num {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.1;
}
.stat-num.primary {
  color: var(--primary);
}
.stat-num.green {
  color: var(--solution);
}
.stat-num.amber {
  color: var(--problem);
}
.stat-label {
  font-size: 11.5px;
  color: var(--text-2);
  margin-top: 3px;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
