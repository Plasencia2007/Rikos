import { useId, type ReactNode, type SelectHTMLAttributes } from 'react'

interface Props extends SelectHTMLAttributes<HTMLSelectElement> {
  etiqueta: string
  ayuda?: string
  error?: string
  children: ReactNode
}

/** Igual criterio que Campo: etiqueta asociada por id, nunca suelta. */
export function Select({ etiqueta, ayuda, error, id, className = '', children, ...resto }: Props) {
  const idPropio = useId()
  const idFinal = id ?? idPropio
  const idAyuda = ayuda ? `${idFinal}-ayuda` : undefined
  const idError = error ? `${idFinal}-error` : undefined

  return (
    <div className={`campo ${error ? 'campo-invalido' : ''} ${className}`}>
      <label htmlFor={idFinal} className="campo-etiqueta">
        {etiqueta}
      </label>
      <select
        id={idFinal}
        className="campo-control campo-select"
        aria-invalid={Boolean(error) || undefined}
        aria-describedby={[idAyuda, idError].filter(Boolean).join(' ') || undefined}
        {...resto}
      >
        {children}
      </select>
      {ayuda && !error && (
        <p id={idAyuda} className="campo-ayuda">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={idError} className="campo-error">
          {error}
        </p>
      )}
    </div>
  )
}
