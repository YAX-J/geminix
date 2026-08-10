import { defineStore } from 'pinia'
import { demoIssues } from '@/mock/demoData'
import { getIssueList, createIssue, updateIssue, deleteIssue, toggleIssueStatus } from '@/api/issues'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export const useIssueStore = defineStore('issues', {
  state: () => ({
    items: [],
    loaded: false
  }),

  getters: {
    total: (s) => s.items.length,
    doneCount: (s) => s.items.filter((i) => i.status === 'done').length,
    openCount: (s) => s.items.filter((i) => i.status === 'open').length,
    solveRate: (s) => (s.items.length ? Math.round((s.items.filter((i) => i.status === 'done').length / s.items.length) * 100) : 0),
    byDate: (s) => (date) => s.items.filter((i) => i.reportDate === date)
  },

  actions: {
    async load() {
      if (this.loaded) return
      if (USE_MOCK) {
        this.items = [...demoIssues]
      } else {
        const page = await getIssueList({ page: 1, size: 200 })
        this.items = page?.records || page || []
      }
      this.loaded = true
    },

    async addIssue(issue) {
      if (USE_MOCK) {
        this.items.unshift({ ...issue, id: Date.now() })
        return
      }
      const data = await createIssue(issue)
      this.items.unshift(data)
    },

    async updateIssue(id, data) {
      if (USE_MOCK) {
        const idx = this.items.findIndex((i) => i.id === id)
        if (idx >= 0) this.items[idx] = { ...this.items[idx], ...data }
        return
      }
      const updated = await updateIssue(id, data)
      const idx = this.items.findIndex((i) => i.id === id)
      if (idx >= 0) this.items[idx] = updated
    },

    async toggleStatus(id) {
      const it = this.items.find((i) => i.id === id)
      if (!it) return
      if (USE_MOCK) {
        it.status = it.status === 'done' ? 'open' : 'done'
        return
      }
      const updated = await toggleIssueStatus(id)
      const idx = this.items.findIndex((i) => i.id === id)
      if (idx >= 0) this.items[idx] = updated
    },

    async removeIssue(id) {
      if (USE_MOCK) {
        this.items = this.items.filter((i) => i.id !== id)
        return
      }
      await deleteIssue(id)
      this.items = this.items.filter((i) => i.id !== id)
    }
  }
})
