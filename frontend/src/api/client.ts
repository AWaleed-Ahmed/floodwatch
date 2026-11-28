import type { Area, Dashboard, RiskAssessment, Shelter, SupportContact } from './types'

const BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090/api'

export class ApiError extends Error {
  readonly status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(BASE + path, { headers: { Accept: 'application/json' } })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new ApiError(res.status, body.error ?? res.statusText)
  }
  return res.json() as Promise<T>
}

export const api = {
  areas: () => get<Area[]>('/areas'),
  dashboard: (areaId: string) => get<Dashboard>(`/dashboard?areaId=${encodeURIComponent(areaId)}`),
  shelters: (areaId?: string) => get<Shelter[]>(`/shelters${areaId ? `?areaId=${encodeURIComponent(areaId)}` : ''}`),
  support: () => get<SupportContact>('/support'),
  risk: (areaId: string) => get<RiskAssessment>(`/risk?areaId=${encodeURIComponent(areaId)}`),
  riskWeek: (areaId: string) => get<RiskAssessment[]>(`/risk/week?areaId=${encodeURIComponent(areaId)}`),
}
