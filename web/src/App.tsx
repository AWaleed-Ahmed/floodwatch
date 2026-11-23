import { NavLink, Route, Routes } from 'react-router'
import { useTranslation } from 'react-i18next'
import { DashboardPage } from './pages/DashboardPage'
import { PlaceholderPage } from './pages/PlaceholderPage'
import { SafetyInfoPage } from './pages/SafetyInfoPage'
import { ShelterMapPage } from './pages/ShelterMapPage'

const NAV = [
  { to: '/', key: 'nav.dashboard' },
  { to: '/risk', key: 'nav.risk' },
  { to: '/alerts', key: 'nav.alerts' },
  { to: '/shelters', key: 'nav.shelters' },
  { to: '/incidents', key: 'nav.incidents' },
  { to: '/relief', key: 'nav.relief' },
  { to: '/safety', key: 'nav.safety' },
]

export default function App() {
  const { t, i18n } = useTranslation()
  const other = i18n.language === 'ur' ? 'en' : 'ur'
  return (
    <>
      <header className="topbar">
        <span className="brand">{t('app.name')}</span>
        <nav>
          {NAV.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.to === '/'}>
              {t(n.key)}
            </NavLink>
          ))}
        </nav>
        <button className="lang" onClick={() => i18n.changeLanguage(other)}>
          {other === 'ur' ? 'اردو' : 'English'}
        </button>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/risk" element={<PlaceholderPage titleKey="nav.risk" useCase="UC-02 · Feature 2" owner="Abdullah + Faheem" />} />
          <Route path="/alerts" element={<PlaceholderPage titleKey="nav.alerts" useCase="UC-03 · Feature 3" owner="Abdullah + Faheem" />} />
          <Route path="/shelters" element={<ShelterMapPage />} />
          <Route path="/incidents" element={<PlaceholderPage titleKey="nav.incidents" useCase="UC-05 · Feature 5" owner="Ehtisham" />} />
          <Route path="/relief" element={<PlaceholderPage titleKey="nav.relief" useCase="UC-S1 · Feature 6" owner="Ehtisham" />} />
          <Route path="/safety" element={<SafetyInfoPage />} />
        </Routes>
      </main>
      <footer className="footer">{t('common.footer')}</footer>
    </>
  )
}
