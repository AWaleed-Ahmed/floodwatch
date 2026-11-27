import { useTranslation } from 'react-i18next'
import type { ForecastSeries } from '../api/types'
import { StatusBadge } from './StatusBadge'

/** Every chart says where its data came from, in what unit, and when (FR-03). */
export function SourceLine({ series }: { series: ForecastSeries }) {
  const { t } = useTranslation()
  return (
    <p className="mt-3 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-ink-600">
      <StatusBadge status={series.status} />
      <span>
        {t('common.source')}: {series.source}
      </span>
      <span aria-hidden="true">·</span>
      <span>
        {t('common.unit')}: {series.unit}
      </span>
      <span aria-hidden="true">·</span>
      <span>
        {t('common.updated')}: {new Date(series.fetchedAt).toLocaleString()}
      </span>
    </p>
  )
}
