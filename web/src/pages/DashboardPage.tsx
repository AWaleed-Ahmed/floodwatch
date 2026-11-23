import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api/client'
import type { Dashboard } from '../api/types'
import { AreaPicker } from '../components/AreaPicker'
import { SourceLine } from '../components/SourceLine'
import { useSelectedArea } from '../hooks/useSelectedArea'

/** Feature 1: Flood Monitoring Dashboard (UC-01). */
export function DashboardPage() {
  const { t } = useTranslation()
  const { areas, areaId, setAreaId, error: areaError } = useSelectedArea()
  const [data, setData] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!areaId) return
    setData(null)
    setError(null)
    api.dashboard(areaId).then(setData).catch((e: Error) => setError(e.message))
  }, [areaId])

  // Rain is hourly; sum it per day so a week fits on a phone screen.
  const rainByDay = useMemo(() => {
    const days = new Map<string, { day: string; mm: number; maxProb: number }>()
    for (const p of data?.rain.points ?? []) {
      const day = new Date(p.time).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' })
      const row = days.get(day) ?? { day, mm: 0, maxProb: 0 }
      row.mm = Math.round((row.mm + (p.value ?? 0)) * 10) / 10
      row.maxProb = Math.max(row.maxProb, p.probability ?? 0)
      days.set(day, row)
    }
    return [...days.values()]
  }, [data])

  const discharge = useMemo(
    () => (data?.discharge.points ?? []).map((p) => ({
      day: new Date(p.time).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' }),
      m3s: p.value,
    })),
    [data],
  )

  // A CACHED series younger than the backend's cache time is just a fast answer, not a problem.
  const STALE_MS = 30 * 60 * 1000
  const isOld = (s: { status: string; fetchedAt: string }) => s.status === 'CACHED' && Date.now() - new Date(s.fetchedAt).getTime() > STALE_MS
  const degraded = data && (isOld(data.rain) || isOld(data.discharge))
  const offline = data && (data.rain.status === 'SAMPLE' || data.discharge.status === 'SAMPLE')

  return (
    <section>
      <h1>{t('dashboard.title')}</h1>
      <AreaPicker areas={areas} areaId={areaId} onChange={setAreaId} />
      {(areaError || error) && <div className="banner banner-error">{t('common.backendDown')} ({areaError ?? error})</div>}
      {!data && !error && !areaError && <p>{t('common.loading')}</p>}
      {data && (
        <>
          {offline && <div className="banner banner-warn">{t('dashboard.sampleWarning')}</div>}
          {degraded && !offline && <div className="banner banner-info">{t('dashboard.cachedNote')}</div>}

          <div className="card">
            <h2>{t('dashboard.rainTitle')}</h2>
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={rainByDay}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis unit=" mm" width={60} />
                <Tooltip formatter={(v, name) => (name === 'mm' ? [`${v} mm`, t('dashboard.rain')] : [`${v} %`, t('dashboard.rainChance')])} />
                <Bar dataKey="mm" fill="#2f6db5" />
              </BarChart>
            </ResponsiveContainer>
            <SourceLine series={data.rain} />
          </div>

          <div className="card">
            <h2>{t('dashboard.riverTitle', { river: data.area.riverName })}</h2>
            {data.discharge.status === 'UNAVAILABLE' ? (
              <p>{t('dashboard.noRiver')}</p>
            ) : (
              <ResponsiveContainer width="100%" height={240}>
                <LineChart data={discharge}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="day" />
                  <YAxis unit=" m³/s" width={80} />
                  <Tooltip formatter={(v) => [`${v} m³/s`, t('dashboard.discharge')]} />
                  <Line type="monotone" dataKey="m3s" stroke="#1f3a5f" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            )}
            <SourceLine series={data.discharge} />
          </div>
          <p className="disclaimer">{t('common.disclaimer')}</p>
        </>
      )}
    </section>
  )
}
