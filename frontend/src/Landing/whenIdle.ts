/** Run work once the browser is idle (or after `timeout` ms at the latest). Returns a cancel function. */
export const whenIdle = (work: () => void, timeout = 1500) => {
  if (typeof window.requestIdleCallback === 'function') {
    const id = window.requestIdleCallback(work, { timeout })
    return () => window.cancelIdleCallback(id)
  }
  const id = setTimeout(work, 300)
  return () => clearTimeout(id)
}
