import { useEffect, useState } from 'react'
import { consultarSalud, type Salud } from '../../api/cliente'

const SERVICIOS = ['/api/salud', '/facturacion/salud'] as const

/**
 * Comprobacion visual del esqueleto (Sprint 0, HT-01). Vivia embebida en
 * PantallaPin como atajo; se separo a su propia ruta en el Sprint 1 porque
 * PantallaPin ya cumple su funcion real (US-003).
 */
export default function Diagnostico() {
  const [resultados, setResultados] = useState<Record<string, string>>({})

  useEffect(() => {
    let cancelado = false

    Promise.allSettled(SERVICIOS.map((ruta) => consultarSalud(ruta))).then((respuestas) => {
      if (cancelado) return
      const siguiente: Record<string, string> = {}
      respuestas.forEach((r, i) => {
        const ruta = SERVICIOS[i]
        siguiente[ruta] =
          r.status === 'fulfilled'
            ? `${(r.value as Salud).servicio} · perfil ${(r.value as Salud).perfilActivo} · bd ${(r.value as Salud).baseDeDatos ?? 'n/a'} · ${(r.value as Salud).estado}`
            : 'sin respuesta'
      })
      setResultados(siguiente)
    })

    return () => {
      cancelado = true
    }
  }, [])

  return (
    <div style={{ padding: 24, fontFamily: 'monospace' }}>
      <h1>Diagnostico del esqueleto</h1>
      <p>Salud de los servicios, vista a traves del gateway:</p>
      {SERVICIOS.map((ruta) => (
        <p key={ruta}>
          {ruta}: {resultados[ruta] ?? 'comprobando…'}
        </p>
      ))}
    </div>
  )
}
