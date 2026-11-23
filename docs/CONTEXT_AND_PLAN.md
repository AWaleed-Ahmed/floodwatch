# FloodWatch – Context and Theming Plan

This document is the input for choosing a visual theme for FloodWatch. It describes the product, its users, every screen and state, the semantic states that need consistent colour and icons, the constraints, the current frontend, and a plan with open questions.

Sources: this repository (`main` at the time of writing, plus open PR #4 for risk levels) and the Phase 1 proposal and Phase 2 design report. Anything not built yet is marked **planned**.

---

## 1. Product context

**Problem.** People who live near flood-prone rivers in Pakistan find it hard to get timely, understandable information for their own location, because rainfall forecasts, river bulletins, emergency contacts and relief records are spread across separate channels. After the 2025 flash floods, a World Bank survey of 1,207 households in Buner, Shangla and Swat found that 85% received no warning, and half of those who were warned had less than 30 minutes. FloodWatch puts public forecast data, shelter guidance, safety steps and a practice incident and relief workflow on one simple website. It does not predict every flash flood and does not issue official warnings.

**Who it is for.** Residents and families in flood-prone towns (the primary users), plus a small team of staff who practise logging incidents and managing relief: responders, relief coordinators and an administrator.

**Setting.** The Swat–Kabul river corridor in Khyber Pakhtunkhwa, during and around the July–September monsoon. The seeded areas are in `data/areas.json`:

| `id` | `name` | `riverName` |
|---|---|---|
| `mingora` | Mingora (Swat) | Swat River |
| `charsadda` | Charsadda | Swat River |
| `nowshera` | Nowshera | Kabul River |

**Status.** FloodWatch is an educational prototype for the Software Construction PBL (MCS, NUST), built by Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari and Faheem Ahmad Khan. It is **not an official warning service**. Every forecast and risk view must carry a disclaimer pointing to the PMD Flood Forecasting Division (FFD, https://ffd.pmd.gov.pk/) and Rescue 1122.

**Tone the UI must have.**
- **Calm.** It is read by worried people, possibly while water is rising. Nothing should flash, shout or look like a siren.
- **Trustworthy.** Every number shows its source, unit and update time. Old or made-up data is labelled plainly.
- **Clear in an emergency.** The most important thing (risk level, nearest open shelter, "call 1122") is visible first, in large and plain text.
- **Never alarmist.** HIGH risk is serious but not panicky. No red full-screen takeovers and no dramatic imagery.
- **Never "official-looking".** It must not copy the look of PMD, NDMA, PDMA or Rescue 1122. That means no government crests, no green-and-white state branding and no "Government of Pakistan" style headers. It should look like a helpful community tool.

---

## 2. Users and roles

The backend `Role` enum (`api/.../model/Role.java`) has `RESIDENT`, `RESPONDER`, `COORDINATOR` and `ADMIN`. A Visitor is anyone without a token. In the Phase 2 actor hierarchy, every staff role can also do everything a Visitor can.

| Role | Login | What they do | Screens they use |
|---|---|---|---|
| **Visitor** | No | Check rain and river for an area, read the risk level, find shelters, read safety steps, switch language. | Dashboard, Risk, Shelters, Safety Info, Login, Support |
| **Resident** (`resident1`, `resident2`) | Yes | Everything a Visitor does, plus subscribe to an area and receive alerts (browser pop-ups while the site is open, and an inbox). | + Alerts and inbox |
| **Responder** (`responder1`, team `team-01`) | Yes | Log an incident, assign a team, move it Open → Assigned → Resolved. | + Incidents board |
| **Relief Coordinator** (`coordinator1`) | Yes | Record supplies and stock, allocate quantities to incidents, see volunteers including phone numbers. | + Relief |
| **Admin** (`admin1`) | Yes | Trigger DEMO alerts, manage shelters and alert rules, act on incidents (including resolving a false alarm directly from Open). | + Alerts (demo trigger), Incidents, Relief, shelter and rule management (planned) |

Login exists in the backend (`POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`). Tokens last 8 hours and live only in server memory, so **a server restart logs everyone out**. The UI needs a calm "Your session ended, please log in again" state. There is no public sign-up, by design. The login screen in the frontend is **planned**.

---

## 3. Screen inventory

The routes are defined in `web/src/App.tsx`. Every screen shares the top bar (brand, navigation, language switch), the footer disclaimer and the Support button (planned, see 5.6).

The shared states listed below apply to every screen that loads data. They are not repeated for each screen.

| Shared state | Today | Notes |
|---|---|---|
| Loading | Plain text `common.loading` ("Loading…") | No skeleton or spinner yet |
| Backend unreachable | `.banner-error` with `common.backendDown` plus the raw error text in brackets | The raw text is developer-facing |
| Not logged in (401) | planned | Ask to log in, keep the user on the page |
| Wrong role (403) | planned | Explain which role is needed |

### 3.1 Dashboard (`/`) – Feature 1, UC-01 – **built**

**Purpose.** Show the next 7 days of rain and modeled river flow for one area.

**Data.** `GET /api/dashboard?areaId=` returns `DashboardDto`:
- `area`: `{ id, name, latitude, longitude, riverName }`
- `rain`: a `ForecastSeries`, hourly. The fields are `areaId`, `source` (`"Open-Meteo Forecast API"`), `unit` (`"mm"`), `fetchedAt` (ISO instant), `status` (`DataStatus`), and `points[]` of `{ time, value (mm, nullable), probability (%, 0–100, nullable) }`.
- `discharge`: a `ForecastSeries`, daily. `source` is `"Open-Meteo Flood API (GloFAS)"`, `unit` is `"m³/s"`, and `points[].value` is m³/s with `probability: null`.
- `disclaimer`: a string from the backend.

**What it shows.**
- An area picker (`AreaPicker`). The last choice is remembered on the device.
- Card 1, "Rainfall – next 7 days (daily total)": a Recharts bar chart of daily mm. The hourly points are summed per day in the frontend. The daily maximum rain chance is computed, but no chart draws it yet (FR-01 asks for probability to be shown).
- Card 2, "{river} – modeled discharge": a Recharts line chart of m³/s.
- Under each chart, `SourceLine` shows the status badge, then source, unit and updated time.
- The forecast disclaimer.

**Main actions.** Change the area. Read the tooltips.

**States.**

| State | What appears (current text keys) |
|---|---|
| Loading | `common.loading` |
| Areas or dashboard failed | `.banner-error` with `common.backendDown` |
| Any series SAMPLE | `.banner-warn` with `dashboard.sampleWarning`, plus the SAMPLE badge on that chart |
| Any series CACHED and older than 30 min | `.banner-info` with `dashboard.cachedNote` (a fresh CACHED series shows only its badge) |
| Discharge UNAVAILABLE (point is off the GloFAS river grid) | The chart is replaced by `dashboard.noRiver`; the source line stays |
| Individual null values | The rain sum treats null as 0; discharge nulls leave gaps in the line |
| Empty (no areas seeded) | Not handled specially today |

### 3.2 Risk (`/risk`) – Feature 2, UC-02 – **planned** (backend in open PR #4, frontend is a placeholder)

**Purpose.** Give a Low, Medium or High level for a chosen day, explain why in plain words, and point to official bulletins.

**Data (PR #4).** `GET /api/risk?areaId=&date=` returns one `RiskAssessment`. `GET /api/risk/week?areaId=` returns 7 of them.
- `areaId`, `date` (`YYYY-MM-DD`, Pakistan day)
- `level`: `LOW`, `MEDIUM`, `HIGH`, or **`null` (unknown)**
- `discharge` (m³/s), `baseline` (m³/s, the median July–September discharge over the last five seasons), `ratio` (discharge ÷ baseline, a plain number such as 2.3), `rainMm` (mm for the day). Any of these can be `null`.
- `reason`: a plain-language string from the rules, for example "River flow is 2.3× the normal monsoon level.", "Rainfall of 62.0 mm is above 50 mm.", "No river baseline yet; judged on rainfall only.", "Live river data is unavailable." or "Rainfall for this day is unknown, so it was not used."
- `dataStatus`: the weaker of the two series' statuses
- `disclaimer`: "Educational only, not an official warning. Check PMD Flood Forecasting Division bulletins."

**Rules (for context).** A ratio above 2.0 gives HIGH. A ratio above 1.3, or rain above 50 mm, gives MEDIUM. Otherwise the level is LOW. SAMPLE or UNAVAILABLE river data gives `null`, so the app never guesses.

**What it shows (planned, from UC-02).** A day picker for the next 7 days, a large level badge, the reason, the numbers with units, the disclaimer, and an "Open FFD bulletins" link that opens in a new tab. A 7-day strip comes from `/risk/week`.

**States.** Loading. Unknown level (grey "Unknown" badge with the reason). CACHED or SAMPLE `dataStatus` badge. No baseline yet (the reason says so). `400` for a date in the past or more than 6 days ahead. `404` for an unknown area. Backend down.

### 3.3 Alerts and inbox (`/alerts`) – Feature 3, UC-03 – **planned** (placeholder)

**Purpose.** A Resident subscribes to areas and gets alerts. An Admin can trigger a DEMO alert.

**Data (Phase 2 design).**
- `POST /api/subscriptions` and `DELETE /api/subscriptions/{id}`. A subscription has `userId`, `areaId` and `minLevel`.
- `GET /api/alerts/mine?since=`, polled every 60 s. An alert has `areaId`, `level`, `message`, `createdAt` and `demo`.
- `POST /api/admin/alerts/demo` (Admin).
- Rules are checked every 15 minutes, with at most one alert per area every 6 hours. Both values are already in `application.properties`.

**Main actions.** Subscribe or unsubscribe per area (with a minimum level). Allow browser notifications. Read the inbox. Admin: trigger a demo alert.

**States.**
- Not logged in: explain and link to Login.
- Wrong role: Visitor, Responder and Coordinator see a read-only explanation.
- Notification permission not yet asked, granted, or denied. When denied, alerts appear in the inbox only, with a hint to enable notifications.
- Empty inbox. Unread and read alerts.
- **DEMO alert**, labelled "DEMO – not a real warning".
- Loading. Error.

### 3.4 Shelters map (`/shelters`) – Feature 4, UC-04 – **built**

**Purpose.** Find nearby shelters, see their status and get directions.

**Data.** `GET /api/shelters?areaId=` returns `Shelter[]`: `id`, `areaId`, `name`, `latitude`, `longitude`, `status` (`ShelterStatus`), `contact` (all seeded as "Rescue 1122"), `capacity` (people, a whole number) and `sample` (boolean; **all 5 seeded shelters are `sample: true`**).

**What it shows.**
- An area picker.
- A Leaflet map (`.map`, 420 px high) with OpenStreetMap tiles, centred on the area at zoom 12.
- One `CircleMarker` per shelter, coloured by status. A popup shows the name, a SAMPLE tag (`shelters.sampleTag`, "SAMPLE – not verified"), status, capacity, contact and a "Directions" link to an OpenStreetMap route in a new tab.
- Below the map, a list of shelters, each with a coloured dot, the name and the status text.

**States.**
- Loading: nothing is shown yet; there is no loading text today.
- Backend down: `.banner-error`.
- No shelters in the area: an empty list with no message today.
- Map tiles fail (UC-04 A2): the list below still works.
- Every shelter is SAMPLE today.

### 3.5 Incidents board (`/incidents`) – Feature 5, UC-05 – **planned** (placeholder; `IncidentStatus` is built)

**Purpose.** Log incidents and track them through Open → Assigned → Resolved, with a history of every change.

**Data (Phase 2 design).** `GET/POST /api/incidents` and `PATCH /api/incidents/{id}/status`.
- An incident has `id`, `title`, `type` (for example `FLOODED_ROAD`), `latitude`, `longitude`, `status`, `assignedTeamId`, `createdBy` and `createdAt`.
- `history[]` holds `{ from, to, changedBy, changedAt }` entries.

**Allowed moves** (`IncidentStatus.canMoveTo`): OPEN → ASSIGNED, OPEN → RESOLVED, ASSIGNED → RESOLVED, and ASSIGNED → ASSIGNED (re-assign). Nothing leaves RESOLVED, and nothing moves back to OPEN.

**Main actions.** New incident (title, type, location, description). Assign a team. Resolve. View the history.

**States.**
- Not logged in (401).
- Wrong role (403). Only Responder and Admin may use the board.
- Empty board. Loading.
- `409` "This change is not allowed" for an illegal move.
- `400` or `422` for invalid fields.

### 3.6 Relief (`/relief`) – Feature 6, UC-S1 – **planned** (placeholder)

**Purpose.** Track supplies, their stock and allocations to incidents, and the volunteer list.

**Data (Phase 2 design).**
- `GET/POST /api/supplies`. A supply has `item`, `unit`, `available` and `allocated`.
- `POST /api/allocations`. An allocation has `supplyId`, `incidentId` and `quantity`, and may only target an ASSIGNED incident.
- `GET /api/volunteers`. A volunteer has `name`, `skills` and `phone`. **The phone is shown to Coordinator and Admin only** and is never sent in public responses.

**Main actions.** Add a supply. Change stock. Allocate to an incident. View volunteers.

**States.**
- Not logged in. Wrong role.
- Empty stock. Low stock.
- **The allocation is more than the stock available** (rejected; the backend's business-rule error is `422`, endpoint not built yet).
- Allocation to an incident that is not ASSIGNED.
- Loading. Error.

### 3.7 Safety Info (`/safety`) – Feature 7, UC-S2 – **built**

**Purpose.** Six short flood-safety steps in English and Urdu.

**Data.** Static strings from `safety.steps` in `en.json` and `ur.json`. There is no API call.

**What it shows.** A numbered list (`.safety-steps`, line height 1.6) and a review note: "Urdu text to be checked by a fluent reviewer before the final demo."

**States.** None beyond the language. It works offline once the page is loaded.

### 3.8 Login – **planned** (backend built)

**Purpose.** Staff and residents log in with seeded demo accounts.

**Data.** `POST /api/auth/login` with `{ username, password }` returns `{ token, expiresAt, username, displayName, role }`. Usernames are not case-sensitive; passwords are.

**States.**
- Empty fields: `400` with `details[]`.
- Wrong username or password: `401` "Invalid username or password." The same message is used for both, on purpose.
- **Five failures within one minute**: `429` "Too many failed logins. Try again in a minute."
- Password longer than 72 bytes: `400`.
- Logged in: show `displayName` and `role` in the top bar, with Log out.
- Session expired or server restarted: `401` on the next protected call.

### 3.9 Support – **planned**

A "Support" button at the end of every page. It opens a new email to **mrfaheem010@gmail.com** (`mailto:` link). It needs no data and has no states beyond focus and hover. If the device has no mail app, the address should also be readable as text.

### 3.10 Placeholder page (current stand-in)

`PlaceholderPage.tsx` shows the page title, `placeholder.text` ("This feature is designed in the Phase 2 report and will be built in Phase 3.") and the use case and owner. It is used by Risk, Alerts, Incidents and Relief until they are built.

---

## 4. Semantic states (need consistent colour and icon)

**Rule for all of them:** colour is never the only signal. Every state shows **a text label, and an icon or shape** as well. Items marked ★ are safety-critical and **must be readable by colour-blind users**. Red/green confusion is the most common type, and the current palette relies on red/green for shelters.

### 4.1 Data status (`DataStatus`) ★

| Value | Meaning | Current class | Current colours | Label EN / UR |
|---|---|---|---|---|
| `LIVE` | Fresh from Open-Meteo | `.status-live` | green on light green | LIVE / تازہ |
| `CACHED` | A saved copy (fresh if under 30 min, otherwise older) | `.status-cached` | navy on light blue | CACHED / محفوظ شدہ |
| `SAMPLE` | Made-up data because the APIs failed and nothing was cached. **Not for decisions.** | `.status-sample` | brown on light amber | SAMPLE / نمونہ |
| `UNAVAILABLE` | No data (for example, off the river grid) | `.status-unavailable` | grey on grey | UNAVAILABLE / دستیاب نہیں |

The badge is rendered by `StatusBadge.tsx`. Suggested distinct icons, for the designer to decide: a dot or pulse (LIVE), a clock (CACHED), a flask or "demo" mark (SAMPLE), a slashed circle (UNAVAILABLE).

### 4.2 Risk level (`RiskLevel` plus null) ★

| Value | Meaning | Today |
|---|---|---|
| `LOW` | At or below normal monsoon levels | not styled yet |
| `MEDIUM` | Flow above 1.3× normal, or rain above 50 mm | not styled yet |
| `HIGH` | Flow above 2× normal | not styled yet |
| unknown (`level: null`) | Not enough real data, so the app does not guess | UC-02 asks for **grey "Unknown"** |

HIGH must feel serious without being alarming (see Tone). Risk must not reuse the exact colours of the shelter statuses, so that "HIGH" is never confused with "FULL".

### 4.3 Shelter status (`ShelterStatus`) ★

| Value | Current pin colour (`ShelterMapPage.tsx` `COLOURS`) | Label EN / UR |
|---|---|---|
| `OPEN` | `#2e7d32` green | Open / کھلی |
| `FULL` | `#c62828` red | Full / بھری ہوئی |
| `CLOSED` | `#616161` grey | Closed / بند |
| `UNCONFIRMED` | `#f9a825` amber | Unconfirmed / غیر تصدیق شدہ |

Today all pins are the same circle and differ only by colour. OPEN (green) and FULL (red) are indistinguishable for red/green colour-blind users, so the pins need different shapes or icons. The list under the map already shows the status as text.

### 4.4 Incident status (`IncidentStatus`)

`OPEN`, then `ASSIGNED`, then `RESOLVED`. These are used as board columns and as chips in the history. The columns already carry their text, so colour is secondary here. RESOLVED should read as "done", not as "safe".

### 4.5 DEMO alerts and sample-data tags ★

- **DEMO alert**: "DEMO – not a real warning" (UC-03). It must look clearly different from a real alert in the inbox and in the browser pop-up text.
- **SAMPLE shelter tag**: `.tag`, "SAMPLE – not verified", currently brown on amber.
- **SAMPLE data badge**: see 4.1.

It is an open question whether these three share one "not real" visual language (see 8.3).

### 4.6 Errors

The backend sends every error as `ApiError`: `{ error, status, path, timestamp, details[] }`. The `error` field is a short English sentence. Today the frontend `ApiError` keeps only `status` and `error`.

| Code | Backend message (examples) | What the user should see |
|---|---|---|
| 400 | "Some fields are invalid." with `details[]`; "Missing required parameter 'areaId'." | Inline messages next to the fields |
| 401 | "Missing, invalid or expired login token." / "Invalid username or password." | "Please log in", with a link to Login. On the login form, an inline error. |
| 403 | "Your role is not allowed to use this." | "This page is for {role}." No retry button. |
| 404 | "Unknown area: …" / "No endpoint at this path." | "Not found" with a way back to the Dashboard |
| 409 | "This change is not allowed" (illegal incident move) | Inline on the card. The state stays unchanged. |
| 413 | Request body too large (limit 64 KB) | "Too much text" inline |
| 422 | Business-rule failures (for example, allocation above stock) | Inline explanation |
| 429 | "Too many failed logins. Try again in a minute." | Inline on Login, with a calm countdown or wait note |
| 503 | "Data storage or an external service is unavailable. Please try again shortly." | Non-blocking banner and a Retry button. Any cached or sample data stays visible. |
| 500 | "Something went wrong on the server. Please try again." | Generic banner and Retry |
| network | (no response) | `common.backendDown` banner |

The current banner classes are `.banner-error` (red), `.banner-warn` (amber) and `.banner-info` (blue). Errors (★) need an icon and a heading, not only a coloured box.

---

## 5. Design constraints

### 5.1 Phone-first, low-end, outdoors
- Phase 1 says users open the site "on a phone or PC". The Phase 2 target is a dashboard that loads in under 3 seconds on 4G.
- Assume low-end Android phones, slow 3G or 4G, and possibly bright sunlight. That means high contrast, no thin light-grey text on white, and no reliance on subtle tints.
- Keep the weight low. Avoid large images and decorative fonts. Every web font adds bytes, and a Nastaliq font is large (see 5.2).
- Today there is one breakpoint-free layout: `main` is limited to 1000 px, the top bar wraps, and the charts are 240 px high. On phones the 7 navigation links wrap onto several lines, so a phone navigation pattern is needed.

### 5.2 Urdu, right-to-left and Nastaliq
- `i18n.ts` sets `<html lang>` and `dir="rtl"` for Urdu and saves the choice. The switch in the top bar shows the *other* language's name ("اردو" or "English").
- `index.css` names `'Noto Nastaliq Urdu'` and `'Jameel Noori Nastaleeq'` for `[dir='rtl'] body`, but **no font file is loaded**. On most phones this falls back to a system Naskh font or a generic one.
- Nastaliq needs a **much larger line height** (about 1.8–2.2× is common) and a slightly larger font size than Latin text. Its letters reach far above and below the line, so tight boxes, badges and buttons clip it.
- **Text expands.** Urdu strings are often longer than English ones, for example `dashboard.sampleWarning` and `common.disclaimer`. Badges such as "محفوظ شدہ" (CACHED) are wider than the English label.
- **Mirroring.** Use logical CSS properties (`margin-inline-start` and similar) so the layout flips correctly. Directional icons (arrows, "back") flip. Charts keep time running left to right, even in Urdu, by convention. Numbers stay Western digits as written in the locale files (for example "1122" and "15").
- Mixed text: units ("mm", "m³/s"), river names and source names stay Latin inside Urdu sentences.

### 5.3 Accessibility
- Contrast: WCAG AA at least (4.5:1 for body text, 3:1 for large text and UI parts). Aim higher for outdoor use.
- Touch targets of at least 44×44 px. Today's nav links are about 32 px high, and the area `<select>` is small.
- Numbers and units should be large and **tabular** so values line up. The unit always sits next to the value ("12.4 mm", "1,820 m³/s").
- Every state in section 4 has text plus an icon, never colour alone.
- A visible focus ring on links, buttons, map popups and the language switch.
- Charts need a text alternative, such as a summary sentence or a table, for screen readers and colour-blind users.
- Respect `prefers-reduced-motion`.

### 5.4 Leaflet map and Recharts styling hooks
- **Map.** `.map` sets the container size, radius and border. Pins are `CircleMarker` elements with `pathOptions` `color`/`fillColor` taken from `COLOURS` (hard-coded hex values in `ShelterMapPage.tsx`, radius 10, fill opacity 0.8). Popups use Leaflet's default `.leaflet-popup-content-wrapper` styles from `leaflet/dist/leaflet.css`. The tile URL and attribution come from `VITE_OSM_TILE_URL` and `VITE_OSM_ATTRIBUTION`, with OSM tiles by default. **The OSM attribution must stay visible.** OSM tiles cannot be recoloured with CSS without filters, which matters for dark mode.
- **Charts.** The rain `Bar` uses `fill="#2f6db5"` and the discharge `Line` uses `stroke="#1f3a5f"` with `strokeWidth={2}`. Both are **hard-coded in `DashboardPage.tsx`**. There is also a `CartesianGrid` with `strokeDasharray="3 3"`, default axes with `unit=" mm"` and `unit=" m³/s"`, and the default `Tooltip`. To theme them, read CSS variables (for example `var(--chart-rain)`). SVG `fill` and `stroke` accept `var()`.

### 5.5 Light and dark mode
- Today there is **light mode only**. Colours are in `:root`, there is no `prefers-color-scheme` rule, and there is no toggle.
- Dark mode helps at night and on battery. Map tiles and the shelter pins need a separate check in dark mode.

### 5.6 Support button (planned)
- It appears at the end of every page, above or inside the footer, in both languages.
- It opens `mailto:mrfaheem010@gmail.com`.
- It must not compete with emergency information. Support is for website help, not for emergencies. 1122 is.

### 5.7 Footer disclaimer
- `common.footer`: "FloodWatch – educational prototype (PBL, MCS NUST). Not an official warning service." It is in `.footer` on every page (muted, 0.8 rem).
- Separately, `common.disclaimer` appears on forecast screens: "…This is not an official warning. Check PMD Flood Forecasting Division bulletins and call 1122 in an emergency." Both must stay readable. They are small, but they must never be faint.

---

## 6. Content and voice

- **Short labels.** Navigation items are one or two words: Dashboard, Risk, Alerts, Shelters, Incidents, Relief, Safety Info.
- **Units are always shown**: in axis units (`" mm"`, `" m³/s"`), in tooltips (`"12 mm"`, `"40 %"`) and in `SourceLine` ("Unit: mm").
- **Every chart and card names its source, unit and update time** (FR-03), for example "Source: Open-Meteo Flood API (GloFAS) · Unit: m³/s · Updated: …".
- **Plain-language risk reasons**, using numbers people can picture: "River flow is 2.3× the normal monsoon level." Avoid jargon such as "GloFAS percentile" or "anomaly".
- **Honest about data**: "SAMPLE data is shown. Do not use it for decisions."
- **Point to official help**: PMD FFD, NDMA/PDMA, and "call 1122".

Example strings from the locale files:

| Key | English | Urdu |
|---|---|---|
| `dashboard.title` | Flood Monitoring Dashboard | سیلاب نگرانی ڈیش بورڈ |
| `dashboard.rainTitle` | Rainfall – next 7 days (daily total) | بارش – اگلے 7 دن (روزانہ کل) |
| `dashboard.riverTitle` | {{river}} – modeled discharge | {{river}} – ماڈل شدہ پانی کا بہاؤ |
| `dashboard.sampleWarning` | Data unavailable: live sources could not be reached, so SAMPLE data is shown. Do not use it for decisions. | ڈیٹا دستیاب نہیں: … اس پر فیصلے نہ کریں۔ |
| `dashboard.cachedNote` | Showing recently saved (cached) data. It refreshes automatically. | حال ہی میں محفوظ شدہ ڈیٹا دکھایا جا رہا ہے۔ … |
| `dashboard.noRiver` | No modeled river at this point. Only rainfall is available. | اس مقام پر کوئی ماڈل شدہ دریا نہیں۔ … |
| `shelters.sampleTag` | SAMPLE – not verified | نمونہ – تصدیق شدہ نہیں |
| `safety.steps[2]` | Never walk or drive through flood water. Even 15 cm of moving water can knock a person down. | سیلابی پانی میں کبھی پیدل نہ چلیں … |
| `common.backendDown` | Cannot reach the FloodWatch server. Is the backend running? | فلڈ واچ سرور سے رابطہ نہیں ہو سکا۔ … |

Notes:
- `common.backendDown` is aimed at developers ("Is the backend running?"). A public wording is still to be written.
- Dates and times use the **browser's** locale (`toLocaleDateString(undefined, …)`), not the chosen app language.
- The Urdu text still needs review by a fluent speaker.

---

## 7. Current frontend state

**Stack.** React 19, TypeScript, Vite 8, React Router 8, Recharts 3, Leaflet 1.9 with react-leaflet 5, and i18next with react-i18next. There is no UI component library and no CSS framework. All styles are in one global file.

**Files.**

| Path | What it is |
|---|---|
| `web/index.html` | Shell, `<title>FloodWatch</title>`, favicon `public/favicon.svg` |
| `web/src/main.tsx` | Mounts the app, imports `i18n` and `index.css` |
| `web/src/App.tsx` | Top bar (`.topbar`, `.brand`, `nav`, `.lang`), routes, footer (`.footer`) |
| `web/src/index.css` | All styles (about 40 lines) |
| `web/src/i18n.ts` | Language setup, RTL switch, saved choice |
| `web/src/locales/en.json`, `ur.json` | All interface strings |
| `web/src/api/client.ts`, `types.ts` | `fetch` wrapper with GETs only and no auth header yet; types that mirror the Java model |
| `web/src/hooks/useSelectedArea.ts` | Loads the areas and remembers the selected one |
| `web/src/components/AreaPicker.tsx` | Labelled `<select>` (`.area-picker`) |
| `web/src/components/StatusBadge.tsx` | `<span class="status status-{live\|cached\|sample\|unavailable}">` |
| `web/src/components/SourceLine.tsx` | Badge with source, unit and updated time (`.source-line`) |
| `web/src/pages/DashboardPage.tsx` | Built (3.1) |
| `web/src/pages/ShelterMapPage.tsx` | Built (3.4) |
| `web/src/pages/SafetyInfoPage.tsx` | Built (3.7) |
| `web/src/pages/PlaceholderPage.tsx` | Stand-in for Risk, Alerts, Incidents and Relief |

**CSS variables in `:root` today.**

```css
--navy:   #1f3a5f;  /* top bar, info banner text */
--bg:     #f4f7fb;  /* page background */
--card:   #ffffff;  /* card background */
--text:   #1b2430;  /* body text */
--muted:  #5c6b80;  /* source lines, disclaimer, footer */
--border: #d5dde8;  /* card, select and map borders */
font-family: system-ui, 'Segoe UI', Roboto, 'Noto Nastaliq Urdu', sans-serif;
```

**Classes.**
- Layout and type: `.topbar`, `.brand`, `.lang`, `main`, `h1` (1.5 rem), `h2` (1.1 rem).
- Containers: `.card` (radius 10 px, padding 16 px), `.area-picker`.
- Banners: `.banner`, `.banner-error`, `.banner-warn`, `.banner-info`.
- Data labels: `.source-line`, `.status` with `.status-live`, `.status-cached`, `.status-sample`, `.status-unavailable`.
- Other: `.disclaimer`, `.map`, `.tag`, `.shelter-list`, `.dot`, `.safety-steps`, `.footer`.

**Colours that bypass the variables** (these must move to tokens before a theme can control them):
- The banner and status backgrounds and text in `index.css`: `#fdecea`, `#8a1c1c`, `#fff4e0`, `#7a4b00`, `#e8f1fb`, `#e3f4e5`, `#1e6b28`, `#eee`, `#555`.
- The `.topbar` link colour `#dbe6f3` and the white top-bar text.
- The chart colours in `DashboardPage.tsx`: `#2f6db5` and `#1f3a5f`.
- The shelter pin `COLOURS` in `ShelterMapPage.tsx`, which are also used for the list dots through an inline `style`.

**Placeholders and gaps.**
- Not built yet: Risk, Alerts, Incidents and Relief pages; Login; the Support button.
- Not present: dark mode, a loaded Urdu font, a phone navigation pattern, skeleton loaders and icons. There is no icon set in the project.

**How to theme without changing behaviour.**
1. Replace the hex values in `index.css` with semantic variables.
2. Make the chart `fill` and `stroke` props and the map `COLOURS` read the same variables, or a small `theme.ts` that reads them.
3. Add `[data-theme]` or `prefers-color-scheme` overrides.

The class names and components can stay as they are.

---

## 8. Plan

### 8.1 Order to theme the screens
1. **Foundations**: tokens in `:root` (and dark mode), base type for English and Urdu, the Urdu font, the top bar with phone navigation and the language switch, the footer, the disclaimer and the Support button. These touch every page.
2. **Shared parts**: `StatusBadge`, `SourceLine`, `.banner-*`, `.card`, `AreaPicker`, `.tag`, buttons, form fields, focus ring, and loading and empty states.
3. **Dashboard**: chart palette, axes, grid, tooltip and the SAMPLE and CACHED banners. It is the most-used screen and is already built.
4. **Shelters map**: pin shapes and colours, popup, list and dark-mode tiles. Already built.
5. **Safety Info**: long-text reading in both languages. Already built.
6. **Risk**: the risk badge, the reason and the 7-day strip. The backend is ready in PR #4.
7. **Login**, then **Alerts and inbox** (DEMO treatment, permission prompts).
8. **Incidents board**, then **Relief** (staff screens, denser data, and the wrong-role and 409/422 states).

### 8.2 Proposed design tokens to define

These are names and roles only. The values are the designer's choice.

**Colour, by role.**
- Surfaces: `--color-bg`, `--color-surface`, `--color-surface-raised`, `--color-border`, `--color-overlay`
- Text: `--color-text`, `--color-text-muted`, `--color-text-on-brand`, `--color-link`
- Brand: `--color-brand`, `--color-brand-strong`, `--color-on-brand`
- Focus: `--color-focus`
- Feedback: `--color-error`, `--color-error-bg`, `--color-warning`, `--color-warning-bg`, `--color-info`, `--color-info-bg`, `--color-success`, `--color-success-bg`
- Data status: `--status-live-fg`/`-bg`, `--status-cached-fg`/`-bg`, `--status-sample-fg`/`-bg`, `--status-unavailable-fg`/`-bg`
- Risk: `--risk-low-*`, `--risk-medium-*`, `--risk-high-*`, `--risk-unknown-*`
- Incident: `--incident-open-*`, `--incident-assigned-*`, `--incident-resolved-*`
- Not-real marker: `--demo-fg`/`-bg` for DEMO and SAMPLE tags
- Emergency: `--color-emergency` for "call 1122". This must not look like official branding.

**Type scale.**
- `--font-latin`, `--font-urdu`, `--font-numeric` (tabular figures)
- Sizes: `--text-xs`, `--text-sm`, `--text-base`, `--text-lg`, `--text-xl`, `--text-2xl`, plus a large display size for the risk level and key numbers
- **Separate values for Urdu**: `--leading-latin` (about 1.4–1.6) and `--leading-urdu` (about 1.8–2.2), and an optional `--urdu-size-boost`, set under `[dir='rtl']`
- Weights: regular, semibold and bold. Nastaliq fonts often ship with fewer weights.

**Spacing.** A 4 px or 8 px based scale (`--space-1` … `--space-8`), a minimum touch size (`--touch-min: 44px`) and a page gutter.

**Radius.** `--radius-sm` (badges, tags), `--radius-md` (buttons, inputs), `--radius-lg` (cards, map) and `--radius-pill`.

**Shadow levels.** `--shadow-0` (flat, with a border only), `--shadow-1` (cards), `--shadow-2` (popups, menus) and `--shadow-3` (dialogs). Dark mode usually swaps shadows for lighter surfaces.

**Chart palette.**
- Series: `--chart-rain`, `--chart-rain-probability`, `--chart-discharge`, `--chart-baseline` (for a future "normal level" line)
- Chart chrome: `--chart-grid`, `--chart-axis`, `--chart-tooltip-bg`, `--chart-tooltip-text`
- Data-status overlays: `--chart-sample-pattern` (hatched or dashed for SAMPLE) and `--chart-cached-tint`

**Map pin palette.**
- Pin colours: `--pin-open`, `--pin-full`, `--pin-closed`, `--pin-unconfirmed`, plus `--pin-stroke` (an outline for contrast on the tiles) and `--pin-sample-marker`
- A matching shape or icon per status
- `--map-popup-bg` and `--map-popup-text`

### 8.3 Open questions for the designer

These are listed as options. None are decided here.

1. **Brand colour.** Keep the current navy (`#1f3a5f`)? Use a water-blue or teal? Use a neutral slate with colour reserved for states only? Avoid green-and-white state branding in any case.
2. **Risk colours.** Use the classic green, amber and red? Use a single-hue blue scale (light to dark), which avoids "traffic-light alarm"? Use neutral colours with an icon and the word as the main signal? How strong should HIGH be?
3. **Shelter pins.** Keep circles and add an icon inside each? Use different shapes per status (for example circle, square, triangle, diamond)? Use teardrop pins with a glyph? Show capacity on the pin?
4. **"Not real" language.** Should DEMO alerts, SAMPLE data and SAMPLE shelters share one treatment (for example a striped or hatched tag)? Or should each have its own?
5. **Data status display.** Keep the text badge only? Use a badge plus an icon? Show the age ("saved 40 min ago") for CACHED? Draw SAMPLE charts dashed or hatched?
6. **Urdu font.** Self-host Noto Nastaliq Urdu (large file, best look)? Use a Naskh font for the interface and Nastaliq for long text only? Use the system font? How should the font load on slow connections (a swap or fallback strategy)?
7. **Phone navigation.** A bottom tab bar (which 4–5 items)? A hamburger menu? A scrollable top strip? Should staff-only links be hidden for Visitors, or shown with a lock?
8. **Dark mode.** Follow the system only, add a manual toggle, or both? Darken the map tiles with a CSS filter, or switch to a different tile style (which must still follow the OSM tile policy)?
9. **Language switch.** Keep the current button that shows the other language's name? Use a segmented "EN | اردو" control? Should it also be offered on first visit?
10. **Emergency prominence.** Should "call 1122" be a fixed element on every page, a block on the Dashboard only, or text in the disclaimer only?
11. **Support button.** A full-width footer button, a small link, or a floating button? What should it say ("Support", "Help with this site", "Contact the team")?
12. **Icons.** Which open-source icon set (or custom icons), and outline or filled style?
13. **Density for staff screens.** Should the Incidents board and Relief use the same spacing as the public screens, or a denser "work" mode?
14. **Rain probability.** Draw it as a second series (a line over the bars), as a label on each bar, or as a separate small chart?
