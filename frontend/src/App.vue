<template>
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else class="shell">
    <el-aside width="210px" class="side">
      <div class="logo"><span class="mark">EC</span><b>Empty Classroom</b></div>
      <el-menu :default-active="$route.path" router
               background-color="#304156" text-color="#BFCBD9" active-text-color="#409EFF">
        <el-menu-item index="/find">Find a room</el-menu-item>
        <el-menu-item index="/rooms">All rooms</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/manage">Change room use time</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="nav">
        <div class="crumb">Home / <b>{{ title }}</b></div>
        <div class="right">
          <el-tag :type="auth.isAdmin ? 'warning' : 'primary'" effect="light">{{ auth.role }}</el-tag>
          <span class="who">{{ auth.displayName }}</span>
          <el-button link type="primary" @click="signOut">Sign out</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const title = computed(() => ({ '/manage': 'Change room use time', '/rooms': 'All rooms' }[route.path] || 'Find a room'))

function signOut() {
  auth.clear()
  router.push('/login')
}
</script>

<style>
html, body, #app { height: 100%; margin: 0; }
body { font-family: -apple-system, "Helvetica Neue", Helvetica, "PingFang HK", Arial, sans-serif; }
.shell { height: 100vh; }
.side { background: #304156; }
.logo { height: 52px; display: flex; align-items: center; gap: 8px; padding: 0 16px; color: #fff; }
.logo .mark { width: 20px; height: 20px; border-radius: 4px; background: #409EFF; display: inline-flex;
               align-items: center; justify-content: center; font-size: 11px; font-weight: 700; }
.nav { background: #fff; border-bottom: 1px solid #EBEEF5; display: flex; align-items: center;
       justify-content: space-between; }
.crumb { color: #8A94A6; font-size: 13px; }
.crumb b { color: #303133; font-weight: 500; }
.right { display: flex; align-items: center; gap: 12px; }
.who { font-size: 13px; color: #606266; }
.content { background: #F5F7FA; }
</style>
