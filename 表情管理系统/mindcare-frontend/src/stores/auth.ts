import { defineStore } from 'pinia'
import type { User, AuthState } from '@/types'
import axios from 'axios'

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    currentUser: null,
    sessionId: null,
    isAdmin: false
  }),

  getters: {
    isAuthenticated: (state: AuthState): boolean => !!state.sessionId && !!state.currentUser,
  },

  actions: {
    async setUser(user: User, sessionId: string) {
      // 先清除其他用户的登录状态
      localStorage.clear()
      
      this.currentUser = { ...user }
      this.sessionId = sessionId
      this.isAdmin = false
      
      // 存储当前用户的状态
      this.persistState()
      
      // 设置 axios 默认头部
      this.setupAxiosHeaders()
    },

    async setAdmin(admin: User, sessionId: string) {
      // 先清除其他用户的登录状态
      localStorage.clear()
      
      this.currentUser = { ...admin }
      this.sessionId = sessionId
      this.isAdmin = true
      
      // 存储当前用户的状态
      this.persistState()
      
      // 设置 axios 默认头部
      this.setupAxiosHeaders()
    },

    updateUserAvatar(newAvatarUrl: string) {
      if (this.currentUser) {
        this.currentUser = {
          ...this.currentUser,
          avatar: newAvatarUrl
        }
        this.persistState()
      }
    },

    logout() {
      this.currentUser = null
      this.sessionId = null
      this.isAdmin = false
      localStorage.clear()
      
      // 清除 axios 头部
      delete axios.defaults.headers.common['X-Session-ID']
      delete axios.defaults.headers.common['X-User-Id']
    },

    persistState() {
      const stateKey = `auth_${this.currentUser?.id}`
      localStorage.setItem(stateKey, JSON.stringify({
        currentUser: this.currentUser,
        sessionId: this.sessionId,
        isAdmin: this.isAdmin
      }))
    },

    restoreState() {
      if (this.currentUser?.id) {
        const stateKey = `auth_${this.currentUser.id}`
        const stored = localStorage.getItem(stateKey)
        if (stored) {
          try {
            const state = JSON.parse(stored)
            this.currentUser = state.currentUser
            this.sessionId = state.sessionId
            this.isAdmin = state.isAdmin
            this.setupAxiosHeaders()
          } catch (e) {
            console.error('Failed to restore auth state:', e)
            this.logout()
          }
        }
      }
    },

    setupAxiosHeaders() {
      if (this.sessionId && this.currentUser?.id) {
        axios.defaults.headers.common['X-Session-ID'] = this.sessionId
        axios.defaults.headers.common['X-User-Id'] = this.currentUser.id.toString()
      }
    }
  }
}) 