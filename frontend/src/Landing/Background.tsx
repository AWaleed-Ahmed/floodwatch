import type { ReactNode } from 'react'
import Topography from './Component/Topography.tsx'

interface BackgroundProps {
  children?: ReactNode
}

/**
 * Full-screen river gradient (deep teal fading to misty blue-white) with a slow,
 * faint contour-line terrain drifting through the lower half, like a survey map
 * of the valley the river runs through.
 */
export default function Background({ children }: BackgroundProps) {
  return (
    <section className="relative isolate min-h-svh overflow-hidden bg-[linear-gradient(180deg,var(--color-river-950)_0%,var(--color-river-900)_18%,var(--color-river-800)_32%,var(--color-river-600)_50%,var(--color-river-400)_64%,var(--color-river-200)_78%,var(--color-river-100)_90%,var(--color-river-50)_100%)]">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 bottom-0 -z-10 h-[78%] opacity-45"
      >
        <Topography
          color="#000000"
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
          markers="none"
          dpr={1.5}
        />
      </div>
      <div className="relative">{children}</div>
    </section>
  )
}
