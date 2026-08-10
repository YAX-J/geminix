import { defineStore } from 'pinia'

// 全局 UI 状态：搜索词、筛选、弹窗开关
export const useUiStore = defineStore('ui', {
  state: () => ({
    searchKw: '',
    reportFilter: 'all',
    issueFilter: 'all',
    reportModalOpen: false,
    reportModalDate: '',
    issueModalOpen: false,
    heatmapTarget: '' // 热力图点击的定位目标日期
  }),

  actions: {
    openReportModal(date = '') {
      this.reportModalDate = date
      this.reportModalOpen = true
    },
    closeReportModal() {
      this.reportModalOpen = false
    },
    openIssueModal() {
      this.issueModalOpen = true
    },
    closeIssueModal() {
      this.issueModalOpen = false
    }
  }
})
