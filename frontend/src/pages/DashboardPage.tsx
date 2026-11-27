import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api/client'
import type { Dashboard, ForecastSeries } from '../api/types'
import { AreaPicker } from '../components/AreaPicker'
import { Banner } from '../components/Banner'
import { SourceLine } from '../components/SourceLine'
import { useSelectedArea } from '../hooks/useSelectedArea'

/** A CACHED series younger than the backend's cache time is just a fast answer, not a problem. */
const STALE_MS = 30 * 60 * 1000

const CHART = { rain: '#009689', river: '#0a0a0a', grid: '#e4e4e4', axis: '#5c5c5c' }

interface Result {
  areaId: string
  data?: Dashboard
  error?: string
  receivedAt: number
}

const dayLabel = (time: string) => new Date(time).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' })

const isOld = (series: ForecastSeries, now: number) =>
  series.status === 'CACHED' && now - new Date(series.fetchedAt).getTime() > STALE_MS

function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-6 rounded-3xl border border-ink-100 bg-paper p-5 sm:p-6">
      <h2 className="font-display text-xl tracking-[-0.01em] text-ink-950">{title}</h2>
      <div className="mt-4">{children}</div>
    </section>
  )
}

/** Feature 1: Flood Monitoring Dashboard (UC-01). */
export function DashboardPage() {
  const { t } = useTranslation()
  const { areas, areaId, setAreaId, error: areaError } = useSelectedArea()
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    if (!areaId) return
    let current = true
    api
      .dashboard(areaId)
      .then((data) => current && setResult({ areaId, data, receivedAt: Date.now() }))
      .catch((e: Error) => current && setResult({ areaId, error: e.message, receivedAt: Date.now() }))
    return () => {
      current = false
    }
  }, [areaId])

  const fresh = result && result.areaId === areaId ? result : null
  const data = fresh?.data ?? null
  const error = fresh?.error ?? null

  /** Rain is hourly; sum it per day so a week fits on a phone screen. */
  const rainByDay = useMemo(() => {
    const days = new Map<string, { day: string; mm: number; maxProb: number }>()
    for (const p of data?.rain.points ?? []) {
      const day = dayLabel(p.time)
      const row = days.get(day) ?? { day, mm: 0, maxProb: 0 }
      row.mm = Math.round((row.mm + (p.value ?? 0)) * 10) / 10
      row.maxProb = Math.max(row.maxProb, p.probability ?? 0)
      days.set(day, row)
    }
    return [...days.values()]
  }, [data])

  const discharge = useMemo(() => (data?.discharge.points ?? []).map((p) => ({ day: dayLabel(p.time), m3s: p.value })), [data])

  const offline = data && (data.rain.status === 'SAMPLE' || data.discharge.status === 'SAMPLE')
  const degraded = data && fresh && (isOld(data.rain, fresh.receivedAt) || isOld(data.discharge, fresh.receivedAt))

  return (
    <>
      <h1 className="font-display text-[clamp(2.25rem,5vw,3.5rem)] leading-tight tracking-[-0.02em] text-ink-950">{t('dashboard.title')}</h1>
      <div className="mt-6">
        <AreaPicker areas={areas} areaId={areaId} onChange={setAreaId} />
      </div>

      {(areaError || error) && (
        <Banner tone="error">
          {t('common.backendDown')} ({areaError ?? error})
        </Banner>
      )}
      {!data && !error && !areaError && <p className="mt-6 text-ink-600">{t('common.loading')}</p>}

      {data && (
        <>
          {offline && <Banner tone="warn">{t('dashboard.sampleWarning')}</Banner>}
          {degraded && !offline && <Banner tone="info">{t('dashboard.cachedNote')}</Banner>}

          <Card title={t('dashboard.rainTitle')}>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={rainByDay}>
                <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} vertical={false} />
                <XAxis dataKey="day" stroke={CHART.axis} tickLine={false} />
                <YAxis unit=" mm" width={64} stroke={CHART.axis} tickLine={false} />
                <Tooltip
                  cursor={{ fill: '#f5f5f5' }}
                  formatter={(v, name) => (name === 'mm' ? [`${v} mm`, t('dashboard.rain')] : [`${v} %`, t('dashboard.rainChance')])}
                />
                <Bar dataKey="mm" fill={CHART.rain} radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
            <SourceLine series={data.rain} />
          </Card>

          <Card title={t('dashboard.riverTitle', { river: data.area.riverName })}>
            {data.discharge.status === 'UNAVAILABLE' ? (
              <p className="text-ink-800">{t('dashboard.noRiver')}</p>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={discharge}>
                  <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} vertical={false} />
                  <XAxis dataKey="day" stroke={CHART.axis} tickLine={false} />
                  <YAxis unit=" m³/s" width={84} stroke={CHART.axis} tickLine={false} />
                  <Tooltip formatter={(v) => [`${v} m³/s`, t('dashboard.discharge')]} />
                  <Line type="monotone" dataKey="m3s" stroke={CHART.river} strokeWidth={2} dot={{ r: 3, fill: CHART.river }} />
                </LineChart>
              </ResponsiveContainer>
            )}
            <SourceLine series={data.discharge} />
          </Card>

          <p className="mt-6 text-sm leading-relaxed text-ink-600">{t('common.disclaimer')}</p>
        </>
      )}
    </>
  )
}
