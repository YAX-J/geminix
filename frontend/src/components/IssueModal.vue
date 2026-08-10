<template>
  <Teleport to="body">
    <div v-if="ui.issueModalOpen" class="overlay" @click.self="close">
      <div class="modal">
        <div class="modal-head">
          <h3>⚠ 记录问题</h3>
          <button class="modal-close" @click="close">✕</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label>问题标题 <span class="req">*</span></label>
            <input v-model="form.title" type="text" placeholder="一句话概括，如：Redis 缓存击穿导致慢查询" />
          </div>
          <div class="field">
            <label>问题描述 <span class="req">*</span></label>
            <textarea v-model="form.desc" placeholder="背景、现象、影响范围…"></textarea>
          </div>
          <div class="field">
            <label>解决方案（选填，可添加多个，⭐ 标记最佳）</label>
            <div class="solution-list">
              <div v-for="(s, idx) in form.solutions" :key="idx" class="solution-row">
                <span
                  class="best-toggle"
                  :class="{ on: s.best }"
                  :title="s.best ? '取消标记为最佳' : '标记为最佳方案'"
                  @click="toggleBest(idx)"
                >⭐</span>
                <input
                  v-model="s.content"
                  type="text"
                  placeholder="方案描述，如：本地缓存 + Redis 分布式锁双重兜底"
                  @keyup.enter="addSolution"
                />
                <button
                  v-if="form.solutions.length > 1"
                  class="sol-del"
                  title="移除该方案"
                  @click="removeSolution(idx)"
                >✕</button>
              </div>
            </div>
            <button class="btn btn-ghost small" @click="addSolution">＋ 添加方案</button>
            <div class="hint">填写任意方案后自动标记为已解决；⭐ 最佳方案在问题卡片高亮展示</div>
          </div>
          <div class="field-row">
            <div class="field">
              <label>标签</label>
              <div class="chip-select">
                <span
                  v-for="t in tagOptions"
                  :key="t"
                  class="tag-chip"
                  :class="{ active: form.tag === t }"
                  @click="form.tag = t"
                >{{ t }}</span>
              </div>
            </div>
            <div class="field">
              <label>关联日报日期（可选）</label>
              <input v-model="form.reportDate" type="date" />
              <div class="hint">留空则为独立问题，不依赖日报</div>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-ghost" @click="close">取消</button>
          <button class="btn btn-problem" @click="save">保存问题</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { reactive, watch, computed, onMounted } from 'vue'
import { useUiStore } from '@/stores/ui'
import { useIssueStore } from '@/stores/issues'
import { useTagStore } from '@/stores/tags'
import { toast } from '@/utils/toast'

const ui = useUiStore()
const issueStore = useIssueStore()
const tagStore = useTagStore()

// 标签选项：动态加载（兜底五类）
const tagOptions = computed(() => (tagStore.names.length ? tagStore.names : ['后端', '前端', '数据库', 'Redis', '运维']))

onMounted(() => tagStore.load())

const form = reactive({ title: '', desc: '', solutions: [], tag: '后端', reportDate: '' })

watch(
  () => ui.issueModalOpen,
  (open) => {
    if (!open) return
    form.title = ''
    form.desc = ''
    form.solutions = [{ content: '', best: true }]
    form.tag = '后端'
    form.reportDate = ''
  }
)

function addSolution() {
  form.solutions.push({ content: '', best: false })
}

function removeSolution(idx) {
  form.solutions.splice(idx, 1)
}

function toggleBest(idx) {
  form.solutions.forEach((s, i) => {
    s.best = i === idx
  })
}

function close() {
  ui.closeIssueModal()
}

function save() {
  if (!form.title.trim() || !form.desc.trim()) {
    toast('问题标题与描述为必填项')
    return
  }
  const now = new Date()
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const solutions = form.solutions
    .map((s) => ({ content: s.content.trim(), best: !!s.best }))
    .filter((s) => s.content)
  const hasSolution = solutions.length > 0
  issueStore.addIssue({
    title: form.title.trim(),
    desc: form.desc.trim(),
    solution: hasSolution ? solutions.find((s) => s.best)?.content || solutions[0].content : '',
    solutions: hasSolution ? solutions : [],
    tag: form.tag,
    status: hasSolution ? 'done' : 'open',
    favorite: false,
    createdAt: today,
    reportDate: form.reportDate
  })
  close()
  toast(hasSolution ? '问题已解决并存档 ✔' : '问题已记录，待解决 ⏳')
}
</script>

<style scoped>
.solution-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}
.solution-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.solution-row input[type='text'] {
  flex: 1;
}
.best-toggle {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 14px;
  cursor: pointer;
  background: var(--bg);
  border: 1px solid var(--border);
  filter: grayscale(1);
  opacity: 0.55;
  transition: all 0.15s;
  user-select: none;
}
.best-toggle.on {
  filter: none;
  opacity: 1;
  background: var(--solution-bg);
  border-color: #fbbf24;
}
.sol-del {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--text-3);
  font-size: 13px;
}
.sol-del:hover {
  background: #fef2f2;
  color: #dc2626;
}
</style>
