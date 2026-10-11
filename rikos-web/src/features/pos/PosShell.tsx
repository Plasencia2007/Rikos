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

interface Props {
  vista: VistaDelPos
  titulo: string
  subtitulo: string
  IconoDelTitulo: LucideIcon
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
  IconoDelTitulo,
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
          <span className="pos-marca-logo" aria-hidden="true">
            R
          </span>
          <div className="pos-marca-texto">
            <strong>Riko's</strong>
            <span>Punto de venta</span>
          </div>
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
              <span>Turno y cobros</span>
            </span>
          </button>
        </nav>
        <div className="pos-mozo">
          <span className="pos-avatar" aria-hidden="true">
            {mozo.slice(0, 2).toUpperCase()}
          </span>
          <div className="pos-mozo-texto">
            <strong>{mozo}</strong>
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
            <span className="pos-cabezal-icono" aria-hidden="true">
              <IconoDelTitulo size={21} strokeWidth={1.8} />
            </span>
            <div className="pos-cabezal-titulo">
              <h1>{titulo}</h1>
              <span>{subtitulo}</span>
            </div>
            <div className="pos-cabezal-espacio" />
            {derecha}
            <span className="pos-local">
              <MapPin size={15} strokeWidth={1.9} aria-hidden="true" />
              {local}
            </span>
          </header>
        )}
        <main className="pos-contenido">{children}</main>
      </div>
    </div>
  )
}
