import { useI18n } from 'vue-i18n'

/**
 * 把后端的错误变成当前语言的提示：
 * 后端返回一个错误码（BAD_EMAIL / TIMETABLE_PRIORITY / …），优先用本地的译文；
 * 本地没有这个码就退回后端给的原文（英文），再退到调用方给的兜底 key。
 * 用法：catch (e) { ElMessage.error(apiError(e, 'manage.failed')) }
 */
export function useApiError() {
  const { t, te } = useI18n()
  return (e, fallbackKey) => {
    const data = e && e.response && e.response.data
    const code = data && data.error
    if (code === 'UNAUTHORIZED') return t('errors.UNAUTHORIZED')
    if (code && te('errors.' + code)) return t('errors.' + code)
    if (data && data.message) return data.message
    return fallbackKey ? t(fallbackKey) : String((e && e.message) || e)
  }
}
