import type { ButtonHTMLAttributes } from 'react'

type Variante = 'primario' | 'secundario' | 'peligro' | 'texto'
type Tamano = 'normal' | 'tactil'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante
  tamano?: Tamano
  cargando?: boolean
}

export function Boton({
  variante = 'primario',
  tamano = 'normal',
  cargando = false,
  disabled,
  className = '',
  children,
  ...resto
}: Props) {
  return (
    <button
      className={`boton boton-${variante} boton-${tamano} ${cargando ? 'boton-cargando' : ''} ${className}`}
      disabled={disabled || cargando}
      aria-busy={cargando || undefined}
      {...resto}
    >
      {cargando && <span className="boton-espiral" aria-hidden="true" />}
      <span className="boton-contenido">{children}</span>
    </button>
  )
}
