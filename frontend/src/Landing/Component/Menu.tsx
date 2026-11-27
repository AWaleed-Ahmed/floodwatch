import { useEffect, useId, useRef, useState, type MouseEvent, type TransitionEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { transitionTo } from '../../components/PageTransition'
import { whenIdle } from '../whenIdle.ts'

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

const URDU_FONT = '400 1em "Noto Nastaliq Urdu"'

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

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

  /**
   * Menu links close the menu and hand over to the teal-dot page transition.
   * Ctrl/Cmd/Shift or middle clicks keep their normal behaviour (new tab or window).
   */
  const go = (to: string) => (event: MouseEvent<HTMLAnchorElement>) => {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    close()
    transitionTo(to)
  }
  const [pendingLang, setPendingLang] = useState<string | null>(null)
  /** The language pill slides as soon as an option is pressed, before the text finishes swapping. */
  const urdu = (pendingLang ?? i18n.language) === 'ur'
  const firstLinkRef = useRef<HTMLAnchorElement>(null)

  const switchingRef = useRef(false)

  /**
   * When the menu opens (the switch is inside it), get Urdu ready in the
   * background: download the font, then lay out the Urdu menu and hero text once
   * in an invisible element. Nastaliq's first layout is slow, and doing it here,
   * while the browser is idle, means the real switch doesn't hitch.
   */
  const warmedRef = useRef(false)
  useEffect(() => {
    if (!open || warmedRef.current || i18n.language === 'ur') return
    warmedRef.current = true
    const ur = i18n.getFixedT('ur')
    const sample = [
      ...MAIN.map(link => `${ur(`nav.${link.key}`)} ${ur(`menu.hints.${link.key}`)}`),
      ...STAFF.map(link => ur(`nav.${link.key}`)),
      ur('menu.staff'), ur('menu.call'), ur('menu.login'), ur('menu.close'), ur('menu.open'),
      ur('hero.taglineTop'), ur('hero.taglineBottom'),
    ].join(' ')
    void document.fonts
      ?.load(URDU_FONT, sample)
      .then(() => whenIdle(() => {
        const probe = document.createElement('div')
        probe.setAttribute('aria-hidden', 'true')
        probe.lang = 'ur'
        probe.dir = 'rtl'
        probe.textContent = sample
        probe.style.cssText = 'position:fixed;left:-9999px;top:0;visibility:hidden;font-family:"Noto Nastaliq Urdu";font-size:2rem;width:40rem'
        document.body.append(probe)
        void probe.offsetHeight
        setTimeout(() => probe.remove(), 3000)
      }))
      .catch(() => {})
  }, [open, i18n])

  /**
   * Switches language without freezing anything: the text marked
   * data-lang-fade fades out, the language (and direction) swaps while it is
   * invisible, then it fades back in. The terrain and menu keep animating.
   */
  const switchLanguage = async (lang: string) => {
    if (lang === i18n.language || switchingRef.current) return
    switchingRef.current = true
    setPendingLang(lang)
    const root = document.documentElement
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    const font = lang === 'ur' ? document.fonts?.load(URDU_FONT, 'اردو').catch(() => []) : undefined
    try {
      if (!reduce) {
        root.classList.add('lang-out')
        await Promise.all([wait(180), Promise.race([font, wait(700)])])
      }
      await i18n.changeLanguage(lang)
    } finally {
      /* Fade back in on the next painted frame; the timer is a fallback for throttled background tabs. */
      const fadeIn = () => root.classList.remove('lang-out')
      requestAnimationFrame(() => requestAnimationFrame(fadeIn))
      setTimeout(fadeIn, 120)
      switchingRef.current = false
      setPendingLang(null)
    }
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

      <div data-topography-ignore className="fixed top-6 right-[5vw] z-50">
        <div
          className={`absolute top-0 right-0 min-h-12 min-w-[6.5rem] overflow-hidden bg-ink-950 text-paper transition-[width,height,border-radius] motion-reduce:transition-none ${
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
            className={`absolute top-0 right-0 flex ${PANEL_SIZE} flex-col px-8 pt-20 pb-7`}
          >
            <ul data-lang-fade className="flex flex-col gap-1 ur:gap-0">
              {MAIN.map((link, index) => (
                <li key={link.href} className={reveal(open)} style={stagger(index, open)}>
                  <Link
                    ref={index === 0 ? firstLinkRef : undefined}
                    to={link.href}
                    onClick={go(link.href)}
                    className="group flex items-baseline justify-between gap-4 rounded-xl py-1 outline-none focus-visible:ring-2 focus-visible:ring-teal-500"
                  >
                    <span className="font-display text-[clamp(2rem,4.2vw,2.75rem)] leading-tight font-medium ur:text-[clamp(1.6rem,3.4vw,2.2rem)] ur:leading-[1.9] tracking-[-0.01em] transition-[translate] duration-300 group-hover:translate-x-2 group-focus-visible:translate-x-2 rtl:group-hover:-translate-x-2 rtl:group-focus-visible:-translate-x-2">
                      {t(`nav.${link.key}`)}
                      <span
                        aria-hidden="true"
                        className="ms-[0.04em] inline-block size-[0.17em] scale-0 rounded-full bg-linear-to-tr from-teal-600 via-teal-400 to-paper align-baseline transition-transform duration-300 group-hover:scale-100 group-focus-visible:scale-100"
                      />
                    </span>
                    <span className="hidden text-sm text-ink-400 transition-colors duration-300 group-hover:text-ink-200 sm:block">
                      {t(`menu.hints.${link.key}`)}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>

            <div data-lang-fade className={`mt-6 border-t border-paper/10 pt-5 ${reveal(open)}`} style={stagger(MAIN.length, open)}>
              <p className="text-xs font-medium tracking-[0.12em] text-ink-400 uppercase">{t('menu.staff')}</p>
              <ul className="mt-2 flex flex-wrap gap-x-5 gap-y-1">
                {STAFF.map(link => (
                  <li key={link.href}>
                    <Link
                      to={link.href}
                      onClick={go(link.href)}
                      className="rounded text-lg text-ink-200 outline-none transition-colors hover:text-paper focus-visible:ring-2 focus-visible:ring-teal-500"
                    >
                      {t(`nav.${link.key}`)}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>

            <div
              dir="ltr"
              className={`mt-auto flex flex-nowrap items-center gap-3 whitespace-nowrap ${reveal(open)}`}
              style={stagger(MAIN.length + 1, open)}
            >
              <a
                href="tel:1122"
                className="flex h-11 items-center gap-2 rounded-full bg-linear-to-tr from-teal-600 to-teal-400 px-5 text-sm font-semibold text-ink-950 outline-none focus-visible:ring-2 focus-visible:ring-paper"
              >
                <span data-lang-fade>{t('menu.call')}</span>
              </a>
              <Link
                to="/login"
                onClick={go('/login')}
                className="flex h-11 items-center rounded-full border border-paper/20 px-5 text-sm font-semibold outline-none transition-colors hover:bg-paper/10 focus-visible:ring-2 focus-visible:ring-teal-500"
              >
                <span data-lang-fade>{t('menu.login')}</span>
              </Link>
              <div
                role="group"
                aria-label={t('menu.language')}
                className="relative ms-auto grid h-11 w-[7.5rem] shrink-0 grid-cols-2 rounded-full border border-paper/20 p-1 text-sm font-semibold"
              >
                <span
                  aria-hidden="true"
                  className={`absolute inset-y-1 left-1 w-[calc(50%-0.25rem)] rounded-full bg-paper transition-transform duration-300 ease-spring motion-reduce:transition-none ${
                    urdu ? 'translate-x-full' : 'translate-x-0'
                  }`}
                />
                {[
                  { lang: 'en', label: 'EN', active: !urdu },
                  { lang: 'ur', label: 'اردو', active: urdu },
                ].map(option => (
                  <button
                    key={option.lang}
                    type="button"
                    lang={option.lang}
                    aria-pressed={option.active}
                    onClick={() => void switchLanguage(option.lang)}
                    className={`relative rounded-full leading-none outline-none transition-colors duration-300 focus-visible:ring-2 focus-visible:ring-teal-500 ${
                      option.active ? 'text-ink-950' : 'text-ink-200 hover:text-paper'
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
            onClick={() => setOpen(value => !value)}
            className="absolute top-0 right-0 flex h-12 w-[6.5rem] items-center justify-center rounded-full text-[0.95rem] font-semibold text-paper outline-none focus-visible:ring-2 focus-visible:ring-teal-500 focus-visible:ring-inset"
          >
            <span data-lang-fade>{open ? t('menu.close') : t('menu.open')}</span>
          </button>
        </div>
      </div>
    </>
  )
}
