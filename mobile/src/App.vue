<template>
  <div class="app">
    <header class="topbar">
      <button v-if="showBack" class="back" @click="goBack">‹</button>
      <h1>{{ title }}</h1>
      <span class="spacer" />
      <select :value="lang" @change="setLang($event.target.value)" aria-label="Language">
        <option v-for="(name, code) in LANG_NAMES" :key="code" :value="code">{{ name }}</option>
      </select>
    </header>

    <router-view />

    <nav v-if="!isLogin" class="tabbar">
      <router-link to="/find" :class="{ active: isFind }">
        <span class="ico">🔍</span>{{ t('nav.find') }}
      </router-link>
      <router-link to="/me" :class="{ active: isMe }">
        <span class="ico">👤</span>{{ t('nav.me') }}
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { t, lang, setLang, LANG_NAMES } from './i18n.js'

const route = useRoute()
const router = useRouter()

const isLogin = computed(() => route.path === '/login')
const isMe = computed(() => route.path === '/me')
const isFind = computed(() => route.path === '/find' || route.path === '/')
const isRoom = computed(() => route.path.startsWith('/room/'))

const showBack = computed(() => isRoom.value)

const title = computed(() => {
  if (isLogin.value) return t('app.title')
  if (isRoom.value) return route.params.code
  if (isMe.value) return t('me.account')
  return t('app.title')
})

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/find')
}
</script>
