<template>
  <div class="login-page">
    <div class="deco deco-1"></div>
    <div class="deco deco-2"></div>

    <div class="login-card">
      <div class="brand">
        <div class="brand-logo">记</div>
        <div class="brand-name">工作日志台</div>
        <p>记录每日工作 · 沉淀问题解决方案</p>
      </div>

      <div class="tab-switch">
        <button class="tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">登 录</button>
        <button class="tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">注 册</button>
      </div>

      <form class="form" @submit.prevent="onSubmit">
        <div class="field">
          <label>用户名</label>
          <input
            v-model.trim="form.username"
            type="text"
            placeholder="3-20 位字母 / 数字 / 下划线"
            autocomplete="username"
          />
        </div>
        <div class="field">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            :placeholder="mode === 'login' ? '请输入密码' : '6-32 位密码'"
            autocomplete="current-password"
          />
        </div>
        <div v-if="mode === 'register'" class="field">
          <label>昵称（可选）</label>
          <input v-model.trim="form.nickname" type="text" placeholder="展示用昵称" />
        </div>

        <button class="btn-submit" :disabled="loading">
          {{ loading ? '请稍候…' : mode === 'login' ? '登 录' : '注册并登录' }}
        </button>
      </form>

      <p v-if="mode === 'login'" class="hint">默认账号：admin / admin123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const mode = ref('login')
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

async function onSubmit() {
  if (!form.username || !form.password) {
    toast('请输入用户名和密码')
    return
  }
  if (mode.value === 'register' && form.password.length < 6) {
    toast('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login({ username: form.username, password: form.password })
      toast('欢迎回来 👋')
    } else {
      await auth.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname
      })
      toast('注册成功，已自动登录 ✔')
    }
    router.replace(route.query.redirect || '/')
  } catch (e) {
    toast(e.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2ff 0%, #f5f6fa 55%, #ecfdf5 100%);
  position: relative;
  overflow: hidden;
  padding: 24px;
}
.deco {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.45;
}
.deco-1 {
  width: 380px;
  height: 380px;
  background: #c7d2fe;
  top: -120px;
  right: -80px;
}
.deco-2 {
  width: 320px;
  height: 320px;
  background: #a7f3d0;
  bottom: -100px;
  left: -60px;
}

.login-card {
  position: relative;
  z-index: 2;
  width: 400px;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: 0 24px 60px rgba(30, 41, 59, 0.12);
  padding: 36px 34px 30px;
  animation: rise 0.4s ease both;
}
.brand {
  text-align: center;
  margin-bottom: 22px;
}
.brand-logo {
  width: 52px;
  height: 52px;
  margin: 0 auto 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 24px;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.35);
}
.brand-name {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}
.brand p {
  font-size: 12.5px;
  color: var(--text-2);
  margin-top: 6px;
}

.tab-switch {
  display: flex;
  gap: 4px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 20px;
}
.tab {
  flex: 1;
  padding: 9px 0;
  border-radius: 9px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-2);
  transition: all 0.15s;
}
.tab.active {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.field label {
  display: block;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-2);
  margin-bottom: 6px;
}
.field input {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 13px;
  font-size: 14px;
  background: var(--bg);
  transition: all 0.15s;
  color: var(--text);
}
.field input:focus {
  border-color: var(--primary);
  background: #fff;
  box-shadow: 0 0 0 3px var(--primary-bg);
}
.btn-submit {
  margin-top: 6px;
  padding: 11px 0;
  border-radius: 10px;
  background: var(--primary);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 4px;
  transition: all 0.15s;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3);
}
.btn-submit:hover:not(:disabled) {
  background: var(--primary-deep);
  transform: translateY(-1px);
}
.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.hint {
  text-align: center;
  font-size: 12px;
  color: var(--text-3);
  margin-top: 16px;
}
</style>
