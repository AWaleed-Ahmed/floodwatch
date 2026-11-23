import { useTranslation } from 'react-i18next'

/** Feature 7: Urdu & English Safety Info (UC-S2). */
export function SafetyInfoPage() {
  const { t } = useTranslation()
  const steps = t('safety.steps', { returnObjects: true }) as string[]
  return (
    <section>
      <h1>{t('safety.title')}</h1>
      <ol className="safety-steps">
        {steps.map((s) => <li key={s}>{s}</li>)}
      </ol>
      <p className="disclaimer">{t('safety.reviewNote')}</p>
    </section>
  )
}
