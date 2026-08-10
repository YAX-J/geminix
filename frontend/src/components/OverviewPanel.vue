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
import { computed, onMounted } from 'vue'
import { useReportStore } from '@/stores/reports'
import { useIssueStore } from '@/stores/issues'
import { tagClassMap } from '@/mock/demoData'

const reportStore = useReportStore()
const issueStore = useIssueStore()

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

onMounted(async () => {
  await Promise.all([reportStore.load(), issueStore.load()])
})
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
