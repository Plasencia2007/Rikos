import { IconoBorrar } from './iconos'

/**
 * Teclado numerico tactil.
 *
 * Lo usan cuatro pantallas distintas: el PIN del mozo, el DNI y el PIN del
 * kiosco, las cantidades del pedido y el conteo de efectivo del cierre de caja.
 * Por eso vale la pena que este bien hecho desde el principio.
 */
interface Props {
  onDigito: (digito: string) => void
  onBorrar: () => void
  deshabilitado?: boolean
}

const TECLAS = ['1', '2', '3', '4', '5', '6', '7', '8', '9']

export function TecladoNumerico({ onDigito, onBorrar, deshabilitado }: Props) {
  return (
    <div className="teclado">
      {TECLAS.map((t) => (
        <button
          key={t}
          type="button"
          className="tecla"
          disabled={deshabilitado}
          onClick={() => onDigito(t)}
        >
          {t}
        </button>
      ))}
      <button
        type="button"
        className="tecla tecla-secundaria"
        disabled={deshabilitado}
        onClick={onBorrar}
        aria-label="Borrar"
      >
        <IconoBorrar width={22} height={22} />
      </button>
      <button
        type="button"
        className="tecla"
        disabled={deshabilitado}
        onClick={() => onDigito('0')}
      >
        0
      </button>
      <span />
    </div>
  )
}
