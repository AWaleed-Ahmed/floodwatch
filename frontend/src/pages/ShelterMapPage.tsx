import 'maplibre-gl/dist/maplibre-gl.css'
import { Map as MapLibre, Marker, NavigationControl, Popup, setWorkerUrl } from 'maplibre-gl'
import mapWorkerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?url'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import type { Shelter, ShelterStatus } from '../api/types'
import { AreaPicker } from '../components/AreaPicker'
import { Banner } from '../components/Banner'
import { useSelectedArea } from '../hooks/useSelectedArea'

/** Dark blue-grey vector basemap (OpenFreeMap Fiord, free, no key; attribution is shown by the map). Override with VITE_MAP_STYLE_URL. */
const STYLE_URL = import.meta.env.VITE_MAP_STYLE_URL || 'https://tiles.openfreemap.org/styles/fiord'

const AREA_ZOOM = 12

/**
 * Brighter water than the base style, since rivers are what this map is about.
 * Applied on top of whichever style loads; layers the style doesn't have are skipped.
 */
const WATER_PAINT: [layer: string, property: Parameters<MapLibre['setPaintProperty']>[1], value: string | number][] = [
  ['water', 'fill-color', '#4c82c4'],
  ['waterway', 'line-color', '#6ea6e6'],
  ['waterway', 'line-opacity', 0.95],
  ['water_name', 'text-color', '#b4d3f5'],
]

/*
 * MapLibre finds its tile worker next to its own file, which breaks once Vite
 * bundles the library; point it at the worker file directly (works in dev and
 * in the production build).
 */
setWorkerUrl(mapWorkerUrl)

/**
 * Each status has its own colour and shape (circle, square, cross, diamond),
 * so pins can be told apart without colour. Colours are bright enough to read
 * on the dark map.
 */
const STATUS: Record<ShelterStatus, { colour: string; shape: string }> = {
  OPEN: { colour: '#34d399', shape: 'rounded-full' },
  FULL: { colour: '#f87171', shape: 'rounded-[3px]' },
  CLOSED: { colour: '#a3a3a3', shape: '[clip-path:polygon(20%_0,50%_30%,80%_0,100%_20%,70%_50%,100%_80%,80%_100%,50%_70%,20%_100%,0_80%,30%_50%,0_20%)]' },
  UNCONFIRMED: { colour: '#fbbf24', shape: 'rotate-45 scale-[0.8] rounded-[2px]' },
}

interface Result {
  areaId: string
  shelters: Shelter[]
  error?: string
}

const directions = (s: Shelter) => `https://www.openstreetmap.org/directions?route=%3B${s.latitude}%2C${s.longitude}`

/** The same shape and colour as the map pin, for the list and the legend. */
function StatusMark({ status, size = 'size-3' }: { status: ShelterStatus; size?: string }) {
  return (
    <span aria-hidden="true" className={`inline-block shrink-0 ${size} ${STATUS[status].shape}`} style={{ background: STATUS[status].colour }} />
  )
}

/** Small DOM helper for the map popups (built from text nodes, never HTML strings). */
function el<K extends keyof HTMLElementTagNameMap>(tag: K, className: string, text?: string) {
  const node = document.createElement(tag)
  node.className = className
  if (text !== undefined) node.textContent = text
  return node
}

