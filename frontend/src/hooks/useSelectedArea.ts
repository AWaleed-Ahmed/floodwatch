import { useEffect, useState } from 'react'
import { api } from '../api/client'
import type { Area } from '../api/types'

const KEY = 'floodwatch.areaId'

function readSaved(): string | null {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

/** Loads the seeded areas and remembers the user's last choice on this device. */
export function useSelectedArea() {
  const [areas, setAreas] = useState<Area[]>([])
  const [areaId, setAreaIdState] = useState<string | null>(readSaved)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.areas()
      .then((list) => {
        setAreas(list)
        setAreaIdState((current) => (current && list.some((a) => a.id === current) ? current : list[0]?.id ?? null))
      })
      .catch((e: Error) => setError(e.message))
  }, [])

  const setAreaId = (id: string) => {
    setAreaIdState(id)
    try {
      localStorage.setItem(KEY, id)
    } catch {
      /* private mode: just don't remember */
    }
  }

  return { areas, areaId, setAreaId, error }
}
