import type { ReactNode } from 'react'

export interface Pestana {
  id: string
  etiqueta: string
  icono?: ReactNode
  /** Punto rojo cuando la pestaña tiene un error de validacion sin resolver. */
  conError?: boolean
}

interface Props {
  pestanas: Pestana[]
  activa: string
  onCambiar: (id: string) => void
}

/** Barra de pestañas del sistema de diseño. El contenido de cada pestaña lo renderiza quien la usa. */
export function Tabs({ pestanas, activa, onCambiar }: Props) {
  return (
    <div className="tabs" role="tablist">
      {pestanas.map((p) => (
        <button
          key={p.id}
          type="button"
          role="tab"
          aria-selected={p.id === activa}
          className={`tabs-boton ${p.id === activa ? 'activa' : ''}`}
          onClick={() => onCambiar(p.id)}
        >
          {p.icono}
          {p.etiqueta}
          {p.conError && <span className="tabs-punto-error" aria-label="Tiene campos por corregir" />}
        </button>
      ))}
    </div>
  )
}
