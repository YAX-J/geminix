<template>
  <header class="topbar">
    <div class="brand">
      <div class="brand-logo">记</div>
      <div class="brand-name">工作日志台<small>日报 · 问题 · 双独立模块</small></div>
    </div>

    <!-- 全局搜索：同时检索日报与问题 -->
    <div class="search-box">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
        <circle cx="11" cy="11" r="7" />
        <path d="m21 21-4.3-4.3" />
      </svg>
      <input
        ref="searchInput"
        v-model="ui.searchKw"
        type="text"
        placeholder="搜索日报或问题，如「缓存击穿」「N+1」「08-10」…"
      />
      <span class="kbd">Ctrl K</span>
    </div>

    <div class="topbar-actions">
      <div class="export-drop">
        <button class="btn btn-ghost" @click="exportOpen = !exportOpen">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3v12m0 0 4-4m-4 4-4-4" />
            <path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
          </svg>
          导出
          <span class="caret">▾</span>
        </button>
        <div v-if="exportOpen" class="drop-menu">
          <div class="drop-item" @click="exportWeek">周报（Markdown）</div>
          <div class="drop-item" @click="exportMonth">月报（Markdown）</div>
          <div class="drop-item" @click="exportReports">日报 Excel</div>
          <div class="drop-item" @click="exportIssues">问题 Excel</div>
        </div>
      </div>
      <button class="btn btn-ghost" @click="ui.openTagModal()">🏷 标签</button>
      <button class="btn btn-problem" @click="ui.openIssueModal()">⚠ 记录问题</button>
      <button class="btn btn-primary" @click="ui.openReportModal()">＋ 记录日报</button>
      <div class="user-box">
        <div class="avatar">{{ avatarText }}</div>
        <div class="user-meta">
          <span class="user-name">{{ auth.nickname }}</span>
          <button class="logout" @click="onLogout">退出登录</button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'
import { useReportStore } from '@/stores/reports'
import { useIssueStore } from '@/stores/issues'
import { exportWeekly, exportMonthly, exportReportsExcel } from '@/api/reports'
import { exportIssuesExcel } from '@/api/issues'
import { toast } from '@/utils/toast'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

const router = useRouter()
const ui = useUiStore()
const auth = useAuthStore()
const reportStore = useReportStore()
const issueStore = useIssueStore()
const searchInput = ref(null)
const exportOpen = ref(false)

const avatarText = computed(() => (auth.nickname || '用').slice(0, 1))

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 下载 Blob 文件 */
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

/** 导出本周 Markdown 周报 */
async function exportWeek() {
  try {
    const date = todayStr()
    const blob = await exportWeekly(date)
    downloadBlob(blob, `worklog-week-${date}.md`)
    toast('周报已导出 ✔')
  } catch (e) {
    toast(e.message || '导出失败')
  }
}

/** 导出本月 Markdown 月报 */
async function exportMonth() {
  try {
    const date = todayStr()
    const blob = await exportMonthly(date)
    downloadBlob(blob, `worklog-month-${date}.md`)
    toast('月报已导出 ✔')
  } catch (e) {
    toast(e.message || '导出失败')
  }
}

/** 日报导出：API 模式 xlsx，mock 模式 CSV */
async function exportReports() {
  if (USE_MOCK) {
    await reportStore.load()
    const rows = reportStore.items.map((r) => ({
      日期: r.date,
      星期: r.week || '',
      时间段: r.time || '',
      标题: r.title,
      工作内容: (r.tasks || []).join('\n'),
      标签: (r.tags || []).join(' / ')
    }))
    downloadCSV(rows, 'worklog-reports.csv')
    toast('日报已导出 ✔')
    return
  }
  try {
    const blob = await exportReportsExcel()
    downloadBlob(blob, 'worklog-reports.xlsx')
    toast('日报 Excel 已导出 ✔')
  } catch (e) {
    toast(e.message || '导出失败')
  }
}

/** 问题导出：API 模式 xlsx，mock 模式 CSV */
async function exportIssues() {
  if (USE_MOCK) {
    await issueStore.load()
    const rows = issueStore.items.map((i) => ({
      问题标题: i.title,
      问题描述: i.desc,
      解决方案: i.solution || '',
      标签: i.tag,
      状态: i.status === 'done' ? '已解决' : '待解决',
      关联日报: i.reportDate || '',
      收藏: i.favorite ? '是' : '否'
    }))
    downloadCSV(rows, 'worklog-issues.csv')
    toast('问题已导出 ✔')
    return
  }
  try {
    const blob = await exportIssuesExcel()
    downloadBlob(blob, 'worklog-issues.xlsx')
    toast('问题 Excel 已导出 ✔')
  } catch (e) {
    toast(e.message || '导出失败')
  }
}

/** mock 模式 CSV 下载（带 BOM 保证 Excel 中文不乱码） */
function downloadCSV(rows, filename) {
  if (!rows.length) {
    toast('暂无数据可导出')
    return
  }
  const header = Object.keys(rows[0])
  const esc = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = [header.join(','), ...rows.map((r) => header.map((h) => esc(r[h])).join(','))].join('\n')
  downloadBlob(new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' }), filename)
}

function onLogout() {
  if (!window.confirm('确定退出登录？')) return
  auth.logout()
  toast('已退出登录')
  router.replace('/login')
}

function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    searchInput.value?.focus()
  }
  if (e.key === 'Escape') exportOpen.value = false
}

function onDocClick(e) {
  if (!e.target.closest('.export-drop')) exportOpen.value = false
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('click', onDocClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('click', onDocClick)
})
</script>

<style scoped>
.topbar {
  height: 60px;
  flex-shrink: 0;
  background: var(--card);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 20px;
  z-index: 30;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 172px;
}
.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 16px;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.35);
}
.brand-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  white-space: nowrap;
}
.brand-name small {
  display: block;
  font-size: 11px;
  font-weight: 400;
  color: var(--text-3);
  margin-top: 1px;
}

.search-box {
  flex: 1;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 8px 14px;
  transition: all 0.2s;
}
.search-box:focus-within {
  border-color: var(--primary);
  background: #fff;
  box-shadow: 0 0 0 3px var(--primary-bg);
}
.search-box svg {
  flex-shrink: 0;
  color: var(--text-3);
}
.search-box input {
  flex: 1;
  border: none;
  background: none;
  font-size: 14px;
  color: var(--text);
}
.search-box input::placeholder {
  color: var(--text-3);
}
.kbd {
  font-size: 11px;
  color: var(--text-2);
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 5px;
  padding: 1px 6px;
  flex-shrink: 0;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 300px;
  justify-content: flex-end;
}
.export-drop {
  position: relative;
}
.caret {
  font-size: 10px;
  margin-left: 2px;
  opacity: 0.7;
}
.drop-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 168px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.14);
  padding: 6px;
  z-index: 60;
  animation: rise 0.18s ease both;
}
.drop-item {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.12s;
  white-space: nowrap;
}
.drop-item:hover {
  background: var(--primary-bg);
  color: var(--primary);
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}
.user-box {
  display: flex;
  align-items: center;
  gap: 9px;
}
.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.user-name {
  font-size: 13px;
  font-weight: 700;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.logout {
  font-size: 11px;
  color: var(--text-3);
  text-align: left;
  transition: color 0.15s;
}
.logout:hover {
  color: #dc2626;
}

@media (max-width: 700px) {
  .kbd {
    display: none;
  }
  .topbar-actions .btn-ghost {
    display: none;
  }
}
</style>
