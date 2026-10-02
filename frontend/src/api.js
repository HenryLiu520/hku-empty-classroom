/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
import axios from 'axios'
import { useAuthStore } from './stores/auth'
import router from './router'

const api = axios.create({ baseURL: '/api', timeout: 15000 })

api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = 'Bearer ' + auth.token
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response && err.response.status
    const code = err.response && err.response.data && err.response.data.error
    // 401 = 没登录。403 且 error 是 UNAUTHORIZED = token 已失效（后端重启后旧 token 全废）。
    // 不能看到 403 就登出：学生越权也会 403（error 是 FORBIDDEN），那不是登录问题。
    if (status === 401 || (status === 403 && code === 'UNAUTHORIZED')) {
      const auth = useAuthStore()
      auth.clear()
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

export default api
