<template>
  <article :id="'issue-' + issue.id" class="issue-card" :class="issue.status">
    <div class="issue-head">
      <span v-if="issue.status === 'done'" class="status-pill done">✔ 已解决</span>
      <span v-else class="status-pill open">⏳ 待解决</span>
      <span v-if="issue.favorite" class="fav-pill" title="已收藏（常见问题置顶）">⭐ 收藏</span>
      <h3>{{ issue.title }}</h3>
      <span class="tag" :class="tagClassMap[issue.tag] || 'tag-backend'">{{ issue.tag }}</span>
    </div>

    <p class="issue-desc">{{ issue.desc }}</p>

    <!-- 解决方案：最佳高亮，其余折叠 -->
    <div v-if="solutionList.length" class="issue-solutions">
      <div
        v-for="(s, i) in visibleSolutions"
        :key="i"
        class="solution-item"
        :class="{ best: s.best }"
      >
        <span v-if="s.best" class="best-badge">⭐ 最佳方案</span>
        <span class="solution-text">{{ s.content }}</span>
      </div>
      <div v-if="hiddenCount > 0" class="more-toggle" @click="expanded = !expanded">
        {{ expanded ? '收起方案 ▲' : `还有 ${hiddenCount} 个方案 ▼` }}
      </div>
    </div>

    <div class="issue-foot">
      <span>创建于 {{ issue.createdAt }}</span>
      <span v-if="issue.reportDate" class="link" @click="gotoReport(issue.reportDate)">关联日报 {{ issue.reportDate.slice(5) }} ›</span>
      <span v-else class="orphan">独立问题 · 未关联日报</span>

      <span class="ops">
        <button class="op-btn fav" :class="{ on: issue.favorite }" @click="onFavorite">
          {{ issue.favorite ? '★ 已收藏' : '☆ 收藏' }}
        </button>
        <button v-if="issue.status === 'done'" class="op-btn done" @click="onToggle">↺ 重新打开</button>
        <button v-else class="op-btn done" @click="onToggle">✔ 标记已解决</button>
        <button class="op-btn del" @click="onDelete">删除</button>
      </span>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useIssueStore } from '@/stores/issues'
import { toast } from '@/utils/toast'
import { tagClassMap } from '@/mock/demoData'

const props = defineProps({
  issue: { type: Object, required: true }
})

const router = useRouter()
const issueStore = useIssueStore()
const expanded = ref(false)

/** 兼容旧数据：solutions 为空但有 solution 时，构造单条最佳方案 */
const solutionList = computed(() => {
  if (Array.isArray(props.issue.solutions) && props.issue.solutions.length) {
    return props.issue.solutions.filter((s) => s && s.content)
  }
  if (props.issue.solution) {
    return [{ content: props.issue.solution, best: true }]
  }
  return []
})

const visibleSolutions = computed(() => {
  if (expanded.value) return solutionList.value
  const best = solutionList.value.find((s) => s.best)
  return best ? [best] : solutionList.value.slice(0, 1)
})

const hiddenCount = computed(() => solutionList.value.length - visibleSolutions.value.length)

function onFavorite() {
  issueStore.toggleFavorite(props.issue.id)
  toast(props.issue.favorite ? '已取消收藏' : '已收藏，将置顶展示 ⭐')
}

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
.fav-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 999px;
  flex-shrink: 0;
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
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
.issue-solutions {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.solution-item {
  background: var(--bg);
  border-left: 3px solid var(--solution);
  border-radius: 0 8px 8px 0;
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.6;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.solution-item.best {
  background: var(--solution-bg);
  border-left-color: #f59e0b;
}
.best-badge {
  color: #b45309;
  font-size: 11px;
  font-weight: 800;
}
.solution-text {
  color: var(--text);
}
.more-toggle {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
  cursor: pointer;
  padding: 4px 2px;
  user-select: none;
}
.more-toggle:hover {
  text-decoration: underline;
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
.op-btn.fav {
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fde68a;
}
.op-btn.fav:hover,
.op-btn.fav.on {
  background: #f59e0b;
  color: #fff;
  border-color: #f59e0b;
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
