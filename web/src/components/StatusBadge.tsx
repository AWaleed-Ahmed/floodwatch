import { useTranslation } from 'react-i18next'
import type { DataStatus } from '../api/types'

export function StatusBadge({ status }: { status: DataStatus }) {
  const { t } = useTranslation()
  return <span className={`status status-${status.toLowerCase()}`}>{t(`status.${status}`)}</span>
}
