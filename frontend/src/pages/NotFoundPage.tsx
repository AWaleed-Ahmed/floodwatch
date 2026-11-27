import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

export function NotFoundPage() {
  const { t } = useTranslation()
  return (
    <>
      <h1 className="font-display text-[clamp(2.25rem,5vw,3.5rem)] leading-tight tracking-[-0.02em] text-ink-950 ur:leading-[1.8] ur:tracking-normal">{t('notFound.title')}</h1>
      <Link
        to="/"
        className="mt-6 inline-flex h-11 items-center rounded-full bg-ink-950 px-5 text-sm font-semibold text-paper outline-none focus-visible:ring-2 focus-visible:ring-teal-500 focus-visible:ring-offset-2"
      >
        {t('notFound.back')}
      </Link>
    </>
  )
}
