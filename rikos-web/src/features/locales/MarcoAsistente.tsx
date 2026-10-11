import type { ReactNode } from 'react'
import { Check, ChevronLeft } from 'lucide-react'

export type IdPaso = 'datos' | 'contacto' | 'operacion' | 'aspecto'

export const PASOS: { id: IdPaso; etiqueta: string }[] = [
  { id: 'datos', etiqueta: 'Datos del local' },
  { id: 'contacto', etiqueta: 'Contacto' },
  { id: 'operacion', etiqueta: 'Operación' },
  { id: 'aspecto', etiqueta: 'Aspecto' },
]

interface Props {
  paso: IdPaso
  /** Un local nuevo todavía no existe: solo el primer paso es alcanzable. */
  pasosHabilitados: boolean
  ruta: string[]
  titulo: string
  subtitulo: string
  onIrAPaso: (paso: IdPaso) => void
  onVolverALista: () => void
  lateral: ReactNode
  pie: ReactNode
  children: ReactNode
}

/**
 * Armazón común de los 4 pasos del asistente de locales: encabezado con
 * migas, barra de progreso, tarjeta principal + panel lateral, y una barra
 * inferior fija con las acciones del paso.
 */
export function MarcoAsistente({
  paso, pasosHabilitados, ruta, titulo, subtitulo, onIrAPaso, onVolverALista, lateral, pie, children,
}: Props) {
  const actual = PASOS.findIndex((p) => p.id === paso)

  return (
    <div className="as-pagina">
      <div className="as-cabecera">
        <button type="button" className="as-volver" onClick={onVolverALista} aria-label="Volver a Locales">
          <ChevronLeft size={18} aria-hidden="true" />
        </button>
        <div className="as-cabecera-texto">
          <nav className="as-migas" aria-label="Ruta">
            {ruta.map((m, i) => (
              <span key={i} className={i === ruta.length - 1 ? 'as-miga-actual' : undefined}>
                {i > 0 && ' / '}
                {m}
              </span>
            ))}
          </nav>
          <h1>{titulo}</h1>
          <p>{subtitulo}</p>
        </div>
        <span className="as-contador">Paso {actual + 1} de 4</span>
      </div>

      <ol className="as-progreso" aria-label="Progreso">
        {PASOS.map((p, i) => {
          const hecho = i < actual
          const vigente = i === actual
          const alcanzable = pasosHabilitados || vigente
          return (
            <li key={p.id} className={`as-progreso-item ${i < 3 ? 'as-progreso-item-con-linea' : ''}`}>
              <button
                type="button"
                className={`as-progreso-boton ${hecho ? 'hecho' : ''} ${vigente ? 'vigente' : ''}`}
                aria-current={vigente ? 'step' : undefined}
                disabled={!alcanzable}
                onClick={() => onIrAPaso(p.id)}
              >
                <span className="as-progreso-circulo">{hecho ? <Check size={16} strokeWidth={2.6} aria-hidden="true" /> : i + 1}</span>
                <span className="as-progreso-etiqueta">{p.etiqueta}</span>
              </button>
              {i < 3 && <span className={`as-progreso-linea ${i < actual ? 'hecha' : ''}`} aria-hidden="true" />}
            </li>
          )
        })}
      </ol>

      <div className="as-cuerpo">
        <section className="as-tarjeta">{children}</section>
        <aside className="as-lateral">{lateral}</aside>
      </div>

      <footer className="as-pie">{pie}</footer>
    </div>
  )
}
