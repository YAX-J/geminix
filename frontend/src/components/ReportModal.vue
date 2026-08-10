<template>
  <Teleport to="body">
    <div v-if="ui.reportModalOpen" class="overlay" @click.self="close">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ isEdit ? '✏️ 编辑日报' : '📋 记录日报' }}</h3>
          <button class="modal-close" @click="close">✕</button>
        </div>
        <div class="modal-body">
          <div class="field-row">
            <div class="field">
              <label>日期 <span class="req">*</span></label>
              <input v-model="form.date" type="date" />
            </div>
            <div class="field">
              <label>时间段（可选）</label>
              <input v-model="form.time" type="text" placeholder="如 09:30 - 18:20" />
            </div>
          </div>
          <div class="field">
            <label>今日工作内容 <span class="req">*</span></label>
            <textarea
              v-model="form.content"
              placeholder="分条列出，如：&#10;1. 完成订单列表接口压测&#10;2. 修复库存扣减并发问题"
            ></textarea>
          </div>
          <div class="field">
            <label>标签</label>
            <div class="chip-select">
              <span
                v-for="t in tagOptions"
                :key="t"
                class="tag-chip"
                :class="{ active: form.tags.includes(t) }"
                @click="toggleTag(t)"
              >{{ t }}</span>
            </div>
            <div class="hint">遇到问题请使用「⚠ 记录问题」独立沉淀，日报仅记录工作内容</div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-ghost" @click="close">取消</button>
          <button class="btn btn-primary" @click="save">{{ isEdit ? '保存修改' : '保存日报' }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { reactive, watch, computed, onMounted } from 'vue'
import { useUiStore } from '@/stores/ui'
import { useReportStore } from '@/stores/reports'
import { useTagStore } from '@/stores/tags'
import { toast } from '@/utils/toast'
import { weekCN } from '@/mock/demoData'

const ui = useUiStore()
const reportStore = useReportStore()
const tagStore = useTagStore()

// 标签选项：动态加载（兜底五类）
const tagOptions = computed(() => (tagStore.names.length ? tagStore.names : ['后端', '前端', '数据库', 'Redis', '运维']))

onMounted(() => tagStore.load())

const form = reactive({ date: '', time: '', content: '', tags: [] })

const isEdit = computed(() => !!ui.reportModalEdit)

watch(
  () => ui.reportModalOpen,
  (open) => {
    if (!open) return
    const edit = ui.reportModalEdit
    if (edit) {
      // 编辑模式：预填原日报内容
      form.date = edit.date
      form.time = edit.time || ''
      form.content = (edit.tasks || []).map((t, i) => `${i + 1}. ${t}`).join('\n')
      form.tags = [...(edit.tags || [])]
    } else {
      form.date = ui.reportModalDate || todayStr()
      form.time = ''
      form.content = ''
      form.tags = []
    }
  }
)

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function toggleTag(t) {
  const idx = form.tags.indexOf(t)
  if (idx >= 0) form.tags.splice(idx, 1)
  else form.tags.push(t)
}

function close() {
  ui.closeReportModal()
}

function save() {
  if (!form.date || !form.content.trim()) {
    toast('日期与工作内容为必填项')
    return
  }
  const dt = new Date(form.date.replace(/-/g, '/'))
  const tasks = form.content
    .split('\n')
    .map((s) => s.replace(/^\d+[.、]\s*/, '').trim())
    .filter(Boolean)
  const payload = {
    date: form.date,
    week: '星期' + weekCN[dt.getDay()],
    isToday: form.date === todayStr(),
    time: form.time.trim() || '09:00 - 18:00',
    title: tasks[0] || '未命名记录',
    tags: form.tags.length ? [...form.tags] : ['后端'],
    tasks
  }
  if (isEdit.value) {
    reportStore.updateReport(ui.reportModalEdit.id, payload)
    toast('日报已更新 ✔')
  } else {
    reportStore.addReport(payload)
    toast('日报已保存 ✔')
  }
  close()
}
</script>
