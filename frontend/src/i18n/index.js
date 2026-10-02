import { createI18n } from 'vue-i18n'
import messages from './messages'

export const SUPPORTED = [
  { value: 'en', label: 'English' },
  { value: 'zh-CN', label: '简体中文' },
  { value: 'zh-TW', label: '繁體中文' }
]

const STORAGE_KEY = 'ec_lang'

function initialLocale() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved && SUPPORTED.some(l => l.value === saved)) return saved
  const nav = (navigator.language || '').toLowerCase()
  if (nav.startsWith('zh')) {
    return /tw|hk|mo|hant/.test(nav) ? 'zh-TW' : 'zh-CN'
  }
  return 'en'
}

const i18n = createI18n({
  legacy: false,          // 用组合式 API：组件里 t('...')
  globalInjection: true,  // 模板里也能直接 $t('...')
  locale: initialLocale(),
  fallbackLocale: 'en',
  messages
})

export function setLocale(value) {
  if (!SUPPORTED.some(l => l.value === value)) return
  i18n.global.locale.value = value
  localStorage.setItem(STORAGE_KEY, value)
  document.documentElement.setAttribute('lang', value)
}

export default i18n
