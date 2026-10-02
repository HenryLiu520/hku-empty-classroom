<template>
  <el-config-provider :locale="epLocale">
    <router-view v-if="$route.path === '/login'" />

    <el-container v-else class="shell">
      <el-aside width="210px" class="side">
        <div class="logo"><span class="mark">EC</span><b>{{ t('app.name') }}</b></div>
        <el-menu :default-active="$route.path" router
                 background-color="#304156" text-color="#BFCBD9" active-text-color="#409EFF">
          <el-menu-item index="/find">{{ t('nav.find') }}</el-menu-item>
          <el-menu-item index="/rooms">{{ t('nav.rooms') }}</el-menu-item>
          <el-menu-item v-if="auth.isAdmin" index="/manage">{{ t('nav.manage') }}</el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="nav">
          <div class="crumb">{{ t('nav.home') }} / <b>{{ title }}</b></div>
          <div class="right">
            <el-dropdown @command="changeLang">
              <el-button link type="primary">{{ langLabel }} ▾</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="l in languages" :key="l.value" :command="l.value"
                                    :disabled="l.value === locale">{{ l.label }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-tag :type="auth.isAdmin ? 'warning' : 'primary'" effect="light">{{ auth.role }}</el-tag>
            <span class="who">{{ auth.displayName }}</span>
            <el-button link type="primary" @click="signOut">{{ t('nav.signOut') }}</el-button>
          </div>
        </el-header>
        <el-main class="content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { SUPPORTED, setLocale } from './i18n'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import zhTw from 'element-plus/es/locale/lang/zh-tw'

const { t, locale } = useI18n()
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const languages = SUPPORTED
const epLocales = { en, 'zh-CN': zhCn, 'zh-TW': zhTw }
const epLocale = computed(() => epLocales[locale.value] || en)
const langLabel = computed(() => (SUPPORTED.find(l => l.value === locale.value) || {}).label || 'English')

const title = computed(() => ({
  '/manage': t('nav.manage'), '/rooms': t('nav.rooms')
}[route.path] || t('nav.find')))

function changeLang(value) {
  setLocale(value)
}

function signOut() {
  auth.clear()
  router.push('/login')
}
</script>

<style>
html, body, #app { height: 100%; margin: 0; }
body { font-family: -apple-system, "Helvetica Neue", Helvetica, "PingFang HK", "Microsoft YaHei", Arial, sans-serif; }
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
