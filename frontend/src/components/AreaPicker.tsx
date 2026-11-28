import { useEffect, useId, useLayoutEffect, useRef, useState, type KeyboardEvent, type TransitionEvent } from 'react'
import { useTranslation } from 'react-i18next'
import type { Area } from '../api/types'

interface Props {
  areas: Area[]
  areaId: string | null
  onChange: (id: string) => void
}

/** Closed pill size; the open card grows out of it. */
const PILL_HEIGHT = 44
const OPTION_HEIGHT = 56
const CARD_PADDING = 8
const CARD_MIN_WIDTH = 288

/**
 * Area picker in the same spirit as the Menu pill: the pill itself grows (width,
 * height and corner radius on the spring curve) into a rounded card listing the
 * areas, whose options rise in one after another; choosing one shrinks it back
 * with a small settling bounce.
 *
 * It is a proper listbox: Enter/Space/Arrow keys open it, arrows move, Home/End
 * jump, typing a letter jumps to that area, Enter picks, Escape or a click
 * outside closes, and screen readers hear the role, the choice and the
 * selection. The pill keeps its own space in the layout, so opening never
 * pushes the page around.
 */
export function AreaPicker({ areas, areaId, onChange }: Props) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const [pillWidth, setPillWidth] = useState(0)
  const rootRef = useRef<HTMLDivElement>(null)
  const pillRef = useRef<HTMLButtonElement>(null)
  const shapeRef = useRef<HTMLDivElement>(null)
  const listRef = useRef<HTMLUListElement>(null)
  const labelId = useId()
  const listId = useId()

  const selectedIndex = Math.max(0, areas.findIndex((a) => a.id === areaId))
  const selected = areas[selectedIndex]
  const openHeight = PILL_HEIGHT + CARD_PADDING * 2 + areas.length * OPTION_HEIGHT
  const openWidth = Math.max(pillWidth, CARD_MIN_WIDTH)

  /*
   * The closed pill's width follows its text (area name, font, language), so
   * watch its size and keep the shape growing from exactly that width.
   */
  useLayoutEffect(() => {
    const pill = pillRef.current
    if (!pill) return
    setPillWidth(pill.offsetWidth)
    const observer = new ResizeObserver(() => setPillWidth(pill.offsetWidth))
    observer.observe(pill)
    return () => observer.disconnect()
  }, [])

  const show = () => {
    setActive(selectedIndex)
    setOpen(true)
  }
  const hide = (refocus = true) => {
    setOpen(false)
    if (refocus) pillRef.current?.focus({ preventScroll: true })
  }
  const choose = (index: number) => {
    const area = areas[index]
    if (area && area.id !== areaId) onChange(area.id)
    hide()
  }

  /* Close on a click anywhere else. */
  useEffect(() => {
    if (!open) return
    const onPointer = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) hide(false)
    }
    window.addEventListener('pointerdown', onPointer)
    const focusTimer = window.setTimeout(() => listRef.current?.focus({ preventScroll: true }), 60)
    return () => {
      window.removeEventListener('pointerdown', onPointer)
      window.clearTimeout(focusTimer)
    }
  }, [open])

  const onPillKey = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(event.key)) {
      event.preventDefault()
      show()
    }
  }

  const onListKey = (event: KeyboardEvent<HTMLUListElement>) => {
    const last = areas.length - 1
    if (event.key === 'ArrowDown') setActive((i) => Math.min(last, i + 1))
    else if (event.key === 'ArrowUp') setActive((i) => Math.max(0, i - 1))
    else if (event.key === 'Home') setActive(0)
    else if (event.key === 'End') setActive(last)
    else if (event.key === 'Enter' || event.key === ' ') choose(active)
    else if (event.key === 'Escape' || event.key === 'Tab') {
      if (event.key === 'Escape') event.preventDefault()
      hide(event.key === 'Escape')
      return
    } else if (event.key.length === 1) {
      const letter = event.key.toLowerCase()
      const match = areas.findIndex((a, i) => i > active && a.name.toLowerCase().startsWith(letter))
      const wrap = match === -1 ? areas.findIndex((a) => a.name.toLowerCase().startsWith(letter)) : match
      if (wrap !== -1) setActive(wrap)
      return
    } else return
    event.preventDefault()
  }

  /** When the card has shrunk back into the pill, one small settling bounce (like the Menu pill). */
  const landBounce = (event: TransitionEvent<HTMLDivElement>) => {
    if (open || event.target !== event.currentTarget || event.propertyName !== 'height') return
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return
    event.currentTarget.animate(
      [{ scale: '1' }, { scale: '1.04', offset: 0.35 }, { scale: '0.99', offset: 0.7 }, { scale: '1' }],
      { duration: 360, easing: 'ease-out' },
    )
  }

  const stagger = (index: number) => ({ transitionDelay: open ? `${140 + index * 45}ms` : '0ms' })

  return (
    <div className="inline-flex items-center gap-3 text-sm font-medium text-ink-800">
      <span id={labelId}>{t('common.area')}</span>
      {/* Holds the closed pill's space so the page never shifts while the card is open. */}
      <div ref={rootRef} className="relative" style={{ width: pillWidth || undefined, height: PILL_HEIGHT }}>
        <div
          ref={shapeRef}
          onTransitionEnd={landBounce}
          className={`absolute top-0 left-0 z-30 overflow-hidden border border-ink-200 bg-paper transition-[width,height,border-radius,box-shadow] motion-reduce:transition-none ${
            open
              ? 'rounded-[1.5rem] shadow-[0_18px_48px_rgba(0,0,0,0.14)] duration-[700ms] ease-spring'
              : 'rounded-[1.375rem] shadow-none duration-[420ms] ease-[cubic-bezier(0.6,0,0.25,1)]'
          }`}
          style={{ width: open ? openWidth : pillWidth || 'auto', height: open ? openHeight : PILL_HEIGHT }}
        >
          <button
            ref={pillRef}
            type="button"
            aria-haspopup="listbox"
            aria-expanded={open}
            aria-controls={listId}
            aria-labelledby={`${labelId} ${listId}-value`}
            onClick={() => (open ? hide() : show())}
            onKeyDown={onPillKey}
            className="inline-flex h-11 items-center gap-3 rounded-full px-4 text-base whitespace-nowrap text-ink-950 outline-none focus-visible:ring-2 focus-visible:ring-teal-500 focus-visible:ring-inset"
          >
            <span id={`${listId}-value`}>{selected ? selected.name : '…'}</span>
            <svg
              aria-hidden="true"
              viewBox="0 0 16 16"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
              className={`size-4 shrink-0 text-ink-600 transition-transform duration-300 ${open ? 'rotate-180' : ''}`}
            >
              <path d="M4 6l4 4 4-4" />
            </svg>
          </button>

          <ul
            ref={listRef}
            id={listId}
            role="listbox"
            tabIndex={-1}
            aria-labelledby={labelId}
            aria-activedescendant={open ? `${listId}-${active}` : undefined}
            onKeyDown={onListKey}
            className="px-2 pb-2 outline-none"
            inert={!open}
          >
            {areas.map((area, index) => {
              const isSelected = index === selectedIndex
              const isActive = index === active
              return (
                <li
                  key={area.id}
                  id={`${listId}-${index}`}
                  role="option"
                  aria-selected={isSelected}
                  onPointerEnter={() => setActive(index)}
                  onClick={() => choose(index)}
                  style={{ height: OPTION_HEIGHT, ...stagger(index) }}
                  className={`flex cursor-pointer items-center gap-3 rounded-2xl px-3 transition-[opacity,translate,background-color] duration-300 motion-reduce:transition-none ${
                    open ? 'translate-y-0 opacity-100' : 'translate-y-2 opacity-0'
                  } ${isActive ? 'bg-ink-50' : ''}`}
                >
                  <span
                    aria-hidden="true"
                    className={`size-2.5 shrink-0 rounded-full bg-linear-to-tr from-teal-600 via-teal-400 to-paper transition-transform duration-300 ${
                      isSelected ? 'scale-100' : 'scale-0'
                    }`}
                  />
                  <span className="flex min-w-0 flex-col">
                    <span className="truncate text-base font-medium text-ink-950">{area.name}</span>
                    <span className="truncate text-xs text-ink-600">{area.riverName}</span>
                  </span>
                </li>
              )
            })}
          </ul>
        </div>
      </div>
    </div>
  )
}
