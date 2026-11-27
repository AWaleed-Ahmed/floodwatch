import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { whenIdle } from '../Landing/whenIdle'
import i18n from '../i18n'
import { prefetchPage } from '../pageLoaders'

/** Wait for the menu to finish closing before anything moves, in ms. */
const MENU_CLOSE = 520

/** Timeline of the drop, in ms from when the dot starts falling. */
const T = {
  land: 720,
  squash: 980,
  settle: 1140,
  hold: 1320,
  whiten: 1900,
  expand: 2400,
}

/** The floor the dot lands on, as a fraction of the screen height. */
const FLOOR = 0.72

/** Corner radius the shape keeps while it grows to the screen edges, in px. */
const COVER_RADIUS = 56

/**
 * Colour of the dot over time, as a CSS filter on its own teal gradient:
 * teal at the start, a soft light grey while it lands and stretches, and pure
 * white at the end (brightness 4 clips every channel to white, matching the page).
 * Filters run on the GPU, so changing colour costs nothing extra per frame.
 */
const TEAL = 'saturate(1) brightness(1)'
const SOFT = 'saturate(0.4) brightness(1.08)'
const GREY = 'saturate(0) brightness(1.22)'
const WHITE = 'saturate(0) brightness(4)'

/** While this class is on <html> the terrain fades out (index.css). */
export const TRANSITIONING = 'is-transitioning'

/** Added once the terrain has faded out; the terrain stops drawing while it is set. */
const TERRAIN_PAUSED = 'terrain-paused'

/** Matches the terrain fade in index.css, in ms. */
const TERRAIN_FADE = 450

const nextPaint = () => new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))
const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))
const idle = (timeout: number) => new Promise<void>((resolve) => void whenIdle(() => resolve(), timeout))

/** Page-to-page arch: how tall its curve is, as a fraction of the screen height. */
const ARCH_CURVE = 0.24

/** Label shown in the arch for each page. */
const PAGE_LABELS: Record<string, string> = {
  '/dashboard': 'nav.dashboard',
  '/risk': 'nav.risk',
  '/shelters': 'nav.shelters',
  '/safety': 'nav.safety',
  '/alerts': 'nav.alerts',
  '/incidents': 'nav.incidents',
  '/relief': 'nav.relief',
  '/login': 'menu.login',
}

let runner: ((to: string) => void) | null = null

/**
 * Go to a page with the teal-dot transition. Falls back to plain navigation
 * if the transition layer isn't mounted.
 */
export function transitionTo(to: string) {
  if (runner) runner(to)
  else window.location.assign(to)
}

/** Position, size and corner radius of the dot at one moment. */
const frame = (x: number, y: number, w: number, h: number, r: number) => ({
  left: `${x}px`,
  top: `${y}px`,
  width: `${w}px`,
  height: `${h}px`,
  borderRadius: `${r}px`,
})

/**
 * Page transitions.
 *
 * Leaving the landing page, the wordmark's own teal dot performs it:
 *
 *  1. The menu finishes closing.
 *  2. The dot is lifted out of the wordmark and drops straight down from where
 *     it sits, speeding up and stretching slightly as it falls, fading from
 *     teal to a soft grey.
 *  3. It hits an imaginary floor and squashes into a squared-off rounded
 *     rectangle, then settles.
 *  4. The rectangle's corners stretch out past the edges of the screen, keeping
 *     their rounded shape, and it turns white at the very end.
 *  5. Only now, with the screen fully white, the new page's code is loaded and
 *     the route changes. A plain white cover (switched on, not animated) holds
 *     the screen until the page has rendered and the browser is idle, then fades.
 *
 * Performance: one small fixed element is animated (no children, size
 * contained so its changes never reach the rest of the page), the colour change
 * is a GPU filter, nothing loads during the animation, and the WebGL terrain
 * fades out and then stops drawing until the transition is over.
 */
