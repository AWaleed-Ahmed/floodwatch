import { forwardRef, useImperativeHandle, useRef, useState, type CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
import type { TopographyProjection, TopographyPulse } from './Component/Topography'
import { FLOOD_HISTORY, type FloodPlace } from './floodHistory'

/** How long each marker stays, in ms (drives the ripple-marker fade in index.css). */
const LIFETIME = 3600

/** At most this many markers on the map at once. */
const MAX_MARKERS = 3

interface Marker {
  id: number
  tx: number
  tz: number
  place: FloodPlace
}

export interface RippleMarkersHandle {
  /** Pin the next city to the map spot where a ripple just started. */
  add: (pulse: TopographyPulse) => void
  /** Move every marker to where its map spot is this frame (called by the terrain after each frame). */
  project: (project: (tx: number, tz: number) => TopographyProjection | null) => void
}

/** Every city once in random order, then a new random order, so the same city doesn't come up twice in a row. */
function shuffled() {
  const list = [...FLOOD_HISTORY]
  for (let i = list.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[list[i], list[j]] = [list[j], list[i]]
  }
  return list
}

/**
 * City markers pinned to the 3D terrain where the ripples start: a flat dot on
 * the ground, a thin pin, and the city's name with its historical flood risk.
 * Each marker rides along with the terrain as it drifts, shrinks with distance
 * and fades into the same fog as the lines.
 *
 * Every frame the terrain hands over a projection function; each marker's
 * element is moved with a GPU transform straight from there, so following the
 * map never re-renders React. React only renders when a marker is added or
 * removed (about every five seconds).
 */
const RippleMarkers = forwardRef<RippleMarkersHandle>(function RippleMarkers(_props, ref) {
  const { t, i18n } = useTranslation()
  const [markers, setMarkers] = useState<Marker[]>([])
  const markersRef = useRef<Marker[]>([])
  const elementsRef = useRef(new Map<number, HTMLDivElement>())
  const queueRef = useRef<FloodPlace[]>([])
  const nextIdRef = useRef(0)

  useImperativeHandle(ref, () => ({
    add: ({ tx, tz }) => {
      if (queueRef.current.length === 0) queueRef.current = shuffled()
      const place = queueRef.current.shift()!
      const id = nextIdRef.current++
      const next = [...markersRef.current.slice(-(MAX_MARKERS - 1)), { id, tx, tz, place }]
      markersRef.current = next
      setMarkers(next)
      window.setTimeout(() => {
        markersRef.current = markersRef.current.filter((marker) => marker.id !== id)
        setMarkers(markersRef.current)
      }, LIFETIME)
    },
    project: (project) => {
      for (const marker of markersRef.current) {
        const element = elementsRef.current.get(marker.id)
        if (!element) continue
        const spot = project(marker.tx, marker.tz)
        if (!spot) {
          element.style.opacity = '0'
          continue
        }
        element.style.transform = `translate3d(${spot.x.toFixed(1)}px, ${spot.y.toFixed(1)}px, 0) scale(${spot.scale.toFixed(3)})`
        element.style.opacity = spot.fade.toFixed(3)
      }
    },
  }))

  const urdu = i18n.language === 'ur'

  return (
    <>
      {markers.map(({ id, place }) => (
        <div
          key={id}
          ref={(element) => {
            if (element) elementsRef.current.set(id, element)
            else elementsRef.current.delete(id)
          }}
          className="absolute top-0 left-0 origin-top-left opacity-0 will-change-transform"
        >
          <div className="ripple-marker flex flex-col items-center" style={{ '--life': `${LIFETIME}ms` } as CSSProperties}>
            <div
              lang={urdu ? 'ur' : 'en'}
              dir={urdu ? 'rtl' : 'ltr'}
              className="flex flex-col items-center whitespace-nowrap text-center leading-tight [text-shadow:0_0_6px_rgba(255,255,255,0.85)] ur:leading-[1.8]"
            >
              <span className="text-[0.8rem] font-semibold text-ink-950">{urdu ? place.name.ur : place.name.en}</span>
              <span className="text-[0.7rem] font-medium text-ink-800">
                {t(`riskLevel.${place.level}`)} · {place.year}
              </span>
            </div>
            <span className="ripple-pin mt-1 h-6 w-px bg-ink-950/70" />
            <span className="ripple-dot h-1 w-2.5 rounded-full bg-ink-950" />
          </div>
        </div>
      ))}
    </>
  )
})

export default RippleMarkers
