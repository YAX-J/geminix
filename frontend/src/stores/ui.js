import { defineStore } from 'pinia'

// 全局 UI 状态：搜索词、筛选、弹窗开关
export const useUiStore = defineStore('ui', {
  state: () => ({
    searchKw: '',
    reportFilter: 'all',
    issueFilter: 'all',
    reportModalOpen: false,
    reportModalDate: '',
    reportModalEdit: null, // 编辑中的日报对象（null = 新增）
    issueModalOpen: false,
    tagModalOpen: false,
    heatmapTarget: '' // 热力图点击的定位目标日期
  }),

  actions: {
    openReportModal(date = '') {
      this.reportModalDate = date
      this.reportModalEdit = null
      this.reportModalOpen = true
    },
    openEditReport(report) {
      this.reportModalEdit = report
      this.reportModalDate = report.date
      this.reportModalOpen = true
    },
    closeReportModal() {
      this.reportModalOpen = false
      this.reportModalEdit = null
    },
    openIssueModal() {
      this.issueModalOpen = true
    },
    closeIssueModal() {
      this.issueModalOpen = false
    },
    openTagModal() {
      this.tagModalOpen = true
    },
    closeTagModal() {
      this.tagModalOpen = false
    }
  }
})
