import type { ReactNode } from 'react'

interface Props {
  mensaje?: string
  accion?: ReactNode
}

export function EstadoError({ mensaje = 'Algo salio mal. Intenta de nuevo.', accion }: Props) {
  return (
    <div className="estado-error" role="alert">
      <p>{mensaje}</p>
      {accion}
    </div>
  )
}
