<template>
  <div class="app">
    <TopBar />
    <div class="main">
      <SidebarHeatmap />
      <!-- 中栏：日报 / 问题 双主视图（路由驱动） -->
      <main class="center">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
      <OverviewPanel />
    </div>
  </div>
</template>

<script setup>
import TopBar from '@/components/TopBar.vue'
import SidebarHeatmap from '@/components/SidebarHeatmap.vue'
import OverviewPanel from '@/components/OverviewPanel.vue'
</script>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.main {
  flex: 1;
  display: grid;
  grid-template-columns: 264px 1fr 332px;
  overflow: hidden;
}

.center {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

@media (max-width: 1360px) {
  .main {
    grid-template-columns: 232px 1fr 300px;
  }
}
@media (max-width: 1180px) {
  .main {
    grid-template-columns: 1fr 320px;
  }
  :deep(.panel-left) {
    display: none;
  }
}
@media (max-width: 900px) {
  .main {
    grid-template-columns: 1fr;
  }
  :deep(.panel-right) {
    display: none;
  }
}
</style>
