import { useTranslation } from 'react-i18next'
import { SAFETY_ICONS } from '../components/SafetyIcons'

/** Water depths for the gauge panel (US National Weather Service figures), deepest first. */
const DEPTHS = [
  { cm: 60, key: 'd60' },
  { cm: 30, key: 'd30' },
  { cm: 15, key: 'd15' },
] as const

const GAUGE_MAX = 70

/** Feature 7: Urdu & English Safety Info (UC-S2), as an illustrated guide. */
export function SafetyInfoPage() {
  const { t } = useTranslation()
  const steps = t('safety.steps', { returnObjects: true }) as string[]
  const headings = t('safety.headings', { returnObjects: true }) as string[]

  return (
    <>
      <h1 className="font-display text-[clamp(2.25rem,5vw,3.5rem)] leading-tight tracking-[-0.02em] text-ink-950 ur:leading-[1.8] ur:tracking-normal">
        {t('safety.title')}
      </h1>
      <p className="mt-3 max-w-2xl text-lg text-ink-600">{t('safety.intro')}</p>

      {/* The six steps, each with its own drawing. Order matters (before, then during a flood), so they keep their numbers. */}
      <ol className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {steps.map((step, index) => {
          const icon = SAFETY_ICONS[index]
          return (
            <li key={step} className="flex flex-col rounded-3xl border border-ink-100 bg-paper p-6">
              <div className="flex items-start justify-between">
                {icon && (
                  <span className="grid size-14 place-items-center rounded-2xl bg-ink-50">
                    <icon.Icon aria-hidden="true" strokeWidth={1.6} className={`size-7 ${icon.tint}`} />
                  </span>
                )}
                <span className="font-display text-sm text-ink-400 tabular-nums">{String(index + 1).padStart(2, '0')}</span>
              </div>
              <h2 className="mt-5 font-display text-xl tracking-[-0.01em] text-ink-950 ur:leading-[1.8] ur:tracking-normal">
                {headings[index]}
              </h2>
              <p className="mt-2 leading-relaxed text-ink-800">{step}</p>
            </li>
          )
        })}
      </ol>

      {/* How deep is too deep: a water-level staff with the three danger marks. */}
      <section className="mt-10 grid gap-8 rounded-3xl border border-ink-100 bg-ink-50 p-6 sm:p-8 md:grid-cols-[auto_1fr] md:items-center">
        <div aria-hidden="true" className="relative mx-auto h-56 w-24 md:mx-0">
          {/* Water, filling the staff to the highest mark. Its surface (the meniscus) moves: two wave layers drift at different speeds and the level gently rises and falls. */}
          <div
            className="water-bob absolute inset-x-0 bottom-0 rounded-b-xl bg-linear-to-t from-teal-600/45 to-teal-400/25"
            style={{ height: `${(60 / GAUGE_MAX) * 100}%` }}
          >
            <div className="absolute inset-x-0 -top-2.5 h-3 overflow-hidden">
              <svg viewBox="0 0 216 14" preserveAspectRatio="none" className="water-wave-back absolute top-0 left-0 h-full w-[200%] text-teal-500 opacity-40">
                <path d="M0 6 C 12 0, 24 0, 36 6 S 60 12, 72 6 S 96 0, 108 6 S 132 12, 144 6 S 168 0, 180 6 S 204 12, 216 6 L 216 14 L 0 14 Z" fill="currentColor" />
              </svg>
              <svg viewBox="0 0 216 14" preserveAspectRatio="none" className="water-wave absolute top-0 left-0 h-full w-[200%] text-teal-400/60">
                <path d="M0 6 C 12 0, 24 0, 36 6 S 60 12, 72 6 S 96 0, 108 6 S 132 12, 144 6 S 168 0, 180 6 S 204 12, 216 6 L 216 14 L 0 14 Z" fill="currentColor" />
              </svg>
            </div>
          </div>
          {/* The staff itself, with a tick every 10 cm */}
          <div className="absolute inset-y-0 left-1/2 w-3 -translate-x-1/2 rounded-full border-2 border-ink-950 bg-paper" />
          {Array.from({ length: GAUGE_MAX / 10 + 1 }, (_, i) => i * 10).map((cm) => (
            <span
              key={cm}
              className="absolute left-1/2 h-0.5 w-5 -translate-x-1/2 bg-ink-950"
              style={{ bottom: `${(cm / GAUGE_MAX) * 100}%` }}
            />
          ))}
          {DEPTHS.map(({ cm }) => (
            <span
              key={cm}
              className="absolute left-[calc(50%+1rem)] translate-y-1/2 font-display text-sm text-ink-950 tabular-nums"
              style={{ bottom: `${(cm / GAUGE_MAX) * 100}%` }}
            >
              {cm}
            </span>
          ))}
        </div>

        <div>
          <h2 className="font-display text-2xl tracking-[-0.01em] text-ink-950 ur:leading-[1.8] ur:tracking-normal">{t('safety.depthTitle')}</h2>
          <p className="mt-1 text-ink-600">{t('safety.depthIntro')}</p>
          <dl className="mt-5 divide-y divide-ink-100 border-y border-ink-100">
            {DEPTHS.map(({ cm, key }) => (
              <div key={cm} className="flex items-baseline gap-4 py-3">
                <dt className="w-20 shrink-0 font-display text-xl text-ink-950 tabular-nums">{cm} cm</dt>
                <dd className="text-ink-800">{t(`safety.${key}`)}</dd>
              </div>
            ))}
          </dl>
          <p className="mt-3 text-sm font-medium text-ink-950">{t('safety.turnAround')}</p>
          <p className="mt-1 text-xs text-ink-600">{t('safety.depthSource')}</p>
        </div>
      </section>

      {/* In an emergency */}
      <section className="mt-6 flex flex-wrap items-center justify-between gap-4 rounded-3xl bg-ink-950 p-6 text-paper sm:p-8">
        <div>
          <h2 className="font-display text-2xl tracking-[-0.01em] ur:leading-[1.8] ur:tracking-normal">{t('safety.emergencyTitle')}</h2>
          <p className="mt-1 max-w-xl text-paper/75">{t('safety.emergencyText')}</p>
        </div>
        <a
          href="tel:1122"
          className="shine relative inline-flex h-12 items-center overflow-hidden rounded-full bg-linear-to-tr from-teal-600 to-teal-400 px-6 text-base font-semibold text-ink-950 outline-none focus-visible:ring-2 focus-visible:ring-paper focus-visible:ring-offset-2 focus-visible:ring-offset-ink-950"
        >
          {t('dashboard.call')}
        </a>
      </section>

      <p className="mt-8 text-sm text-ink-600">{t('safety.reviewNote')}</p>
    </>
  )
}
