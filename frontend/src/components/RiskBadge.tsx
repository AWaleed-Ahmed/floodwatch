import { useTranslation } from 'react-i18next'
import type { RiskLevel } from '../api/types'

type Level = RiskLevel | 'UNKNOWN'

/**
 * Each level has its own word, colour and shape (circle, triangle, square, ring),
 * so it can be told apart without relying on colour. The risk colours are kept
 * apart from the shelter colours, so High never looks like a "Full" shelter.
 */
const STYLES: Record<Level, { badge: string; mark: string }> = {
  LOW: { badge: 'bg-teal-50 text-teal-900 ring-teal-600/25', mark: 'rounded-full bg-teal-600' },
  MEDIUM: {
    badge: 'bg-amber-50 text-amber-900 ring-amber-600/30',
    mark: 'bg-amber-600 [clip-path:polygon(50%_0,100%_100%,0_100%)]',
  },
  HIGH: { badge: 'bg-orange-50 text-orange-900 ring-orange-700/30', mark: 'rounded-[2px] bg-orange-700' },
  UNKNOWN: { badge: 'bg-ink-50 text-ink-800 ring-ink-200', mark: 'rounded-full border-2 border-ink-600' },
}

interface Props {
  level: RiskLevel | null
  size?: 'sm' | 'lg'
}

export function RiskBadge({ level, size = 'sm' }: Props) {
  const { t } = useTranslation()
  const key: Level = level ?? 'UNKNOWN'
  const style = STYLES[key]
  const large = size === 'lg'
  return (
    <span
      className={`inline-flex items-center rounded-full font-semibold ring-1 ring-inset ${style.badge} ${
        large ? 'gap-2.5 px-4 py-2 text-xl' : 'gap-1.5 px-2 py-0.5 text-xs'
      }`}
    >
      <span aria-hidden="true" className={`inline-block shrink-0 ${style.mark} ${large ? 'size-3.5' : 'size-2'}`} />
      {t(`riskLevel.${key}`)}
    </span>
  )
}
