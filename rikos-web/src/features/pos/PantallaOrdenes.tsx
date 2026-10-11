import { useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowRight, Clock, RefreshCw, ShoppingBag, UtensilsCrossed } from 'lucide-react'
import { Boton, EstadoCargando, EstadoError, EstadoVacio } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import { tiempoTranscurrido } from '../../comun/formato/tiempo'
import { listarPedidos, type ResumenDePedido } from '../../api/cliente'
import { ETIQUETA_DE_CANAL, ETIQUETA_DE_ESTADO, horaDe, tituloDelPedido } from './pedidosVista'

const SEGUNDOS_ACTUALIZACION = 15

type Filtro = 'todas' | 'mesa' | 'para_llevar'

/** Órdenes activas: un pedido por tarjeta, con sus líneas a la vista. Se actualiza solo. */
export function PantallaOrdenes({
  token,
  onVerPedido,
}: {
  token: string
  onVerPedido: (pedidoId: string, titulo: string) => void
}) {
  const [pedidos, setPedidos] = useState<ResumenDePedido[] | null>(null)
  const [error, setError] = useState(false)
  const [filtro, setFiltro] = useState<Filtro>('todas')
  const [actualizando, setActualizando] = useState(false)

  const cargar = useCallback(() => {
    setActualizando(true)
    return listarPedidos(token, { situacion: 'en_curso', tamano: 100 })
      .then((pagina) => {
        setPedidos(pagina.contenido)
        setError(false)
      })
      .catch(() => setError(true))
      .finally(() => setActualizando(false))
  }, [token])

  useEffect(() => {
    cargar()
    const reloj = window.setInterval(cargar, SEGUNDOS_ACTUALIZACION * 1000)
    return () => window.clearInterval(reloj)
  }, [cargar])

  const visibles = useMemo(
    () => (pedidos ?? []).filter((p) => filtro === 'todas' || p.canal === filtro),
    [pedidos, filtro],
  )

  if (error && !pedidos) {
    return <EstadoError mensaje="No se pudieron cargar las órdenes." accion={<Boton onClick={() => cargar()}>Reintentar</Boton>} />
  }
  if (!pedidos) return <EstadoCargando />

  const cuenta = (c: Filtro) => (c === 'todas' ? pedidos.length : pedidos.filter((p) => p.canal === c).length)

  return (
    <div className="pos-ordenes">
      <div className="pos-pestanas" role="tablist" aria-label="Tipo de orden">
        {(
          [
            ['todas', 'Todas'],
            ['mesa', 'Mesas'],
            ['para_llevar', 'Para llevar'],
          ] as const
        ).map(([valor, texto]) => (
          <button
            key={valor}
            type="button"
            role="tab"
            aria-selected={filtro === valor}
            className={filtro === valor ? 'elegida' : ''}
            onClick={() => setFiltro(valor)}
          >
            {texto}
            <span>{cuenta(valor)}</span>
          </button>
        ))}
        <button type="button" className="pos-pestana-inactiva" disabled>
          Delivery <em className="pos-pronto pos-pronto-claro">Pronto</em>
        </button>
        <span className="pos-pestanas-espacio" />
        <button type="button" className="pos-actualizar" onClick={() => cargar()} disabled={actualizando}>
          <RefreshCw size={17} aria-hidden="true" className={actualizando ? 'pos-girando' : ''} />
          Actualizar
        </button>
      </div>

      {visibles.length === 0 ? (
        <EstadoVacio
          titulo="No hay órdenes en curso"
          descripcion="Cuando abras una mesa o un pedido para llevar, aparecerá aquí."
        />
      ) : (
        <div className="pos-tarjetas">
          {visibles.map((p) => {
            const llevar = p.canal === 'para_llevar'
            return (
              <article key={p.id} className="pos-orden">
                <div className={`pos-orden-cabeza ${llevar ? 'llevar' : 'mesa'}`}>
                  <div className="pos-orden-titulo">
                    {llevar ? <ShoppingBag size={17} aria-hidden="true" /> : <UtensilsCrossed size={17} aria-hidden="true" />}
                    <strong>{tituloDelPedido(p)}</strong>
                    <span>{ETIQUETA_DE_CANAL[p.canal]}</span>
                  </div>
                  <div>Pedido #{p.numero}</div>
                  <div>
                    {horaDe(p.abiertoEn)} · Mozo: {p.mozoNombre ?? '—'}
                    {p.salonNombre ? ` · ${p.salonNombre}` : ''}
                    {p.nombreCliente ? ` · ${p.nombreCliente}` : ''}
                  </div>
                </div>
                <ul className="pos-orden-lineas">
                  {(p.lineas ?? []).map((l, i) => (
                    <li key={i}>
                      <span>
                        <b>{l.cantidad}</b> · {l.nombre}
                      </span>
                      {l.componentes.map((c) => (
                        <em key={c}>{c}</em>
                      ))}
                    </li>
                  ))}
                  {(p.lineas ?? []).length === 0 && <li className="pos-orden-sin-lineas">Sin productos todavía</li>}
                </ul>
                <div className="pos-orden-pie">
                  <div className="pos-orden-fila">
                    <span className="pos-tiempo-pildora">
                      <Clock size={14} aria-hidden="true" />
                      {tiempoTranscurrido(p.abiertoEn)}
                    </span>
                    <span className={`pos-estado pos-estado-${p.estado}`}>{ETIQUETA_DE_ESTADO[p.estado]}</span>
                    <strong className="pos-orden-total">{enSoles(p.total)}</strong>
                  </div>
                  <button
                    type="button"
                    className="pos-orden-ver"
                    onClick={() => onVerPedido(p.id, tituloDelPedido(p))}
                  >
                    Ver pedido
                    <ArrowRight size={16} aria-hidden="true" />
                  </button>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </div>
  )
}
