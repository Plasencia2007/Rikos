import { useEffect, useMemo, useRef, useState } from 'react'
import {
  ArrowRight,
  Armchair,
  ChevronRight,
  ClipboardList,
  CreditCard,
  Lock,
  Plus,
  ReceiptText,
  ShoppingBag,
} from 'lucide-react'
import { Boton, EstadoCargando, EstadoError } from '../../comun/componentes'
import { tiempoTranscurrido } from '../../comun/formato/tiempo'
import { listarMesas, listarPedidos, listarSalones, resumenDePedidos, type Mesa, type ResumenDePedido, type Salon } from '../../api/cliente'
import { ETIQUETA_DE_ESTADO, tituloDelPedido } from './pedidosVista'

const SEGUNDOS_ACTUALIZACION = 15
/** Suficiente para contar por canal los pedidos en curso de un local; la lista muestra los primeros. */
const PEDIDOS_A_CONTAR = 50
const PEDIDOS_A_MOSTRAR = 5

type Destino = 'mesas' | 'rapido' | 'ordenes'
type Filtro = 'todos' | 'mesa' | 'para_llevar'

const FILTROS: { id: Filtro; titulo: string }[] = [
  { id: 'todos', titulo: 'Todos' },
  { id: 'mesa', titulo: 'En salón' },
  { id: 'para_llevar', titulo: 'Para llevar' },
]

/** Cómo va un salón: lleno, casi lleno o con mesas disponibles. */
function estadoDelSalon(pct: number): { texto: string; clase: string } {
  if (pct >= 100) return { texto: 'Lleno', clase: 'pos-insignia-lleno' }
  if (pct >= 70) return { texto: 'Casi lleno', clase: 'pos-insignia-casi' }
  return { texto: 'Disponible', clase: 'pos-insignia-libre' }
}

/** El botón "Nuevo pedido" abre las dos formas de empezar una atención. */
function NuevoPedido({
  onIrA,
  variante,
}: {
  onIrA: (destino: Destino) => void
  variante: 'principal' | 'secundario'
}) {
  const [abierto, setAbierto] = useState(false)
  const raiz = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!abierto) return
    const fuera = (e: PointerEvent) => {
      if (raiz.current && !raiz.current.contains(e.target as Node)) setAbierto(false)
    }
    const escape = (e: KeyboardEvent) => e.key === 'Escape' && setAbierto(false)
    document.addEventListener('pointerdown', fuera)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('pointerdown', fuera)
      document.removeEventListener('keydown', escape)
    }
  }, [abierto])

  return (
    <div className="pos-nuevo" ref={raiz}>
      <button
        type="button"
        className={variante === 'principal' ? 'pos-nuevo-principal' : 'pos-nuevo-secundario'}
        aria-haspopup="menu"
        aria-expanded={abierto}
        onClick={() => setAbierto((a) => !a)}
      >
        <Plus size={variante === 'principal' ? 22 : 18} strokeWidth={2.2} aria-hidden="true" />
        {variante === 'principal' ? 'Nuevo pedido' : 'Crear pedido'}
      </button>
      {abierto && (
        <div className="pos-nuevo-menu" role="menu">
          <button type="button" role="menuitem" onClick={() => onIrA('mesas')}>
            <Armchair size={20} aria-hidden="true" />
            <span>
              <strong>En una mesa</strong>
              <span>Salón o terraza</span>
            </span>
          </button>
          <button type="button" role="menuitem" onClick={() => onIrA('rapido')}>
            <ShoppingBag size={20} aria-hidden="true" />
            <span>
              <strong>Para llevar</strong>
              <span>Pedido rápido</span>
            </span>
          </button>
        </div>
      )}
    </div>
  )
}

/** Anillo de ocupación total del local. */
function Anillo({ porcentaje }: { porcentaje: number }) {
  const radio = 42
  const perimetro = 2 * Math.PI * radio
  return (
    <svg className="pos-anillo" viewBox="0 0 108 108" role="img" aria-label={`${porcentaje}% de ocupación`}>
      <circle className="pos-anillo-pista" cx="54" cy="54" r={radio} />
      <circle
        className="pos-anillo-valor"
        cx="54"
        cy="54"
        r={radio}
        strokeDasharray={perimetro}
        strokeDashoffset={perimetro * (1 - porcentaje / 100)}
      />
      <text x="54" y="54" dominantBaseline="central" textAnchor="middle">
        {porcentaje}%
      </text>
    </svg>
  )
}

