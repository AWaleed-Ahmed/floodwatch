import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from './locales/en.json'
import ur from './locales/ur.json'

const KEY = 'floodwatch.lang'
let saved = 'en'
try {
  saved = localStorage.getItem(KEY) ?? 'en'
} catch {
  /* ignore */
}

i18n.use(initReactI18next).init({
  resources: { en: { translation: en }, ur: { translation: ur } },
  lng: saved,
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
})

// Urdu is written right-to-left: flip the whole page layout.
const applyDirection = (lng: string) => {
  document.documentElement.lang = lng
  document.documentElement.dir = lng === 'ur' ? 'rtl' : 'ltr'
  try {
    localStorage.setItem(KEY, lng)
  } catch {
    /* ignore */
  }
}
applyDirection(i18n.language)
i18n.on('languageChanged', applyDirection)

export default i18n
