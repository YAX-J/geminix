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
            <label>解决方案（选填，填写后自动标记为已解决）</label>
            <textarea v-model="form.solution" placeholder="如何排查与解决的关键步骤…"></textarea>
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
import { reactive, watch } from 'vue'
import { useUiStore } from '@/stores/ui'
import { useIssueStore } from '@/stores/issues'
import { toast } from '@/utils/toast'

const ui = useUiStore()
const issueStore = useIssueStore()

const tagOptions = ['后端', '前端', '数据库', 'Redis', '运维']

const form = reactive({ title: '', desc: '', solution: '', tag: '后端', reportDate: '' })

watch(
  () => ui.issueModalOpen,
  (open) => {
    if (!open) return
    form.title = ''
    form.desc = ''
    form.solution = ''
    form.tag = '后端'
    form.reportDate = ''
  }
)

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
  issueStore.addIssue({
    title: form.title.trim(),
    desc: form.desc.trim(),
    solution: form.solution.trim(),
    tag: form.tag,
    status: form.solution.trim() ? 'done' : 'open',
    createdAt: today,
    reportDate: form.reportDate
  })
  close()
  toast(form.solution.trim() ? '问题已解决并存档 ✔' : '问题已记录，待解决 ⏳')
}
</script>
