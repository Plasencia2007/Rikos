import { useEffect, useState } from 'react'

/**
 * Cuenta regresiva en segundos. Usada por las pantallas de login y PIN
 * para mostrar cuanto falta de un bloqueo en vez de rechazar en silencio.
 */
export function useCuentaRegresiva() {
  const [hasta, setHasta] = useState<number | null>(null)
  const [segundosRestantes, setSegundosRestantes] = useState(0)

  function iniciar(segundosIniciales: number) {
    setHasta(Date.now() + segundosIniciales * 1000)
    setSegundosRestantes(segundosIniciales)
  }

  function limpiar() {
    setHasta(null)
    setSegundosRestantes(0)
  }

  useEffect(() => {
    if (hasta == null) return

    const id = window.setInterval(() => {
      const restante = Math.max(0, Math.round((hasta - Date.now()) / 1000))
      setSegundosRestantes(restante)
      if (restante <= 0) {
        setHasta(null)
      }
    }, 1000)

    return () => window.clearInterval(id)
  }, [hasta])

  return { activa: hasta != null, segundosRestantes, iniciar, limpiar }
}
