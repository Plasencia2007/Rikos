import { useState } from 'react'
import { Boton, Campo, Modal } from '../../comun/componentes'

/** La nota para cocina de una linea pendiente ("sin cebolla"). Vacia = sin nota. */
export function DialogoNota({
  inicial,
  trabajando,
  onGuardar,
  onCerrar,
}: {
  inicial: string | null
  trabajando: boolean
  onGuardar: (nota: string | null) => void
  onCerrar: () => void
}) {
  const [nota, setNota] = useState(inicial ?? '')
  return (
    <Modal
      abierto
      titulo="Nota para cocina"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} onClick={() => onGuardar(nota.trim() || null)}>
            Guardar
          </Boton>
        </>
      }
    >
      <div className="ped-dialogo">
        <Campo etiqueta="Nota" maxLength={250} value={nota} autoFocus onChange={(e) => setNota(e.target.value)} />
      </div>
    </Modal>
  )
}
