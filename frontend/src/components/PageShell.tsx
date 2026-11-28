import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { ThemeToggle } from './ThemeToggle'
import { Wordmark } from './Wordmark'

/** Layout for every inner page: wordmark home link at the top (the theme switch and Menu pill float top right), content, then the disclaimer footer. Marked .themed, so it follows the light/dark choice. */
export function PageShell({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  return (
    <div className="themed themed-page flex min-h-svh flex-col bg-paper text-ink-950">
      <ThemeToggle />
      <header dir="ltr" className="flex h-24 items-center pr-[calc(5vw+11rem)] pl-[5vw]">
        <Link to="/" className="rounded-lg outline-none focus-visible:ring-2 focus-visible:ring-teal-500">
          <Wordmark className="text-3xl" />
        </Link>
      </header>
      <main data-lang-fade className="mx-auto w-full max-w-5xl flex-1 px-[5vw] pb-16 lg:px-0">{children}</main>
      <footer data-lang-fade className="border-t border-ink-100 px-[5vw] py-6 text-center text-sm text-ink-600">{t('common.footer')}</footer>
    </div>
  )
}
