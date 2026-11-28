import { lazy, Suspense, useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import type { TopographyProjection, TopographyPulse } from './Component/Topography'
import RippleMarkers, { type RippleMarkersHandle } from './RippleMarkers'
import { whenIdle } from './whenIdle.ts'

/** The WebGL terrain is the heaviest part of the page, so its code loads in a separate chunk. */
const Topography = lazy(() => import('./Component/Topography.tsx'))

interface BackgroundProps {
  children?: ReactNode
}

/**
 * Full-screen ink gradient (paper white fading to graphite) with a slow,
 * faint contour-line terrain drifting through the lower half, like a survey map
 * of the valley the river runs through.
 *
 * The terrain is only mounted once the browser is idle after the first paint,
 * so the title, menu and fonts appear and respond straight away; the terrain
 * then rises in with its own intro animation.
 */
export default function Background({ children }: BackgroundProps) {
  const [showTerrain, setShowTerrain] = useState(false)
  const markersRef = useRef<RippleMarkersHandle>(null)
  /* Each automatic ripple pins a city marker to its spot on the map; every frame the markers follow the terrain. */
  const onPulse = useCallback((pulse: TopographyPulse) => markersRef.current?.add(pulse), [])
  const onProject = useCallback(
    (project: (tx: number, tz: number) => TopographyProjection | null) => markersRef.current?.project(project),
    [],
  )

  useEffect(() => whenIdle(() => setShowTerrain(true), 1200), [])

  return (
    <section className="relative isolate min-h-svh overflow-hidden bg-[linear-gradient(180deg,var(--color-paper)_0%,var(--color-ink-50)_30%,var(--color-ink-100)_50%,var(--color-ink-200)_68%,var(--color-ink-400)_86%,var(--color-ink-600)_100%)]">
      <div
        aria-hidden="true"
        className="terrain-layer pointer-events-none absolute inset-x-0 bottom-0 -z-10 h-[78%] opacity-45"
      >
        {showTerrain && (
          <Suspense fallback={null}>
            <Topography
              color="#000000"
              rippleColor="#009689"
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
              mouseInteraction={false}
              parallax={0}
              clickEffect="none"
              autoPulse={5}
              onPulse={onPulse}
              onProject={onProject}
              markers="none"
              dpr={Math.min(window.devicePixelRatio || 1, 1.5)}
              maxFps={60}
              meshScale={1}
            />
          </Suspense>
        )}
      </div>
      {/* Same box as the terrain, at full opacity, so the markers sit exactly on the map. */}
      <div aria-hidden="true" className="terrain-layer pointer-events-none absolute inset-x-0 bottom-0 h-[78%]">
        <RippleMarkers ref={markersRef} />
      </div>
      <div className="relative min-h-svh">{children}</div>
    </section>
  )
}
