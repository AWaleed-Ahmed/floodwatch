/** "FloodWatch" in Clash Display with the teal gradient full stop. Size it with a text-* class. */
export function Wordmark({ className = '' }: { className?: string }) {
  return (
    <span className={`font-display leading-[0.9] tracking-[-0.02em] text-ink-950 ${className}`}>
      FloodWatch
      <span
        aria-hidden="true"
        data-teal-dot
        className="shine relative ms-[0.04em] inline-block size-[0.17em] overflow-hidden rounded-full bg-linear-to-tr from-teal-600 via-teal-400 to-paper align-baseline"
      />
    </span>
  )
}
