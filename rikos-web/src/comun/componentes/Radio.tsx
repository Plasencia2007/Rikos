import type { InputHTMLAttributes } from 'react'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  etiqueta: string
}

/** Mismo criterio que Checkbox: la etiqueta envuelve al control. */
export function Radio({ etiqueta, className = '', ...resto }: Props) {
  return (
    <label className={`opcion ${className}`}>
      <input type="radio" className="opcion-control" {...resto} />
      <span className="opcion-etiqueta">{etiqueta}</span>
    </label>
  )
}
