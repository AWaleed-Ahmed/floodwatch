import { useTranslation } from 'react-i18next'
import type { RiskLevel } from '../api/types'

type Level = RiskLevel | 'UNKNOWN'

/**
 * How many of the three gauge ticks are filled, and in what colour. Colour is
 * never the only signal: the number of filled ticks and the word both carry
 * the level. Risk colours stay apart from the shelter colours.
 */
const LEVELS: Record<Level, { filled: number; colour: string }> = {
  LOW: { filled: 1, colour: 'bg-teal-600' },
  MEDIUM: { filled: 2, colour: 'bg-amber-500' },
  HIGH: { filled: 3, colour: 'bg-orange-700' },
  UNKNOWN: { filled: 0, colour: '' },
}

interface Props {
  level: RiskLevel | null
  size?: 'sm' | 'lg'
}

/**
 * Risk level as a small river gauge: three stacked ticks like the markings on
 * a water-level staff, filled from the bottom (one for Low, two for Medium,
 * three for High; dashed outlines when unknown), with the word beside it in
 * plain ink. No pill or background, so it sits naturally on light cards, dark
 * cards and the dark theme; unfilled ticks and the word take the surrounding
 * text colour.
 */
export function RiskBadge({ level, size = 'sm' }: Props) {
  const { t } = useTranslation()
  const key: Level = level ?? 'UNKNOWN'
  const { filled, colour } = LEVELS[key]
  const large = size === 'lg'

  return (
    <span className={`inline-flex items-center ${large ? 'gap-3' : 'gap-1.5'}`}>
      <span aria-hidden="true" className={`flex flex-col-reverse ${large ? 'gap-[3px]' : 'gap-[2px]'}`}>
        {[0, 1, 2].map((tick) => {
          const on = tick < filled
          return (
            <span
              key={tick}
              className={`block rounded-[1px] ${large ? 'h-[5px] w-6' : 'h-[3px] w-3.5'} ${
                key === 'UNKNOWN'
                  ? 'border border-dashed border-current opacity-40'
                  : on
                    ? colour
                    : 'bg-current opacity-15'
              }`}
            />
          )
        })}
      </span>
      <span className={large ? 'font-display text-2xl leading-none' : 'text-xs font-semibold'}>{t(`riskLevel.${key}`)}</span>
    </span>
  )
}
