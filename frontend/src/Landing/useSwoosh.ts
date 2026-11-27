import { useCallback, useEffect, useRef } from 'react'

/** Run work once the browser is idle (or after `timeout` ms at the latest). */
export const whenIdle = (work: () => void, timeout = 1500) => {
  if (typeof window.requestIdleCallback === 'function') {
    const id = window.requestIdleCallback(work, { timeout })
    return () => window.cancelIdleCallback(id)
  }
  const id = setTimeout(work, 300)
  return () => clearTimeout(id)
}

/**
 * Preloads a short sound into memory once the page is idle and returns a
 * `play()` that starts it with almost no delay. Web Audio is used instead of
 * an <audio> element because a decoded buffer starts instantly, while an
 * <audio> element still has to fetch and decode on the first click.
 */
export function useSwoosh(url: string, { volume = 1, start = 0 } = {}) {
  const contextRef = useRef<AudioContext | null>(null)
  const bufferRef = useRef<AudioBuffer | null>(null)

  useEffect(() => {
    let cancelled = false
    const cancelIdle = whenIdle(async () => {
      try {
        const context = new AudioContext()
        const data = await (await fetch(url)).arrayBuffer()
        const buffer = await context.decodeAudioData(data)
        if (cancelled) {
          void context.close()
          return
        }
        contextRef.current = context
        bufferRef.current = buffer
      } catch {
        bufferRef.current = null
      }
    })

    /** Browsers keep audio suspended until the first user gesture; wake it early so the first play is instant. */
    const unlock = () => void contextRef.current?.resume()
    window.addEventListener('pointerdown', unlock, { capture: true })
    window.addEventListener('keydown', unlock, { capture: true })

    return () => {
      cancelled = true
      cancelIdle()
      window.removeEventListener('pointerdown', unlock, { capture: true })
      window.removeEventListener('keydown', unlock, { capture: true })
      void contextRef.current?.close()
      contextRef.current = null
    }
  }, [url])

  return useCallback(() => {
    const context = contextRef.current
    const buffer = bufferRef.current
    if (!context || !buffer) return
    void context.resume()
    const gain = context.createGain()
    gain.gain.value = volume
    const source = context.createBufferSource()
    source.buffer = buffer
    source.connect(gain).connect(context.destination)
    source.start(0, start)
  }, [volume, start])
}
