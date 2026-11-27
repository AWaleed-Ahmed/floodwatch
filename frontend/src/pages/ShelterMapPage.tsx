import 'leaflet/dist/leaflet.css'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CircleMarker, MapContainer, Popup, TileLayer, useMap } from 'react-leaflet'
import { api } from '../api/client'
import type { Shelter, ShelterStatus } from '../api/types'
import { AreaPicker } from '../components/AreaPicker'
import { Banner } from '../components/Banner'
import { useSelectedArea } from '../hooks/useSelectedArea'

const TILE_URL = import.meta.env.VITE_OSM_TILE_URL ?? 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
const ATTRIBUTION = import.meta.env.VITE_OSM_ATTRIBUTION ?? '© OpenStreetMap contributors'
const COLOURS: Record<ShelterStatus, string> = { OPEN: '#2e7d32', FULL: '#c62828', CLOSED: '#616161', UNCONFIRMED: '#f9a825' }

interface Result {
  areaId: string
  shelters: Shelter[]
  error?: string
}

function Recenter({ lat, lon }: { lat: number; lon: number }) {
  const map = useMap()
  useEffect(() => {
    map.setView([lat, lon], 12)
  }, [map, lat, lon])
  return null
}

const directions = (s: Shelter) => `https://www.openstreetmap.org/directions?route=%3B${s.latitude}%2C${s.longitude}`

/** Feature 4: Emergency Info & Shelter Map (UC-04). */
export function ShelterMapPage() {
  const { t } = useTranslation()
  const { areas, areaId, setAreaId } = useSelectedArea()
  const [result, setResult] = useState<Result | null>(null)
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

  return (
    <>
      <h1 className="font-display text-[clamp(2.25rem,5vw,3.5rem)] leading-tight tracking-[-0.02em] text-ink-950">{t('shelters.title')}</h1>
      <div className="mt-6">
        <AreaPicker areas={areas} areaId={areaId} onChange={setAreaId} />
      </div>
      {fresh?.error && (
        <Banner tone="error">
          {t('common.backendDown')} ({fresh.error})
        </Banner>
      )}

      {area && (
        <div className="relative isolate z-0 mt-6 overflow-hidden rounded-3xl border border-ink-100">
          <MapContainer center={[area.latitude, area.longitude]} zoom={12} className="h-[420px] w-full">
            <TileLayer url={TILE_URL} attribution={ATTRIBUTION} />
            <Recenter lat={area.latitude} lon={area.longitude} />
            {shelters.map((s) => (
              <CircleMarker
                key={s.id}
                center={[s.latitude, s.longitude]}
                radius={10}
                pathOptions={{ color: COLOURS[s.status], fillColor: COLOURS[s.status], fillOpacity: 0.8 }}
              >
                <Popup>
                  <strong>{s.name}</strong>
                  {s.sample && (
                    <div className="my-1 inline-block rounded bg-amber-50 px-1.5 text-xs font-semibold text-amber-900">{t('shelters.sampleTag')}</div>
                  )}
                  <div>
                    {t('shelters.status')}: {t(`shelterStatus.${s.status}`)}
                  </div>
                  <div>
                    {t('shelters.capacity')}: {s.capacity}
                  </div>
                  <div>
                    {t('shelters.contact')}: {s.contact}
                  </div>
                  <a href={directions(s)} target="_blank" rel="noreferrer">
                    {t('shelters.directions')}
                  </a>
                </Popup>
              </CircleMarker>
            ))}
          </MapContainer>
        </div>
      )}

      <ul className="mt-6 divide-y divide-ink-100 rounded-3xl border border-ink-100">
        {shelters.map((s) => (
          <li key={s.id} className="flex items-center gap-3 px-5 py-3">
            <span aria-hidden="true" className="size-2.5 shrink-0 rounded-full" style={{ background: COLOURS[s.status] }} />
            <span className="font-medium text-ink-950">{s.name}</span>
            <span className="ms-auto text-sm text-ink-600">{t(`shelterStatus.${s.status}`)}</span>
          </li>
        ))}
      </ul>
    </>
  )
}
