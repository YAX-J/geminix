<template>
  <article class="report-card">
    <div class="rc-head">
      <span class="rc-time">{{ report.time }}</span>
      <h3>{{ report.title }}</h3>
      <span
        v-for="t in report.tags"
        :key="t"
        class="tag"
        :class="tagClassMap[t] || 'tag-backend'"
      >{{ t }}</span>
    </div>

    <ul class="rc-tasks">
      <li v-for="(task, i) in report.tasks" :key="i">{{ task }}</li>
    </ul>

    <!-- 关联问题入口（当天有独立记录的问题时显示） -->
    <div v-if="linkedIssues.length" class="rc-links">
      <span
        v-for="iss in linkedIssues"
        :key="iss.id"
        class="rc-link"
        @click="gotoIssue(iss.id)"
      >⚠ {{ iss.title }}</span>
    </div>

    <footer class="rc-foot">
      <span class="rc-edit" @click="onEdit">编辑</span>
      <span class="rc-edit danger" @click="onDelete">删除</span>
    </footer>
  </article>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useReportStore } from '@/stores/reports'
import { toast } from '@/utils/toast'
import { tagClassMap } from '@/mock/demoData'

const props = defineProps({
  report: { type: Object, required: true },
  linkedIssues: { type: Array, default: () => [] }
})

const router = useRouter()
const reportStore = useReportStore()

function gotoIssue(id) {
  router.push({ path: '/issues', query: { focus: id } })
}

function onEdit() {
  toast('日报编辑功能开发中')
}

function onDelete() {
  if (!window.confirm(`确定删除 ${props.report.date} 的日报「${props.report.title}」？`)) return
  reportStore.removeReport(props.report.id)
  toast('日报已删除')
}
</script>

<style scoped>
.report-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 18px 20px;
  margin-bottom: 14px;
  animation: rise 0.35s ease both;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.report-card:hover {
  box-shadow: 0 2px 4px rgba(30, 41, 59, 0.06), 0 10px 28px rgba(30, 41, 59, 0.1);
  border-color: #d7dbe7;
}
.rc-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.rc-time {
  font-size: 11.5px;
  color: var(--text-3);
  background: var(--bg);
  padding: 3px 9px;
  border-radius: 6px;
  font-weight: 600;
}
.rc-head h3 {
  font-size: 15.5px;
  font-weight: 700;
  flex: 1;
  min-width: 200px;
}
.rc-tasks {
  list-style: none;
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rc-tasks li {
  display: flex;
  gap: 9px;
  font-size: 13.5px;
  color: var(--text);
  line-height: 1.55;
}
.rc-tasks li::before {
  content: '';
  flex-shrink: 0;
  width: 15px;
  height: 15px;
  margin-top: 3px;
  border-radius: 5px;
  background: var(--solution-bg)
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2310b981' stroke-width='3.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M20 6 9 17l-5-5'/%3E%3C/svg%3E")
    center / 11px no-repeat;
}

.rc-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 13px;
}
.rc-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--problem-deep);
  background: var(--problem-bg);
  border: 1px solid #fde68a;
  border-radius: 999px;
  padding: 5px 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.rc-link:hover {
  background: var(--problem);
  color: #fff;
  border-color: var(--problem);
}

.rc-foot {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
}
.rc-edit {
  font-size: 12px;
  color: var(--text-3);
  cursor: pointer;
}
.rc-edit:hover {
  color: var(--primary);
}
.rc-edit.danger:hover {
  color: #dc2626;
}
</style>
