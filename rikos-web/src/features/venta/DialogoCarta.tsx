import { useMemo, useState } from 'react'
import { Boton, Campo, EstadoVacio, Modal } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import type { ProductoOfrecido } from '../../api/cliente'

/** La carta del local (sin los agotados): tocar un producto lo agrega con cantidad 1; la cantidad se cambia en la lista. */
export function DialogoCarta({
  carta,
  trabajando,
  onElegir,
  onCerrar,
}: {
  carta: ProductoOfrecido[]
  trabajando: boolean
  onElegir: (producto: ProductoOfrecido) => void
  onCerrar: () => void
}) {
  const [busqueda, setBusqueda] = useState('')
  const visibles = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return q ? carta.filter((p) => p.nombre.toLowerCase().includes(q)) : carta
  }, [carta, busqueda])

  return (
    <Modal
      abierto
      titulo="Carta"
      onCerrar={onCerrar}
      pie={
        <Boton variante="secundario" onClick={onCerrar}>
          Cerrar
        </Boton>
      }
    >
      <div className="ped-dialogo">
        {carta.length === 0 ? (
          <EstadoVacio
            titulo="La carta está vacía"
            descripcion="No hay productos con precio disponibles en este local (o están todos agotados)."
          />
        ) : (
          <>
            <Campo etiqueta="Buscar" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
            <ul className="ped-lista-carta">
              {visibles.map((p) => (
                <li key={p.id}>
                  <button type="button" className="ped-producto" disabled={trabajando} onClick={() => onElegir(p)}>
                    <span>{p.nombre}</span>
                    <strong>{enSoles(p.precio)}</strong>
                  </button>
                </li>
              ))}
              {visibles.length === 0 && <li className="prod-vacio">Nada coincide con "{busqueda}".</li>}
            </ul>
          </>
        )}
      </div>
    </Modal>
  )
}
