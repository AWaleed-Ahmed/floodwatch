import { useTranslation } from 'react-i18next'

/** Feature 7: Urdu & English Safety Info (UC-S2). */
export function SafetyInfoPage() {
  const { t } = useTranslation()
  const steps = t('safety.steps', { returnObjects: true }) as string[]
  return (
    <>
      <h1 className="font-display text-[clamp(2.25rem,5vw,3.5rem)] leading-tight tracking-[-0.02em] text-ink-950">{t('safety.title')}</h1>
      <ol className="mt-8 max-w-3xl list-decimal space-y-4 ps-6 text-lg leading-relaxed text-ink-800 marker:font-semibold marker:text-teal-700">
        {steps.map((s) => (
          <li key={s} className="ps-2">
            {s}
          </li>
        ))}
      </ol>
      <p className="mt-8 text-sm text-ink-600">{t('safety.reviewNote')}</p>
    </>
  )
}
