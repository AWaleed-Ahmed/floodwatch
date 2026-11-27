# FloodWatch – Handoff

Status as of **2026-11-23** (`main` after PR #11). Read this first if you are picking the project up. Then read `AGENT.md` and `CODING_RULE.md` before you change anything, and `docs/CONTEXT_AND_PLAN.md` before any UI work.

FloodWatch is the Software Construction PBL project (MCS, NUST) of Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari and Faheem Ahmad Khan. It is a flood monitoring and response web app for the Swat–Kabul river corridor (Mingora, Charsadda, Nowshera). It is an **educational prototype**, never an official warning service. Phase 2 and Phase 3 are both due **3 December 2026**.

## 1. Where things stand

| Area | State |
|---|---|
| Backend (`api/`) | **All 7 PBL features built**, plus login, roles, admin, support and storage hardening. 231 tests pass and the style check reports 0 violations. |
| Frontend (`frontend/`) | React 19, Vite and Tailwind CSS 4, with the new monochrome theme (white-to-graphite gradient, Clash Display and Manrope, teal accent). Landing page (WebGL contour terrain, morphing Menu pill with EN/Urdu switch), Dashboard, Shelter map and Safety Info are built. Risk, Alerts, Incidents, Relief and Login are placeholders. There is no Support button or admin screens yet. The old `web/` app was retired once its pages moved here. |
| Storage | Supabase Storage bucket `floodwatch-data` (private) when `SUPABASE_URL` and `SUPABASE_SERVICE_KEY` are set; otherwise the local `data/` folder. |
| CI | GitHub Actions on every PR and on `main`: Java style check, `./mvnw test`, frontend type check and build. |
| Deployment | Not done yet (see section 6). |

## 2. Run it

```bash
cd api && ./mvnw spring-boot:run        # http://localhost:8090/api/health
cd frontend && npm install && npm run dev    # http://localhost:5173
cd api && ./mvnw test                   # all backend tests
python3 scripts/check-style.py          # must print "0 violations"
```

- Settings come from the root `.env`. It is ignored by git; copy `.env.example` to make one. The backend reads it through `spring.config.import`, and Vite only exposes variables that start with `VITE_`.
- The backend runs on **port 8090**, because 8080 was already taken on the development machine.
- Demo accounts are listed in `README.md` under "Demo accounts": `resident1`, `resident2`, `responder1`, `coordinator1` and `admin1`.
- To run without touching Supabase: `SUPABASE_URL= LOCAL_DATA_DIR=<copy of data/> ./mvnw spring-boot:run`.

## 3. Architecture

A hybrid of three styles: **client–server** (browser to API), **MVC** (React views, Spring controllers, Java models plus JSON files) and **layered** inside the backend. Each layer only calls the one below it: `controller → service → integration / repository`.

| Package (`api/src/main/java/com/floodwatch/`) | Contents |
|---|---|
| `config` | `AppProperties` (all `floodwatch.*` settings), `HttpConfig` (shared RestClient with timeouts), `WebConfig` (CORS and the auth interceptor), `ClockConfig`, `RequestSizeFilter` (64 KB body limit) |
| `controller` | REST endpoints, request/response records, and `ApiExceptionHandler` with the single `ApiError` format |
| `service` | Business logic, grouped as `alerts/`, `incidents/`, `relief/`, `risk/` and `admin/`. Also `AuthService`, `FloodDataFacade`, `ShelterService`, `SupportService`, and the `ApiException` hierarchy (400/401/403/404/409/413/422/429/503) |
| `integration` | Open-Meteo adapters, caching proxies, `SampleDataProvider` and `JsonValidator` |
| `repository` | `JsonFileRepository<T>` plus one repository per entity; `LocalFileStorage` / `SupabaseStorageClient`; `StartupDataCheck` |
| `security` | `@RequiresRole`, `AuthInterceptor`, `BearerToken`, `CurrentUser` |
| `model` | Records and enums (`Area`, `ForecastSeries`, `RiskAssessment`, `Alert`, `Incident`, `Supply`, ...) |

**Design patterns in the code** (these match the Phase 2 report):

| Pattern | Where |
|---|---|
| Facade | `FloodDataFacade` |
| Adapter | `OpenMeteoWeatherAdapter`, `OpenMeteoFloodAdapter`, `OpenMeteoFloodHistoryAdapter` |
| Proxy | `CachingWeatherProxy` and `CachingRiverProxy`, with the shared logic in `SeriesCache` |
| Chain of Responsibility | `RiskRule`, `HighDischargeRule`, `HeavyRainRule`, `BaselineDefaultRule` |
| Observer | `AlertService` with `AlertListener`; `IncidentService` with `IncidentListener` (`ReliefReleaseListener`) |
| Builder | `Incident.Builder` |
| Singleton | `SupabaseStorageClient` and every Spring bean |
| Repository | `Repository<T, ID>`, `JsonFileRepository<T>` |
| State machine | `IncidentStatus.canMoveTo` |

## 4. API (37 endpoints)

All JSON. Errors are always `{error, status, path, timestamp, details}`. Protected endpoints need `Authorization: Bearer <token>` from `POST /api/auth/login`.

| Method | Path | Who |
|---|---|---|
| GET | `/api/health`, `/api/areas`, `/api/dashboard?areaId=`, `/api/shelters?areaId=`, `/api/support` | public |
| GET | `/api/risk?areaId=&date=`, `/api/risk/week?areaId=` | public |
| POST | `/api/auth/login`, `/api/auth/logout` | public |
| GET | `/api/auth/me` | any logged-in user |
| POST, GET, DELETE | `/api/subscriptions`, `/api/subscriptions/mine`, `/api/subscriptions/{id}` | RESIDENT, ADMIN |
| GET | `/api/alerts/mine?since=` | RESIDENT, ADMIN |
| GET | `/api/incidents?status=`, `/api/incidents/{id}`, `/api/teams` | RESPONDER, COORDINATOR, ADMIN |
| POST, PATCH | `/api/incidents`, `/api/incidents/{id}/status` | RESPONDER, ADMIN |
| GET | `/api/supplies`, `/api/allocations?incidentId=`, `/api/volunteers` (phone numbers hidden from responders) | RESPONDER, COORDINATOR, ADMIN |
| POST, PATCH | `/api/supplies`, `/api/supplies/{id}`, `/api/allocations`, `/api/allocations/{id}/deliver`, `/api/volunteers` | COORDINATOR, ADMIN |
| DELETE | `/api/supplies/{id}` | ADMIN |
| GET, PUT | `/api/admin/alert-rules`, `/api/admin/alert-rules/{areaId}` | ADMIN |
| POST | `/api/admin/alerts/demo` | ADMIN |
| POST, PUT, DELETE | `/api/admin/areas`, `/api/admin/areas/{id}`, `/api/admin/shelters`, `/api/admin/shelters/{id}` | ADMIN |

## 5. What has been done (PR history)

| PR | What |
|---|---|
| #1 | Team Java style applied everywhere, `scripts/check-style.py`, CI workflow |
| #2 | Shared error format, validation, 64 KB request limit, injectable `Clock`, a test profile that never touches Supabase |
| #3 | Login and roles: seeded BCrypt accounts, tokens (8 h), `@RequiresRole`, a 429 after 5 failed logins, the same 401 for an unknown user or a wrong password |
| #5 | `docs/CONTEXT_AND_PLAN.md`, the theming context for the designer |
| #6 | Risk levels: monsoon baseline (median of the last 5 complete Jul–Sep seasons, cached for 30 days), the rule chain, PKT day boundaries, and "never guess on sample or unavailable data" (replaced #4) |
| #7 | Alerts: subscriptions, a 15-minute scheduler with a 6 h cooldown and immediate escalation, DEMO alerts (1 per area per minute), 30-day retention, Observer listeners |
| #8 | Incidents: Builder, teams, the Open → Assigned → Resolved state machine, admin-only false-alarm resolve, optimistic locking (409), append-only history, the listener hook |
| #9 | Relief: supplies, allocations (reserve, deliver, release on resolve), atomic stock checks, undo of a half-finished write, a repair job for stuck reservations, phone privacy |
| #10 | Admin for shelters and areas (refusing to delete an area that is in use), `GET /api/support` with `SUPPORT_EMAIL` |
| #11 | Storage hardening: damaged files are reported and never overwritten, a startup data check, a 5 MB file cap, and retry after an outage |

Live checks were run against the real Open-Meteo APIs before each merge. For example, Nowshera's monsoon baseline is 1,022 m³/s from 460 days of 2022–2026 data.

## 6. What is left

**Frontend (needs the designer's theme first):**
1. API client: login and token storage, the Bearer header, POST/PATCH/PUT/DELETE helpers, and handling 401 (log in again) and 403 (wrong role).
2. Pages:
   - Risk: the badge, the reason, the 7-day strip and the FFD link.
   - Alerts: subscribe, the inbox, and browser notifications by polling `/api/alerts/mine` every 60 s.
   - Incidents board.
   - Relief.
   - Login.
   - Admin screens: shelters, areas, alert rules and the demo alert.
   - The Support button, which reads `/api/support`.
3. Apply the theme to every page: light and dark mode, the Urdu right-to-left layout and font.

**Testing extras:**
- JaCoCo coverage report.
- `api/requests.http`, with every endpoint and edge case ready for the VS Code REST Client extension (add it to the recommended extensions).
- A README section on running and testing.

**Deployment:**
1. Hosting: a Dockerfile for the backend on Render (or Railway), the frontend on Vercel, and CORS set to the Vercel domain.
2. Upload every `data/*.json` seed file to the Supabase bucket. Only `areas.json` and `shelters.json` are there today, so **login will not work against Supabase until `users.json` is uploaded**.
3. Rotate the Supabase secret key before deploying, then set the environment variables on the host.

**Phase 3 report (due 3 Dec):** screenshots of every feature, a user manual (install, set up, run in the IDE and outside it), and the test results.

## 7. Known limitations (by design for a prototype)

- Tokens live in memory, so a backend restart logs everyone out. There is a single backend instance.
- Notifications only arrive while the site is open (polling plus the browser Notifications API). There is no Web Push.
- Free hosting tiers sleep when idle, so the first request after a pause is slow.
- Risk levels are educational comparisons with the monsoon baseline, not official warnings.

## 8. Ownership (Phase 3)

| Member | Area |
|---|---|
| Abdullah Waleed Ahmed (509666) | Risk and alert rules, seed data, report integration |
| Muhammad Rehan Haider (508160) | Spring Boot, Open-Meteo integration, JUnit tests |
| Faheem Ahmad Khan (534662) | React pages, charts, map, notifications, Urdu/English |
| Syed Ehtisham Haider Bukhari (511754) | Storage, incidents, relief, auth, deployment, user manual |
