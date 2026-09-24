import { Modal } from './Modal'
import { Boton } from './Boton'
import { Aviso } from './Aviso'

interface Props {
  abierto: boolean
  titulo: string
  mensaje: string
  textoConfirmar?: string
  peligro?: boolean
  confirmando?: boolean
  /** Si la confirmacion fallo (ej. no se pudo borrar), se muestra aca sin cerrar el modal. */
  error?: string
  onConfirmar: () => void
  onCancelar: () => void
}

/**
 * Para cualquier accion que no se pueda deshacer con un segundo clic:
 * eliminar un local, desactivar un usuario, etc. El mensaje explica la
 * consecuencia, no solo pregunta "?seguro?".
 */
export function ModalDeConfirmacion({
  abierto,
  titulo,
  mensaje,
  textoConfirmar = 'Confirmar',
  peligro = false,
  confirmando = false,
  error,
  onConfirmar,
  onCancelar,
}: Props) {
  return (
    <Modal
      abierto={abierto}
      titulo={titulo}
      onCerrar={onCancelar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCancelar} disabled={confirmando}>
            Cancelar
          </Boton>
          <Boton variante={peligro ? 'peligro' : 'primario'} onClick={onConfirmar} cargando={confirmando}>
            {textoConfirmar}
          </Boton>
        </>
      }
    >
      <p>{mensaje}</p>
      {error && <Aviso tono="error">{error}</Aviso>}
    </Modal>
  )
}
