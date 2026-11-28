import { useSyncExternalStore } from 'react'

export type Theme = 'light' | 'dark'

const KEY = 'floodwatch.theme'

const read = (): Theme => {
  try {
    return localStorage.getItem(KEY) === 'dark' ? 'dark' : 'light'
  } catch {
    return 'light'
  }
}

let current: Theme = read()
const listeners = new Set<() => void>()

/*
 * The choice lives on <html data-theme>. Only elements marked .themed (the inner
 * pages, the Menu pill there and the page transitions) use the dark palette; the
 * landing page always keeps its own light look.
 */
document.documentElement.dataset.theme = current

/** Switch theme, letting colours glide for a moment instead of snapping (only during the switch). */
export function setTheme(theme: Theme) {
  if (theme === current) return
  const root = document.documentElement
  const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  if (!reduce) root.classList.add('theme-switching')
  current = theme
  root.dataset.theme = theme
  try {
    localStorage.setItem(KEY, theme)
  } catch {
    /* private mode: just don't remember */
  }
  listeners.forEach((listener) => listener())
  if (!reduce) window.setTimeout(() => root.classList.remove('theme-switching'), 420)
}

const subscribe = (listener: () => void) => {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/** Current theme, re-rendering when it changes. */
export function useTheme(): Theme {
  return useSyncExternalStore(subscribe, () => current)
}
