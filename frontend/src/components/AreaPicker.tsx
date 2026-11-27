import { useTranslation } from 'react-i18next'
import type { Area } from '../api/types'

interface Props {
  areas: Area[]
  areaId: string | null
  onChange: (id: string) => void
}

export function AreaPicker({ areas, areaId, onChange }: Props) {
  const { t } = useTranslation()
  return (
    <label className="inline-flex items-center gap-3 text-sm font-medium text-ink-800">
      {t('common.area')}
      <select
        value={areaId ?? ''}
        onChange={(e) => onChange(e.target.value)}
        className="h-11 min-w-48 rounded-full border border-ink-200 bg-paper px-4 text-base text-ink-950 outline-none focus-visible:ring-2 focus-visible:ring-teal-500"
      >
        {areas.map((a) => (
          <option key={a.id} value={a.id}>
            {a.name} ({a.riverName})
          </option>
        ))}
      </select>
    </label>
  )
}
