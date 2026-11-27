import type { ReactNode } from 'react'

type Tone = 'error' | 'warn' | 'info'

const TONES: Record<Tone, string> = {
  error: 'bg-red-50 text-red-900 ring-red-600/20',
  warn: 'bg-amber-50 text-amber-900 ring-amber-600/25',
  info: 'bg-teal-50 text-teal-900 ring-teal-600/20',
}

const MARKS: Record<Tone, string> = { error: '!', warn: '!', info: 'i' }

/** Full-width notice; the tone is carried by the mark and the words as well as the colour. */
export function Banner({ tone, children }: { tone: Tone; children: ReactNode }) {
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`my-4 flex items-start gap-3 rounded-2xl px-4 py-3 ring-1 ring-inset ${TONES[tone]}`}>
      <span aria-hidden="true" className="mt-0.5 grid size-5 shrink-0 place-items-center rounded-full bg-current/15 text-xs font-bold">
        {MARKS[tone]}
      </span>
      <p className="leading-relaxed">{children}</p>
    </div>
  )
}
