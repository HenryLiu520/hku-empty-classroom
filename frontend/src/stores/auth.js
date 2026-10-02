import { defineStore } from 'pinia'
import api from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('ec_token') || '',
    username: localStorage.getItem('ec_user') || '',
    displayName: localStorage.getItem('ec_name') || '',
    role: localStorage.getItem('ec_role') || ''
  }),
  getters: {
    signedIn: (s) => !!s.token,
    isAdmin: (s) => s.role === 'admin'
  },
  actions: {
    async login(username, password) {
      const { data } = await api.post('/auth/login', { username, password })
      this.token = data.token
      this.username = data.username
      this.displayName = data.displayName
      this.role = data.role
      localStorage.setItem('ec_token', this.token)
      localStorage.setItem('ec_user', this.username)
      localStorage.setItem('ec_name', this.displayName)
      localStorage.setItem('ec_role', this.role)
      return data
    },
    clear() {
      this.token = ''
      this.username = ''
      this.displayName = ''
      this.role = ''
      localStorage.removeItem('ec_token')
      localStorage.removeItem('ec_user')
      localStorage.removeItem('ec_name')
      localStorage.removeItem('ec_role')
    }
  }
})
