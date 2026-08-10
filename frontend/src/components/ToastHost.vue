<template>
  <div class="toast-host">
    <div v-for="t in toasts" :key="t.id" class="toast">
      <span class="ico">✓</span>{{ t.msg }}
    </div>
  </div>
</template>

<script setup>
import { toasts } from '@/utils/toast'
</script>

<style scoped>
.toast-host {
  position: fixed;
  bottom: 26px;
  right: 26px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 300;
}
.toast {
  background: #1e293b;
  color: #fff;
  font-size: 13px;
  padding: 11px 18px;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.35);
  display: flex;
  align-items: center;
  gap: 8px;
  animation: rise 0.3s ease both;
}
.toast .ico {
  color: #34d399;
  font-weight: 800;
}
</style>
