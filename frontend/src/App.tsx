import { lazy, Suspense, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Route, Routes } from 'react-router'
import { PageShell } from './components/PageShell'
import PageTransition from './components/PageTransition'
import Background from './Landing/Background.tsx'
import Menu from './Landing/Component/Menu.tsx'
import HelpLine from './Landing/HelpLine.tsx'
import Hero from './Landing/Hero.tsx'
import { loadDashboard, loadNotFound, loadPlaceholder, loadSafety, loadShelters } from './pageLoaders'

/** Each page loads in its own chunk, so the landing page never downloads the chart or map libraries. */
const DashboardPage = lazy(() => loadDashboard().then((m) => ({ default: m.DashboardPage })))
const ShelterMapPage = lazy(() => loadShelters().then((m) => ({ default: m.ShelterMapPage })))
const SafetyInfoPage = lazy(() => loadSafety().then((m) => ({ default: m.SafetyInfoPage })))
const PlaceholderPage = lazy(() => loadPlaceholder().then((m) => ({ default: m.PlaceholderPage })))
const NotFoundPage = lazy(() => loadNotFound().then((m) => ({ default: m.NotFoundPage })))

function Inner({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  return (
    <PageShell>
      <Suspense fallback={<p className="text-ink-600">{t('common.loading')}</p>}>{children}</Suspense>
    </PageShell>
  )
}

export default function App() {
  return (
    <>
      <Routes>
        <Route
          path="/"
          element={
            <Background>
              <Hero />
              <HelpLine />
            </Background>
          }
        />
        <Route path="/dashboard" element={<Inner><DashboardPage /></Inner>} />
        <Route path="/shelters" element={<Inner><ShelterMapPage /></Inner>} />
        <Route path="/safety" element={<Inner><SafetyInfoPage /></Inner>} />
        <Route path="/risk" element={<Inner><PlaceholderPage titleKey="nav.risk" useCase="UC-02 · Feature 2" owner="Abdullah + Faheem" /></Inner>} />
        <Route path="/alerts" element={<Inner><PlaceholderPage titleKey="nav.alerts" useCase="UC-03 · Feature 3" owner="Abdullah + Faheem" /></Inner>} />
        <Route path="/incidents" element={<Inner><PlaceholderPage titleKey="nav.incidents" useCase="UC-05 · Feature 5" owner="Ehtisham" /></Inner>} />
        <Route path="/relief" element={<Inner><PlaceholderPage titleKey="nav.relief" useCase="UC-S1 · Feature 6" owner="Ehtisham" /></Inner>} />
        <Route path="/login" element={<Inner><PlaceholderPage titleKey="menu.login" useCase="Login" owner="Faheem" /></Inner>} />
        <Route path="*" element={<Inner><NotFoundPage /></Inner>} />
      </Routes>
      <Menu />
      <PageTransition />
    </>
  )
}
