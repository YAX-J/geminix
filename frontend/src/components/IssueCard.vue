<template>
  <article :id="'issue-' + issue.id" class="issue-card" :class="issue.status">
    <div class="issue-head">
      <span v-if="issue.status === 'done'" class="status-pill done">✔ 已解决</span>
      <span v-else class="status-pill open">⏳ 待解决</span>
      <h3>{{ issue.title }}</h3>
      <span class="tag" :class="tagClassMap[issue.tag] || 'tag-backend'">{{ issue.tag }}</span>
    </div>

    <p class="issue-desc">{{ issue.desc }}</p>

    <div v-if="issue.status === 'done' && issue.solution" class="issue-solution">
      <b>✔ 解决方案</b>{{ issue.solution }}
    </div>

    <div class="issue-foot">
      <span>创建于 {{ issue.createdAt }}</span>
      <span v-if="issue.reportDate" class="link" @click="gotoReport(issue.reportDate)">关联日报 {{ issue.reportDate.slice(5) }} ›</span>
      <span v-else class="orphan">独立问题 · 未关联日报</span>

      <span class="ops">
        <button v-if="issue.status === 'done'" class="op-btn done" @click="onToggle">↺ 重新打开</button>
        <button v-else class="op-btn done" @click="onToggle">✔ 标记已解决</button>
        <button class="op-btn del" @click="onDelete">删除</button>
      </span>
    </div>
  </article>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useIssueStore } from '@/stores/issues'
import { toast } from '@/utils/toast'
import { tagClassMap } from '@/mock/demoData'

const props = defineProps({
  issue: { type: Object, required: true }
})

const router = useRouter()
const issueStore = useIssueStore()

function onToggle() {
  issueStore.toggleStatus(props.issue.id)
  toast(props.issue.status === 'done' ? '问题已重新打开' : '问题已标记解决 ✔')
}

function onDelete() {
  if (!window.confirm(`确定删除问题「${props.issue.title}」？`)) return
  issueStore.removeIssue(props.issue.id)
  toast('问题已删除')
}

function gotoReport(date) {
  router.push({ path: '/', query: { date } })
}
</script>

<style scoped>
.issue-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-left: 4px solid var(--problem);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 16px 18px;
  margin-bottom: 12px;
  animation: rise 0.35s ease both;
  transition: box-shadow 0.2s;
}
.issue-card:hover {
  box-shadow: 0 2px 4px rgba(30, 41, 59, 0.06), 0 10px 28px rgba(30, 41, 59, 0.1);
}
.issue-card.done {
  border-left-color: var(--solution);
}
.issue-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.status-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 999px;
  flex-shrink: 0;
}
.status-pill.open {
  background: var(--problem-bg);
  color: var(--problem-deep);
}
.status-pill.done {
  background: var(--solution-bg);
  color: var(--solution-deep);
}
.issue-head h3 {
  font-size: 15px;
  font-weight: 700;
  flex: 1;
  min-width: 180px;
}
.issue-desc {
  font-size: 13.5px;
  color: var(--text);
  line-height: 1.65;
  margin-top: 10px;
}
.issue-solution {
  background: var(--solution-bg);
  border-left: 3px solid var(--solution);
  border-radius: 0 8px 8px 0;
  padding: 10px 12px;
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.6;
}
.issue-solution b {
  color: var(--solution-deep);
  font-size: 11.5px;
  display: block;
  margin-bottom: 4px;
}
.issue-foot {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
  font-size: 12px;
  color: var(--text-3);
  flex-wrap: wrap;
}
.issue-foot .link {
  color: var(--primary);
  font-weight: 600;
  cursor: pointer;
}
.issue-foot .link:hover {
  text-decoration: underline;
}
.issue-foot .orphan {
  opacity: 0.75;
}
.ops {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.op-btn {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 11px;
  border-radius: 8px;
  transition: all 0.15s;
}
.op-btn.done {
  color: var(--solution-deep);
  background: var(--solution-bg);
}
.op-btn.done:hover {
  background: var(--solution);
  color: #fff;
}
.op-btn.del {
  color: #dc2626;
  background: #fef2f2;
}
.op-btn.del:hover {
  background: #dc2626;
  color: #fff;
}
</style>
