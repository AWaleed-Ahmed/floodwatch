import type { ReactNode } from 'react'
import Topography from './Component/Topography.tsx'

interface BackgroundProps {
  children?: ReactNode
}

/**
 * Full-screen ink gradient (paper white fading to graphite) with a slow,
 * faint contour-line terrain drifting through the lower half, like a survey map
 * of the valley the river runs through.
 */
export default function Background({ children }: BackgroundProps) {
  return (
    <section className="relative isolate min-h-svh overflow-hidden bg-[linear-gradient(180deg,var(--color-paper)_0%,var(--color-ink-50)_30%,var(--color-ink-100)_50%,var(--color-ink-200)_68%,var(--color-ink-400)_86%,var(--color-ink-600)_100%)]">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 bottom-0 -z-10 h-[78%] opacity-45"
      >
        <Topography
          color="#000000"
          rippleColor="#0b3fb3"
          lightMode
          lineCount={20}
          lineWidth={1}
          elevation={0.9}
          tilt={28}
          speed={0.12}
          direction="forward"
          morph={0.15}
          glow={0}
          fog={0.5}
          depthOfField={0}
          brightness={1.1}
          mouseStrength={0.35}
          parallax={0.3}
          clickEffect="pulse"
          autoPulse={5}
          markers="none"
          dpr={1.5}
        />
      </div>
      <div className="relative">{children}</div>
    </section>
  )
}
