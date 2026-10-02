<template>
  <div class="login-wrap">
    <el-card class="login-card" shadow="always">
      <div class="lg"><span class="mark">EC</span></div>
      <h2>Empty Classroom Plan</h2>
      <p class="tagline">See which teaching rooms are free between classes</p>

      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="username" :placeholder="mode === 'login' ? 'HKU email or username' : 'Username'" size="large" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'">
          <el-input v-model="email" placeholder="HKU email, e.g. name@connect.hku.hk" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="Password" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="full" :loading="loading" @click="submit">
          {{ mode === 'login' ? 'Sign in' : 'Create account' }}
        </el-button>
      </el-form>

      <p v-if="mode === 'register'" class="hint">
        Sign-up is for HKU addresses only: the email has to end in hku.hk
        (@connect.hku.hk, @hku.hk, ...). This prototype sends no verification email.
      </p>

      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mt" />

      <div class="foot">
        <template v-if="mode === 'login'">
          Demo accounts &middot; user1 / user123 &nbsp;|&nbsp; admin1 / admin123<br />
          <a href="#" @click.prevent="switchToRegister">Create an account with your HKU email</a>
        </template>
        <template v-else>
          <a href="#" @click.prevent="switchToLogin">Back to sign in</a>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
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
    ElMessage.success((signUp ? 'Account created. Signed in as ' : 'Signed in as ') + data.displayName)
    router.push(data.role === 'admin' ? '/manage' : '/find')
  } catch (e) {
    error.value = e.response?.data?.message || (signUp ? 'Sign-up failed' : 'Sign in failed')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #EAF3FF 0%, #F5F7FA 55%, #EFFAF3 100%); }
.login-card { width: 360px; border-radius: 8px; }
.hint { font-size: 12px; color: #8A94A6; line-height: 1.5; margin: 4px 0 0; }
.lg { display: flex; justify-content: center; margin-bottom: 6px; }
.lg .mark { width: 24px; height: 24px; border-radius: 6px; background: #409EFF; color: #fff;
  display: inline-flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px; }
h2 { text-align: center; margin: 0 0 4px; font-size: 16px; }
.tagline { text-align: center; color: #8A94A6; font-size: 12px; margin: 0 0 18px; }
.full { width: 100%; }
.mt { margin-top: 12px; }
.foot { text-align: center; color: #A8ABB2; font-size: 11px; margin-top: 14px; }
</style>
