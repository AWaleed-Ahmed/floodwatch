# FloodWatch

Flood monitoring and response prototype for the Software Construction PBL (MCS, NUST).
Design: see the Phase 2 report. Java (Spring Boot) backend + React/TypeScript frontend.

```
floodwatch/
├── .env            shared settings (API URLs, ports, Supabase keys) – never commit
├── .env.example    same file without secrets – safe to commit
├── data/           JSON storage used until Supabase is configured
├── api/            Spring Boot 4.2, Java 17  (controller → service → integration / repository)
└── web/            React 19 + TypeScript + Vite
```

## Run it (two terminals)

Backend – http://localhost:8090/api/health
```bash
cd api && ./mvnw spring-boot:run
```

Frontend – http://localhost:5173
```bash
cd web && npm install && npm run dev
```

Tests: `cd api && ./mvnw test`

## What works now
| Feature | Status | Owner |
|---|---|---|
| 1. Dashboard (live Open-Meteo rain + GloFAS discharge, cache, sample fallback) | Working | Rehan / Faheem |
| 4. Shelter map (Leaflet + OSM, seeded sample shelters) | Working | Faheem |
| 7. Urdu / English safety info (RTL) | Working – Urdu needs a fluent review | Faheem |
| 2. Risk levels (RiskRule chain) | To build | Abdullah |
| 3. Alerts + browser notifications | To build | Abdullah / Faheem |
| 5. Incidents (state machine, Builder) – `IncidentStatus` done | To build | Ehtisham |
| 6. Relief & volunteers | To build | Ehtisham |
| Login with seeded demo accounts | To build | Ehtisham |

## Storage
With `SUPABASE_URL` and `SUPABASE_SERVICE_KEY` empty in `.env`, the backend reads and writes `data/*.json`.
Fill both in (and create the private bucket `floodwatch-data`, then upload the files from `data/`) to switch to Supabase.

## Notes
- Port 8090 is used because 8080 is often taken; change `SERVER_PORT` and `VITE_API_BASE_URL` in `.env` together.
- All weather/river/map APIs are free and keyless. Forecasts are educational only, not official warnings.
