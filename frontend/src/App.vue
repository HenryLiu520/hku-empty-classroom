<template>
  <el-config-provider :locale="epLocale">
    <router-view v-if="$route.path === '/login'" />

    <el-container v-else class="shell">
      <el-aside width="210px" class="side">
        <div class="logo">
          <img class="crest" :src="crest" alt="The University of Hong Kong" />
          <b>{{ t('app.name') }}</b>
        </div>
        <el-menu :default-active="$route.path" router
                 background-color="#024638" text-color="rgba(255,255,255,.74)" active-text-color="#ffffff">
          <el-menu-item index="/find">{{ t('nav.find') }}</el-menu-item>
          <el-menu-item index="/rooms">{{ t('nav.rooms') }}</el-menu-item>
          <el-menu-item v-if="auth.isAdmin" index="/manage">{{ t('nav.manage') }}</el-menu-item>
          <el-menu-item v-if="auth.isSuper" index="/accounts">{{ t('nav.accounts') }}</el-menu-item>
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
            <el-tag :class="roleClass" effect="plain">{{ roleLabel }}</el-tag>
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
import crest from './assets/hku-shield-white.svg'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import zhTw from 'element-plus/es/locale/lang/zh-tw'

const { t, locale, te } = useI18n()
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const languages = SUPPORTED
const epLocales = { en, 'zh-CN': zhCn, 'zh-TW': zhTw }
const epLocale = computed(() => epLocales[locale.value] || en)
const langLabel = computed(() => (SUPPORTED.find(l => l.value === locale.value) || {}).label || 'English')

const title = computed(() => ({
  '/manage': t('nav.manage'), '/rooms': t('nav.rooms'), '/accounts': t('nav.accounts')
}[route.path] || t('nav.find')))

const roleLabel = computed(() => {
  const key = 'roles.' + auth.role
  return te(key) ? t(key) : auth.role
})
const roleClass = computed(() => ({ superadmin: 'ec-tag-gold', admin: 'ec-tag-green' }[auth.role] || 'ec-tag-grey'))

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

/* 侧栏：港大深绿，底部略深一点 */
.side {
  background: linear-gradient(180deg, #024638 0%, #013a2e 100%);
  border-right: 1px solid rgba(0, 0, 0, .10);
}
.logo {
  height: 64px; display: flex; align-items: center; gap: 10px; padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, .10);
}
.logo .crest { height: 26px; display: block; }
.logo b { color: #fff; font-size: 15px; font-weight: 600; letter-spacing: .02em; }

.side .el-menu { border-right: none; padding: 10px 8px; }
.side .el-menu-item {
  position: relative; height: 44px; line-height: 44px; font-size: 14px;
  border-radius: 8px; margin-bottom: 4px;
}
.side .el-menu-item:hover { background: rgba(255, 255, 255, .08) !important; }
.side .el-menu-item.is-active { background: rgba(255, 255, 255, .12) !important; font-weight: 600; }
/* 选中项左侧那一道金线 —— 港大的金 */
.side .el-menu-item.is-active::before {
  content: ''; position: absolute; left: 0; top: 12px; bottom: 12px; width: 3px;
  border-radius: 0 3px 3px 0; background: #b49764;
}

.nav {
  background: #fff; border-bottom: 1px solid #e6ebe9; display: flex; align-items: center;
  justify-content: space-between; box-shadow: 0 1px 2px rgba(16, 40, 34, .04);
}
.crumb { color: #7d8c88; font-size: 13px; letter-spacing: .01em; }
.crumb b { color: #024638; font-weight: 600; }
.right { display: flex; align-items: center; gap: 12px; }
.who { font-size: 13px; color: #5c6b67; }
.content { background: #f4f7f5; }
</style>
