<template>
  <div class="login-wrap">
    <el-card class="login-card" shadow="always">
      <div class="lg"><span class="mark">EC</span></div>
      <h2>{{ t('app.name') }}</h2>
      <p class="tagline">{{ t('login.tagline') }}</p>

      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="username" size="large"
                    :placeholder="mode === 'login' ? t('login.usernameOrEmail') : t('login.username')" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'">
          <el-input v-model="email" :placeholder="t('login.email')" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" :placeholder="t('login.password')"
                    size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="full" :loading="loading" @click="submit">
          {{ mode === 'login' ? t('login.signIn') : t('login.createAccount') }}
        </el-button>
      </el-form>

      <p v-if="mode === 'register'" class="hint">{{ t('login.hint') }}</p>

      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mt" />

      <div class="foot">
        <template v-if="mode === 'login'">
          {{ t('login.demo') }} &middot; user1 / user123 &nbsp;|&nbsp; admin1 / admin123
          &nbsp;|&nbsp; super1 / super123<br />
          <a href="#" @click.prevent="switchToRegister">{{ t('login.createLink') }}</a>
        </template>
        <template v-else>
          <a href="#" @click.prevent="switchToLogin">{{ t('login.backLink') }}</a>
        </template>
        <div class="langs">
          <a v-for="l in languages" :key="l.value" href="#"
             :class="{ on: l.value === locale }" @click.prevent="setLocale(l.value)">{{ l.label }}</a>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { SUPPORTED, setLocale } from '../i18n'
import { useApiError } from '../i18n/apiError'

const { t, locale } = useI18n()
const apiError = useApiError()
const auth = useAuthStore()
const router = useRouter()

const languages = SUPPORTED
const mode = ref('login')
const username = ref('user1')
const email = ref('')
const password = ref('user123')
const loading = ref(false)
const error = ref('')

function switchToRegister() {
  mode.value = 'register'
  username.value = ''
  email.value = ''
  password.value = ''
  error.value = ''
}

function switchToLogin() {
  mode.value = 'login'
  username.value = 'user1'
  password.value = 'user123'
  error.value = ''
}

async function submit() {
  error.value = ''
  loading.value = true
  const signUp = mode.value === 'register'
  try {
    const data = signUp
      ? await auth.register(username.value, email.value, password.value)
      : await auth.login(username.value, password.value)
    ElMessage.success(t(signUp ? 'login.created' : 'login.signedIn', { name: data.displayName }))
    router.push(['admin', 'superadmin'].includes(data.role) ? '/manage' : '/find')
  } catch (e) {
    error.value = apiError(e, signUp ? 'login.errSignUp' : 'login.errSignIn')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #EAF3FF 0%, #F5F7FA 55%, #EFFAF3 100%); }
.login-card { width: 380px; border-radius: 8px; }
.hint { font-size: 12px; color: #8A94A6; line-height: 1.5; margin: 4px 0 0; }
.lg { display: flex; justify-content: center; margin-bottom: 6px; }
.lg .mark { width: 24px; height: 24px; border-radius: 6px; background: #409EFF; color: #fff;
  display: inline-flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px; }
footer, h2 { text-align: center; }
h2 { margin: 6px 0 2px; font-size: 18px; color: #303133; }
.tagline { text-align: center; color: #8A94A6; font-size: 12px; margin: 0 0 16px; }
.full { width: 100%; }
.mt { margin-top: 12px; }
.foot { margin-top: 14px; color: #8A94A6; font-size: 12px; text-align: center; line-height: 1.8; }
.foot a { color: #409EFF; text-decoration: none; }
.langs { margin-top: 8px; display: flex; justify-content: center; gap: 10px; }
.langs a.on { color: #303133; font-weight: 600; }
</style>
