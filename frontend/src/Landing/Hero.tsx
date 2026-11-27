import { useTranslation } from 'react-i18next'
import { Wordmark } from '../components/Wordmark'

/** FloodWatch wordmark and tagline, set in black near the top-left of the background. */
export default function Hero() {
  const { t } = useTranslation()
  return (
    <div className="flex flex-col items-start ps-[5vw] pe-4 pt-[16svh] text-start">
      <h1>
        <Wordmark className="block text-[clamp(2.75rem,9vw,8rem)]" />
      </h1>
      <p className="mt-4 ps-[0.08em] text-[clamp(1rem,1.6vw,1.25rem)] font-medium tracking-[-0.01em] text-ink-800">
        {t('hero.taglineTop')}
        <br />
        {t('hero.taglineBottom')}
      </p>
    </div>
  )
}
