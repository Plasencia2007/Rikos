import { useId, type InputHTMLAttributes, type ReactNode } from 'react'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  etiqueta: ReactNode
  ayuda?: string
  error?: string
  icono?: ReactNode
  sufijo?: ReactNode
}

/** La etiqueta esta asociada al input por id, nunca suelta al lado. */
export function Campo({ etiqueta, ayuda, error, icono, sufijo, id, className = '', ...resto }: Props) {
  const idPropio = useId()
  const idFinal = id ?? idPropio
  const idAyuda = ayuda ? `${idFinal}-ayuda` : undefined
  const idError = error ? `${idFinal}-error` : undefined

  return (
    <div className={`campo ${error ? 'campo-invalido' : ''} ${className}`}>
      <label htmlFor={idFinal} className="campo-etiqueta">
        {icono && <span className="campo-etiqueta-icono" aria-hidden="true">{icono}</span>}
        {etiqueta}
      </label>
      <div className={`campo-control-envoltorio ${sufijo ? 'con-sufijo' : ''}`}>
        <input
          id={idFinal}
          className="campo-control"
          aria-invalid={Boolean(error) || undefined}
          aria-describedby={[idAyuda, idError].filter(Boolean).join(' ') || undefined}
          {...resto}
        />
        {sufijo && <div className="campo-sufijo">{sufijo}</div>}
      </div>
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
