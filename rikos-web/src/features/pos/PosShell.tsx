import { useEffect, useState, type ReactNode } from 'react'
import {
  ClipboardList,
  Menu,
  X,
  CreditCard,
  History,
  House,
  LayoutGrid,
  Lock,
  MapPin,
  PanelLeftClose,
  PanelLeftOpen,
  Zap,
  type LucideIcon,
} from 'lucide-react'
import { LogoDelRestaurante } from '../../comun/componentes'
import { RanuraDelCabezal } from '../../comun/hooks/ranuraDelCabezal'

export type VistaDelPos = 'inicio' | 'mesas' | 'rapido' | 'ordenes' | 'historial' | 'caja'

interface ItemDeMenu {
  vista: VistaDelPos
  titulo: string
  /** Rotulo corto para la barra inferior del celular. */
  corto?: string
  detalle: string
  Icono: LucideIcon
  /** Hasta que su pantalla exista, el ítem se ve atenuado con "Pronto". */
  disponible: boolean
}

const ITEMS: ItemDeMenu[] = [
  { vista: 'inicio', titulo: 'Inicio', detalle: 'Resumen', Icono: House, disponible: true },
  { vista: 'mesas', titulo: 'Mesas', detalle: 'Salón', Icono: LayoutGrid, disponible: true },
  { vista: 'rapido', titulo: 'Pedido rápido', corto: 'Rápido', detalle: 'Para llevar', Icono: Zap, disponible: true },
  {
    vista: 'ordenes',
    titulo: 'Órdenes activas',
    corto: 'Órdenes',
    detalle: 'En curso',
    Icono: ClipboardList,
    disponible: true,
  },
  {
    vista: 'historial',
    titulo: 'Historial',
    corto: 'Historial',
    detalle: 'Órdenes pasadas',
    Icono: History,
    disponible: true,
  },
]

/** Sigue navigator.onLine: el pie y el cabezal avisan cuando el equipo se queda sin red. */
function useEnLinea() {
  const [enLinea, setEnLinea] = useState(() => navigator.onLine)
  useEffect(() => {
    const si = () => setEnLinea(true)
    const no = () => setEnLinea(false)
    window.addEventListener('online', si)
    window.addEventListener('offline', no)
    return () => {
      window.removeEventListener('online', si)
      window.removeEventListener('offline', no)
    }
  }, [])
  return enLinea
}

interface Props {
  vista: VistaDelPos
  titulo: string
  /** Vacío cuando la pantalla trae su propio título grande (el Inicio). */
  subtitulo: string
  /** La pantalla muestra su propio h1: en la ruta del cabezal el título va como texto. */
  tituloEnContenido?: boolean
  /** La pantalla pone su propio título en el cabezal (sin la ruta ni el botón de inicio). */
  tituloDePantalla?: boolean
  mozo: string
  local: string
  colapsada: boolean
  /** Datos de contexto a la derecha del título (por ejemplo, el resumen del pedido). */
  derecha?: ReactNode
  /** Pedidos en curso, para el contador del menú. */
  contadorOrdenes?: number
  /** La pantalla de pedido trae su propio encabezado: el del shell se oculta. */
  sinCabezal?: boolean
  onAlternarBarra: () => void
  onIr: (vista: VistaDelPos) => void
  onCambiarDeMozo: () => void
  children: ReactNode
}

/**
 * El "shell" del POS (docs/producto/ui-ux/03-pos-rediseno.md): barra lateral
 * colapsable, cabezal con título y local, y el contenido de cada pantalla.
 * Es solo presentación: la navegación y la sesión viven en apps/pos.
 */
