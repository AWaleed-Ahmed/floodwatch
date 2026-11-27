// Mirrors the Java model classes in api/src/main/java/com/floodwatch/model

export type DataStatus = 'LIVE' | 'CACHED' | 'SAMPLE' | 'UNAVAILABLE'
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'
export type ShelterStatus = 'OPEN' | 'FULL' | 'CLOSED' | 'UNCONFIRMED'

export interface Area {
  id: string
  name: string
  latitude: number
  longitude: number
  riverName: string
}

export interface ForecastPoint {
  time: string
  value: number | null
  probability: number | null
}

export interface ForecastSeries {
  areaId: string
  source: string
  unit: string
  fetchedAt: string
  status: DataStatus
  points: ForecastPoint[]
}

export interface Dashboard {
  area: Area
  rain: ForecastSeries
  discharge: ForecastSeries
  disclaimer: string
}

export interface Shelter {
  id: string
  areaId: string
  name: string
  latitude: number
  longitude: number
  status: ShelterStatus
  contact: string
  capacity: number
  sample: boolean
}

/** GET /api/support: where to send website help (set by SUPPORT_EMAIL on the server). */
export interface SupportContact {
  email: string
  subject: string
  mailto: string
}
