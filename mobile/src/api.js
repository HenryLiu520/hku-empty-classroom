import axios from 'axios'

// 同一个后端：所有请求都走 /api，由 Vite 代理到 8080
export const api = axios.create({ baseURL: '/api', timeout: 10000 })

export function token() {
  return localStorage.getItem('ec.token') || ''
}

export function setToken(t) {
  if (t) localStorage.setItem('ec.token', t)
  else localStorage.removeItem('ec.token')
}

api.interceptors.request.use((cfg) => {
  const t = token()
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

// 401 一律回登录页（token 过期或未登录）
api.interceptors.response.use(
  (r) => r,
  (e) => {
    if (e.response && e.response.status === 401) {
      setToken('')
      localStorage.removeItem('ec.user')
      if (location.hash !== '#/login') location.hash = '#/login'
    }
    return Promise.reject(e)
  }
)

export function errorText(e) {
  return e?.response?.data?.message || e?.response?.data?.error || e?.message || 'Request failed'
}
