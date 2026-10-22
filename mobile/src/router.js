import { createRouter, createWebHashHistory } from 'vue-router'
import { token } from './api.js'
import Login from './views/Login.vue'
import Find from './views/Find.vue'
import Room from './views/Room.vue'
import Me from './views/Me.vue'

const routes = [
  { path: '/', redirect: '/find' },
  { path: '/login', component: Login, meta: { public: true } },
  { path: '/find', component: Find },
  { path: '/room/:code', component: Room },
  { path: '/me', component: Me },
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  if (!to.meta.public && !token()) return '/login'
  if (to.path === '/login' && token()) return '/find'
  return true
})

export default router
