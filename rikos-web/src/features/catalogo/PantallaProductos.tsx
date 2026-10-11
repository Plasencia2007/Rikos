import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoBorrar,
  IconoBusqueda,
  IconoCerrar,
  IconoLapiz,
  IconoMas,
  ModalDeConfirmacion,
} from '../../comun/componentes'
import {
  activarProducto,
  desactivarProducto,
  eliminarProducto,
  ErrorApi,
  listarCategorias,
  listarPresentaciones,
  listarProductos,
  listarUnidades,
  SERVICIOS,
  type Categoria,
  type Presentacion,
  type Producto,
  type Servicio,
  type Unidad,
} from '../../api/cliente'
import {
  costoEnSoles,
  ETIQUETA_SERVICIO,
  formatearFecha,
  Interruptor,
  margen,
  Mosaico,
  PastillaUso,
  precioEnSoles,
  rutaCategoria,
  soles,
  tonoMargen,
  USOS,
  usosDe,
  type Uso,
} from './productoComun'

type Estado = 'activos' | 'inactivos' | 'todos'

const TITULO_NATURALEZA: Record<string, string> = {
  componente_menu: 'Componente de menú',
  producto_fijo: 'Producto fijo',
  extra: 'Extra',
  insumo: 'Insumo',
  subreceta: 'Subreceta',
  menu: 'Menú del día',
}

