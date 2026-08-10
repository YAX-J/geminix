import { reactive } from 'vue'

// 轻量全局 Toast（不依赖 UI 组件库）
export const toasts = reactive([])

export function toast(msg) {
  const item = { id: Date.now() + Math.random(), msg }
  toasts.push(item)
  setTimeout(() => {
    const idx = toasts.findIndex((t) => t.id === item.id)
    if (idx >= 0) toasts.splice(idx, 1)
  }, 2400)
}
