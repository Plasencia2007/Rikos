import type { SVGProps, ReactNode } from 'react'
import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { NavLink, useLocation } from 'react-router-dom'
import {
  IconoInicio,
  IconoCajaRegistradora,
  IconoCubiertos,
  IconoMoto,
  IconoPersonas,
  IconoCalendarioSemana,
  IconoLibro,
  IconoCaja,
  IconoCarrito,
  IconoRecibo,
  IconoGrafico,
  IconoUsuario,
  IconoEngranaje,
  IconoGorroChef,
  IconoFlechaAbajo,
  IconoFlechaIzquierda,
  IconoFlechaDerecha,
  IconoEscudo,
  IconoSalir,
  IconoHorario,
} from '../../comun/componentes'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { obtenerConteoAbonosPendientes } from '../../api/cliente'

interface SubItemNav {
  etiqueta: string
  ruta?: string
}

interface ItemNav {
  etiqueta: string
  ruta?: string
  /** Otras rutas que también marcan este ítem como activo (pantallas que viven dentro de él). */
  rutasRelacionadas?: string[]
  Icono: (props: SVGProps<SVGSVGElement>) => ReactNode
  subItems?: SubItemNav[]
  /** Si se define, el item solo aparece cuando la sesion tiene este permiso efectivo (US-006). */
  requierePermiso?: string
}

interface SeccionNav {
  titulo: string
  items: ItemNav[]
}

/**
 * Mapa completo del panel administrativo, fiel a la imagen de diseño profesional de RIKO'S.
 */
