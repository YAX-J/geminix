import { defineStore } from 'pinia'
import { demoReports } from '@/mock/demoData'
import { getReportList, createReport, updateReport, deleteReport } from '@/api/reports'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export const useReportStore = defineStore('reports', {
  state: () => ({
    items: [],
    loaded: false
  }),

  getters: {
    total: (s) => s.items.length,
    monthCount: (s) => {
      const now = new Date()
      const ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      return s.items.filter((r) => r.date.startsWith(ym)).length
    },
    byDate: (s) => (date) => s.items.find((r) => r.date === date)
  },

  actions: {
    async load() {
      if (this.loaded) return
      if (USE_MOCK) {
        this.items = [...demoReports]
      } else {
        const page = await getReportList({ page: 1, size: 200 })
        // 后端字段 reportDate/timeRange -> 前端 date/time 模型
        this.items = (page?.records || page || []).map((r) => ({ ...r, date: r.reportDate, time: r.timeRange }))
      }
      this.loaded = true
    },

    async addReport(report) {
      if (USE_MOCK) {
        const idx = this.items.findIndex((r) => r.date === report.date)
        if (idx >= 0) this.items.splice(idx, 1)
        this.items.unshift({ ...report, id: Date.now() })
        return
      }
      const data = await createReport({
        date: report.date,
        timeRange: report.time,
        title: report.title,
        tasks: report.tasks,
        tags: report.tags
      })
      this.items.unshift({ ...report, id: data.id })
    },

    async removeReport(id) {
      if (USE_MOCK) {
        this.items = this.items.filter((r) => r.id !== id)
        return
      }
      await deleteReport(id)
      this.items = this.items.filter((r) => r.id !== id)
    },

    async updateReport(id, report) {
      if (USE_MOCK) {
        const idx = this.items.findIndex((r) => r.id === id)
        if (idx >= 0) this.items[idx] = { ...this.items[idx], ...report }
        return
      }
      const data = await updateReport(id, {
        date: report.date,
        timeRange: report.time,
        title: report.title,
        tasks: report.tasks,
        tags: report.tags
      })
      const idx = this.items.findIndex((r) => r.id === id)
      if (idx >= 0) this.items[idx] = { ...this.items[idx], ...data, date: data.reportDate }
    }
  }
})
