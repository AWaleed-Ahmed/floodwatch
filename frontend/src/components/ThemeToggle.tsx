import { useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { setTheme, useTheme } from '../theme'

/**
 * Light/dark switch for the inner pages: a round button the same height and
 * colour as the Menu pill, fixed just to its left. The sun and moon cross-fade
 * with a quarter turn, and the button gives a small press bounce.
 */
export function ThemeToggle() {
  const { t } = useTranslation()
  const dark = useTheme() === 'dark'
  const buttonRef = useRef<HTMLButtonElement>(null)
  const label = t(dark ? 'menu.themeLight' : 'menu.themeDark')

  const toggle = () => {
    setTheme(dark ? 'light' : 'dark')
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return
    buttonRef.current?.animate([{ scale: '1' }, { scale: '0.9', offset: 0.3 }, { scale: '1.04', offset: 0.65 }, { scale: '1' }], {
      duration: 380,
      easing: 'ease-out',
    })
  }

  return (
    <button
      ref={buttonRef}
      type="button"
      onClick={toggle}
      aria-label={label}
      aria-pressed={dark}
      title={label}
      className="fixed top-6 right-[calc(5vw+7.25rem)] z-50 grid size-12 place-items-center rounded-full bg-ink-950 text-paper outline-none focus-visible:ring-2 focus-visible:ring-teal-500 focus-visible:ring-offset-2"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        aria-hidden="true"
        className={`col-start-1 row-start-1 size-5 transition-[opacity,rotate,scale] duration-500 ease-spring ${
          dark ? 'scale-100 rotate-0 opacity-100' : 'scale-50 -rotate-90 opacity-0'
        }`}
      >
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2.5v2M12 19.5v2M4.6 4.6 6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4 6 18M18 6l1.4-1.4" />
      </svg>
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
        aria-hidden="true"
        className={`col-start-1 row-start-1 size-5 transition-[opacity,rotate,scale] duration-500 ease-spring ${
          dark ? 'scale-50 rotate-90 opacity-0' : 'scale-100 rotate-0 opacity-100'
        }`}
      >
        <path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5Z" />
      </svg>
    </button>
  )
}
