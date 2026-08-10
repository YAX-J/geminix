<template>
  <Teleport to="body">
    <div v-if="ui.tagModalOpen" class="overlay" @click.self="close">
      <div class="modal tag-modal">
        <div class="modal-head">
          <h3>🏷 标签管理</h3>
          <button class="modal-close" @click="close">✕</button>
        </div>
        <div class="modal-body">
          <div class="add-row">
            <input
              v-model.trim="newName"
              type="text"
              placeholder="新标签名称，如：微服务"
              @keyup.enter="onAdd"
            />
            <button class="btn btn-primary small" @click="onAdd">＋ 添加</button>
          </div>
          <div v-if="tagStore.all.length" class="tag-admin-list">
            <div v-for="t in tagStore.all" :key="t.id" class="tag-admin-row">
              <span class="tag" :class="t.color || 'tag-backend'">{{ t.name }}</span>
              <input
                v-model="editName[t.id]"
                class="rename-input"
                type="text"
                @change="onRename(t)"
                placeholder="重命名…"
              />
              <button class="op-btn del" @click="onRemove(t)">删除</button>
            </div>
          </div>
          <div v-else class="no-data">暂无标签</div>
          <div class="hint">日报/问题的标签选项将实时同步；内置五类可重命名或删除。</div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useUiStore } from '@/stores/ui'
import { useTagStore } from '@/stores/tags'
import { toast } from '@/utils/toast'

const ui = useUiStore()
const tagStore = useTagStore()

const newName = ref('')
const editName = reactive({})

watch(
  () => ui.tagModalOpen,
  async (open) => {
    if (!open) return
    await tagStore.load(true)
    tagStore.all.forEach((t) => {
      editName[t.id] = t.name
    })
  }
)

async function onAdd() {
  const tag = await tagStore.add(newName.value)
  if (!tag) {
    toast('标签名无效或已存在')
    return
  }
  editName[tag.id] = tag.name
  newName.value = ''
  toast(`标签「${tag.name}」已添加 ✔`)
}

async function onRename(t) {
  const val = (editName[t.id] || '').trim()
  if (!val || val === t.name) return
  await tagStore.rename(t.id, val)
  toast(`已重命名为「${val}」`)
}

async function onRemove(t) {
  if (!window.confirm(`确定删除标签「${t.name}」？已有数据不受影响。`)) return
  await tagStore.remove(t.id)
  toast(`标签「${t.name}」已删除`)
}

function close() {
  ui.tagModalOpen = false
}
</script>

<style scoped>
.tag-modal .modal-body {
  gap: 14px;
}
.add-row {
  display: flex;
  gap: 8px;
}
.add-row input {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 9px 12px;
  font-size: 13.5px;
  background: var(--bg);
}
.add-row input:focus {
  border-color: var(--primary);
  background: #fff;
  box-shadow: 0 0 0 3px var(--primary-bg);
}
.tag-admin-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}
.tag-admin-row {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 8px 12px;
}
.tag-admin-row .tag {
  flex-shrink: 0;
  min-width: 52px;
  text-align: center;
}
.rename-input {
  flex: 1;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 13px;
  background: transparent;
  color: var(--text);
}
.rename-input:focus {
  border-color: var(--primary);
  background: #fff;
}
.op-btn.del {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 11px;
  border-radius: 8px;
  color: #dc2626;
  background: #fef2f2;
  flex-shrink: 0;
}
.op-btn.del:hover {
  background: #dc2626;
  color: #fff;
}
.no-data {
  color: var(--text-3);
  font-size: 12.5px;
  text-align: center;
  padding: 20px 0;
}
</style>
