import { defineStore } from 'pinia'
import { loginApi, registerApi, meApi } from '@/api/auth'

const TOKEN_KEY = 'worklog_token'
const USER_KEY = 'worklog_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: readUser()
  }),

  getters: {
    isLoggedIn: (s) => !!s.token,
    nickname: (s) => s.userInfo?.nickname || s.userInfo?.username || '用户'
  },

  actions: {
    async login(form) {
      const data = await loginApi(form)
      this.setAuth(data)
      return data.user
    },

    async register(form) {
      const data = await registerApi(form)
      this.setAuth(data)
      return data.user
    },

    /** 拉取当前用户信息（用于校验 token 有效性） */
    async fetchMe() {
      if (!this.token) return null
      try {
        this.userInfo = await meApi()
        localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
        return this.userInfo
      } catch (e) {
        this.logout()
        return null
      }
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },

    setAuth(data) {
      this.token = data.token
      this.userInfo = data.user
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    }
  }
})
