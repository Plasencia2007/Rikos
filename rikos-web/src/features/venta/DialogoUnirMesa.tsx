import { Aviso, Boton, EstadoCargando, EstadoVacio, Modal } from '../../comun/componentes'
import type { Mesa } from '../../api/cliente'
import { useMesasDelLocal } from './useMesasDelLocal'

/**
 * US-029: unir una mesa libre a la del pedido (para un grupo grande) y ver/separar las que ya estan unidas. El pedido
 * se opera en conjunto, por cualquiera de las mesas. Separar la deja libre e independiente.
 */
export function DialogoUnirMesa({
  token,
  mesaActualId,
  trabajando,
  onUnir,
  onSeparar,
  onCerrar,
}: {
  token: string
  mesaActualId: string
  trabajando: boolean
  onUnir: (mesa: Mesa) => void
  onSeparar: (mesa: Mesa) => void
  onCerrar: () => void
}) {
  const { mesas, error } = useMesasDelLocal(token)
  const libres = (mesas ?? []).filter((m) => m.estado === 'libre' && !m.tomandoPedido && m.id !== mesaActualId)
  const unidas = (mesas ?? []).filter((m) => m.mesaPrincipalId === mesaActualId)

  return (
    <Modal
      abierto
      titulo="Unir mesas"
      onCerrar={onCerrar}
      pie={
        <Boton variante="secundario" onClick={onCerrar}>
          Cerrar
        </Boton>
      }
    >
      <div className="ped-dialogo">
        {error && <Aviso tono="error">{error}</Aviso>}
        {!mesas && !error && <EstadoCargando />}
        {unidas.length > 0 && (
          <section aria-label="Mesas unidas">
            <h3 className="ped-subtitulo">Unidas a esta mesa</h3>
            <ul className="ped-lista-carta">
              {unidas.map((m) => (
                <li key={m.id} className="ped-unida">
                  <span>Mesa {m.numero}</span>
                  <Boton variante="secundario" cargando={trabajando} onClick={() => onSeparar(m)}>
                    Separar
                  </Boton>
                </li>
              ))}
            </ul>
          </section>
        )}
        {mesas && (
          <section aria-label="Mesas libres">
            <h3 className="ped-subtitulo">Unir una mesa libre</h3>
            {libres.length === 0 ? (
              <EstadoVacio titulo="No hay mesas libres" descripcion="Cuando se libere una, podrás unirla." />
            ) : (
              <ul className="ped-lista-carta">
                {libres.map((m) => (
                  <li key={m.id}>
                    <button type="button" className="ped-producto" disabled={trabajando} onClick={() => onUnir(m)}>
                      <span>
                        Mesa {m.numero}
                        {m.nombre ? <span className="cta-nota"> · {m.nombre}</span> : null}
                      </span>
                      {m.capacidad != null && <span className="cta-nota">{m.capacidad} sillas</span>}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}
      </div>
    </Modal>
  )
}
