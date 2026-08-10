import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

// 日报与问题是两个平级的独立视图，通过路由区分
const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', name: 'reports', component: () => import('@/components/ReportTimeline.vue') },
      { path: 'issues', name: 'issues', component: () => import('@/components/IssueList.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
