import type { InputHTMLAttributes } from 'react'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  etiqueta: string
}

/**
 * La etiqueta ENVUELVE al control: es la forma mas simple de que el texto
 * y la caja queden alineados y de que tocar el texto tambien marque la
 * casilla, sin depender de ids que alguien puede romper al copiar y pegar.
 */
export function Checkbox({ etiqueta, className = '', ...resto }: Props) {
  return (
    <label className={`opcion ${className}`}>
      <input type="checkbox" className="opcion-control" {...resto} />
      <span className="opcion-etiqueta">{etiqueta}</span>
    </label>
  )
}
