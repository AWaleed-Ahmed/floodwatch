import { useTranslation } from 'react-i18next'

interface Props {
  titleKey: string
  useCase: string
  owner: string
}

/** Shown for features that are designed (Phase 2) but not built yet. */
export function PlaceholderPage({ titleKey, useCase, owner }: Props) {
  const { t } = useTranslation()
  return (
    <>
      <h1 className="font-display text-[clamp(2.25rem,5vw,3.5rem)] leading-tight tracking-[-0.02em] text-ink-950">{t(titleKey)}</h1>
      <div className="mt-6 rounded-3xl border border-dashed border-ink-200 p-6 text-ink-800">
        <p>{t('placeholder.text')}</p>
        <p className="mt-2 text-sm text-ink-600">
          {useCase} · {owner}
        </p>
      </div>
    </>
  )
}
