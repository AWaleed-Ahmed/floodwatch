import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Area,
  Bar,
  CartesianGrid,
  ComposedChart,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { api } from '../api/client'
import type { Dashboard, ForecastSeries, RiskAssessment, Shelter } from '../api/types'
import { AreaPicker } from '../components/AreaPicker'
import { Banner } from '../components/Banner'
import { transitionTo } from '../components/PageTransition'
import { RiskBadge } from '../components/RiskBadge'
import { SourceLine } from '../components/SourceLine'
import { StatusBadge } from '../components/StatusBadge'
import { useSelectedArea } from '../hooks/useSelectedArea'
import { useTheme } from '../theme'

/** A CACHED series younger than the backend's cache time is just a fast answer, not a problem. */
const STALE_MS = 30 * 60 * 1000

/** Show the "normal monsoon level" line only when the river is high enough for it to fit the chart usefully. */
const SHOW_BASELINE_FROM = 0.5

const FFD_URL = import.meta.env.VITE_PMD_FFD_URL ?? 'https://ffd.pmd.gov.pk/'

/** Chart colours for each theme (Recharts takes plain colour values). */
const CHARTS = {
  light: { rain: '#009689', rainTop: '#2dd4bf', rainBottom: '#00796e', riverArea: '#0a0a0a', riverAreaOpacity: 0.1, chance: '#2b2b2b', river: '#0a0a0a', normal: '#b45309', grid: '#e4e4e4', axis: '#5c5c5c', cursor: '#f5f5f5', tooltip: '#ffffff', tooltipText: '#0a0a0a', tooltipBorder: '#e4e4e4' },
  dark: { rain: '#22c3ad', rainTop: '#5eead4', rainBottom: '#0f766e', riverArea: '#3fd9c3', riverAreaOpacity: 0.22, chance: '#d3d6da', river: '#eceef0', normal: '#fbbf24', grid: '#343a40', axis: '#a3a8ae', cursor: '#262a2f', tooltip: '#262a2f', tooltipText: '#eceef0', tooltipBorder: '#4a5057' },
}

interface Result {
  areaId: string
  receivedAt: number
  data?: Dashboard
  error?: string
  risk?: RiskAssessment[]
  shelters?: Shelter[]
}

const dayLabel = (time: string) => new Date(time).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' })
const dayName = (date: string) => new Date(`${date}T12:00:00`).toLocaleDateString(undefined, { weekday: 'short' })
const dayOfMonth = (date: string) => new Date(`${date}T12:00:00`).getDate()

/** Risk days are Pakistan days, so hourly forecast times are grouped by the date in Asia/Karachi. */
const pakistanDate = (time: string) => new Date(time).toLocaleDateString('en-CA', { timeZone: 'Asia/Karachi' })

const LEVEL_ORDER = { LOW: 1, MEDIUM: 2, HIGH: 3 } as const
const number = (value: number, digits = 0) => value.toLocaleString(undefined, { maximumFractionDigits: digits })

const isOld = (series: ForecastSeries, now: number) =>
  series.status === 'CACHED' && now - new Date(series.fetchedAt).getTime() > STALE_MS

const directions = (s: Shelter) => `https://www.openstreetmap.org/directions?route=%3B${s.latitude}%2C${s.longitude}`

function Card({ title, aside, children, className = '' }: { title: string; aside?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`rounded-3xl border border-ink-100 bg-paper p-5 sm:p-6 ${className}`}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="font-display text-xl tracking-[-0.01em] text-ink-950 ur:leading-[1.8] ur:tracking-normal">{title}</h2>
        {aside}
      </div>
      <div className="mt-4">{children}</div>
    </section>
  )
}

function Metric({ label, value, note }: { label: string; value: ReactNode; note?: ReactNode }) {
  return (
    <div className="rounded-2xl bg-ink-50 px-4 py-3">
      <p className="text-sm text-ink-600">{label}</p>
      <p className="mt-1 font-display text-2xl text-ink-950 tabular-nums">{value}</p>
      {note && <p className="mt-0.5 text-sm text-ink-600">{note}</p>}
    </div>
  )
}

