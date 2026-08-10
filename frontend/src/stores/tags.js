import { defineStore } from 'pinia'
import { getTagList, createTag, updateTag, deleteTag } from '@/api/tags'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 默认五类标签（mock 模式与后端未初始化时兜底） */
export const DEFAULT_TAGS = [
  { id: 1, name: '后端', color: 'tag-backend', sort: 1 },
  { id: 2, name: '前端', color: 'tag-frontend', sort: 2 },
  { id: 3, name: '数据库', color: 'tag-db', sort: 3 },
  { id: 4, name: 'Redis', color: 'tag-redis', sort: 4 },
  { id: 5, name: '运维', color: 'tag-ops', sort: 5 }
]

const MOCK_KEY = 'worklog_custom_tags'

export const useTagStore = defineStore('tags', {
  state: () => ({
    items: [],
    loaded: false
  }),

  getters: {
    /** 标签名称列表（弹窗/筛选用） */
    names: (s) => s.items.map((t) => t.name),
    /** 名称 -> 颜色类映射 */
    colorMap: (s) => Object.fromEntries(s.items.map((t) => [t.name, t.color])),
    /** 标签管理列表（mock 时含本地自定义标签） */
    all: (s) => s.items
  },

  actions: {
    async load(force = false) {
      if (this.loaded && !force) return
      if (USE_MOCK) {
        const local = JSON.parse(localStorage.getItem(MOCK_KEY) || '[]')
        const localNames = new Set(local.map((t) => t.name))
        this.items = [...DEFAULT_TAGS.filter((t) => !localNames.has(t.name)), ...local]
      } else {
        const list = await getTagList()
        this.items = list && list.length ? list : DEFAULT_TAGS
      }
      this.loaded = true
    },

    async add(name, color = '') {
      if (!name || !name.trim()) return null
      const trimmed = name.trim()
      if (this.items.some((t) => t.name === trimmed)) return null
      if (USE_MOCK) {
        const tag = { id: Date.now(), name: trimmed, color: color || null, sort: this.items.length + 1 }
        this.items.push(tag)
        this.persistLocal()
        return tag
      }
      const tag = await createTag(trimmed, color)
      this.items.push(tag)
      return tag
    },

    async remove(id) {
      if (USE_MOCK) {
        this.items = this.items.filter((t) => t.id !== id)
        this.persistLocal()
        return
      }
      await deleteTag(id)
      this.items = this.items.filter((t) => t.id !== id)
    },

    async rename(id, name) {
      const tag = this.items.find((t) => t.id === id)
      if (!tag || !name || !name.trim()) return
      if (USE_MOCK) {
        tag.name = name.trim()
        this.persistLocal()
        return
      }
      const updated = await updateTag(id, { name: name.trim() })
      Object.assign(tag, updated)
    },

    persistLocal() {
      // mock 模式：自定义标签持久化到 localStorage，内置五类不落盘
      const defaults = new Set(DEFAULT_TAGS.map((t) => t.name))
      localStorage.setItem(MOCK_KEY, JSON.stringify(this.items.filter((t) => !defaults.has(t.name))))
    }
  }
})
