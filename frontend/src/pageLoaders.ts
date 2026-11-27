/**
 * Code for each page, loaded on demand. App uses these for its lazy routes, and
 * the page transition calls prefetchPage() as soon as a link is clicked, so the
 * page's code is already downloaded by the time the transition reveals it.
 */
export const loadDashboard = () => import('./pages/DashboardPage')
export const loadShelters = () => import('./pages/ShelterMapPage')
export const loadSafety = () => import('./pages/SafetyInfoPage')
export const loadPlaceholder = () => import('./pages/PlaceholderPage')
export const loadNotFound = () => import('./pages/NotFoundPage')

const BY_PATH: Record<string, () => Promise<unknown>> = {
  '/dashboard': loadDashboard,
  '/shelters': loadShelters,
  '/safety': loadSafety,
  '/risk': loadPlaceholder,
  '/alerts': loadPlaceholder,
  '/incidents': loadPlaceholder,
  '/relief': loadPlaceholder,
  '/login': loadPlaceholder,
}

/** Start loading a page's code; resolves when it is ready (never rejects). */
export function prefetchPage(path: string): Promise<void> {
  const load = BY_PATH[path]
  return load ? load().then(() => undefined, () => undefined) : Promise.resolve()
}
