import { useTranslation } from 'react-i18next'
import type { ForecastSeries } from '../api/types'
import { StatusBadge } from './StatusBadge'

/** Every chart says where its data came from, in what unit, and when (FR-03). */
export function SourceLine({ series }: { series: ForecastSeries }) {
  const { t } = useTranslation()
  return (
    <p className="source-line">
      <StatusBadge status={series.status} /> {t('common.source')}: {series.source} · {t('common.unit')}: {series.unit} ·{' '}
      {t('common.updated')}: {new Date(series.fetchedAt).toLocaleString()}
    </p>
  )
}
