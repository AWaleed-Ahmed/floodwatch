import { useEffect, useId, useRef, useState, type TransitionEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { useSwoosh } from '../useSwoosh.ts'

/** `key` names both the label (nav.<key>) and the hint (menu.hints.<key>) in the locale files. */
interface NavLink {
  key: string
  href: string
}

const MAIN: NavLink[] = [
  { key: 'dashboard', href: '/dashboard' },
  { key: 'risk', href: '/risk' },
  { key: 'shelters', href: '/shelters' },
  { key: 'safety', href: '/safety' },
]

const STAFF: NavLink[] = [
  { key: 'alerts', href: '/alerts' },
  { key: 'incidents', href: '/incidents' },
  { key: 'relief', href: '/relief' },
]

/** swoosh1.mp3 is near-silent for its first ~0.15 s; start there so the sound lands with the click. */
const SWOOSH_START = 0.15

/** Open panel size; the pill grows to exactly this. */
const PANEL_SIZE = 'w-[min(30rem,90vw)] h-[min(36rem,calc(100svh-3rem))]'

/** Delay for the nth item so links rise in one after another once the shape has opened. */
const stagger = (index: number, open: boolean) => ({ transitionDelay: open ? `${300 + index * 45}ms` : '0ms' })

const reveal = (open: boolean) =>
  `transition-[opacity,translate] duration-500 ease-out motion-reduce:transition-none ${
    open ? 'translate-y-0 opacity-100' : 'translate-y-3 opacity-0'
  }`

/**
 * One black shape, fixed to the top right. Closed, it is the "Menu" pill; opened,
 * that same pill grows (width, height and corner radius on a spring curve) into
 * the rounded nav panel. Its minimum size is the pill, so the closing spring can
 * never shrink it smaller than the pill.
 */
export default function Menu() {
  const { t, i18n } = useTranslation()
  const [open, setOpen] = useState(false)
  const panelId = useId()
  const close = () => setOpen(false)
  const urdu = i18n.language === 'ur'
  const firstLinkRef = useRef<HTMLAnchorElement>(null)
  const playSwoosh = useSwoosh('/Sound/swoosh1.mp3', { volume: 0.015, start: SWOOSH_START })

  /** Opens or closes the card; the swoosh plays on opening only. */
  const toggle = () => {
    if (!open) playSwoosh()
    setOpen(!open)
  }

  /** Once the shape has shrunk back into the pill, give it one small settling bounce. */
  const landBounce = (event: TransitionEvent<HTMLDivElement>) => {
    if (open || event.target !== event.currentTarget || event.propertyName !== 'width') return
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return
    event.currentTarget.animate(
      [{ scale: '1' }, { scale: '1.05', offset: 0.35 }, { scale: '0.99', offset: 0.7 }, { scale: '1' }],
      { duration: 380, easing: 'ease-out' }
    )
  }

  useEffect(() => {
    if (!open) return
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    window.addEventListener('keydown', onKey)
    const focusTimer = window.setTimeout(() => firstLinkRef.current?.focus({ preventScroll: true }), 350)
    return () => {
      window.removeEventListener('keydown', onKey)
      window.clearTimeout(focusTimer)
    }
  }, [open])

  return (
    <>
      <div
        aria-hidden="true"
        data-topography-ignore
        onClick={() => setOpen(false)}
        className={`fixed inset-0 z-40 ${open ? '' : 'pointer-events-none'}`}
      />

      <div data-topography-ignore className="fixed top-6 end-[5vw] z-50">
        <div
          className={`absolute top-0 end-0 min-h-12 min-w-[6.5rem] overflow-hidden bg-ink-950 text-white transition-[width,height,border-radius] motion-reduce:transition-none ${
            open
              ? `${PANEL_SIZE} rounded-[2rem] duration-[800ms] ease-spring`
              : 'h-12 w-[6.5rem] rounded-[1.5rem] duration-[500ms] ease-[cubic-bezier(0.6,0,0.25,1)]'
          }`}
          onTransitionEnd={landBounce}
        >
          <nav
            id={panelId}
            aria-label="Main"
            inert={!open}
            className={`absolute top-0 end-0 flex ${PANEL_SIZE} flex-col px-8 pt-20 pb-7`}
          >
            <ul className="flex flex-col gap-1">
              {MAIN.map((link, index) => (
                <li key={link.href} className={reveal(open)} style={stagger(index, open)}>
                  <Link
                    ref={index === 0 ? firstLinkRef : undefined}
                    to={link.href}
                    onClick={close}
                    className="group flex items-baseline justify-between gap-4 rounded-xl py-1 outline-none focus-visible:ring-2 focus-visible:ring-teal-500"
                  >
                    <span className="font-display text-[clamp(2rem,4.2vw,2.75rem)] leading-tight font-medium tracking-[-0.01em] transition-[translate] duration-300 group-hover:translate-x-2 group-focus-visible:translate-x-2 rtl:group-hover:-translate-x-2 rtl:group-focus-visible:-translate-x-2">
                      {t(`nav.${link.key}`)}
                      <span
                        aria-hidden="true"
                        className="ms-[0.04em] inline-block size-[0.17em] scale-0 rounded-full bg-linear-to-tr from-teal-600 via-teal-400 to-white align-baseline transition-transform duration-300 group-hover:scale-100 group-focus-visible:scale-100"
                      />
                    </span>
                    <span className="hidden text-sm text-ink-400 transition-colors duration-300 group-hover:text-ink-200 sm:block">
                      {t(`menu.hints.${link.key}`)}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>

            <div className={`mt-6 border-t border-white/10 pt-5 ${reveal(open)}`} style={stagger(MAIN.length, open)}>
              <p className="text-xs font-medium tracking-[0.12em] text-ink-400 uppercase">{t('menu.staff')}</p>
              <ul className="mt-2 flex flex-wrap gap-x-5 gap-y-1">
                {STAFF.map(link => (
                  <li key={link.href}>
                    <Link
                      to={link.href}
                      onClick={close}
                      className="rounded text-lg text-ink-200 outline-none transition-colors hover:text-white focus-visible:ring-2 focus-visible:ring-teal-500"
                    >
                      {t(`nav.${link.key}`)}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>

            <div
              className={`mt-auto flex flex-wrap items-center gap-3 ${reveal(open)}`}
              style={stagger(MAIN.length + 1, open)}
            >
              <a
                href="tel:1122"
                className="flex h-11 items-center gap-2 rounded-full bg-linear-to-tr from-teal-600 to-teal-400 px-5 text-sm font-semibold text-ink-950 outline-none focus-visible:ring-2 focus-visible:ring-white"
              >
                {t('menu.call')}
              </a>
              <Link
                to="/login"
                onClick={close}
                className="flex h-11 items-center rounded-full border border-white/20 px-5 text-sm font-semibold outline-none transition-colors hover:bg-white/10 focus-visible:ring-2 focus-visible:ring-teal-500"
              >
                {t('menu.login')}
              </Link>
              <div className="ms-auto flex h-11 items-center rounded-full border border-white/20 p-1 text-sm font-semibold">
                {[
                  { lang: 'en', label: 'EN', active: !urdu },
                  { lang: 'ur', label: 'اردو', active: urdu },
                ].map(option => (
                  <button
                    key={option.lang}
                    type="button"
                    lang={option.lang}
                    aria-pressed={option.active}
                    onClick={() => void i18n.changeLanguage(option.lang)}
                    className={`h-full rounded-full px-3 outline-none focus-visible:ring-2 focus-visible:ring-teal-500 ${
                      option.active ? 'bg-white text-ink-950' : 'text-ink-200 hover:text-white'
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
          </nav>

          <button
            type="button"
            aria-expanded={open}
            aria-controls={panelId}
            onClick={toggle}
            className="absolute top-0 end-0 flex h-12 w-[6.5rem] items-center justify-center rounded-full text-[0.95rem] font-semibold text-white outline-none focus-visible:ring-2 focus-visible:ring-teal-500 focus-visible:ring-inset"
          >
            {open ? t('menu.close') : t('menu.open')}
          </button>
        </div>
      </div>
    </>
  )
}
