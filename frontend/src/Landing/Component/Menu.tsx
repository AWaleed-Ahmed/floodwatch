import { useEffect, useId, useRef, useState, type TransitionEvent } from 'react'

interface NavLink {
  label: string
  href: string
  hint?: string
}

const MAIN: NavLink[] = [
  { label: 'Dashboard', href: '/dashboard', hint: '7-day rain and river' },
  { label: 'Risk', href: '/risk', hint: 'Low, Medium or High' },
  { label: 'Shelters', href: '/shelters', hint: 'Map and directions' },
  { label: 'Safety', href: '/safety', hint: 'What to do in a flood' },
]

const STAFF: NavLink[] = [
  { label: 'Alerts', href: '/alerts' },
  { label: 'Incidents', href: '/incidents' },
  { label: 'Relief', href: '/relief' },
]

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
  const [open, setOpen] = useState(false)
  const panelId = useId()
  const firstLinkRef = useRef<HTMLAnchorElement>(null)

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
        onClick={() => setOpen(false)}
        className={`fixed inset-0 z-40 ${open ? '' : 'pointer-events-none'}`}
      />

      <div className="fixed top-6 right-[5vw] z-50">
        <div
          className={`absolute top-0 right-0 min-h-12 min-w-[6.5rem] overflow-hidden bg-ink-950 text-white transition-[width,height,border-radius] motion-reduce:transition-none ${
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
            <ul className="flex flex-col gap-1">
              {MAIN.map((link, index) => (
                <li key={link.href} className={reveal(open)} style={stagger(index, open)}>
                  <a
                    ref={index === 0 ? firstLinkRef : undefined}
                    href={link.href}
                    className="group flex items-baseline justify-between gap-4 rounded-xl py-1 outline-none focus-visible:ring-2 focus-visible:ring-teal-500"
                  >
                    <span className="font-display text-[clamp(2rem,4.2vw,2.75rem)] leading-tight font-medium tracking-[-0.01em] transition-[translate] duration-300 group-hover:translate-x-2 group-focus-visible:translate-x-2">
                      {link.label}
                      <span
                        aria-hidden="true"
                        className="ms-[0.04em] inline-block size-[0.17em] scale-0 rounded-full bg-linear-to-tr from-teal-600 via-teal-400 to-white align-baseline transition-transform duration-300 group-hover:scale-100 group-focus-visible:scale-100"
                      />
                    </span>
                    <span className="hidden text-sm text-ink-400 transition-colors duration-300 group-hover:text-ink-200 sm:block">
                      {link.hint}
                    </span>
                  </a>
                </li>
              ))}
            </ul>

            <div className={`mt-6 border-t border-white/10 pt-5 ${reveal(open)}`} style={stagger(MAIN.length, open)}>
              <p className="text-xs font-medium tracking-[0.12em] text-ink-400 uppercase">Staff</p>
              <ul className="mt-2 flex flex-wrap gap-x-5 gap-y-1">
                {STAFF.map(link => (
                  <li key={link.href}>
                    <a
                      href={link.href}
                      className="rounded text-lg text-ink-200 outline-none transition-colors hover:text-white focus-visible:ring-2 focus-visible:ring-teal-500"
                    >
                      {link.label}
                    </a>
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
                Call 1122
              </a>
              <a
                href="/login"
                className="flex h-11 items-center rounded-full border border-white/20 px-5 text-sm font-semibold outline-none transition-colors hover:bg-white/10 focus-visible:ring-2 focus-visible:ring-teal-500"
              >
                Log in
              </a>
              <div className="ms-auto flex h-11 items-center rounded-full border border-white/20 p-1 text-sm font-semibold">
                <button type="button" aria-pressed="true" className="h-full rounded-full bg-white px-3 text-ink-950">
                  EN
                </button>
                <button type="button" aria-pressed="false" className="h-full rounded-full px-3 text-ink-200 hover:text-white">
                  اردو
                </button>
              </div>
            </div>
          </nav>

          <button
            type="button"
            aria-expanded={open}
            aria-controls={panelId}
            onClick={() => setOpen(value => !value)}
            className="absolute top-0 right-0 flex h-12 w-[6.5rem] items-center justify-center rounded-full text-[0.95rem] font-semibold text-white outline-none focus-visible:ring-2 focus-visible:ring-teal-500 focus-visible:ring-inset"
          >
            {open ? 'Close' : 'Menu'}
          </button>
        </div>
      </div>
    </>
  )
}
