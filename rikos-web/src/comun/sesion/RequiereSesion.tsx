import type { ReactNode } from 'react'
import { useSesion } from './SesionContexto'
import { PantallaLogin } from '../../features/autenticacion'

/** Envuelve una superficie entera: sin sesion, muestra el login y no deja pasar. */
export function RequiereSesion({ children }: { children: ReactNode }) {
  const { sesion } = useSesion()

  if (!sesion) {
    return <PantallaLogin />
  }

  return <>{children}</>
}
