import { useCallback, useEffect, useState } from 'react'
import { listarMesas, type Mesa } from '../../api/cliente'

/** Las mesas activas del local del token (con su estado), para elegir una en los dialogos de reorganizar. */
export function useMesasDelLocal(token: string) {
  const [mesas, setMesas] = useState<Mesa[] | null>(null)
  const [error, setError] = useState('')

  const cargar = useCallback(() => {
    setError('')
    listarMesas(token)
      .then(setMesas)
      .catch(() => setError('No se pudieron cargar las mesas. Revisa la conexión e inténtalo otra vez.'))
  }, [token])

  useEffect(() => {
    cargar()
  }, [cargar])

  return { mesas, error, recargar: cargar }
}
