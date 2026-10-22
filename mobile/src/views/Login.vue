<template>
  <div class="login-wrap">
    <div class="brand">
      <div class="shield">🏛️</div>
      <h1>{{ t('app.title') }}</h1>
      <p>{{ t('login.sub') }}</p>
    </div>

    <div class="card">
      <label class="field">
        <span>{{ t('login.user') }}</span>
        <input v-model.trim="username" type="text" autocomplete="username" autocapitalize="none" />
      </label>
      <label class="field">
        <span>{{ t('login.pass') }}</span>
        <input v-model="password" type="password" autocomplete="current-password" />
      </label>
      <button class="btn" :disabled="busy" @click="submit">
        {{ busy ? t('common.loading') : t('login.btn') }}
      </button>
      <p v-if="error" class="err">{{ error }}</p>
      <p class="hint" style="margin-top: 12px">{{ t('login.demo') }}</p>
      <p class="hint">{{ t('login.hint') }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, setToken, errorText } from '../api.js'
import { t } from '../i18n.js'

const router = useRouter()
const username = ref('')
const password = ref('')
const busy = ref(false)
const error = ref('')

async function submit() {
  if (!username.value || !password.value) return
  busy.value = true
  error.value = ''
  try {
    const { data } = await api.post('/auth/login', { username: username.value, password: password.value })
    setToken(data.token)
    localStorage.setItem(
      'ec.user',
      JSON.stringify({ username: data.username, displayName: data.displayName, role: data.role })
    )
    router.replace('/find')
  } catch (e) {
    error.value = e?.response?.status === 401 ? 'Wrong username or password.' : errorText(e)
  } finally {
    busy.value = false
  }
}
</script>
