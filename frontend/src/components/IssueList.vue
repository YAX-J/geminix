<template>
  <div class="issue-wrap">
    <div class="issue-inner">
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
          <b>{{ filtered.length }}</b> 个问题 · {{ ui.searchKw ? `筛选「${ui.searchKw}」` : '全部' }}
        </div>
      </div>

      <div class="issue-toolbar">
        <div class="issue-filter">
          <span
            v-for="f in statusFilters"
            :key="f.value"
            class="f-chip"
            :class="{ active: ui.issueFilter === f.value }"
            @click="ui.issueFilter = f.value"
          >{{ f.label }}</span>
        </div>
        <button class="btn btn-problem small" @click="ui.openIssueModal()">＋ 记录问题</button>
      </div>

      <IssueCard v-for="iss in filtered" :key="iss.id" :issue="iss" />

      <div v-if="!filtered.length" class="empty-hint">暂无匹配的问题，点「记录问题」开始沉淀～</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useReportStore } from '@/stores/reports'
import { useIssueStore } from '@/stores/issues'
import { useUiStore } from '@/stores/ui'
import IssueCard from '@/components/IssueCard.vue'

const route = useRoute()
const reportStore = useReportStore()
const issueStore = useIssueStore()
const ui = useUiStore()

const statusFilters = [
  { label: '全部', value: 'all' },
  { label: '⏳ 待解决', value: 'open' },
  { label: '✔ 已解决', value: 'done' }
]

const filtered = computed(() => {
  const kw = ui.searchKw.trim().toLowerCase()
  return issueStore.items
    .filter((i) => {
      const txt = `${i.title} ${i.desc} ${i.solution || ''} ${i.tag} ${i.createdAt}`.toLowerCase()
      const kwOk = !kw || txt.includes(kw)
      const stOk = ui.issueFilter === 'all' || i.status === ui.issueFilter
      return kwOk && stOk
    })
    .sort((a, b) => {
      // 收藏（常见问题）置顶
      const fa = a.favorite ? 1 : 0
      const fb = b.favorite ? 1 : 0
      return fb - fa
    })
})

/* 从日报卡片跳转过来：定位并高亮目标问题 */
watch(
  () => route.query.focus,
  async (focus) => {
    if (!focus) return
    await nextTick()
    const el = document.getElementById('issue-' + focus)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      el.classList.add('flash')
      setTimeout(() => el.classList.remove('flash'), 1600)
    }
  },
  { immediate: true }
)

onMounted(async () => {
  await Promise.all([reportStore.load(), issueStore.load()])
})
</script>

<style scoped>
.issue-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 22px 28px 80px;
}
.issue-inner {
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

.issue-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 10px;
  flex-wrap: wrap;
}
.issue-filter {
  display: flex;
  gap: 6px;
}
.f-chip {
  font-size: 12px;
  padding: 5px 14px;
  border-radius: 999px;
  background: var(--card);
  border: 1px solid var(--border);
  color: var(--text-2);
  font-weight: 600;
  transition: all 0.15s;
  cursor: pointer;
}
.f-chip:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.f-chip.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
.btn.small {
  padding: 6px 14px;
  font-size: 12.5px;
}
.empty-hint {
  text-align: center;
  color: var(--text-3);
  padding: 60px 0;
  font-size: 13.5px;
}
</style>
