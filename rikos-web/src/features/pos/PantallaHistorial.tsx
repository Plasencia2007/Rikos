import { useEffect, useState } from 'react'
import { CalendarDays, Search, User, UtensilsCrossed, ShoppingBag } from 'lucide-react'
import { Boton, EstadoCargando, EstadoError, EstadoVacio } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import { listarPedidos, type PaginaDePedidos } from '../../api/cliente'
import { ETIQUETA_DE_CANAL, tituloDelPedido } from './pedidosVista'

const POR_PAGINA = 24

/**
 * Historial de órdenes cerradas. Hasta el sprint 7 (cobrar y cerrar) el pedido
 * no tiene estado "cerrado": la lista llega vacía y se muestra el estado vacío.
 */
export function PantallaHistorial({
  token,
  onVerPedido,
}: {
  token: string
  onVerPedido: (pedidoId: string, titulo: string) => void
}) {
  const [texto, setTexto] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [canal, setCanal] = useState<'' | 'mesa' | 'para_llevar'>('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [pagina, setPagina] = useState(0)
  const [datos, setDatos] = useState<PaginaDePedidos | null>(null)
  const [error, setError] = useState(false)
  const [intento, setIntento] = useState(0)

  // La busqueda por texto espera a que se deje de escribir.
  useEffect(() => {
    const t = window.setTimeout(() => {
      setBusqueda(texto.trim())
      setPagina(0)
    }, 350)
    return () => window.clearTimeout(t)
  }, [texto])

  useEffect(() => {
    let vivo = true
    listarPedidos(token, {
      situacion: 'cerrados',
      canal: canal || undefined,
      q: busqueda,
      desde,
      hasta,
      pagina,
      tamano: POR_PAGINA,
    })
      .then((p) => {
        if (!vivo) return
        setDatos(p)
        setError(false)
      })
      .catch(() => vivo && setError(true))
    return () => {
      vivo = false
    }
  }, [token, canal, busqueda, desde, hasta, pagina, intento])

  return (
    <div className="pos-historial">
      <section className="pos-filtros" aria-label="Filtros">
        <label className="pos-filtro-busqueda">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Buscar por #orden, cliente o mesa..."
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
          />
        </label>
        <select aria-label="Tipo" value={canal} onChange={(e) => { setCanal(e.target.value as typeof canal); setPagina(0) }}>
          <option value="">Todos los tipos</option>
          <option value="mesa">Para comer aquí</option>
          <option value="para_llevar">Para llevar</option>
        </select>
        <label className="pos-filtro-fecha">
          <CalendarDays size={16} aria-hidden="true" />
          <input type="date" aria-label="Desde" value={desde} onChange={(e) => { setDesde(e.target.value); setPagina(0) }} />
          <span>—</span>
          <input type="date" aria-label="Hasta" value={hasta} onChange={(e) => { setHasta(e.target.value); setPagina(0) }} />
        </label>
      </section>

      {error && !datos ? (
        <EstadoError mensaje="No se pudo cargar el historial." accion={<Boton onClick={() => setIntento((n) => n + 1)}>Reintentar</Boton>} />
      ) : !datos ? (
        <EstadoCargando />
      ) : datos.contenido.length === 0 ? (
        <EstadoVacio
          titulo="Todavía no hay órdenes cerradas"
          descripcion="Aparecerán aquí cuando se cobren y se cierren. Eso llega con la caja."
        />
      ) : (
        <>
          <div className="pos-tarjetas pos-tarjetas-historial">
            {datos.contenido.map((p) => (
              <article key={p.id} className="pos-hist">
                <div className="pos-hist-cabeza">
                  <div>
                    <strong>#{p.numero}</strong>
                    <span>{new Date(p.abiertoEn).toLocaleString('es-PE')}</span>
                  </div>
                  <span className="pos-estado pos-estado-comandado">Cerrada</span>
                </div>
                <div className="pos-hist-cuerpo">
                  <span>
                    <User size={16} aria-hidden="true" />
                    {p.nombreCliente || 'Sin nombre'}
                  </span>
                  <span>
                    {p.canal === 'para_llevar' ? <ShoppingBag size={16} aria-hidden="true" /> : <UtensilsCrossed size={16} aria-hidden="true" />}
                    {ETIQUETA_DE_CANAL[p.canal]}
                    {p.canal === 'mesa' && <em>· {tituloDelPedido(p)}</em>}
                  </span>
                  <span className="pos-hist-mozo">Mozo: {p.mozoNombre ?? '—'}</span>
                </div>
                <div className="pos-hist-pie">
                  <strong>{enSoles(p.total)}</strong>
                  <button type="button" onClick={() => onVerPedido(p.id, tituloDelPedido(p))}>
                    Ver pedido
                  </button>
                </div>
              </article>
            ))}
          </div>
          {datos.totalPaginas > 1 && (
            <div className="pos-paginacion">
              <Boton variante="secundario" disabled={pagina === 0} onClick={() => setPagina((n) => n - 1)}>
                Anterior
              </Boton>
              <span>
                Página {datos.pagina + 1} de {datos.totalPaginas}
              </span>
              <Boton variante="secundario" disabled={pagina + 1 >= datos.totalPaginas} onClick={() => setPagina((n) => n + 1)}>
                Siguiente
              </Boton>
            </div>
          )}
        </>
      )}
    </div>
  )
}
