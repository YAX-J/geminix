<template>
  <div v-if="show" class="reminder-banner">
    <span class="rb-icon">⏰</span>
    <span class="rb-text">今天还没写日报哦，下班前记得记录一下～</span>
    <button class="rb-btn" @click="goWrite">＋ 去记录</button>
    <button class="rb-close" @click="dismiss" title="今天不再提醒">✕</button>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useReportStore } from '@/stores/reports'
import { useUiStore } from '@/stores/ui'
import { getTodayReminderStatus } from '@/api/reminders'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'
const DISMISS_KEY = 'worklog_remind_dismiss'

const reportStore = useReportStore()
const ui = useUiStore()
const remind = ref(false)
const dismissed = ref(false)

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const writtenToday = computed(() => !!reportStore.byDate(todayStr()))
const show = computed(() => !writtenToday.value && remind.value && !dismissed.value)

async function check() {
  if (USE_MOCK) {
    remind.value = !writtenToday.value
    return
  }
  try {
    const st = await getTodayReminderStatus()
    remind.value = !!st.remind
  } catch (e) {
    remind.value = false
  }
}

function dismiss() {
  dismissed.value = true
  localStorage.setItem(DISMISS_KEY, todayStr())
}

function goWrite() {
  ui.openReportModal(todayStr())
}

onMounted(async () => {
  dismissed.value = localStorage.getItem(DISMISS_KEY) === todayStr()
  await reportStore.load()
  await check()
})

// 写完日报后自动收起提醒
watch(writtenToday, (v) => {
  if (v) remind.value = false
})
</script>

<style scoped>
.reminder-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(90deg, #fff7ed, #fffbeb);
  border-bottom: 1px solid #fde68a;
  padding: 9px 20px;
  font-size: 13px;
  color: #92400e;
  flex-shrink: 0;
  animation: rise 0.3s ease both;
}
.rb-icon {
  font-size: 15px;
}
.rb-text {
  flex: 1;
}
.rb-btn {
  font-size: 12.5px;
  font-weight: 700;
  color: #fff;
  background: var(--problem);
  border-radius: 8px;
  padding: 5px 13px;
  transition: background 0.15s;
}
.rb-btn:hover {
  background: #d97706;
}
.rb-close {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  color: #b45309;
  display: grid;
  place-items: center;
  font-size: 12px;
}
.rb-close:hover {
  background: #fef3c7;
}
</style>