export function PosShell({
  vista,
  titulo,
  subtitulo,
  tituloEnContenido,
  tituloDePantalla = false,
  mozo,
  local,
  colapsada,
  derecha,
  contadorOrdenes,
  sinCabezal,
  onAlternarBarra,
  onIr,
  onCambiarDeMozo,
  children,
}: Props) {
  // Celular: el menú es un cajón que se abre con la hamburguesa de la barra superior.
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [ranura, setRanura] = useState<HTMLDivElement | null>(null)
  const enLinea = useEnLinea()
  const estadoSistema = enLinea ? 'Sistema en línea' : 'Sin conexión'
  useEffect(() => {
    if (!menuAbierto) return
    const cerrar = (e: KeyboardEvent) => e.key === 'Escape' && setMenuAbierto(false)
    window.addEventListener('keydown', cerrar)
    return () => window.removeEventListener('keydown', cerrar)
  }, [menuAbierto])
  const ir = (v: VistaDelPos) => {
    setMenuAbierto(false)
    onIr(v)
  }

  return (
    <div className={`pos-raiz ${colapsada ? 'pos-colapsada' : ''} ${menuAbierto ? 'pos-menu-abierto' : ''}`}>
      <div className="pos-fondo-menu" onClick={() => setMenuAbierto(false)} aria-hidden="true" />
      <aside className="pos-lateral" aria-label="Menú del punto de venta">
        <div className="pos-marca">
          {/* Contraída, la barra no tiene ancho para el logo: queda la inicial. */}
          <span className="pos-marca-logo" aria-hidden="true">
            R
          </span>
          <LogoDelRestaurante className="pos-marca-imagen" />
          <button
            type="button"
            className="pos-cerrar-menu"
            aria-label="Cerrar el menú"
            onClick={() => setMenuAbierto(false)}
          >
            <X size={22} />
          </button>
          <button
            type="button"
            className="pos-alternar"
            aria-label={colapsada ? 'Expandir el menú' : 'Contraer el menú'}
            onClick={onAlternarBarra}
          >
            {colapsada ? <PanelLeftOpen size={20} /> : <PanelLeftClose size={20} />}
          </button>
        </div>
        <nav className="pos-menu">
          <span className="pos-seccion">Operación</span>
          {ITEMS.map(({ vista: v, titulo: t, corto, detalle, Icono, disponible }) => (
            <button
              key={v}
              type="button"
              className={`pos-item ${vista === v ? 'activo' : ''}`}
              disabled={!disponible}
              aria-current={vista === v ? 'page' : undefined}
              title={colapsada ? t : undefined}
              onClick={() => ir(v)}
            >
              <Icono size={21} strokeWidth={1.7} aria-hidden="true" />
              <span className="pos-item-texto">
                <strong>
                  <span className="pos-largo">{t}</span>
                  <span className="pos-corto">{corto ?? t}</span>
                </strong>
                <span className="pos-detalle">{detalle}</span>
              </span>
              {!disponible && <span className="pos-pronto">Pronto</span>}
              {v === 'ordenes' && contadorOrdenes ? <span className="pos-contador">{contadorOrdenes}</span> : null}
            </button>
          ))}
          <div className="pos-separador" />
          <button
            type="button"
            className={`pos-item ${vista === 'caja' ? 'activo' : ''}`}
            aria-current={vista === 'caja' ? 'page' : undefined}
            title={colapsada ? 'Caja y turno' : undefined}
            onClick={() => ir('caja')}
          >
            <CreditCard size={21} strokeWidth={1.7} aria-hidden="true" />
            <span className="pos-item-texto">
              <strong>Caja</strong>
              <span className="pos-detalle">Turno y cobros</span>
            </span>
          </button>
        </nav>
        <div className="pos-mozo">
          <span className="pos-avatar" aria-hidden="true">
            {mozo.slice(0, 2).toUpperCase()}
          </span>
          <div className="pos-mozo-texto">
            <strong>
              {mozo}
              <span className="pos-punto pos-punto-activo" aria-hidden="true" />
            </strong>
            <span>Mozo activo</span>
          </div>
        </div>
        <button
          type="button"
          className="pos-cambiar"
          onClick={onCambiarDeMozo}
          title={colapsada ? 'Cambiar de mozo' : undefined}
        >
          <Lock size={18} strokeWidth={1.8} aria-hidden="true" />
          <span className="pos-largo">Cambiar de mozo</span>
          <span className="pos-corto">Mozo</span>
        </button>
      </aside>
      <div className="pos-principal">
        <div className="pos-movil-barra">
          <button
            type="button"
            className="pos-hamburguesa"
            aria-label="Abrir el menú"
            aria-expanded={menuAbierto}
            onClick={() => setMenuAbierto(true)}
          >
            <Menu size={24} />
          </button>
          <div className="pos-movil-titulo">
            <strong>{titulo}</strong>
            <span>
              <MapPin size={13} strokeWidth={1.9} aria-hidden="true" />
              {local}
            </span>
          </div>
        </div>
        {!sinCabezal && (
          <header className="pos-cabezal">
            {tituloDePantalla ? (
              <div className="pos-cabezal-ranura" ref={setRanura} />
            ) : (
              <>
                <button type="button" className="pos-cabezal-icono" aria-label="Ir al inicio" onClick={() => ir('inicio')}>
                  <House size={20} strokeWidth={1.8} />
                </button>
                <div className="pos-cabezal-titulo">
                  <nav className="pos-ruta" aria-label="Ruta">
                    <span className="pos-ruta-raiz">Punto de venta</span>
                    <span className="pos-ruta-separador" aria-hidden="true">
                      /
                    </span>
                    {tituloEnContenido ? <strong>{titulo}</strong> : <h1>{titulo}</h1>}
                  </nav>
                  {subtitulo && <span>{subtitulo}</span>}
                </div>
              </>
            )}
            <div className="pos-cabezal-espacio" />
            {derecha}
            <span className="pos-local">
              <MapPin size={16} strokeWidth={1.9} aria-hidden="true" />
              {local}
            </span>
            <span className={`pos-sistema ${enLinea ? '' : 'pos-sistema-caido'}`} role="status">
              <span className={`pos-punto ${enLinea ? 'pos-punto-activo' : 'pos-punto-caido'}`} aria-hidden="true" />
              {estadoSistema}
            </span>
          </header>
        )}
        <main className="pos-contenido">
          <RanuraDelCabezal.Provider value={tituloDePantalla && !sinCabezal ? ranura : null}>{children}</RanuraDelCabezal.Provider>
        </main>
        <footer className="pos-pie">
          <span className={enLinea ? 'pos-pie-ok' : 'pos-pie-caido'}>
            <span className={`pos-punto ${enLinea ? 'pos-punto-activo' : 'pos-punto-caido'}`} aria-hidden="true" />
            {estadoSistema}
          </span>
          <span className="pos-pie-separador" aria-hidden="true" />
          <span>Sesión de mozo activa</span>
          <span className="pos-cabezal-espacio" />
          <span className="pos-pie-local">
            <strong>Riko's</strong> · {local}
          </span>
        </footer>
      </div>
    </div>
  )
}
