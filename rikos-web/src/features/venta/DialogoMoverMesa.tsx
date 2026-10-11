import { Aviso, Boton, EstadoCargando, EstadoVacio, Modal } from '../../comun/componentes'
import type { Mesa } from '../../api/cliente'
import { useMesasDelLocal } from './useMesasDelLocal'

/** US-029: mover el pedido a otra mesa LIBRE. La mesa de hoy queda libre sola. */
export function DialogoMoverMesa({
  token,
  mesaActualId,
  trabajando,
  onMover,
  onCerrar,
}: {
  token: string
  mesaActualId: string | null
  trabajando: boolean
  onMover: (mesa: Mesa) => void
  onCerrar: () => void
}) {
  const { mesas, error } = useMesasDelLocal(token)
  const libres = (mesas ?? []).filter((m) => m.estado === 'libre' && !m.tomandoPedido && m.id !== mesaActualId)

  return (
    <Modal
      abierto
      titulo="Mover a otra mesa"
      onCerrar={onCerrar}
      pie={
        <Boton variante="secundario" onClick={onCerrar}>
          Cancelar
        </Boton>
      }
    >
      <div className="ped-dialogo">
        {error && <Aviso tono="error">{error}</Aviso>}
        {!mesas && !error && <EstadoCargando />}
        {mesas && libres.length === 0 && (
          <EstadoVacio titulo="No hay mesas libres" descripcion="Cuando se libere una, podrás mover el pedido." />
        )}
        <ul className="ped-lista-carta">
          {libres.map((m) => (
            <li key={m.id}>
              <button type="button" className="ped-producto" disabled={trabajando} onClick={() => onMover(m)}>
                <span>
                  Mesa {m.numero}
                  {m.nombre ? <span className="cta-nota"> · {m.nombre}</span> : null}
                </span>
                {m.capacidad != null && <span className="cta-nota">{m.capacidad} sillas</span>}
              </button>
            </li>
          ))}
        </ul>
      </div>
    </Modal>
  )
}
