import { useEffect, useRef, useState } from 'react'
import { PantallaPin } from '../../features/autenticacion'
import type { SesionIniciada } from '../../api/cliente'
import { Boton } from '../../comun/componentes/Boton'

const MINUTOS_INACTIVIDAD = Number(import.meta.env.VITE_POS_MINUTOS_INACTIVIDAD ?? 5)

/**
 * US-003: "sin perder nada" -- el pedido pertenece a la mesa, no al mozo.
 * Por eso cambiar de mozo (boton o inactividad) solo vuelve a la pantalla
 * de PIN; no cierra la sesion del local ni descarta nada en curso.
 */
export default function POS() {
  const [mozoActivo, setMozoActivo] = useState<SesionIniciada | null>(null)
  const temporizador = useRef<ReturnType<typeof window.setTimeout> | undefined>(undefined)

  useEffect(() => {
    if (!mozoActivo) return

    function reiniciar() {
      if (temporizador.current) window.clearTimeout(temporizador.current)
      temporizador.current = window.setTimeout(() => setMozoActivo(null), MINUTOS_INACTIVIDAD * 60_000)
    }

    reiniciar()
    window.addEventListener('pointerdown', reiniciar)
    window.addEventListener('keydown', reiniciar)
    return () => {
      window.removeEventListener('pointerdown', reiniciar)
      window.removeEventListener('keydown', reiniciar)
      if (temporizador.current) window.clearTimeout(temporizador.current)
    }
  }, [mozoActivo])

  if (!mozoActivo) {
    return <PantallaPin onExito={setMozoActivo} />
  }

  return (
    <div>
      <header className="encabezado-local">
        <span>Mozo activo: {mozoActivo.nombreUsuario}</span>
        <Boton variante="secundario" onClick={() => setMozoActivo(null)}>
          Cambiar de mozo
        </Boton>
      </header>
      <div className="marcador">POS — por construir (US-027)</div>
    </div>
  )
}
