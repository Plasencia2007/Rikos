import { TecladoNumerico } from './TecladoNumerico'

/**
 * HT-07: un solo componente para dos patrones de entrada numerica tactil.
 *
 * Modo PIN (largoFijo + oculto): puntos en vez de digitos, autoconfirma
 * al llegar al largo fijo. Lo usan el login del mozo/kiosco y el PIN del
 * formulario de usuario.
 *
 * Modo cantidad (sin oculto): se ve lo tecleado, sin autoconfirmar --
 * quien lo use decide cuando confirmar (boton aparte). Para cuando
 * existan las pantallas de cantidad de pedido y cierre de caja.
 */
interface Props {
  valor: string
  onCambiar: (valor: string) => void
  largoFijo?: number
  oculto?: boolean
  alCompletar?: (valor: string) => void
  deshabilitado?: boolean
}

export function EntradaNumerica({ valor, onCambiar, largoFijo, oculto, alCompletar, deshabilitado }: Props) {
  function agregarDigito(digito: string) {
    if (deshabilitado) return
    if (largoFijo && valor.length >= largoFijo) return
    const nuevo = valor + digito
    onCambiar(nuevo)
    if (largoFijo && nuevo.length === largoFijo) alCompletar?.(nuevo)
  }

  function borrar() {
    if (deshabilitado) return
    onCambiar(valor.slice(0, -1))
  }

  function limpiar() {
    if (deshabilitado) return
    onCambiar('')
  }

  return (
    <div className="entrada-numerica">
      {oculto && largoFijo ? (
        <div className="entrada-numerica-puntos" aria-label={`${valor.length} de ${largoFijo} dígitos`}>
          {Array.from({ length: largoFijo }).map((_, i) => (
            <span key={i} className={i < valor.length ? 'punto lleno' : 'punto'} />
          ))}
        </div>
      ) : (
        <div className="entrada-numerica-valor" aria-live="polite">
          {valor || '0'}
        </div>
      )}

      <TecladoNumerico
        onDigito={agregarDigito}
        onBorrar={borrar}
        onLimpiar={limpiar}
        deshabilitado={deshabilitado}
      />
    </div>
  )
}