/**
 * Inicio del POS: lo que hay en el local ahora. Sale de las mesas, los
 * salones y la consulta de pedidos; las ventas del turno llegan con la caja.
 */
export function PantallaInicio({ token, onIrA }: { token: string; onIrA: (vista: Destino) => void }) {
  const [mesas, setMesas] = useState<Mesa[] | null>(null)
  const [salones, setSalones] = useState<Salon[]>([])
  const [enCurso, setEnCurso] = useState<number | null>(null)
  const [pedidos, setPedidos] = useState<ResumenDePedido[]>([])
  const [filtro, setFiltro] = useState<Filtro>('todos')
  const [error, setError] = useState(false)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vivo = true
    const cargar = () =>
      Promise.all([
        listarMesas(token),
        listarSalones(token).catch(() => [] as Salon[]),
        resumenDePedidos(token).catch(() => null),
        listarPedidos(token, { situacion: 'en_curso', tamano: PEDIDOS_A_CONTAR }).catch(() => null),
      ])
        .then(([m, s, r, p]) => {
          if (!vivo) return
          setMesas(m)
          setSalones(s.filter((x) => x.activo))
          setEnCurso(r ? r.enCurso : null)
          setPedidos(p ? p.contenido : [])
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
    const ocupadas = lista.filter(enUso).length
    return {
      total: lista.length,
      ocupadas,
      libres: lista.length - ocupadas,
      porcentaje: lista.length === 0 ? 0 : Math.round((ocupadas / lista.length) * 100),
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

  const conteo = useMemo(
    () => ({
      todos: pedidos.length,
      mesa: pedidos.filter((p) => p.canal === 'mesa').length,
      para_llevar: pedidos.filter((p) => p.canal === 'para_llevar').length,
    }),
    [pedidos],
  )
  const visibles = pedidos.filter((p) => filtro === 'todos' || p.canal === filtro).slice(0, PEDIDOS_A_MOSTRAR)

  if (error && !mesas) {
    return <EstadoError mensaje="No se pudo cargar el resumen." accion={<Boton onClick={() => setIntento((n) => n + 1)}>Reintentar</Boton>} />
  }
  if (!mesas) return <EstadoCargando />

  const pedidosEnCurso = enCurso ?? conteo.todos
  const subtitulo =
    pedidosEnCurso === 0
      ? 'Todo listo para comenzar tu servicio.'
      : `${pedidosEnCurso} ${pedidosEnCurso === 1 ? 'pedido en curso' : 'pedidos en curso'} en este momento.`

  return (
    <div className="pos-inicio">
      <div className="pos-inicio-cabeza">
        <div>
          <h1>Resumen del local</h1>
          <p>{subtitulo}</p>
        </div>
        <NuevoPedido variante="principal" onIrA={onIrA} />
      </div>

      <div className="pos-kpis">
        <div className="pos-kpi">
          <span className="pos-kpi-icono pos-tinta-verde">
            <Armchair size={26} strokeWidth={1.7} aria-hidden="true" />
          </span>
          <div className="pos-kpi-cuerpo">
            <span className="pos-kpi-etiqueta">Mesas ocupadas</span>
            <div className="pos-kpi-fila">
              <strong className="pos-kpi-valor">
                {datos.ocupadas} / {datos.total}
              </strong>
              <span className="pos-kpi-nota">{datos.porcentaje}% de ocupación</span>
            </div>
            <span className="pos-kpi-barra" aria-hidden="true">
              <span style={{ width: `${datos.porcentaje}%` }} />
            </span>
            <span className="pos-kpi-nota">
              {datos.libres} {datos.libres === 1 ? 'mesa disponible' : 'mesas disponibles'}
            </span>
          </div>
        </div>
        <div className="pos-kpi">
          <span className="pos-kpi-icono pos-tinta-azul">
            <ClipboardList size={26} strokeWidth={1.7} aria-hidden="true" />
          </span>
          <div className="pos-kpi-cuerpo">
            <span className="pos-kpi-etiqueta">Pedidos en curso</span>
            <strong className="pos-kpi-valor">{enCurso ?? '—'}</strong>
            <span className="pos-kpi-nota">Mesas y para llevar</span>
          </div>
        </div>
        <div className="pos-kpi">
          <span className="pos-kpi-icono pos-tinta-ambar">
            <CreditCard size={26} strokeWidth={1.7} aria-hidden="true" />
          </span>
          <div className="pos-kpi-cuerpo">
            <span className="pos-kpi-etiqueta">Ventas del turno</span>
            <div className="pos-kpi-fila">
              <strong className="pos-kpi-valor">S/ —</strong>
              <span className="pos-insignia pos-insignia-pronto">
                <Lock size={13} strokeWidth={2} aria-hidden="true" />
                Próximamente
              </span>
            </div>
            <span className="pos-kpi-nota">Disponible al activar caja</span>
          </div>
        </div>
      </div>

      <section className="pos-tarjeta pos-atencion" aria-labelledby="pos-atencion-titulo">
        <h2 id="pos-atencion-titulo">Comienza una nueva atención</h2>
        <div className="pos-atencion-opciones">
          <button type="button" onClick={() => onIrA('mesas')}>
            <span className="pos-atencion-icono">
              <Armchair size={26} strokeWidth={1.7} aria-hidden="true" />
            </span>
            <span className="pos-atencion-texto">
              <strong>Abrir una mesa</strong>
              <span>Atiende en salón o terraza</span>
            </span>
            <ChevronRight size={22} aria-hidden="true" />
          </button>
          <button type="button" onClick={() => onIrA('rapido')}>
            <span className="pos-atencion-icono">
              <ShoppingBag size={26} strokeWidth={1.7} aria-hidden="true" />
            </span>
            <span className="pos-atencion-texto">
              <strong>Pedido para llevar</strong>
              <span>Registra un pedido rápido</span>
            </span>
            <ChevronRight size={22} aria-hidden="true" />
          </button>
        </div>
      </section>

      <div className="pos-dos-columnas">
        <section className="pos-tarjeta" aria-labelledby="pos-activos-titulo">
          <div className="pos-tarjeta-cabeza">
            <h2 id="pos-activos-titulo">Pedidos activos</h2>
            <button type="button" className="pos-enlace" onClick={() => onIrA('ordenes')}>
              Ver órdenes <ArrowRight size={16} aria-hidden="true" />
            </button>
          </div>
          <div className="pos-filtros-chip" role="tablist" aria-label="Filtrar pedidos activos">
            {FILTROS.map((f) => (
              <button
                key={f.id}
                type="button"
                role="tab"
                aria-selected={filtro === f.id}
                className={filtro === f.id ? 'activo' : ''}
                onClick={() => setFiltro(f.id)}
              >
                {f.titulo}
                <span className="pos-chip-conteo">{conteo[f.id]}</span>
              </button>
            ))}
          </div>
          {visibles.length === 0 ? (
            <div className="pos-vacio-grande">
              <span className="pos-vacio-icono">
                <ReceiptText size={30} strokeWidth={1.6} aria-hidden="true" />
              </span>
              <strong>Aún no hay pedidos activos</strong>
              <span>Abre una mesa o crea un pedido para comenzar.</span>
              <NuevoPedido variante="secundario" onIrA={onIrA} />
            </div>
          ) : (
            <ul className="pos-lista">
              {visibles.map((p) => (
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
        </section>

        <section className="pos-tarjeta" aria-labelledby="pos-ocupacion-titulo">
          <div className="pos-tarjeta-cabeza">
            <h2 id="pos-ocupacion-titulo">Ocupación por salón</h2>
            <button type="button" className="pos-enlace" onClick={() => onIrA('mesas')}>
              Ver mapa de mesas <ArrowRight size={16} aria-hidden="true" />
            </button>
          </div>
          <div className="pos-ocupacion-total">
            <Anillo porcentaje={datos.porcentaje} />
            <div>
              <strong>Ocupación total</strong>
              <span>
                {datos.ocupadas} de {datos.total} {datos.total === 1 ? 'mesa ocupada' : 'mesas ocupadas'}
              </span>
            </div>
          </div>
          <div className="pos-barras">
            {datos.porSalon.map((s) => {
              const estado = estadoDelSalon(s.pct)
              return (
                <div key={s.id} className="pos-barra">
                  <span>{s.nombre}</span>
                  <span className="pos-barra-pista">
                    <span style={{ width: `${s.pct}%` }} />
                  </span>
                  <span className="pos-barra-pct">{s.pct}%</span>
                  <span className={`pos-insignia ${estado.clase}`}>{estado.texto}</span>
                </div>
              )
            })}
          </div>
        </section>
      </div>
    </div>
  )
}
