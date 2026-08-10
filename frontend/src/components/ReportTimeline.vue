<template>
  <div class="timeline-wrap">
    <div class="timeline-inner">
      <div class="tl-head">
        <div class="center-tabs">
          <router-link to="/" class="tab-main" :class="{ active: $route.name === 'reports' }">
            📋 日报 <span class="cnt">{{ reportStore.total }}</span>
          </router-link>
          <router-link to="/issues" class="tab-main" :class="{ active: $route.name === 'issues' }">
            ⚠ 问题 <span class="cnt">{{ issueStore.total }}</span>
          </router-link>
        </div>
        <div class="tl-stat">
          共 <b>{{ filteredGroups.length }}</b> 天 · {{ ui.searchKw ? `筛选「${ui.searchKw}」` : '全部记录' }}
        </div>
      </div>

      <section
        v-for="g in filteredGroups"
        :key="g.date"
        :id="'day-' + g.date.replace(/-/g, '')"
        class="day-group"
        :class="{ flash: ui.heatmapTarget === g.date }"
      >
        <header class="day-head">
          <div class="day-cal">
            <span class="d">{{ parseInt(g.date.slice(8), 10) }}</span>
            <span class="m">{{ parseInt(g.date.slice(5, 7), 10) }}月</span>
          </div>
          <div class="day-meta">
            <div class="week">
              {{ g.week }}
              <span v-if="g.isToday" class="today-pill">今天</span>
            </div>
            <div class="desc">{{ g.items.length }} 条工作 · {{ linkedCount(g) }} 个关联问题</div>
          </div>
          <div class="day-actions">
            <button class="btn btn-ghost small" @click="ui.openReportModal(g.date)">＋ 补记</button>
          </div>
        </header>

        <ReportCard
          v-for="rep in g.items"
          :key="rep.id"
          :report="rep"
          :linked-issues="issueStore.byDate(rep.date)"
        />
      </section>

      <div v-if="!filteredGroups.length" class="empty-hint">未找到匹配的日报，换个关键词试试～</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useReportStore } from '@/stores/reports'
import { useIssueStore } from '@/stores/issues'
import { useUiStore } from '@/stores/ui'
import ReportCard from '@/components/ReportCard.vue'

const route = useRoute()
const reportStore = useReportStore()
const issueStore = useIssueStore()
const ui = useUiStore()

/* 搜索 + 标签过滤 */
const filtered = computed(() => {
  const kw = ui.searchKw.trim().toLowerCase()
  return reportStore.items.filter((r) => {
    const txt = `${r.title} ${r.date} ${r.week} ${r.tasks.join(' ')} ${r.tags.join(' ')}`.toLowerCase()
    const kwOk = !kw || txt.includes(kw)
    const tagOk = ui.reportFilter === 'all' || r.tags.includes(ui.reportFilter)
    return kwOk && tagOk
  })
})

/* 按日期分组（同一天可多条） */
const filteredGroups = computed(() => {
  const map = new Map()
  filtered.value.forEach((r) => {
    if (!map.has(r.date)) {
      map.set(r.date, { date: r.date, week: r.week, isToday: r.isToday, items: [] })
    }
    map.get(r.date).items.push(r)
  })
  return [...map.values()]
})

function linkedCount(g) {
  return issueStore.byDate(g.date).length
}

/* 热力图点击定位 */
watch(
  () => ui.heatmapTarget,
  async (target) => {
    if (!target) return
    await nextTick()
    const el = document.getElementById('day-' + target.replace(/-/g, ''))
    if (!el) return
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    setTimeout(() => {
      ui.heatmapTarget = ''
    }, 1600)
  }
)

/* 从问题卡片跳转关联日报定位 */
watch(
  () => route.query.date,
  async (date) => {
    if (!date) return
    await nextTick()
    const el = document.getElementById('day-' + String(date).replace(/-/g, ''))
    if (!el) return
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    el.classList.add('flash')
    setTimeout(() => el.classList.remove('flash'), 1600)
  },
  { immediate: true }
)

onMounted(async () => {
  await Promise.all([reportStore.load(), issueStore.load()])
})
</script>

<style scoped>
.timeline-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 22px 28px 80px;
}
.timeline-inner {
  max-width: 800px;
  margin: 0 auto;
}

.tl-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.center-tabs {
  display: flex;
  gap: 4px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 4px;
  box-shadow: var(--shadow);
}
.tab-main {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 18px;
  border-radius: 9px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-2);
  transition: all 0.15s;
  text-decoration: none;
}
.tab-main:hover {
  color: var(--primary);
}
.tab-main.active {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}
.tab-main .cnt {
  font-size: 11px;
  background: rgba(15, 23, 42, 0.12);
  border-radius: 999px;
  padding: 1px 7px;
}
.tab-main.active .cnt {
  background: rgba(255, 255, 255, 0.25);
}
.tl-stat {
  font-size: 12.5px;
  color: var(--text-2);
}
.tl-stat b {
  color: var(--text);
}

.day-group {
  margin-bottom: 26px;
}
.day-head {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(245, 246, 250, 0.88);
  backdrop-filter: blur(8px);
  padding: 12px 4px;
  border-radius: 12px;
  margin-bottom: 12px;
}
.day-cal {
  width: 46px;
  height: 50px;
  border-radius: 12px;
  background: var(--card);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.day-cal .d {
  font-size: 20px;
  font-weight: 800;
  line-height: 1;
}
.day-cal .m {
  font-size: 10.5px;
  color: var(--text-2);
  margin-top: 3px;
}
.day-head .week {
  font-size: 15px;
  font-weight: 700;
}
.day-head .desc {
  font-size: 12px;
  color: var(--text-2);
  margin-top: 2px;
}
.today-pill {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--primary);
  padding: 3px 9px;
  border-radius: 999px;
  margin-left: 8px;
}
.day-actions {
  margin-left: auto;
}
.btn.small {
  padding: 6px 12px;
  font-size: 12.5px;
}
.empty-hint {
  text-align: center;
  color: var(--text-3);
  padding: 60px 0;
  font-size: 13.5px;
}
</style>
