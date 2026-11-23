import 'leaflet/dist/leaflet.css'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CircleMarker, MapContainer, Popup, TileLayer, useMap } from 'react-leaflet'
import { api } from '../api/client'
import type { Shelter, ShelterStatus } from '../api/types'
import { AreaPicker } from '../components/AreaPicker'
import { useSelectedArea } from '../hooks/useSelectedArea'

const TILE_URL = import.meta.env.VITE_OSM_TILE_URL ?? 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
const ATTRIBUTION = import.meta.env.VITE_OSM_ATTRIBUTION ?? '© OpenStreetMap contributors'
const COLOURS: Record<ShelterStatus, string> = { OPEN: '#2e7d32', FULL: '#c62828', CLOSED: '#616161', UNCONFIRMED: '#f9a825' }

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
  const [shelters, setShelters] = useState<Shelter[]>([])
  const [error, setError] = useState<string | null>(null)
  const area = areas.find((a) => a.id === areaId)

  useEffect(() => {
    if (!areaId) return
    api.shelters(areaId).then(setShelters).catch((e: Error) => setError(e.message))
  }, [areaId])

  return (
    <section>
      <h1>{t('shelters.title')}</h1>
      <AreaPicker areas={areas} areaId={areaId} onChange={setAreaId} />
      {error && <div className="banner banner-error">{t('common.backendDown')} ({error})</div>}
      {area && (
        <MapContainer center={[area.latitude, area.longitude]} zoom={12} className="map">
          <TileLayer url={TILE_URL} attribution={ATTRIBUTION} />
          <Recenter lat={area.latitude} lon={area.longitude} />
          {shelters.map((s) => (
            <CircleMarker key={s.id} center={[s.latitude, s.longitude]} radius={10}
              pathOptions={{ color: COLOURS[s.status], fillColor: COLOURS[s.status], fillOpacity: 0.8 }}>
              <Popup>
                <strong>{s.name}</strong>
                {s.sample && <div className="tag">{t('shelters.sampleTag')}</div>}
                <div>{t('shelters.status')}: {t(`shelterStatus.${s.status}`)}</div>
                <div>{t('shelters.capacity')}: {s.capacity}</div>
                <div>{t('shelters.contact')}: {s.contact}</div>
                <a href={directions(s)} target="_blank" rel="noreferrer">{t('shelters.directions')}</a>
              </Popup>
            </CircleMarker>
          ))}
        </MapContainer>
      )}
      <ul className="shelter-list">
        {shelters.map((s) => (
          <li key={s.id}>
            <span className="dot" style={{ background: COLOURS[s.status] }} /> {s.name} – {t(`shelterStatus.${s.status}`)}
          </li>
        ))}
      </ul>
    </section>
  )
}
