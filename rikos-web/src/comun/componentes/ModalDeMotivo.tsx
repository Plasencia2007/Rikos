import { useState } from 'react'
import { Modal } from './Modal'
import { Boton } from './Boton'
import { Select } from './Select'
import { Campo } from './Campo'

export interface OpcionMotivo {
  id: string
  descripcion: string
  requiereObservacion?: boolean
}

interface Props {
  abierto: boolean
  titulo: string
  motivos: OpcionMotivo[]
  confirmando?: boolean
  onConfirmar: (motivoId: string, observacion: string) => void
  onCancelar: () => void
}

/**
 * Pide un motivo de una lista antes de confirmar una accion (RN-057 para
 * ajustes de inventario, anulaciones, descuentos, rechazo de abono...).
 * La tabla `motivo` con lista configurable por el propietario llega en el
 * sprint 3 (07 S Pendientes funcionales): hasta entonces este componente
 * recibe los motivos como prop, no los busca solo.
 */
export function ModalDeMotivo({ abierto, titulo, motivos, confirmando = false, onConfirmar, onCancelar }: Props) {
  const [motivoId, setMotivoId] = useState('')
  const [observacion, setObservacion] = useState('')

  const motivoElegido = motivos.find((m) => m.id === motivoId)
  const observacionObligatoria = Boolean(motivoElegido?.requiereObservacion)
  const puedeConfirmar = motivoId !== '' && (!observacionObligatoria || observacion.trim() !== '')

  function cerrar() {
    setMotivoId('')
    setObservacion('')
    onCancelar()
  }

  return (
    <Modal
      abierto={abierto}
      titulo={titulo}
      onCerrar={cerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={cerrar} disabled={confirmando}>
            Cancelar
          </Boton>
          <Boton
            variante="primario"
            disabled={!puedeConfirmar}
            cargando={confirmando}
            onClick={() => onConfirmar(motivoId, observacion)}
          >
            Confirmar
          </Boton>
        </>
      }
    >
      <Select etiqueta="Motivo" value={motivoId} onChange={(e) => setMotivoId(e.target.value)} required>
        <option value="" disabled>
          Elige un motivo…
        </option>
        {motivos.map((motivo) => (
          <option key={motivo.id} value={motivo.id}>
            {motivo.descripcion}
          </option>
        ))}
      </Select>
      <Campo
        etiqueta={observacionObligatoria ? 'Observación (obligatoria)' : 'Observación (opcional)'}
        value={observacion}
        onChange={(e) => setObservacion(e.target.value)}
      />
    </Modal>
  )
}