const NAVEGACION: SeccionNav[] = [
  {
    titulo: '',
    items: [{ etiqueta: 'Inicio', Icono: IconoInicio }],
  },
  {
    titulo: 'Operación',
    items: [
      {
        etiqueta: 'Salón y mesas',
        Icono: IconoCubiertos,
        requierePermiso: 'venta.ver',
        // Administrar salones y mesas vive en Configuración (tarjeta "Salones y mesas"), no aquí.
        subItems: [{ etiqueta: 'Mapa de mesas', ruta: '/admin/salon/mesas' }],
      },
      {
        etiqueta: 'Caja y turnos',
        Icono: IconoCajaRegistradora,
        subItems: [{ etiqueta: 'Turnos' }, { etiqueta: 'Cierres de caja' }],
      },
      {
        etiqueta: 'Delivery y recojo',
        Icono: IconoMoto,
        subItems: [{ etiqueta: 'Delivery' }, { etiqueta: 'Recojo programado' }],
      },
    ],
  },
  {
    titulo: 'Negocio',
    items: [
      {
        etiqueta: 'Pensionistas',
        Icono: IconoPersonas,
        subItems: [
          { etiqueta: 'Pensionistas', ruta: '/admin/pensionistas' },
          { etiqueta: 'Cuentas corrientes', ruta: '/admin/pensionistas/cuentas' },
          { etiqueta: 'Abonos pendientes', ruta: '/admin/pensionistas/abonos' },
          { etiqueta: 'Planes tarifarios', ruta: '/admin/pensionistas/planes' },
          { etiqueta: 'Estados de cuenta' },
        ],
      },
      {
        etiqueta: 'Menú semanal',
        Icono: IconoCalendarioSemana,
        subItems: [
          { etiqueta: 'Menú normal y extra', ruta: '/admin/menu/normal-y-extra' },
          { etiqueta: 'Programación semanal', ruta: '/admin/menu/programacion' },
          { etiqueta: 'Disponibilidad', ruta: '/admin/menu/disponibilidad' },
        ],
      },
      {
        etiqueta: 'Catálogo y recetas',
        Icono: IconoLibro,
        subItems: [
          { etiqueta: 'Productos', ruta: '/admin/catalogo/productos' },
          { etiqueta: 'Categorías', ruta: '/admin/catalogo/categorias' },
          { etiqueta: 'Unidades y presentaciones', ruta: '/admin/catalogo/unidades' },
          { etiqueta: 'Recetas', ruta: '/admin/catalogo/recetas' },
        ],
      },
      {
        etiqueta: 'Inventario',
        Icono: IconoCaja,
        requierePermiso: 'inventario.ver',
        subItems: [
          { etiqueta: 'Existencias', ruta: '/admin/inventario/existencias' },
          { etiqueta: 'Kardex', ruta: '/admin/inventario/kardex' },
          { etiqueta: 'Ajustes' },
          { etiqueta: 'Transferencias' },
          { etiqueta: 'Conteos físicos' },
        ],
      },
      {
        etiqueta: 'Producción',
        Icono: IconoGorroChef,
        subItems: [{ etiqueta: 'Producción' }, { etiqueta: 'Consumo interno' }],
      },
      {
        etiqueta: 'Compras',
        Icono: IconoCarrito,
        requierePermiso: 'compras.ver',
        subItems: [{ etiqueta: 'Compras', ruta: '/admin/compras' }, { etiqueta: 'Proveedores', ruta: '/admin/compras/proveedores' }],
      },
      {
        etiqueta: 'Facturación',
        Icono: IconoRecibo,
        subItems: [{ etiqueta: 'Bandeja de comprobantes' }, { etiqueta: 'Series' }],
      },
    ],
  },
  {
    titulo: 'Administración',
    items: [
      {
        etiqueta: 'Reportes',
        Icono: IconoGrafico,
        subItems: [
          { etiqueta: 'Ventas y rentabilidad' },
          { etiqueta: 'Caja y diferencias' },
          { etiqueta: 'Pensionistas y saldos' },
          { etiqueta: 'Inventario y Kardex' },
          { etiqueta: 'Producción' },
          { etiqueta: 'Delivery' },
          { etiqueta: 'Consumos internos' },
        ],
      },
      {
        etiqueta: 'Bitácora',
        ruta: '/admin/auditoria',
        Icono: IconoHorario,
        requierePermiso: 'auditoria.ver',
      },
    ],
  },
  {
    titulo: 'Usuarios y permisos',
    items: [
      { etiqueta: 'Administrar', ruta: '/admin/usuarios', Icono: IconoUsuario },
      { etiqueta: 'Roles', ruta: '/admin/usuarios/roles', Icono: IconoEscudo },
      // Un solo enlace a la portada de Configuración: sus ajustes (motivos, parámetros de cobro, etc.) viven en ella.
      { etiqueta: 'Configuración', ruta: '/admin/configuracion', rutasRelacionadas: ['/admin/locales'], Icono: IconoEngranaje },
    ],
  },
]

const CLAVE_ABIERTO = 'rikos-panel-lateral-abierto'
const CLAVE_COLAPSADA = 'rikos-panel-lateral-colapsada'

// Acordeon: un solo modulo abierto a la vez (pedido del usuario, 2026-09-28).
function leerAbierto(): string | null {
  try {
    return localStorage.getItem(CLAVE_ABIERTO)
  } catch {
    return null
  }
}

function guardarAbierto(etiqueta: string | null) {
  try {
    if (etiqueta) localStorage.setItem(CLAVE_ABIERTO, etiqueta)
    else localStorage.removeItem(CLAVE_ABIERTO)
  } catch {
    // localStorage puede fallar en modo privado -- no es critico
  }
}

function leerColapsada(): boolean {
  try {
    return localStorage.getItem(CLAVE_COLAPSADA) === '1'
  } catch {
    return false
  }
}

function esRutaActiva(ruta: string | undefined, pathname: string): boolean {
  if (!ruta) return false
  // /admin/usuarios solo coincide si NO es /admin/usuarios/roles (modulo independiente)
  if (ruta === '/admin/usuarios') {
    return pathname.startsWith('/admin/usuarios') && !pathname.startsWith('/admin/usuarios/roles')
  }
  return pathname.startsWith(ruta)
}

function obtenerIniciales(nombre?: string): string {
  if (!nombre) return 'CM'
  const partes = nombre.trim().split(/\s+/)
  if (partes.length >= 2) {
    return (partes[0][0] + partes[1][0]).toUpperCase()
  }
  return nombre.slice(0, 2).toUpperCase()
}

