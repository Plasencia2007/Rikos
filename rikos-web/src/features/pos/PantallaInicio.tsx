import { useEffect, useMemo, useState } from 'react'
import { ClipboardList, CreditCard, LayoutGrid, Zap } from 'lucide-react'
import { Boton, EstadoCargando, EstadoError } from '../../comun/componentes'
import { tiempoTranscurrido } from '../../comun/formato/tiempo'
import { listarMesas, listarPedidos, listarSalones, resumenDePedidos, type Mesa, type ResumenDePedido, type Salon } from '../../api/cliente'
import { ETIQUETA_DE_ESTADO, tituloDelPedido } from './pedidosVista'

const SEGUNDOS_ACTUALIZACION = 15

/**
 * Inicio del POS: lo que hay en el local ahora. Sale de las mesas y los
 * salones (ya existen); la lista de pedidos en curso y las ventas del
 * turno llegan con la consulta de pedidos y con el sprint 7.
 */
export function PantallaInicio({
  token,
  onIrA,
}: {
  token: string
  onIrA: (vista: 'mesas' | 'rapido' | 'ordenes') => void
}) {
  const [mesas, setMesas] = useState<Mesa[] | null>(null)
  const [salones, setSalones] = useState<Salon[]>([])
  const [enCurso, setEnCurso] = useState<number | null>(null)
  const [recientes, setRecientes] = useState<ResumenDePedido[]>([])
  const [error, setError] = useState(false)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vivo = true
    const cargar = () =>
      Promise.all([
        listarMesas(token),
        listarSalones(token).catch(() => [] as Salon[]),
        resumenDePedidos(token).catch(() => null),
        listarPedidos(token, { situacion: 'en_curso', tamano: 5 }).catch(() => null),
      ])
        .then(([m, s, r, p]) => {
          if (!vivo) return
          setMesas(m)
          setSalones(s.filter((x) => x.activo))
          setEnCurso(r ? r.enCurso : null)
          setRecientes(p ? p.contenido : [])
          setError(false)
        })
        .catch(() => vivo && setError(true))
    cargar()
    const reloj = window.setInterval(cargar, SEGUNDOS_ACTUALIZACION * 1000)
    return () => {
      vivo = false
      window.clearInterval(reloj)
    }
  }, [token, intento])

  const datos = useMemo(() => {
    const lista = mesas ?? []
    const enUso = (m: Mesa) => m.estado === 'ocupada' || m.estado === 'pendiente_cierre'
    const ocupadas = lista.filter(enUso)
    return {
      total: lista.length,
      ocupadas,
      porcentaje: lista.length === 0 ? 0 : Math.round((ocupadas.length / lista.length) * 100),
      porSalon: salones
        .map((s) => {
          const propias = lista.filter((m) => m.salonId === s.id)
          return {
            id: s.id,
            nombre: s.nombre,
            cantidad: propias.length,
            pct: propias.length === 0 ? 0 : Math.round((propias.filter(enUso).length / propias.length) * 100),
          }
        })
        .filter((s) => s.cantidad > 0),
    }
  }, [mesas, salones])

  if (error && !mesas) {
    return <EstadoError mensaje="No se pudo cargar el resumen." accion={<Boton onClick={() => setIntento((n) => n + 1)}>Reintentar</Boton>} />
  }
  if (!mesas) return <EstadoCargando />

  return (
    <div className="pos-inicio">
      <div className="pos-kpis">
        <div className="pos-kpi">
          <span className="pos-kpi-icono pos-tinta-verde">
            <LayoutGrid size={21} aria-hidden="true" />
          </span>
          <span className="pos-kpi-etiqueta">Mesas ocupadas</span>
          <strong className="pos-kpi-valor">
            {datos.ocupadas.length} / {datos.total}
          </strong>
          <span className="pos-kpi-nota">{datos.porcentaje}% de ocupación</span>
        </div>
        <div className="pos-kpi">
          <span className="pos-kpi-icono pos-tinta-azul">
            <ClipboardList size={21} aria-hidden="true" />
          </span>
          <span className="pos-kpi-etiqueta">Pedidos en curso</span>
          <strong className="pos-kpi-valor">{enCurso ?? '—'}</strong>
          <span className="pos-kpi-nota">Mesas y para llevar</span>
        </div>
        <div className="pos-kpi pos-kpi-pronto">
          <span className="pos-kpi-icono pos-tinta-ambar">
            <CreditCard size={21} aria-hidden="true" />
          </span>
          <span className="pos-kpi-etiqueta">Ventas del turno</span>
          <strong className="pos-kpi-valor">S/ —</strong>
          <span className="pos-kpi-nota">
            Se activa con la caja <em className="pos-pronto pos-pronto-claro">Pronto</em>
          </span>
        </div>
      </div>

      <section className="pos-acciones" aria-label="Acciones rápidas">
        <h2>Acciones rápidas</h2>
        <div>
          <button type="button" onClick={() => onIrA('mesas')}>
            <LayoutGrid size={24} aria-hidden="true" />
            <strong>Mesas</strong>
            <span>Abrir o ver una mesa</span>
          </button>
          <button type="button" onClick={() => onIrA('rapido')}>
            <Zap size={24} aria-hidden="true" />
            <strong>Pedido rápido</strong>
            <span>Para llevar</span>
          </button>
          <button type="button" onClick={() => onIrA('ordenes')}>
            <ClipboardList size={24} aria-hidden="true" />
            <strong>Órdenes activas</strong>
            <span>Pedidos en curso</span>
          </button>
        </div>
      </section>

      <div className="pos-dos-columnas">
        <section className="pos-tarjeta">
          <h2>Pedidos activos</h2>
          {recientes.length === 0 ? (
            <p className="pos-vacio">No hay pedidos en curso ahora.</p>
          ) : (
            <ul className="pos-lista">
              {recientes.map((p) => (
                <li key={p.id}>
                  <div>
                    <strong>{tituloDelPedido(p)}</strong>
                    <span>
                      {p.comensales} {p.comensales === 1 ? 'comensal' : 'comensales'}
                      {p.nombreCliente ? ` · ${p.nombreCliente}` : ''}
                    </span>
                  </div>
                  <span className="pos-tiempo">{tiempoTranscurrido(p.abiertoEn)}</span>
                  <span className={`pos-estado pos-estado-${p.estado}`}>{ETIQUETA_DE_ESTADO[p.estado]}</span>
                </li>
              ))}
            </ul>
          )}
          <button type="button" className="pos-enlace" onClick={() => onIrA('ordenes')}>
            Ver todas las órdenes →
          </button>
        </section>
        <section className="pos-tarjeta">
          <h2>Ocupación por salón</h2>
          <div className="pos-barras">
            {datos.porSalon.map((s) => (
              <div key={s.id} className="pos-barra">
                <span>{s.nombre}</span>
                <span className="pos-barra-pista">
                  <span style={{ width: `${s.pct}%` }} />
                </span>
                <span className="pos-barra-pct">{s.pct}%</span>
              </div>
            ))}
          </div>
          <button type="button" className="pos-enlace" onClick={() => onIrA('mesas')}>
            Ver el mapa de mesas →
          </button>
        </section>
      </div>
    </div>
  )
}
