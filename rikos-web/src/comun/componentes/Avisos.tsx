import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { CircleAlert, CircleCheck, X } from 'lucide-react'

type Tono = 'exito' | 'error'
interface AvisoEnPantalla {
  id: number
  texto: string
  tono: Tono
}

interface Avisos {
  /** Muestra un aviso corto en una esquina; se cierra solo a los 4 s. */
  mostrar: (texto: string, tono?: Tono) => void
}

const SEGUNDOS_VISIBLE = 4
const ContextoAvisos = createContext<Avisos>({ mostrar: () => undefined })

/** Avisos que confirman una accion corta ("Mesa abierta", "Comanda enviada") sin sacar al usuario de la pantalla. */
export function ProveedorDeAvisos({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<AvisoEnPantalla[]>([])
  const siguiente = useRef(1)
  const temporizadores = useRef(new Map<number, number>())

  const cerrar = useCallback((id: number) => {
    setAvisos((lista) => lista.filter((a) => a.id !== id))
    const t = temporizadores.current.get(id)
    if (t) window.clearTimeout(t)
    temporizadores.current.delete(id)
  }, [])

  const mostrar = useCallback(
    (texto: string, tono: Tono = 'exito') => {
      const id = siguiente.current++
      setAvisos((lista) => [...lista.slice(-2), { id, texto, tono }])
      temporizadores.current.set(id, window.setTimeout(() => cerrar(id), SEGUNDOS_VISIBLE * 1000))
    },
    [cerrar],
  )

  useEffect(() => {
    const mapa = temporizadores.current
    return () => mapa.forEach((t) => window.clearTimeout(t))
  }, [])

  const valor = useMemo(() => ({ mostrar }), [mostrar])
  return (
    <ContextoAvisos.Provider value={valor}>
      {children}
      <div className="avisos-pila" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} role={a.tono === 'error' ? 'alert' : 'status'} className={`aviso-flotante aviso-flotante-${a.tono}`}>
            {a.tono === 'error' ? <CircleAlert size={22} aria-hidden="true" /> : <CircleCheck size={22} aria-hidden="true" />}
            <span>{a.texto}</span>
            <button type="button" aria-label="Cerrar aviso" onClick={() => cerrar(a.id)}>
              <X size={16} aria-hidden="true" />
            </button>
          </div>
        ))}
      </div>
    </ContextoAvisos.Provider>
  )
}

export const useAvisos = () => useContext(ContextoAvisos)