export default function PageTransition() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const coverRef = useRef<HTMLDivElement>(null)
  const archRef = useRef<HTMLDivElement>(null)
  const labelRef = useRef<HTMLSpanElement>(null)
  const busyRef = useRef(false)
  const pathRef = useRef(pathname)

  useEffect(() => {
    pathRef.current = pathname
  }, [pathname])

  useEffect(() => {
    /**
     * Page to page: a wide black arch swoops up from the bottom in one quick
     * motion with the next page's name riding in it, covers the page, the route
     * changes underneath, and the cover fades away in place. Transform and
     * opacity only.
     */
    const runArch = async (to: string) => {
      const arch = archRef.current
      const label = labelRef.current
      const page = prefetchPage(to)
      if (!arch || !label) {
        await page
        navigate(to)
        window.scrollTo(0, 0)
        return
      }
      label.textContent = i18n.t(PAGE_LABELS[to] ?? 'app.name')
      label.lang = i18n.language
      label.dir = i18n.dir()

      /* Give the menu a moment to start closing so the two don't collide. */
      await wait(120)

      const root = document.documentElement
      const vh = window.innerHeight
      const curve = vh * ARCH_CURVE
      const at = (y: number) => ({ transform: `translate3d(0, ${y}px, 0)` })
      root.style.pointerEvents = 'none'
      arch.style.visibility = 'visible'
      try {
        /* One swoop from below the screen to fully covering it (its curved top ends just above the screen). */
        label.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 200, delay: 140, easing: 'ease-out', fill: 'forwards' })
        await arch.animate([at(vh), at(-curve)], { duration: 520, easing: 'cubic-bezier(0.7, 0, 0.2, 1)', fill: 'forwards' }).finished
        await page
        navigate(to)
        window.scrollTo(0, 0)
        await nextPaint()
        /* A beat to read the name, then the cover simply fades away in place. */
        await wait(110)
        await arch.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 260, easing: 'ease-out', fill: 'forwards' }).finished
      } finally {
        arch.getAnimations().forEach((animation) => animation.cancel())
        label.getAnimations().forEach((animation) => animation.cancel())
        arch.style.visibility = 'hidden'
        root.style.pointerEvents = ''
      }
    }

    runner = async (to: string) => {
      const cover = coverRef.current
      if (busyRef.current || to === pathRef.current) return
      const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
      if (!cover || reduce) {
        await prefetchPage(to)
        navigate(to)
        window.scrollTo(0, 0)
        return
      }

      /* The teal-dot drop is only for leaving the landing page; between pages the arch is used. */
      if (pathRef.current !== '/') {
        busyRef.current = true
        try {
          await runArch(to)
        } finally {
          busyRef.current = false
        }
        return
      }

      busyRef.current = true
      const root = document.documentElement

      await wait(MENU_CLOSE)

      const vw = window.innerWidth
      const vh = window.innerHeight
      const dot = document.querySelector<HTMLElement>('[data-teal-dot]')
      const rect = dot?.getBoundingClientRect()
      const savedStyle = dot?.style.cssText ?? ''

      root.classList.add(TRANSITIONING)
      root.style.pointerEvents = 'none'
      /* Keep the terrain drawing while it fades, so it never looks frozen, then let it rest. */
      const pauseTerrain = window.setTimeout(() => root.classList.add(TERRAIN_PAUSED), TERRAIN_FADE)

      const showCover = () => {
        cover.style.visibility = 'visible'
        cover.style.opacity = '1'
      }
      const restoreDot = () => {
        if (!dot) return
        dot.getAnimations().forEach((animation) => {
          if (!(animation instanceof CSSAnimation)) animation.cancel()
        })
        dot.style.cssText = savedStyle
      }

      try {
        if (dot && rect) {
          /* Start exactly on the real dot. A tiny header dot grows a little as it falls so the drop is easy to follow. */
          const d0 = rect.width
          const cx = rect.left + rect.width / 2
          const cy = rect.top + rect.height / 2
          const d = Math.max(d0, 22)
          const floor = Math.max(vh * FLOOR, cy + d * 3)
          const round = (size: number, centerY: number, stretch = 1) => {
            const w = size / stretch
            const h = size * stretch
            return frame(cx - w / 2, centerY - h / 2, w, h, Math.max(w, h) / 2)
          }
          const onFloor = (w: number, h: number, r: number) => frame(cx - w / 2, floor - h, w, h, r)
          const at = (ms: number) => ms / T.expand
          const rested = onFloor(d * 2.9, d * 1.12, d * 0.46)

          dot.style.position = 'fixed'
          dot.style.margin = '0'
          dot.style.zIndex = '60'
          dot.style.pointerEvents = 'none'
          /* Size containment: the dot's changing box never makes the browser re-check anything else on the page. */
          dot.style.contain = 'strict'
          dot.style.willChange = 'filter'

          const timing: KeyframeAnimationOptions = { duration: T.expand, fill: 'forwards' }
          /* Shape and colour are two animations on the same element, so each keeps its own smooth curve. */
          const move = dot.animate(
            [
              { offset: 0, ...round(d0, cy), easing: 'cubic-bezier(0.5, 0, 0.9, 0.6)' },
              { offset: at(T.land - 60), ...round(d, floor - d * 0.62, 1.22), easing: 'linear' },
              { offset: at(T.land), ...round(d, floor - d / 2), easing: 'cubic-bezier(0.2, 0.8, 0.3, 1)' },
              { offset: at(T.squash), ...onFloor(d * 3.2, d * 0.95, d * 0.42), easing: 'cubic-bezier(0.4, 0, 0.3, 1)' },
              { offset: at(T.settle), ...rested, easing: 'linear' },
              { offset: at(T.hold), ...rested, easing: 'cubic-bezier(0.65, 0, 0.3, 1)' },
              { offset: 1, ...frame(-COVER_RADIUS, -COVER_RADIUS, vw + COVER_RADIUS * 2, vh + COVER_RADIUS * 2, COVER_RADIUS) },
            ],
            timing,
          )
          dot.animate(
            [
              { offset: 0, filter: TEAL, easing: 'ease-in' },
              { offset: at(T.land), filter: SOFT, easing: 'ease-out' },
              { offset: at(T.settle), filter: GREY },
              { offset: at(T.whiten), filter: GREY, easing: 'ease-in' },
              { offset: 1, filter: WHITE },
            ],
            timing,
          )
          await move.finished
          /* The dot now covers the screen in pure white; the static cover takes over with no visible change. */
          showCover()
        } else {
          cover.style.visibility = 'visible'
          await cover.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 400, easing: 'ease-out', fill: 'forwards' }).finished
          showCover()
        }

        /* Only now load the new page, then swap to it under the cover. */
        await prefetchPage(to)
        navigate(to)
        window.scrollTo(0, 0)
        await nextPaint()
        if (dot?.isConnected) restoreDot()
        /* Let the new page render and settle (charts, map) before revealing it, so the fade never stutters. */
        await idle(700)
        await nextPaint()
        await cover.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 650, easing: 'cubic-bezier(0.4, 0, 0.2, 1)', fill: 'forwards' })
          .finished
      } finally {
        cover.getAnimations().forEach((animation) => animation.cancel())
        cover.style.visibility = 'hidden'
        restoreDot()
        window.clearTimeout(pauseTerrain)
        root.classList.remove(TRANSITIONING, TERRAIN_PAUSED)
        root.style.pointerEvents = ''
        busyRef.current = false
      }
    }
    return () => {
      runner = null
    }
  }, [navigate])

  return (
    <>
      <div ref={coverRef} aria-hidden="true" className="pointer-events-none invisible fixed inset-0 z-[61] bg-paper" />
      {/* The arch: wider than the screen with an elliptical top edge, so its curve is broad and flat, not a semicircle. Flat bottom. */}
      <div
        ref={archRef}
        aria-hidden="true"
        className="pointer-events-none invisible fixed top-0 -right-[12vw] -left-[12vw] z-[61] bg-ink-950 will-change-transform"
        style={{
          height: `${100 + ARCH_CURVE * 100}svh`,
          borderRadius: `50% 50% 0 0 / ${ARCH_CURVE * 100}svh ${ARCH_CURVE * 100}svh 0 0`,
          transform: 'translate3d(0, 100svh, 0)',
        }}
      >
        <span
          ref={labelRef}
          className="absolute inset-x-0 top-[74svh] -translate-y-1/2 text-center font-display text-[clamp(2.1rem,5.8vw,4.6rem)] leading-none tracking-[-0.02em] text-paper opacity-0 ur:leading-[1.6] ur:tracking-normal"
        />
      </div>
    </>
  )
}
