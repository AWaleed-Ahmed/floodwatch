import { useEffect, useState, type CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import type { SupportContact } from '../api/types'

/**
 * "Need immediate help? Contact us" at the bottom-left of the landing page,
 * lined up with the title. "Contact us" opens a new email to the support
 * address from GET /api/support, so the address can change on the server
 * (SUPPORT_EMAIL) without a frontend release. Until that loads, or if the
 * server can't be reached, the words show without a link.
 */
export default function HelpLine() {
  const { t, i18n } = useTranslation()
  const [support, setSupport] = useState<SupportContact | null>(null)

  useEffect(() => {
    let current = true
    api
      .support()
      .then((contact) => current && setSupport(contact))
      .catch(() => {})
    return () => {
      current = false
    }
  }, [])

  return (
    <div dir="ltr" data-topography-ignore className="absolute bottom-[6svh] left-[3.5vw] pr-4">
      <p
        dir={i18n.dir()}
        data-lang-fade
        className="hero-float text-[clamp(0.95rem,1.3vw,1.1rem)] font-medium text-ink-800 ur:leading-[2.1]"
        style={{ '--delay': '1300ms' } as CSSProperties}
      >
        {t('help.question')}{' '}
        {support ? (
          <a
            href={support.mailto}
            title={support.email}
            className="font-semibold text-ink-950 underline decoration-ink-950/40 underline-offset-4 outline-none transition-colors hover:decoration-ink-950 focus-visible:rounded focus-visible:ring-2 focus-visible:ring-teal-500"
          >
            {t('help.contact')}
          </a>
        ) : (
          <span className="font-semibold text-ink-950">{t('help.contact')}</span>
        )}
      </p>
    </div>
  )
}