export function BarraLateral({ abierta, onCerrar }: { abierta: boolean; onCerrar: () => void }) {
  const ubicacion = useLocation()
  const { sesion, salir, tienePermiso } = useSesion()
  const [abierto, setAbierto] = useState<string | null>(leerAbierto)
  const [colapsada, setColapsada] = useState<boolean>(leerColapsada)
  const [conteoPendientes, setConteoPendientes] = useState<number>(0)

  useEffect(() => {
    if (!sesion?.token) return
    obtenerConteoAbonosPendientes(sesion.token)
      .then((res) => setConteoPendientes(res.pendientes))
      .catch(() => {})
  }, [sesion?.token, ubicacion.pathname])

  // Los items con requierePermiso (ej. Bitacora, "auditoria.ver") solo se
  // muestran si la sesion tiene ese permiso efectivo -- ocultos, no solo
  // deshabilitados, para no anunciar una pantalla a la que no se puede entrar.
  const navegacionVisible = NAVEGACION.map((seccion) => ({
    ...seccion,
    items: seccion.items.filter((item) => !item.requierePermiso || tienePermiso(item.requierePermiso)),
  })).filter((seccion) => seccion.items.length > 0)

  const nombreUsuario = sesion?.nombreUsuario || 'Carlos Mendoza'
  const iniciales = obtenerIniciales(nombreUsuario)
  const rolTexto =
    sesion?.roles && sesion.roles.length > 0
      ? sesion.roles.join(', ')
      : 'Administrador de Empresa'

  // Al entrar a una pantalla, se abre el modulo que la contiene (y se
  // cierra el que estuviera abierto): entrar directo a /admin/catalogo/productos
  // deja abierto "Catalogo y recetas".
  useEffect(() => {
    for (const seccion of NAVEGACION) {
      for (const item of seccion.items) {
        if (item.subItems?.some((sub) => esRutaActiva(sub.ruta, ubicacion.pathname))) {
          setAbierto(item.etiqueta)
          guardarAbierto(item.etiqueta)
          return
        }
      }
    }
  }, [ubicacion.pathname])

  function alternar(etiqueta: string) {
    const nuevo = abierto === etiqueta ? null : etiqueta
    setAbierto(nuevo)
    guardarAbierto(nuevo)
  }

  function alternarColapso() {
    setColapsada((actual) => {
      const nuevo = !actual
      try {
        localStorage.setItem(CLAVE_COLAPSADA, nuevo ? '1' : '0')
      } catch {
        // no critico si falla
      }
      return nuevo
    })
  }

  return (
    <>
      {abierta && <div className="panel-lateral-telon" onClick={onCerrar} aria-hidden="true" />}

      <nav
        className={`panel-lateral ${abierta ? 'abierta' : ''} ${colapsada ? 'colapsada' : ''}`}
        aria-label="Navegación del panel"
      >
        <div className="panel-lateral-cabecera">
          <div className="panel-lateral-marca" title="Riko's Restaurante">
            <span className="panel-lateral-marca-corta" aria-hidden="true">
              R
            </span>
            <div className="panel-lateral-marca-texto">
              <span className="panel-lateral-marca-nombre">Riko's</span>
              <span className="panel-lateral-marca-subtitulo">Restaurante</span>
            </div>
          </div>
          <button
            type="button"
            className="panel-lateral-colapsar"
            onClick={alternarColapso}
            title={colapsada ? 'Expandir menú' : 'Contraer menú'}
            aria-label={colapsada ? 'Expandir menú' : 'Contraer menú'}
          >
            {colapsada ? (
              <IconoFlechaDerecha aria-hidden="true" width={18} height={18} />
            ) : (
              <IconoFlechaIzquierda aria-hidden="true" width={18} height={18} />
            )}
          </button>
        </div>

        {/* Lista de Navegación Scrolleable */}
        <div className="panel-lateral-scroll">
          {navegacionVisible.map((seccion, idxSec) => (
            <div className="panel-lateral-seccion" key={seccion.titulo || `sec-${idxSec}`}>
              {seccion.titulo ? <div className="panel-lateral-titulo">{seccion.titulo}</div> : null}

              {seccion.items.map((item) => {
                if (item.subItems) {
                  const grupoActivo = item.subItems.some((sub) => esRutaActiva(sub.ruta, ubicacion.pathname))

                  if (colapsada) {
                    return (
                      <ItemConFlyout key={item.etiqueta} item={item} ubicacion={ubicacion} onNavegar={onCerrar} />
                    )
                  }

                  const expandido = abierto === item.etiqueta

                  return (
                    <div key={item.etiqueta} className="panel-lateral-grupo">
                      <button
                        type="button"
                        className={`panel-lateral-item panel-lateral-item-grupo ${grupoActivo ? 'activo' : ''} ${expandido ? 'expandido' : ''}`}
                        aria-expanded={expandido}
                        title={item.etiqueta}
                        onClick={() => alternar(item.etiqueta)}
                      >
                        <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                        <span className="panel-lateral-item-texto">{item.etiqueta}</span>
                        <IconoFlechaAbajo
                          className={`panel-lateral-item-chevron ${expandido ? 'girado' : ''}`}
                          aria-hidden="true"
                          width={14}
                          height={14}
                        />
                      </button>
                      <div className={`panel-lateral-subitems ${expandido ? 'expandido' : ''}`}>
                        <div className="panel-lateral-subitems-inner">
                          {item.subItems.map((sub) => {
                            const subActivo = esRutaActiva(sub.ruta, ubicacion.pathname)
                            return sub.ruta ? (
                              <NavLink
                                key={sub.etiqueta}
                                to={sub.ruta}
                                className={`panel-lateral-subitem ${subActivo ? 'activo' : ''}`}
                                onClick={onCerrar}
                              >
                                <span className="panel-lateral-subitem-texto">{sub.etiqueta}</span>
                                {sub.etiqueta === 'Abonos pendientes' && conteoPendientes > 0 && (
                                  <span
                                    style={{
                                      marginLeft: 'auto',
                                      backgroundColor: '#F59E0B',
                                      color: '#FFFFFF',
                                      fontSize: '0.72rem',
                                      fontWeight: 700,
                                      padding: '1px 6px',
                                      borderRadius: '10px',
                                      lineHeight: '1.2',
                                    }}
                                  >
                                    {conteoPendientes}
                                  </span>
                                )}
                              </NavLink>
                            ) : (
                              <span key={sub.etiqueta} className="panel-lateral-subitem panel-lateral-subitem-deshabilitado">
                                <span className="panel-lateral-subitem-texto">{sub.etiqueta}</span>
                              </span>
                            )
                          })}
                        </div>
                      </div>
                    </div>
                  )
                }

                const itemActivo =
                  esRutaActiva(item.ruta, ubicacion.pathname) ||
                  Boolean(item.rutasRelacionadas?.some((r) => ubicacion.pathname.startsWith(r)))

                if (colapsada) {
                  return (
                    <div key={item.etiqueta} className="panel-lateral-riel-item">
                      {item.ruta ? (
                        <NavLink
                          to={item.ruta}
                          className={`panel-lateral-item-riel ${itemActivo ? 'activo' : ''}`}
                          onClick={onCerrar}
                          title={item.etiqueta}
                          aria-label={item.etiqueta}
                        >
                          <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                        </NavLink>
                      ) : (
                        <span
                          className="panel-lateral-item-riel panel-lateral-item-deshabilitado"
                          title={item.etiqueta}
                        >
                          <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                        </span>
                      )}
                    </div>
                  )
                }

                return item.ruta ? (
                  <NavLink
                    key={item.etiqueta}
                    to={item.ruta}
                    className={`panel-lateral-item ${itemActivo ? 'activo' : ''}`}
                    onClick={onCerrar}
                  >
                    <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                    <span className="panel-lateral-item-texto">{item.etiqueta}</span>
                  </NavLink>
                ) : (
                  <span key={item.etiqueta} className="panel-lateral-item panel-lateral-item-deshabilitado">
                    <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                    <span className="panel-lateral-item-texto">{item.etiqueta}</span>
                  </span>
                )}
              )}
            </div>
          ))}
        </div>

        <div className="panel-lateral-pie">
          <div className="panel-lateral-usuario" title={`${nombreUsuario} · ${rolTexto}`}>
            <div className="panel-lateral-usuario-avatar" aria-hidden="true">
              {iniciales}
            </div>
            <div className="panel-lateral-usuario-info">
              <span className="panel-lateral-usuario-nombre">{nombreUsuario}</span>
              <span className="panel-lateral-usuario-rol">{rolTexto}</span>
            </div>
          </div>
          <button
            type="button"
            className="panel-lateral-salir"
            onClick={() => {
              onCerrar()
              salir()
            }}
            title="Cerrar sesión"
            aria-label="Cerrar sesión"
          >
            <IconoSalir aria-hidden="true" width={18} height={18} />
          </button>
        </div>
      </nav>
    </>
  )
}

