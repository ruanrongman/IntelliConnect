import { i18n } from '@/i18n'
import { unref, computed } from 'vue'
import app from '@/store/app'
export function useLocale() {
  const curLocale = computed(() => app.state.lang)
  const globalI18n = i18n.global
  async function changeLang(locale) {
    // const curLocale = unref(globalI18n.locale)
    let messages = (await import(`../i18n/lang/${locale}.js`)).default
    globalI18n.setLocaleMessage(locale, messages)
  }
  function getAntdLocale() {
    // console.log()
    // console.log(curLocale)
    return globalI18n.messages[unref(curLocale)].antd
  }

  // const getAnt
  return { changeLang, curLocale, getAntdLocale }
}
