import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Wordmark } from './Wordmark'

/** Layout for every inner page: wordmark home link at the top (the Menu pill floats top right), content, then the disclaimer footer. */
export function PageShell({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  return (
    <div className="flex min-h-svh flex-col bg-paper">
      <header className="flex h-24 items-center ps-[5vw] pe-[calc(5vw+7.5rem)]">
        <Link to="/" className="rounded-lg outline-none focus-visible:ring-2 focus-visible:ring-teal-500">
          <Wordmark className="text-3xl" />
        </Link>
      </header>
      <main className="mx-auto w-full max-w-5xl flex-1 px-[5vw] pb-16 lg:px-0">{children}</main>
      <footer className="border-t border-ink-100 px-[5vw] py-6 text-center text-sm text-ink-600">{t('common.footer')}</footer>
    </div>
  )
}