/** Modo riel: flyout de un modulo colapsado al pasar el cursor */
function ItemConFlyout({
  item,
  ubicacion,
  onNavegar,
}: {
  item: ItemNav
  ubicacion: ReturnType<typeof useLocation>
  onNavegar: () => void
}) {
  const [flyoutAbierto, setFlyoutAbierto] = useState(false)
  const [posicion, setPosicion] = useState({ top: 0, left: 0 })
  const cerrarConDelay = useRef<ReturnType<typeof setTimeout>>(undefined)
  const botonRef = useRef<HTMLButtonElement>(null)

  function abrir() {
    clearTimeout(cerrarConDelay.current)
    const rect = botonRef.current?.getBoundingClientRect()
    if (rect) setPosicion({ top: rect.top, left: rect.right })
    setFlyoutAbierto(true)
  }

  function cerrarConRetraso() {
    cerrarConDelay.current = setTimeout(() => setFlyoutAbierto(false), 200)
  }

  if (!item.subItems) return null
  const grupoActivo = item.subItems.some((sub) => sub.ruta && ubicacion.pathname.startsWith(sub.ruta))

  return (
    <div className="panel-lateral-riel-item" onMouseEnter={abrir} onMouseLeave={cerrarConRetraso}>
      <button
        ref={botonRef}
        type="button"
        className={`panel-lateral-item panel-lateral-item-riel ${grupoActivo ? 'activo' : ''}`}
        aria-expanded={flyoutAbierto}
        aria-label={item.etiqueta}
        onClick={abrir}
        onKeyDown={(e) => {
          if (e.key === 'Escape') setFlyoutAbierto(false)
        }}
      >
        <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
      </button>

      {flyoutAbierto &&
        createPortal(
          <div
            className="panel-lateral-flyout"
            style={{ top: posicion.top, left: posicion.left }}
            onMouseEnter={abrir}
            onMouseLeave={cerrarConRetraso}
          >
            <div className="panel-lateral-flyout-titulo">{item.etiqueta}</div>
            {item.subItems.map((sub) =>
              sub.ruta ? (
                <NavLink
                  key={sub.etiqueta}
                  to={sub.ruta}
                  className={({ isActive }) => `panel-lateral-subitem ${isActive ? 'activo' : ''}`}
                  onClick={() => {
                    setFlyoutAbierto(false)
                    onNavegar()
                  }}
                >
                  <span className="panel-lateral-subitem-texto">{sub.etiqueta}</span>
                </NavLink>
              ) : (
                <span key={sub.etiqueta} className="panel-lateral-subitem panel-lateral-subitem-deshabilitado">
                  <span className="panel-lateral-subitem-texto">{sub.etiqueta}</span>
                </span>
              ),
            )}
          </div>,
          document.body,
        )}
    </div>
  )
}
