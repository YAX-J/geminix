<template>
  <router-view />
  <!-- 全局弹窗与 Toast -->
  <ReportModal />
  <IssueModal />
  <ToastHost />
</template>

<script setup>
import ReportModal from '@/components/ReportModal.vue'
import IssueModal from '@/components/IssueModal.vue'
import ToastHost from '@/components/ToastHost.vue'
</script>
