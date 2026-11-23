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
    <section>
      <h1>{t(titleKey)}</h1>
      <div className="card">
        <p>{t('placeholder.text')}</p>
        <p><strong>{useCase}</strong> · {owner}</p>
      </div>
    </section>
  )
}
