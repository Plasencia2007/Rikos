import type { ReactNode } from 'react'

interface Props {
  icono?: ReactNode
  titulo: string
  descripcion?: string
  accion?: ReactNode
}

/**
 * La pantalla que mas se ve el primer dia de uso -- antes de cargar el
 * primer local o el primer usuario, todo es esto -- y la que siempre se
 * olvida. Icono/ilustracion + texto que explica que hacer + la accion
 * principal, nunca una tabla en blanco sin mas.
 */
export function EstadoVacio({ icono, titulo, descripcion, accion }: Props) {
  return (
    <div className="estado-vacio">
      <div className="estado-vacio-icono" aria-hidden="true">
        {icono ?? '·'}
      </div>
      <h3 className="estado-vacio-titulo">{titulo}</h3>
      {descripcion && <p className="estado-vacio-descripcion">{descripcion}</p>}
      {accion && <div className="estado-vacio-accion">{accion}</div>}
    </div>
  )
}
