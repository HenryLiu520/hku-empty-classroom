<template>
  <div class="page">
    <div class="card">
      <h2>{{ t('me.account') }}</h2>
      <dl class="kv">
        <dt>Name</dt>
        <dd>{{ user.displayName || user.username }}</dd>
        <dt>Username</dt>
        <dd>{{ user.username }}</dd>
        <dt>{{ t('me.role') }}</dt>
        <dd><span class="chip">{{ user.role }}</span></dd>
      </dl>
    </div>

    <div class="card">
      <h2>{{ t('me.lang') }}</h2>
      <div class="row">
        <button
          v-for="(name, code) in LANG_NAMES"
          :key="code"
          class="btn"
          :class="{ ghost: lang !== code }"
          @click="setLang(code)"
        >
          {{ name }}
        </button>
      </div>
    </div>

    <button class="btn ghost" @click="signOut">{{ t('me.signout') }}</button>
    <p class="hint" style="text-align: center; margin-top: 16px">{{ t('me.about') }}</p>
    <p class="hint" style="text-align: center">EC-COMP1110-G08-u3686264-2026</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { setToken } from '../api.js'
import { t, lang, setLang, LANG_NAMES } from '../i18n.js'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('ec.user') || '{}'))

function signOut() {
  setToken('')
  localStorage.removeItem('ec.user')
  router.replace('/login')
}
</script>