export function PantallaProductos() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [productos, setProductos] = useState<Producto[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [unidades, setUnidades] = useState<Unidad[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const [busqueda, setBusqueda] = useState('')
  const [filtroUso, setFiltroUso] = useState<Uso | ''>('')
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const [filtroServicio, setFiltroServicio] = useState<Servicio | ''>('')
  const [filtroEstado, setFiltroEstado] = useState<Estado>('activos')

  const [abiertoId, setAbiertoId] = useState<string | null>(null)
  const [paraEliminar, setParaEliminar] = useState<Producto | null>(null)
  const [eliminando, setEliminando] = useState(false)
  const [errorEliminar, setErrorEliminar] = useState('')

  function cargar() {
    setCargando(true)
    setError('')
    Promise.all([listarProductos(token), listarCategorias(token), listarUnidades(token)])
      .then(([p, c, u]) => {
        setProductos(p)
        setCategorias(c)
        setUnidades(u)
      })
      .catch(() => setError('No se pudieron cargar los productos.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const categoriaPorId = useMemo(() => new Map(categorias.map((c) => [c.id, c])), [categorias])
  const unidadPorId = useMemo(() => new Map(unidades.map((u) => [u.id, u])), [unidades])
  const hojas = categorias.filter((c) => c.esHoja)

  const esProductoFijo = (p: Producto) => categoriaPorId.get(p.categoriaId)?.naturaleza === 'producto_fijo'

  const activos = productos.filter((p) => p.activo)
  const cuenta: Record<Uso, number> = { vendible: 0, insumo: 0, subreceta: 0 }
  activos.forEach((p) => usosDe(p).forEach((u) => cuenta[u]++))

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return productos.filter((p) => {
      if (filtroUso && !usosDe(p).includes(filtroUso)) return false
      if (filtroCategoria && p.categoriaId !== filtroCategoria) return false
      if (filtroServicio && !p.servicios.includes(filtroServicio)) return false
      if (filtroEstado === 'activos' && !p.activo) return false
      if (filtroEstado === 'inactivos' && p.activo) return false
      if (q && !p.nombre.toLowerCase().includes(q) && !p.codigo.toLowerCase().includes(q)) return false
      return true
    })
  }, [productos, busqueda, filtroUso, filtroCategoria, filtroServicio, filtroEstado])

  const hayFiltros = Boolean(busqueda || filtroUso || filtroCategoria || filtroServicio)

  function limpiarFiltros() {
    setBusqueda('')
    setFiltroUso('')
    setFiltroCategoria('')
    setFiltroServicio('')
  }

  // Grupos del desplegable de categoria: solo hojas (unicas que pueden tener
  // productos), agrupadas por la naturaleza de su raiz, igual que en Categorias.
  const gruposCategoria = Object.keys(TITULO_NATURALEZA)
    .map((naturaleza) => ({
      naturaleza,
      items: hojas.filter((c) => c.naturaleza === naturaleza),
    }))
    .filter((g) => g.items.length > 0)

  async function alternarActivo(p: Producto) {
    const nuevo = !p.activo
    setProductos((lista) => lista.map((x) => (x.id === p.id ? { ...x, activo: nuevo } : x)))
    try {
      if (nuevo) await activarProducto(token, p.id)
      else await desactivarProducto(token, p.id)
    } catch {
      setProductos((lista) => lista.map((x) => (x.id === p.id ? { ...x, activo: p.activo } : x)))
    }
  }

  async function confirmarEliminar() {
    if (!paraEliminar) return
    setEliminando(true)
    setErrorEliminar('')
    try {
      await eliminarProducto(token, paraEliminar.id)
      setParaEliminar(null)
      setAbiertoId(null)
      cargar()
    } catch (e) {
      setErrorEliminar(
        e instanceof ErrorApi && e.cuerpo?.error === 'TIENE_HISTORIAL'
          ? 'No se puede eliminar: ya tiene presentaciones de compra registradas. Desactívalo en su lugar.'
          : 'No se pudo eliminar el producto.',
      )
    } finally {
      setEliminando(false)
    }
  }

  function exportar() {
    const filas = [
      ['Código', 'Nombre', 'Categoría', 'Usos', 'Unidad', 'Precio', 'Costo', 'Estado'],
      ...filtrados.map((p) => [
        p.codigo,
        p.nombre,
        rutaCategoria(p.categoriaId, categorias),
        usosDe(p).join(' / '),
        unidadPorId.get(p.unidadBaseId)?.codigo ?? '',
        precioEnSoles(p)?.toFixed(2) ?? '',
        costoEnSoles(p)?.toFixed(2) ?? '',
        p.activo ? 'Activo' : 'Inactivo',
      ]),
    ]
    const csv = filas.map((f) => f.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
    const url = URL.createObjectURL(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' }))
    const a = document.createElement('a')
    a.href = url
    a.download = 'productos.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  const abierto = productos.find((p) => p.id === abiertoId) ?? null

  if (cargando) return <EstadoCargando />
  if (error) return <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Catálogo y recetas</p>
          <h1>Productos</h1>
        </div>
        <div className="prod-acciones-encabezado">
          <Boton variante="secundario" onClick={exportar} disabled={filtrados.length === 0}>
            Exportar
          </Boton>
          <Boton onClick={() => navigate('/admin/catalogo/productos/nuevo')} disabled={hojas.length === 0}>
            <IconoMas width={16} height={16} aria-hidden="true" /> Nuevo producto
          </Boton>
        </div>
      </div>

      {productos.length === 0 ? (
        <div className="panel-admin-tarjeta">
          {hojas.length === 0 ? (
            <EstadoVacio
              titulo="Primero crea una categoría"
              descripcion="Un producto necesita una categoría sin subcategorías para colgar de ella."
              accion={<Boton onClick={() => navigate('/admin/catalogo/categorias')}>Ir a Categorías</Boton>}
            />
          ) : (
            <EstadoVacio
              titulo="Todavía no hay productos"
              descripcion="Crea el primero: elige su categoría, para qué se usa y su precio o costo."
              accion={<Boton onClick={() => navigate('/admin/catalogo/productos/nuevo')}>Nuevo producto</Boton>}
            />
          )}
        </div>
      ) : (
        <>
          <div className="prod-resumen">
            <div className="prod-resumen-cifra">
              <strong>{activos.length}</strong>
              <span>productos activos en el catálogo</span>
              <span className="prod-resumen-nota">Un producto puede tener más de un uso</span>
            </div>
            <div className="prod-resumen-barra" aria-hidden="true">
              {USOS.map((u) => (
                <span key={u.clave} className={`prod-barra-${u.clave}`} style={{ flexGrow: cuenta[u.clave] }} />
              ))}
            </div>
            <div className="prod-segmentos" role="group" aria-label="Filtrar por uso">
              <button
                type="button"
                className={`prod-segmento ${filtroUso === '' ? 'prod-segmento-activo' : ''}`}
                aria-pressed={filtroUso === ''}
                onClick={() => setFiltroUso('')}
              >
                <span className="prod-punto prod-punto-todos" aria-hidden="true" />
                Todos <span className="prod-segmento-num">{activos.length}</span>
              </button>
              {USOS.map((u) => (
                <button
                  key={u.clave}
                  type="button"
                  className={`prod-segmento ${filtroUso === u.clave ? 'prod-segmento-activo' : ''}`}
                  aria-pressed={filtroUso === u.clave}
                  onClick={() => setFiltroUso(filtroUso === u.clave ? '' : u.clave)}
                >
                  <span className={`prod-punto prod-barra-${u.clave}`} aria-hidden="true" />
                  {u.plural} <span className="prod-segmento-num">{cuenta[u.clave]}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="prod-tabla-tarjeta">
            <div className="prod-barra-filtros">
              <label className="prod-buscador">
                <IconoBusqueda width={15} height={15} aria-hidden="true" />
                <input
                  type="text"
                  aria-label="Buscar producto"
                  placeholder="Buscar por nombre o código"
                  value={busqueda}
                  onChange={(e) => setBusqueda(e.target.value)}
                />
              </label>
              <select
                className="prod-select"
                aria-label="Categoría"
                value={filtroCategoria}
                onChange={(e) => setFiltroCategoria(e.target.value)}
              >
                <option value="">Todas las categorías</option>
                {gruposCategoria.map((g) => (
                  <optgroup key={g.naturaleza} label={TITULO_NATURALEZA[g.naturaleza]}>
                    {g.items.map((c) => (
                      <option key={c.id} value={c.id}>
                        {rutaCategoria(c.id, categorias)}
                      </option>
                    ))}
                  </optgroup>
                ))}
              </select>
              <select
                className="prod-select"
                aria-label="Servicio"
                value={filtroServicio}
                onChange={(e) => setFiltroServicio(e.target.value as Servicio | '')}
              >
                <option value="">Cualquier servicio</option>
                {SERVICIOS.map((s) => (
                  <option key={s} value={s}>
                    {ETIQUETA_SERVICIO[s]}
                  </option>
                ))}
              </select>
              {hayFiltros && (
                <button type="button" className="prod-limpiar" onClick={limpiarFiltros}>
                  Limpiar filtros
                </button>
              )}
              <div className="prod-espaciador" />
              <div className="prod-pestanas" role="tablist" aria-label="Estado">
                {(['activos', 'inactivos', 'todos'] as Estado[]).map((e) => (
                  <button
                    key={e}
                    type="button"
                    role="tab"
                    aria-selected={filtroEstado === e}
                    className={`prod-pestana ${filtroEstado === e ? 'prod-pestana-activa' : ''}`}
                    onClick={() => setFiltroEstado(e)}
                  >
                    {e === 'activos' ? 'Activos' : e === 'inactivos' ? 'Inactivos' : 'Todos'}
                  </button>
                ))}
              </div>
            </div>

            <div className="prod-tabla-scroll">
              {filtrados.length === 0 ? (
                <EstadoVacio
                  titulo="Nada coincide con el filtro"
                  descripcion="Prueba con otro texto o quita algún filtro."
                  accion={hayFiltros ? <Boton variante="secundario" onClick={limpiarFiltros}>Limpiar filtros</Boton> : undefined}
                />
              ) : (
                <table className="prod-tabla">
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th>Uso</th>
                      <th>Servicios</th>
                      <th className="prod-num">Precio</th>
                      <th className="prod-num">Costo</th>
                      <th className="prod-num">Margen</th>
                      <th className="prod-centro">Activo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtrados.map((p) => {
                      const usos = usosDe(p)
                      const precio = precioEnSoles(p)
                      const costo = costoEnSoles(p)
                      const m = margen(precio, costo)
                      return (
                        <tr
                          key={p.id}
                          className={`prod-fila ${abiertoId === p.id ? 'prod-fila-abierta' : ''} ${p.activo ? '' : 'prod-fila-inactiva'}`}
                          onClick={() => setAbiertoId(p.id)}
                        >
                          <td>
                            <div className="prod-celda-nombre">
                              <Mosaico nombre={p.nombre} uso={usos[0]} activo={p.activo} />
                              <div>
                                <button
                                  type="button"
                                  className="prod-nombre"
                                  onClick={(e) => {
                                    e.stopPropagation()
                                    setAbiertoId(p.id)
                                  }}
                                >
                                  {p.nombre}
                                </button>
                                <span className="prod-sub">
                                  <span className="prod-codigo">{p.codigo}</span> · {rutaCategoria(p.categoriaId, categorias)}
                                </span>
                              </div>
                            </div>
                          </td>
                          <td>
                            <div className="prod-pastillas">
                              {usos.map((u) => (
                                <PastillaUso key={u} uso={u} />
                              ))}
                            </div>
                          </td>
                          <td>
                            <Servicios producto={p} esFijo={esProductoFijo(p)} />
                          </td>
                          <td className={`prod-num prod-precio ${precio == null ? 'prod-vacio' : ''}`}>{soles(precio)}</td>
                          <td className="prod-num">
                            <span className={costo == null ? 'prod-vacio' : ''}>{soles(costo)}</span>
                            {costo != null && p.origenCosto === 'manual' && <span className="prod-estimado">estimado</span>}
                          </td>
                          <td className="prod-num">
                            {m == null ? (
                              <span className="prod-vacio">—</span>
                            ) : (
                              <span className={`prod-margen prod-margen-${tonoMargen(m)}`}>{m}%</span>
                            )}
                          </td>
                          <td className="prod-centro">
                            {/* US-014: el menu normal y el extra no se desactivan. */}
                            {p.tipoMenu ? (
                              <span className="prod-nota" title="Producto del sistema: siempre activo">
                                Siempre
                              </span>
                            ) : (
                              <Interruptor
                                activo={p.activo}
                                etiqueta={`Activo: ${p.nombre}`}
                                onCambiar={() => alternarActivo(p)}
                              />
                            )}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              )}
            </div>
            <div className="prod-pie">
              <span>
                {filtrados.length} de {productos.length} productos
              </span>
              <span>Haz clic en un producto para ver su ficha</span>
            </div>
          </div>
        </>
      )}

      {abierto && (
        <FichaProducto
          token={token}
          producto={abierto}
          categorias={categorias}
          unidadPorId={unidadPorId}
          esFijo={esProductoFijo(abierto)}
          onCerrar={() => setAbiertoId(null)}
          onEditar={() => navigate(`/admin/catalogo/productos/${abierto.id}/editar`)}
          onEliminar={() => {
            setErrorEliminar('')
            setParaEliminar(abierto)
          }}
        />
      )}

      <ModalDeConfirmacion
        abierto={paraEliminar !== null}
        titulo="Eliminar producto"
        mensaje={`¿Eliminar «${paraEliminar?.nombre}»? Esta acción no se puede deshacer. Si ya se compró alguna vez, desactívalo en su lugar.`}
        textoConfirmar="Eliminar"
        peligro
        confirmando={eliminando}
        error={errorEliminar || undefined}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setParaEliminar(null)}
      />
    </section>
  )
}

function Servicios({ producto, esFijo }: { producto: Producto; esFijo: boolean }) {
  if (!producto.esVendible) return <span className="prod-vacio">—</span>
  // Solo los productos de categorias "producto_fijo" eligen servicio
  // (AsignarServiciosProducto); los del menu del dia salen en almuerzo.
  if (!esFijo) return <span className="prod-nota">Lo fija el menú</span>
  const texto = producto.servicios.map((s) => ETIQUETA_SERVICIO[s as Servicio]).join(', ') || 'Ninguno'
  return (
    <div className="prod-slots" aria-label={`Servicios: ${texto}`}>
      {SERVICIOS.map((s) => {
        const on = producto.servicios.includes(s)
        return (
          <span key={s} className={`prod-slot ${on ? 'prod-slot-on' : ''}`} title={`${ETIQUETA_SERVICIO[s]}${on ? '' : ' (no)'}`}>
            {ETIQUETA_SERVICIO[s][0]}
          </span>
        )
      })}
    </div>
  )
}

function FichaProducto({
  token,
  producto,
  categorias,
  unidadPorId,
  esFijo,
  onCerrar,
  onEditar,
  onEliminar,
}: {
  token: string
  producto: Producto
  categorias: Categoria[]
  unidadPorId: Map<string, Unidad>
  esFijo: boolean
  onCerrar: () => void
  onEditar: () => void
  onEliminar: () => void
}) {
  const [presentaciones, setPresentaciones] = useState<Presentacion[] | null>(null)

  useEffect(() => {
    setPresentaciones(null)
    if (!producto.esInsumo) {
      setPresentaciones([])
      return
    }
    listarPresentaciones(token, producto.id)
      .then(setPresentaciones)
      .catch(() => setPresentaciones([]))
  }, [token, producto.id, producto.esInsumo])

  useEffect(() => {
    function alTeclear(e: KeyboardEvent) {
      if (e.key === 'Escape') onCerrar()
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [onCerrar])

  const usos = usosDe(producto)
  const precio = precioEnSoles(producto)
  const costo = costoEnSoles(producto)
  const m = margen(precio, costo)
  const unidadBase = unidadPorId.get(producto.unidadBaseId)

  const serviciosTexto = !producto.esVendible
    ? 'No se vende'
    : !esFijo
      ? 'Lo fija el menú semanal'
      : producto.servicios.map((s) => ETIQUETA_SERVICIO[s as Servicio]).join(', ') || 'Ninguno'

  return (
    <>
      <div className="prod-ficha-fondo" onClick={onCerrar} />
      <aside className="prod-ficha" aria-label={`Ficha de ${producto.nombre}`}>
        <div className="prod-ficha-cabecera">
          <Mosaico nombre={producto.nombre} uso={usos[0]} activo={producto.activo} grande />
          <div className="prod-ficha-titulo">
            <h2>{producto.nombre}</h2>
            <span className="prod-sub">
              <span className="prod-codigo">{producto.codigo}</span> · {rutaCategoria(producto.categoriaId, categorias)}
            </span>
            <div className="prod-pastillas">
              {usos.map((u) => (
                <PastillaUso key={u} uso={u} />
              ))}
              {!producto.activo && <span className="prod-pastilla prod-pastilla-inactivo">Inactivo</span>}
            </div>
          </div>
          <button type="button" className="prod-icono-boton" aria-label="Cerrar ficha" onClick={onCerrar}>
            <IconoCerrar width={18} height={18} aria-hidden="true" />
          </button>
        </div>

        <div className="prod-ficha-cuerpo">
          <div className="prod-cifras">
            <div>
              <span>Precio</span>
              <strong>{soles(precio)}</strong>
            </div>
            <div>
              <span>Costo</span>
              <strong>{soles(costo)}</strong>
            </div>
            <div>
              <span>Margen</span>
              <strong className={`prod-texto-${tonoMargen(m)}`}>{m == null ? '—' : `${m}%`}</strong>
            </div>
          </div>
          {costo != null && producto.origenCosto === 'manual' && (
            <p className="prod-aviso-estimado">Costo estimado a mano. Se reemplaza con la primera compra registrada.</p>
          )}

          <section className="prod-ficha-seccion">
            <h3>Venta</h3>
            <div className="prod-dato">
              <span>Servicios</span>
              <strong>{serviciosTexto}</strong>
            </div>
          </section>

          <section className="prod-ficha-seccion">
            <h3>Inventario</h3>
            <div className="prod-dato">
              <span>Unidad base</span>
              <strong>{unidadBase ? `${unidadBase.nombre} (${unidadBase.codigo})` : '—'}</strong>
            </div>
            <div className="prod-dato">
              <span>Controla stock</span>
              <strong>{producto.controlaStock ? 'Sí' : 'No'}</strong>
            </div>
          </section>

          {producto.esInsumo && (
            <section className="prod-ficha-seccion">
              <h3>Presentaciones de compra</h3>
              {presentaciones === null ? (
                <span className="prod-nota">Cargando…</span>
              ) : presentaciones.length === 0 ? (
                <span className="prod-nota">Todavía no tiene. Agrégalas desde Editar.</span>
              ) : (
                presentaciones.map((pr) => (
                  <div key={pr.id} className="prod-presentacion">
                    <strong>{pr.nombre}</strong>
                    <span>
                      = {pr.factor} {unidadBase?.codigo ?? ''}
                    </span>
                    {pr.esPredeterminada && <span className="prod-pastilla prod-pastilla-exito">Predeterminada</span>}
                  </div>
                ))
              )}
            </section>
          )}

          <section className="prod-ficha-seccion">
            <h3>Historial</h3>
            <div className="prod-dato">
              <span>Creado</span>
              <strong>{formatearFecha(producto.creadoEn)}</strong>
            </div>
            <div className="prod-dato">
              <span>Última edición</span>
              <strong>{formatearFecha(producto.actualizadoEn)}</strong>
            </div>
            <div className="prod-dato">
              <span>Última compra</span>
              <strong>{producto.fechaUltimaCompra ? formatearFecha(producto.fechaUltimaCompra) : 'Sin compras'}</strong>
            </div>
          </section>
        </div>

        <div className="prod-ficha-pie">
          <Boton variante="secundario" onClick={onEliminar} disabled={Boolean(producto.tipoMenu)}>
            <IconoBorrar width={15} height={15} aria-hidden="true" /> Eliminar
          </Boton>
          <Boton className="prod-ficha-editar" onClick={onEditar}>
            <IconoLapiz width={15} height={15} aria-hidden="true" /> Editar producto
          </Boton>
        </div>
      </aside>
    </>
  )
}
