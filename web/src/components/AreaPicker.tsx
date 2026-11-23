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
    <label className="area-picker">
      {t('common.area')}
      <select value={areaId ?? ''} onChange={(e) => onChange(e.target.value)}>
        {areas.map((a) => (
          <option key={a.id} value={a.id}>
            {a.name} ({a.riverName})
          </option>
        ))}
      </select>
    </label>
  )
}