/** Feature 4: Emergency Info & Shelter Map (UC-04), on a dark blue-grey MapLibre map. */
export function ShelterMapPage() {
  const { t } = useTranslation()
  const { areas, areaId, setAreaId } = useSelectedArea()
  const [result, setResult] = useState<Result | null>(null)
  const [mapFailed, setMapFailed] = useState(false)
  const [mapReady, setMapReady] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<MapLibre | null>(null)
  const markersRef = useRef<Marker[]>([])
  const area = areas.find((a) => a.id === areaId)

  useEffect(() => {
    if (!areaId) return
    let current = true
    api
      .shelters(areaId)
      .then((shelters) => current && setResult({ areaId, shelters }))
      .catch((e: Error) => current && setResult({ areaId, shelters: [], error: e.message }))
    return () => {
      current = false
    }
  }, [areaId])

  const fresh = result && result.areaId === areaId ? result : null
  const shelters = fresh?.shelters ?? []

  /* Create the map once, centred on the first area we know about. */
  useEffect(() => {
    if (!area || mapRef.current || !containerRef.current) return
    try {
      const map = new MapLibre({
        container: containerRef.current,
        style: STYLE_URL,
        center: [area.longitude, area.latitude],
        zoom: AREA_ZOOM,
        attributionControl: { compact: true },
        cooperativeGestures: true,
      })
      map.addControl(new NavigationControl({ showCompass: false }), 'top-right')
      map.on('style.load', () => {
        for (const [layer, property, value] of WATER_PAINT) {
          if (map.getLayer(layer)) map.setPaintProperty(layer, property, value)
        }
      })
      mapRef.current = map
      setMapReady(true)
    } catch {
      setMapFailed(true)
    }
  }, [area])

  /* Remove the map when leaving the page. */
  useEffect(
    () => () => {
      markersRef.current.forEach((marker) => marker.remove())
      mapRef.current?.remove()
      mapRef.current = null
    },
    [],
  )

  /* Glide to the chosen area. */
  useEffect(() => {
    const map = mapRef.current
    if (!map || !area) return
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    const target = { center: [area.longitude, area.latitude] as [number, number], zoom: AREA_ZOOM }
    if (reduce) map.jumpTo(target)
    else map.flyTo({ ...target, speed: 1.4 })
  }, [area])

  /* One pin per shelter, with a popup; replaced whenever the list or language changes. */
  useEffect(() => {
    const map = mapRef.current
    if (!map) return
    markersRef.current.forEach((marker) => marker.remove())
    markersRef.current = shelters.map((s) => {
      const pin = el('button', 'grid size-7 cursor-pointer place-items-center rounded-full bg-[#0a0a0a]/70 ring-2 ring-white/80')
      pin.type = 'button'
      pin.setAttribute('aria-label', `${s.name}, ${t(`shelterStatus.${s.status}`)}`)
      const mark = el('span', `block size-3.5 ${STATUS[s.status].shape}`)
      mark.style.background = STATUS[s.status].colour
      pin.append(mark)

      const body = el('div', 'fw-popup-body')
      body.append(el('strong', 'block text-base', s.name))
      if (s.sample) body.append(el('span', 'mt-1 inline-block rounded bg-amber-300/20 px-1.5 text-xs font-semibold text-amber-200', t('shelters.sampleTag')))
      body.append(el('div', 'mt-2', `${t('shelters.status')}: ${t(`shelterStatus.${s.status}`)}`))
      body.append(el('div', '', `${t('shelters.capacity')}: ${s.capacity}`))
      body.append(el('div', '', `${t('shelters.contact')}: ${s.contact}`))
      const link = el('a', 'mt-2 inline-block font-semibold text-white underline underline-offset-4', t('shelters.directions'))
      link.href = directions(s)
      link.target = '_blank'
      link.rel = 'noreferrer'
      body.append(link)

      return new Marker({ element: pin })
        .setLngLat([s.longitude, s.latitude])
        .setPopup(new Popup({ offset: 18, className: 'fw-popup', maxWidth: '260px' }).setDOMContent(body))
        .addTo(map)
    })
  }, [shelters, t, mapReady])

  return (
    <>
      <h1 className="font-display text-[clamp(2.25rem,5vw,3.5rem)] leading-tight tracking-[-0.02em] text-ink-950 ur:leading-[1.8] ur:tracking-normal">
        {t('shelters.title')}
      </h1>
      <div className="mt-6">
        <AreaPicker areas={areas} areaId={areaId} onChange={setAreaId} />
      </div>
      {fresh?.error && (
        <Banner tone="error">
          {t('common.backendDown')} ({fresh.error})
        </Banner>
      )}

      {!mapFailed && (
        <div className="relative isolate z-0 mt-6 h-[440px] overflow-hidden rounded-3xl border border-ink-100 bg-[#45516e]">
          <div ref={containerRef} className="h-full w-full" />
          <div className="pointer-events-none absolute bottom-3 left-3 flex flex-wrap gap-x-3 gap-y-1 rounded-xl bg-[#0a0a0a]/80 px-3 py-2 text-xs text-white/90">
            {(Object.keys(STATUS) as ShelterStatus[]).map((status) => (
              <span key={status} className="inline-flex items-center gap-1.5">
                <StatusMark status={status} size="size-2.5" />
                {t(`shelterStatus.${status}`)}
              </span>
            ))}
          </div>
        </div>
      )}

      <ul className="mt-6 divide-y divide-ink-100 rounded-3xl border border-ink-100">
        {shelters.map((s) => (
          <li key={s.id} className="flex items-center gap-3 px-5 py-3">
            <StatusMark status={s.status} />
            <span className="font-medium text-ink-950">{s.name}</span>
            <span className="ms-auto text-sm text-ink-600">{t(`shelterStatus.${s.status}`)}</span>
          </li>
        ))}
      </ul>
    </>
  )
}