/** Feature 1: Flood Monitoring Dashboard (UC-01), with today's risk (UC-02), nearest shelter (UC-04) and official help. */
export function DashboardPage() {
  const { t } = useTranslation()
  const { areas, areaId, setAreaId, error: areaError } = useSelectedArea()
  const CHART = CHARTS[useTheme()]
  const tooltipStyle = { contentStyle: { background: CHART.tooltip, borderColor: CHART.tooltipBorder, borderRadius: 12, color: CHART.tooltipText }, labelStyle: { color: CHART.tooltipText } }
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    if (!areaId) return
    let current = true
    /* The forecast is required; risk and shelters are extras, so their failures don't block the page. */
    Promise.allSettled([api.dashboard(areaId), api.riskWeek(areaId), api.shelters(areaId)]).then(([dashboard, risk, shelters]) => {
      if (!current) return
      setResult({
        areaId,
        receivedAt: Date.now(),
        data: dashboard.status === 'fulfilled' ? dashboard.value : undefined,
        error: dashboard.status === 'rejected' ? (dashboard.reason as Error).message : undefined,
        risk: risk.status === 'fulfilled' ? risk.value : undefined,
        shelters: shelters.status === 'fulfilled' ? shelters.value : undefined,
      })
    })
    return () => {
      current = false
    }
  }, [areaId])

  const fresh = result && result.areaId === areaId ? result : null
  const data = fresh?.data ?? null
  const error = fresh?.error ?? null
  const risk = fresh?.risk ?? null
  const today = risk?.[0] ?? null

  /** Rain is hourly; sum it per day (and keep the day's highest rain chance) so a week fits on a phone screen. */
  const rainByDay = useMemo(() => {
    const days = new Map<string, { day: string; mm: number; chance: number }>()
    for (const p of data?.rain.points ?? []) {
      const day = dayLabel(p.time)
      const row = days.get(day) ?? { day, mm: 0, chance: 0 }
      row.mm = Math.round((row.mm + (p.value ?? 0)) * 10) / 10
      row.chance = Math.max(row.chance, p.probability ?? 0)
      days.set(day, row)
    }
    return [...days.values()]
  }, [data])

  const discharge = useMemo(() => (data?.discharge.points ?? []).map((p) => ({ day: dayLabel(p.time), m3s: p.value })), [data])

  /** Highest rain chance for each Pakistan day, for the 7-day cards. */
  const chanceByDate = useMemo(() => {
    const map = new Map<string, number>()
    for (const p of data?.rain.points ?? []) {
      const date = pakistanDate(p.time)
      map.set(date, Math.max(map.get(date) ?? 0, p.probability ?? 0))
    }
    return map
  }, [data])

  /** The week's highest known risk (first day it happens) and its wettest day, for the summary line. */
  const peakDay = (risk ?? []).reduce<RiskAssessment | null>(
    (best, day) => (day.level && (!best?.level || LEVEL_ORDER[day.level] > LEVEL_ORDER[best.level]) ? day : best),
    null,
  )
  const wettestRiskDay = (risk ?? []).reduce<RiskAssessment | null>(
    (best, day) => ((day.rainMm ?? 0) > (best?.rainMm ?? 0) ? day : best),
    null,
  )
  const dayTitle = (day: RiskAssessment, index: number) => (index === 0 ? t('dashboard.today') : dayName(day.date))

  const wettest = rainByDay.reduce<(typeof rainByDay)[number] | null>((best, row) => (!best || row.mm > best.mm ? row : best), null)
  const baseline = today?.baseline ?? null
  const highestFlow = Math.max(0, ...discharge.map((row) => row.m3s ?? 0))
  const showBaseline = baseline !== null && highestFlow >= baseline * SHOW_BASELINE_FROM

  const shelters = fresh?.shelters ?? []
  const nearestOpen = shelters.find((s) => s.status === 'OPEN') ?? null

  const offline = data && (data.rain.status === 'SAMPLE' || data.discharge.status === 'SAMPLE')
  const degraded = data && fresh && (isOld(data.rain, fresh.receivedAt) || isOld(data.discharge, fresh.receivedAt))

  return (
    <>
      <h1 className="font-display text-[clamp(2.25rem,5vw,3.5rem)] leading-tight tracking-[-0.02em] text-ink-950 ur:leading-[1.8] ur:tracking-normal">
        {t('dashboard.title')}
      </h1>
      <div className="mt-6">
        <AreaPicker areas={areas} areaId={areaId} onChange={setAreaId} />
      </div>

      {(areaError || error) && (
        <Banner tone="error">
          {t('common.backendDown')} ({areaError ?? error})
        </Banner>
      )}
      {!fresh && !areaError && <p className="mt-6 text-ink-600">{t('common.loading')}</p>}

      {data && (
        <>
          {offline && <Banner tone="warn">{t('dashboard.sampleWarning')}</Banner>}
          {degraded && !offline && <Banner tone="info">{t('dashboard.cachedNote')}</Banner>}

          {/* 1. Today's risk and the week ahead */}
          <Card
            className="mt-6"
            title={t('dashboard.riskTitle')}
            aside={today && <StatusBadge status={today.dataStatus} />}
          >
            {today ? (
              <>
                <div className="flex flex-wrap items-center gap-4">
                  <RiskBadge level={today.level} size="lg" />
                  <p className="max-w-2xl text-lg leading-relaxed text-ink-800">{today.reason}</p>
                </div>
                <div className="mt-6 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                  <h3 className="text-sm font-medium text-ink-600">{t('dashboard.weekTitle')}</h3>
                  <p className="text-sm text-ink-600">
                    {peakDay?.level
                      ? t('dashboard.weekPeak', { level: t(`riskLevel.${peakDay.level}`), day: dayTitle(peakDay, risk!.indexOf(peakDay)) })
                      : t('dashboard.weekPeakUnknown')}
                    {wettestRiskDay && (wettestRiskDay.rainMm ?? 0) > 0 && (
                      <>
                        {' · '}
                        {t('dashboard.weekWettest', {
                          day: dayTitle(wettestRiskDay, risk!.indexOf(wettestRiskDay)),
                          mm: number(wettestRiskDay.rainMm ?? 0, 1),
                        })}
                      </>
                    )}
                  </p>
                </div>
                <ol className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
                  {risk!.map((day, index) => {
                    const chance = chanceByDate.get(day.date)
                    return (
                      <li
                        key={day.date}
                        title={day.reason}
                        className={`flex flex-col gap-2 rounded-2xl px-3 py-3 ${
                          index === 0 ? 'bg-ink-950 text-paper' : 'bg-ink-50 text-ink-950'
                        }`}
                      >
                        <div className="flex items-baseline justify-between gap-2">
                          <span className="text-sm font-semibold">{dayTitle(day, index)}</span>
                          <span className={`text-xs tabular-nums ${index === 0 ? 'text-paper/70' : 'text-ink-600'}`}>{dayOfMonth(day.date)}</span>
                        </div>
                        <RiskBadge level={day.level} />
                        <dl className={`grid grid-cols-[auto_1fr] gap-x-2 gap-y-0.5 text-xs tabular-nums ${index === 0 ? 'text-paper/80' : 'text-ink-600'}`}>
                          <dt>{t('dashboard.dayRain')}</dt>
                          <dd className="text-end font-medium">{day.rainMm != null ? `${number(day.rainMm, 1)} mm` : '–'}</dd>
                          <dt>{t('dashboard.dayChance')}</dt>
                          <dd className="text-end font-medium">{chance != null ? `${Math.round(chance)}%` : '–'}</dd>
                          <dt>{t('dashboard.dayRiver')}</dt>
                          <dd className="text-end font-medium">
                            {day.ratio != null
                              ? `${number(day.ratio, 1)}×`
                              : day.discharge != null
                                ? `${number(day.discharge)} m³/s`
                                : '–'}
                          </dd>
                        </dl>
                      </li>
                    )
                  })}
                </ol>
                <p className="mt-2 text-xs text-ink-600">{t('dashboard.weekNote')}</p>
              </>
            ) : (
              <p className="text-ink-800">{t('dashboard.riskUnavailable')}</p>
            )}
          </Card>

          {/* 2. Key numbers */}
          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            <Metric label={t('dashboard.rainToday')} value={`${number(rainByDay[0]?.mm ?? 0, 1)} mm`} />
            <Metric
              label={t('dashboard.riverToday')}
              value={today?.discharge != null ? `${number(today.discharge)} m³/s` : '–'}
              note={today?.ratio != null ? t('dashboard.timesNormal', { ratio: number(today.ratio, 1) }) : t('dashboard.noBaseline')}
            />
            <Metric
              label={t('dashboard.wettestDay')}
              value={wettest && wettest.mm > 0 ? wettest.day : t('dashboard.noRainWeek')}
              note={wettest && wettest.mm > 0 ? `${number(wettest.mm, 1)} mm` : undefined}
            />
          </div>

          {/* 3. Rain with rain chance */}
          <Card className="mt-6" title={t('dashboard.rainTitle')}>
            <ResponsiveContainer width="100%" height={260}>
              <ComposedChart data={rainByDay}>
                <defs>
                  <linearGradient id="fw-rain-fill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={CHART.rainTop} />
                    <stop offset="100%" stopColor={CHART.rainBottom} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} vertical={false} />
                <XAxis dataKey="day" stroke={CHART.axis} tickLine={false} />
                <YAxis yAxisId="mm" unit=" mm" width={64} stroke={CHART.axis} tickLine={false} />
                <YAxis yAxisId="chance" orientation="right" unit=" %" domain={[0, 100]} width={52} stroke={CHART.axis} tickLine={false} />
                <Tooltip
                  {...tooltipStyle}
                  cursor={{ fill: CHART.cursor }}
                  formatter={(v, name) => (name === 'mm' ? [`${v} mm`, t('dashboard.rain')] : [`${v} %`, t('dashboard.rainChance')])}
                />
                <Bar yAxisId="mm" dataKey="mm" fill="url(#fw-rain-fill)" radius={[6, 6, 0, 0]} />
                <Line yAxisId="chance" type="monotone" dataKey="chance" stroke={CHART.chance} strokeWidth={1.5} strokeDasharray="4 4" dot={false} />
              </ComposedChart>
            </ResponsiveContainer>
            <p className="mt-2 flex flex-wrap gap-4 text-sm text-ink-600">
              <span className="inline-flex items-center gap-1.5">
                <span aria-hidden="true" className="size-2.5 rounded-sm" style={{ background: `linear-gradient(${CHART.rainTop}, ${CHART.rainBottom})` }} />
                {t('dashboard.rain')} (mm)
              </span>
              <span className="inline-flex items-center gap-1.5">
                <span aria-hidden="true" className="w-4 border-t-2 border-dashed" style={{ borderColor: CHART.chance }} />
                {t('dashboard.rainChance')} (%)
              </span>
            </p>
            <SourceLine series={data.rain} />
          </Card>

          {/* 4. River flow against the normal monsoon level */}
          <Card className="mt-6" title={t('dashboard.riverTitle', { river: data.area.riverName })}>
            {data.discharge.status === 'UNAVAILABLE' ? (
              <p className="text-ink-800">{t('dashboard.noRiver')}</p>
            ) : (
              <>
                <ResponsiveContainer width="100%" height={260}>
                  <ComposedChart data={discharge}>
                    <defs>
                      <linearGradient id="fw-river-fill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor={CHART.riverArea} stopOpacity={CHART.riverAreaOpacity} />
                        <stop offset="100%" stopColor={CHART.riverArea} stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} vertical={false} />
                    <XAxis dataKey="day" stroke={CHART.axis} tickLine={false} />
                    <YAxis unit=" m³/s" width={84} stroke={CHART.axis} tickLine={false} />
                    <Tooltip {...tooltipStyle} formatter={(v) => [`${v} m³/s`, t('dashboard.discharge')]} />
                    {showBaseline && (
                      <ReferenceLine
                        y={baseline!}
                        ifOverflow="extendDomain"
                        stroke={CHART.normal}
                        strokeDasharray="6 4"
                        label={{ value: t('dashboard.normalLevel'), position: 'insideTopRight', fill: CHART.normal, fontSize: 12 }}
                      />
                    )}
                    <Area type="monotone" dataKey="m3s" stroke="none" fill="url(#fw-river-fill)" tooltipType="none" />
                    <Line type="monotone" dataKey="m3s" stroke={CHART.river} strokeWidth={2} dot={{ r: 3, fill: CHART.river }} />
                  </ComposedChart>
                </ResponsiveContainer>
                {baseline !== null && !showBaseline && (
                  <p className="mt-2 text-sm text-ink-600">{t('dashboard.normalCaption', { value: number(baseline) })}</p>
                )}
              </>
            )}
            <SourceLine series={data.discharge} />
          </Card>

          {/* 5 and 6. Nearest open shelter and official help */}
          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <Card title={t('dashboard.shelterTitle')}>
              {nearestOpen ? (
                <>
                  <p className="font-display text-lg text-ink-950">{nearestOpen.name}</p>
                  {nearestOpen.sample && (
                    <p className="mt-1 inline-block rounded bg-amber-50 px-1.5 text-xs font-semibold text-amber-900">{t('shelters.sampleTag')}</p>
                  )}
                  <p className="mt-1 text-ink-600">
                    {t('shelters.capacity')}: {t('dashboard.people', { count: nearestOpen.capacity })} · {nearestOpen.contact}
                  </p>
                  <a
                    href={directions(nearestOpen)}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-3 inline-flex h-10 items-center rounded-full bg-ink-950 px-4 text-sm font-semibold text-paper outline-none focus-visible:ring-2 focus-visible:ring-teal-500 focus-visible:ring-offset-2"
                  >
                    {t('shelters.directions')}
                  </a>
                </>
              ) : (
                <p className="text-ink-800">{t('dashboard.shelterNone')}</p>
              )}
              <button
                type="button"
                onClick={() => transitionTo('/shelters')}
                className="mt-4 block text-sm font-semibold text-ink-950 underline decoration-ink-950/30 underline-offset-4 hover:decoration-ink-950"
              >
                {t('dashboard.shelterAll')} →
              </button>
            </Card>

            <Card title={t('dashboard.helpTitle')}>
              <div className="flex flex-wrap gap-3">
                <a
                  href="tel:1122"
                  className="shine relative inline-flex h-11 items-center overflow-hidden rounded-full bg-linear-to-tr from-teal-600 to-teal-400 px-5 text-sm font-semibold text-ink-950 outline-none focus-visible:ring-2 focus-visible:ring-ink-950"
                >
                  {t('dashboard.call')}
                </a>
                <a
                  href={FFD_URL}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex h-11 items-center rounded-full border border-ink-200 px-5 text-sm font-semibold text-ink-950 outline-none hover:bg-ink-50 focus-visible:ring-2 focus-visible:ring-teal-500"
                >
                  {t('dashboard.ffd')} ↗
                </a>
              </div>
              <p className="mt-4 text-sm leading-relaxed text-ink-600">{t('common.disclaimer')}</p>
            </Card>
          </div>
        </>
      )}
    </>
  )
}
