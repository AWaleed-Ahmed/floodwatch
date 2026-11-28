import type { CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'

const TITLE = 'FloodWatch'

/**
 * FloodWatch wordmark and tagline near the top-left of the background.
 *
 * Intro (CSS only, see the hero-* rules in index.css): a thick line draws in
 * under the title, each letter rises out of it from left to right, the teal dot
 * pops in, the line retracts, and the two tagline lines float up.
 *
 * The block is always laid out left to right so it stays in the same place in
 * Urdu; only the tagline text itself runs right to left.
 */
export default function Hero() {
  const { t, i18n } = useTranslation()
  return (
    <div
      dir="ltr"
      className="flex flex-col items-start pr-4 pl-[3.5vw] text-left"
      style={{ paddingTop: 'calc(var(--hero-line) - var(--hero-title-size) * 0.43)' }}
    >
      <h1
        aria-label={TITLE}
        className="font-display text-(length:--hero-title-size) leading-[0.9] tracking-[-0.02em] text-ink-950"
      >
        <span aria-hidden="true" className="relative inline-flex items-baseline">
          <span className="-mt-[0.15em] flex overflow-hidden pt-[0.15em]">
            {[...TITLE].map((letter, index) => (
              <span key={index} className="hero-letter inline-block" style={{ '--i': index } as CSSProperties}>
                {letter}
              </span>
            ))}
          </span>
          <span data-teal-dot className="hero-dot ms-[0.04em] inline-block size-[0.17em] rounded-full bg-linear-to-tr from-teal-600 via-teal-400 to-paper" />
          <span className="hero-line absolute bottom-0 left-0 h-[0.05em] w-full rounded-full bg-ink-950" />
        </span>
      </h1>

      <p
        dir={i18n.dir()}
        data-lang-fade
        className="mt-4 ps-[0.08em] text-[clamp(1rem,1.6vw,1.25rem)] font-medium tracking-[-0.01em] text-ink-800 ur:mt-2 ur:leading-[2.1] ur:tracking-normal"
      >
        <span className="hero-float block" style={{ '--delay': '1050ms' } as CSSProperties}>
          {t('hero.taglineTop')}
        </span>
        <span className="hero-float block" style={{ '--delay': '1160ms' } as CSSProperties}>
          {t('hero.taglineBottom')}
        </span>
      </p>
    </div>
  )
}
