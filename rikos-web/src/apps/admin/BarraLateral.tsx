import type { SVGProps, ReactNode } from 'react'
import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { NavLink, useLocation } from 'react-router-dom'
import {
  IconoInicio,
  IconoCajaRegistradora,
  IconoMoto,
  IconoPersonas,
  IconoCalendarioSemana,
  IconoLibro,
  IconoCaja,
  IconoCarrito,
  IconoRecibo,
  IconoGrafico,
  IconoUsuario,
  IconoUbicacion,
  IconoEngranaje,
  IconoGorroChef,
  IconoFlechaAbajo,
  IconoFlechaIzquierda,
  IconoEscudo,
} from '../../comun/componentes'

interface SubItemNav {
  etiqueta: string
  ruta?: string
}

interface ItemNav {
  etiqueta: string
  ruta?: string
  Icono: (props: SVGProps<SVGSVGElement>) => ReactNode
  subItems?: SubItemNav[]
}

interface SeccionNav {
  titulo: string
  items: ItemNav[]
}

/**
 * Mapa completo del panel administrativo, fiel a la imagen de diseno profesional.
 */
const NAVEGACION: SeccionNav[] = [
  {
    titulo: '',
    items: [
      { etiqueta: 'Inicio', Icono: IconoInicio },
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
          { etiqueta: 'Pensionistas' },
          { etiqueta: 'Cuentas corrientes' },
          { etiqueta: 'Abonos' },
          { etiqueta: 'Planes tarifarios' },
          { etiqueta: 'Estados de cuenta' },
        ],
      },
      {
        etiqueta: 'Menú semanal',
        Icono: IconoCalendarioSemana,
        subItems: [{ etiqueta: 'Programación semanal' }, { etiqueta: 'Disponibilidad' }],
      },
      {
        etiqueta: 'Catálogo y recetas',
        Icono: IconoLibro,
        subItems: [
          { etiqueta: 'Productos', ruta: '/admin/catalogo/productos' },
          { etiqueta: 'Categorías', ruta: '/admin/catalogo/categorias' },
          { etiqueta: 'Unidades y presentaciones', ruta: '/admin/catalogo/unidades' },
          { etiqueta: 'Recetas' },
        ],
      },
      {
        etiqueta: 'Inventario',
        Icono: IconoCaja,
        subItems: [
          { etiqueta: 'Existencias' },
          { etiqueta: 'Kardex' },
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
        etiqueta: 'Compras y proveedores',
        Icono: IconoCarrito,
        subItems: [{ etiqueta: 'Compras' }, { etiqueta: 'Proveedores', ruta: '/admin/compras/proveedores' }],
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
    ],
  },
  {
    titulo: 'Usuarios y permisos',
    items: [
      { etiqueta: 'Administrar', ruta: '/admin/usuarios', Icono: IconoUsuario },
      { etiqueta: 'Roles', ruta: '/admin/usuarios/roles', Icono: IconoEscudo },
      { etiqueta: 'Locales', ruta: '/admin/locales', Icono: IconoUbicacion },
      {
        etiqueta: 'Configuración',
        Icono: IconoEngranaje,
        subItems: [{ etiqueta: 'Motivos' }, { etiqueta: 'Medios de pago' }, { etiqueta: 'Datos de la empresa' }],
      },
    ],
  },
]

const CLAVE_ABIERTOS = 'rikos-panel-lateral-abiertos'
const CLAVE_COLAPSADA = 'rikos-panel-lateral-colapsada'

function leerAbiertos(): Record<string, boolean> {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_ABIERTOS) ?? '{}')
  } catch {
    return {}
  }
}

function guardarAbiertos(abiertos: Record<string, boolean>) {
  try {
    localStorage.setItem(CLAVE_ABIERTOS, JSON.stringify(abiertos))
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

export function BarraLateral({ abierta, onCerrar }: { abierta: boolean; onCerrar: () => void }) {
  const ubicacion = useLocation()
  const [abiertos, setAbiertos] = useState<Record<string, boolean>>(leerAbiertos)
  const [colapsada, setColapsada] = useState<boolean>(leerColapsada)

  // Si la ruta activa es un hijo de un modulo cerrado, ese modulo se abre solo al entrar
  useEffect(() => {
    for (const seccion of NAVEGACION) {
      for (const item of seccion.items) {
        if (!item.subItems) continue
        const esActivo = item.subItems.some((sub) => esRutaActiva(sub.ruta, ubicacion.pathname))
        if (esActivo && !abiertos[item.etiqueta]) {
          setAbiertos((actual) => {
            const nuevo = { ...actual, [item.etiqueta]: true }
            guardarAbiertos(nuevo)
            return nuevo
          })
        }
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ubicacion.pathname])

  function alternar(etiqueta: string) {
    setAbiertos((actual) => {
      const nuevo = { ...actual, [etiqueta]: !actual[etiqueta] }
      guardarAbiertos(nuevo)
      return nuevo
    })
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
          <div className="panel-lateral-marca">
            <IconoGorroChef className="panel-lateral-marca-icono" aria-hidden="true" />
            <div className="panel-lateral-marca-texto">
              <strong>RIKO'S</strong>
              <span>RESTAURANTE</span>
            </div>
          </div>

          <button
            type="button"
            className="panel-lateral-colapsar"
            onClick={alternarColapso}
            aria-label={colapsada ? 'Expandir menú' : 'Colapsar menú'}
            title={colapsada ? 'Expandir menú' : 'Colapsar menú'}
          >
            <IconoFlechaIzquierda
              className={`panel-lateral-colapsar-icono ${colapsada ? 'girado' : ''}`}
              aria-hidden="true"
              width={14}
              height={14}
            />
          </button>
        </div>

        <div className="panel-lateral-scroll">
          {NAVEGACION.map((seccion, idxSec) => (
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

                  const expandido = Boolean(abiertos[item.etiqueta])

                  return (
                    <div key={item.etiqueta} className="panel-lateral-grupo">
                      <button
                        type="button"
                        className={`panel-lateral-item panel-lateral-item-grupo ${grupoActivo ? 'activo' : ''} ${expandido ? 'expandido' : ''}`}
                        aria-expanded={expandido}
                        onClick={() => alternar(item.etiqueta)}
                      >
                        <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                        <span className="panel-lateral-item-texto">{item.etiqueta}</span>
                        <IconoFlechaAbajo
                          className={`panel-lateral-item-chevron ${expandido ? 'girado' : ''}`}
                          aria-hidden="true"
                          width={12}
                          height={12}
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
                                <span className="panel-lateral-subitem-punto" aria-hidden="true" />
                                <span className="panel-lateral-subitem-texto">{sub.etiqueta}</span>
                              </NavLink>
                            ) : (
                              <span key={sub.etiqueta} className="panel-lateral-subitem panel-lateral-subitem-deshabilitado">
                                <span className="panel-lateral-subitem-punto" aria-hidden="true" />
                                <span className="panel-lateral-subitem-texto">{sub.etiqueta}</span>
                              </span>
                            )
                          })}
                        </div>
                      </div>
                    </div>
                  )
                }

                const itemActivo = esRutaActiva(item.ruta, ubicacion.pathname)

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
                )
              })}
            </div>
          ))}
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
                  <span className="panel-lateral-subitem-punto" aria-hidden="true" />
                  <span className="panel-lateral-subitem-texto">{sub.etiqueta}</span>
                </NavLink>
              ) : (
                <span key={sub.etiqueta} className="panel-lateral-subitem panel-lateral-subitem-deshabilitado">
                  <span className="panel-lateral-subitem-punto" aria-hidden="true" />
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
