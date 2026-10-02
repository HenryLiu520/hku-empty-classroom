import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'

const routes = [
  { path: '/', redirect: '/find' },
  { path: '/login', component: () => import('./views/Login.vue') },
  { path: '/find', component: () => import('./views/FindRoom.vue') },
  { path: '/rooms', component: () => import('./views/AllRooms.vue') },
  { path: '/manage', component: () => import('./views/AdminUpdates.vue'), meta: { admin: true } }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.signedIn) return '/login'
  if (to.meta.admin && !auth.isAdmin) return '/find'
  return true
})

export default router
