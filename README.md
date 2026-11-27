# FloodWatch

Flood monitoring and response prototype for the Software Construction PBL (MCS, NUST).
Design: see the Phase 2 report. Java (Spring Boot) backend + React/TypeScript frontend.

```
floodwatch/
├── .env            shared settings (API URLs, ports, Supabase keys) – never commit
├── .env.example    same file without secrets – safe to commit
├── data/           JSON storage used until Supabase is configured
├── api/            Spring Boot 4.2, Java 17  (controller → service → integration / repository)
└── frontend/       React 19 + TypeScript + Vite + Tailwind CSS 4
```

## Project docs
- [`docs/HANDOFF.md`](docs/HANDOFF.md) – what is built, every endpoint, what is left
- [`CODING_RULE.md`](CODING_RULE.md) – Java style, engineering and testing rules
- [`AGENT.md`](AGENT.md) – rules for AI agents and automation (no agent credit, no agent branch prefixes)
- [`docs/CONTEXT_AND_PLAN.md`](docs/CONTEXT_AND_PLAN.md) – product context and theming plan for the UI

## Run it (two terminals)

Backend – http://localhost:8090/api/health
```bash
cd api && ./mvnw spring-boot:run
```

Frontend – http://localhost:5173
```bash
cd frontend && npm install && npm run dev
```

Tests: `cd api && ./mvnw test`

Style check (must print 0 violations): `python3 scripts/check-style.py`

## Java code style
Every Java file follows the team rules, enforced by `scripts/check-style.py` and CI:
- PascalCase types and files, camelCase methods and variables, UPPER_SNAKE_CASE constants.
- Parameters: name on its own line, then `(`, parameters indented and grouped by type, then `) {`.
- Allman braces for `if`/`else`/`for`/`while`/`try`/`catch`/`finally`; declarations keep `{` on the same line.
- File header + Javadoc on every class, constructor and method; no `//` comments.
- 2 blank lines after the header, around class Javadoc and after every Javadoc; files end at the final `}`.

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

## Demo accounts
Log in with `POST /api/auth/login`. Usernames are not case-sensitive. Passwords are case-sensitive.

| Username | Password | Role |
|---|---|---|
| resident1 | resident1-demo | RESIDENT |
| responder1 | responder1-demo | RESPONDER (team-01) |
| coordinator1 | coordinator1-demo | COORDINATOR |
| admin1 | admin1-demo | ADMIN |
| resident2 | resident2-demo | RESIDENT |

## Notes
- Port 8090 is used because 8080 is often taken; change `SERVER_PORT` and `VITE_API_BASE_URL` in `.env` together.
- All weather/river/map APIs are free and keyless. Forecasts are educational only, not official warnings.
