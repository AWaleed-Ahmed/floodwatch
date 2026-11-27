import { useTranslation } from 'react-i18next'
import type { DataStatus } from '../api/types'

/** Word plus a small shape, so the status never relies on colour alone. */
const STYLES: Record<DataStatus, { badge: string; mark: string }> = {
  LIVE: { badge: 'bg-teal-50 text-teal-800 ring-teal-600/25', mark: 'size-1.5 rounded-full bg-teal-600' },
  CACHED: { badge: 'bg-ink-50 text-ink-800 ring-ink-400/40', mark: 'size-1.5 rounded-[2px] bg-ink-600' },
  SAMPLE: { badge: 'bg-amber-50 text-amber-900 ring-amber-600/30', mark: 'size-1.5 rotate-45 bg-amber-600' },
  UNAVAILABLE: { badge: 'bg-ink-50 text-ink-600 ring-ink-200', mark: 'h-px w-2 bg-ink-600' },
}

export function StatusBadge({ status }: { status: DataStatus }) {
  const { t } = useTranslation()
  const style = STYLES[status]
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-semibold ring-1 ring-inset ${style.badge}`}>
      <span aria-hidden="true" className={`inline-block ${style.mark}`} />
      {t(`status.${status}`)}
    </span>
  )
}
