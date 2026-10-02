<template>
  <div class="login-wrap">
    <el-card class="login-card" shadow="always">
      <div class="lg"><span class="mark">EC</span></div>
      <h2>Empty Classroom Plan</h2>
      <p class="tagline">See which teaching rooms are free between classes</p>

      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="username" placeholder="HKU email or username" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="Password" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="full" :loading="loading" @click="submit">Sign in</el-button>
      </el-form>

      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mt" />

      <div class="foot">
        Demo accounts &middot; user1 / user123 &nbsp;|&nbsp; admin1 / admin123
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
const username = ref('user1')
const password = ref('user123')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const data = await auth.login(username.value, password.value)
    ElMessage.success('Signed in as ' + data.displayName)
    router.push(data.role === 'admin' ? '/manage' : '/find')
  } catch (e) {
    error.value = e.response?.data?.message || 'Sign in failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #EAF3FF 0%, #F5F7FA 55%, #EFFAF3 100%); }
.login-card { width: 360px; border-radius: 8px; }
.lg { display: flex; justify-content: center; margin-bottom: 6px; }
.lg .mark { width: 24px; height: 24px; border-radius: 6px; background: #409EFF; color: #fff;
  display: inline-flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px; }
h2 { text-align: center; margin: 0 0 4px; font-size: 16px; }
.tagline { text-align: center; color: #8A94A6; font-size: 12px; margin: 0 0 18px; }
.full { width: 100%; }
.mt { margin-top: 12px; }
.foot { text-align: center; color: #A8ABB2; font-size: 11px; margin-top: 14px; }
</style>
