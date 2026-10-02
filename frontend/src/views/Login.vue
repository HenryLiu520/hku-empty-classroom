<!--
  Empty Classroom Plan — HKU COMP1110 Group 08
  Author: LIU Haoran (u3686264) · 2026
  Signature: EC-COMP1110-G08-u3686264-2026
-->
<template>
  <div class="login-wrap">
    <div class="login-card">
      <!-- 左边：港大品牌板 -->
      <aside class="brand">
        <img class="lockup" :src="lockup" alt="The University of Hong Kong" />
        <h1>{{ t('app.name') }}</h1>
        <div class="rule"></div>
        <p class="tagline">{{ t('login.tagline') }}</p>
        <div class="langs">
          <a v-for="l in languages" :key="l.value" href="#"
             :class="{ on: l.value === locale }" @click.prevent="setLocale(l.value)">{{ l.label }}</a>
        </div>
        <!-- 署名：作者、学号、组别 -->
        <p class="byline">LIU Haoran &middot; u3686264 &middot; COMP1110 Group 08</p>
      </aside>

      <!-- 右边：表单 -->
      <section class="form">
        <h2>{{ mode === 'login' ? t('login.signIn') : t('login.createAccount') }}</h2>

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
        </div>
      </section>
    </div>
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
import lockup from '../assets/hku-lockup-white.svg'

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
.login-wrap {
  min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px;
  /* 港大主楼照片当背景。登录页要看得清，所以只压一层很薄的浅色纱，卡片本身是不透明的。 */
  background:
    linear-gradient(180deg, rgba(255, 255, 255, .20) 0%, rgba(244, 247, 245, .42) 100%),
    url('../assets/hku-hero.jpg') center / cover no-repeat fixed;
}
.login-card {
  display: flex; width: 760px; max-width: 100%; background: #fff; border-radius: 16px;
  overflow: hidden; box-shadow: 0 18px 50px rgba(2, 70, 56, .13), 0 2px 6px rgba(2, 70, 56, .06);
}

/* 品牌板：港大深绿 + 白色锁定版校徽 */
.brand {
  width: 320px; flex: none; padding: 38px 30px; color: #fff; display: flex; flex-direction: column;
  background: linear-gradient(160deg, #024638 0%, #013a2e 100%);
}
.brand .lockup { width: 196px; display: block; }
.brand h1 { margin: 22px 0 0; font-size: 19px; font-weight: 600; letter-spacing: .02em; }
.brand .rule { width: 34px; height: 2px; background: #b49764; margin: 16px 0 14px; }
.brand .tagline { margin: 0; font-size: 12.5px; line-height: 1.75; color: rgba(255, 255, 255, .74); }
.brand .langs { margin-top: auto; padding-top: 22px; display: flex; gap: 14px; font-size: 12px; }
.brand .langs a { color: rgba(255, 255, 255, .58); text-decoration: none; }
.brand .langs a.on { color: #fff; font-weight: 600; border-bottom: 1px solid #b49764; }
.byline { margin: 12px 0 0; font-size: 10.5px; color: rgba(255, 255, 255, .46); letter-spacing: .01em; }

/* 表单区 */
.form { flex: 1; padding: 38px 34px; }
.form h2 { margin: 0 0 20px; font-size: 17px; font-weight: 600; color: #1f2d2a; }
.full { width: 100%; }
.mt { margin-top: 12px; }
.hint { font-size: 12px; color: #8a9691; line-height: 1.6; margin: 10px 0 0; }
.foot {
  margin-top: 20px; padding-top: 14px; border-top: 1px solid #eef2f0;
  color: #8a9691; font-size: 11.5px; line-height: 1.9;
}
.foot a { color: #024638; text-decoration: none; font-weight: 600; }

@media (max-width: 720px) {
  .login-card { flex-direction: column; }
  .brand { width: auto; }
  .brand .lockup { width: 168px; }
}
</style>
