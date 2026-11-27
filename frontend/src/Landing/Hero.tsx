/** Big FloodWatch wordmark and tagline, left-aligned in the upper half of the background. */
export default function Hero() {
  return (
    <div className="flex flex-col items-start ps-[8vw] pe-4 pt-[18svh] text-start">
      <h1 className="font-display text-[clamp(3.5rem,15vw,13rem)] leading-[0.9] font-semibold tracking-[-0.02em] text-white">
        FloodWatch
      </h1>
      <p className="mt-6 ps-[0.08em] text-[clamp(1.05rem,2vw,1.5rem)] font-medium tracking-[-0.01em] text-white/75">
        Know the river before it reaches you.
      </p>
    </div>
  )
}
