import type { InputHTMLAttributes } from 'react'

interface Props extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'role'> {
  /** Texto accesible cuando el interruptor no lleva etiqueta visible al lado. */
  etiqueta: string
}

/** Interruptor on/off: un checkbox real por debajo, presentado como switch. */
export function Interruptor({ etiqueta, className = '', ...resto }: Props) {
  return (
    <label className={`conmutador ${className}`}>
      <input type="checkbox" role="switch" aria-label={etiqueta} {...resto} />
      <span className="conmutador-pista" aria-hidden="true" />
    </label>
  )
}
