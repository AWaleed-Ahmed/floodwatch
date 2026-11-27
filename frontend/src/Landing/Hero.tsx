/** FloodWatch wordmark and tagline, set in black near the top-left of the background. */
export default function Hero() {
  return (
    <div className="flex flex-col items-start ps-[5vw] pe-4 pt-[16svh] text-start">
      <h1 className="font-display text-[clamp(2.75rem,9vw,8rem)] leading-[0.9] tracking-[-0.02em] text-ink-950">
        FloodWatch
        <span
          aria-hidden="true"
          className="ms-[0.04em] inline-block size-[0.17em] rounded-full bg-linear-to-tr from-teal-600 via-teal-400 to-white align-baseline"
        />
      </h1>
      <p className="mt-4 ps-[0.08em] text-[clamp(1rem,1.6vw,1.25rem)] font-medium tracking-[-0.01em] text-ink-800">
        Rain forecasts, river levels and flood risk
        <br />
        for where you live.
      </p>
    </div>
  )
}
