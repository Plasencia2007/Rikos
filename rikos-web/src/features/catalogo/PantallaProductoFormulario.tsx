import { FotoDelProducto } from './FotoDelProducto'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  IconoBorrar,
  IconoCheck,
  IconoFlechaIzquierda,
  IconoMas,
  ModalDeConfirmacion,
} from '../../comun/componentes'
import {
  activarProducto,
  actualizarProducto,
  asignarServiciosProducto,
  crearPresentacion,
  crearProducto,
  desactivarProducto,
  eliminarProducto,
  ErrorApi,
  listarCategorias,
  listarPresentaciones,
  listarProductos,
  listarUnidades,
  registrarCostoManual,
  SERVICIOS,
  type Categoria,
  type DatosProducto,
  type Presentacion,
  type Producto,
  type Servicio,
  type Unidad,
} from '../../api/cliente'
import {
  ETIQUETA_SERVICIO,
  formatearFecha,
  IconoUso,
  Interruptor,
  margen,
  Mosaico,
  PastillaUso,
  rutaCategoria,
  soles,
  tonoMargen,
  USOS,
  type Uso,
} from './productoComun'

const TITULO_NATURALEZA: Record<string, string> = {
  componente_menu: 'Componente de menú',
  producto_fijo: 'Producto fijo',
  extra: 'Extra',
  insumo: 'Insumo',
  subreceta: 'Subreceta',
  menu: 'Menú del día',
}

// Lo habitual segun el tipo de categoria. Solo se aplica en un producto
// nuevo y mientras el usuario no haya tocado esos campos a mano.
const USOS_SUGERIDOS: Record<string, Uso[]> = {
  componente_menu: ['vendible'],
  producto_fijo: ['vendible'],
  extra: ['vendible'],
  insumo: ['insumo'],
  subreceta: ['insumo', 'subreceta'],
}
const UNIDAD_SUGERIDA: Record<string, string> = { insumo: 'kg', subreceta: 'l' }

// Mismo patron que CrearProducto.FORMATO_CODIGO en el backend.
const FORMATO_CODIGO = /^[A-Za-z0-9][A-Za-z0-9-]*$/

function prefijoDe(nombre: string): string {
  const letras = nombre
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^A-Za-z]/g, '')
    .toUpperCase()
  return letras.slice(0, 3) || 'PRD'
}

function siguienteCodigo(prefijo: string, codigos: string[]): string {
  const patron = new RegExp(`^${prefijo}-(\\d+)$`)
  const mayor = codigos.reduce((max, codigo) => {
    const m = codigo.match(patron)
    return m ? Math.max(max, parseInt(m[1], 10)) : max
  }, 0)
  return `${prefijo}-${String(mayor + 1).padStart(3, '0')}`
}

function mensajeError(e: unknown): string {
  if (e instanceof ErrorApi) {
    switch (e.cuerpo?.error) {
      case 'CODIGO_DUPLICADO':
        return 'Ya existe un producto con ese código.'
      case 'CODIGO_FORMATO_INVALIDO':
        return 'El código solo puede tener letras, números y guiones, sin espacios.'
      case 'CATEGORIA_NO_ES_HOJA':
        return 'Esa categoría tiene subcategorías: elige una de ellas.'
      case 'PRECIO_VENTA_OBLIGATORIO':
        return 'Un producto vendible necesita precio de venta.'
      case 'COSTO_YA_CONFIRMADO_POR_COMPRA':
        return 'El costo ya viene de una compra y no se puede editar a mano.'
      case 'NATURALEZA_INVALIDA':
        return 'Los servicios solo se eligen en productos de categorías "Producto fijo".'
      case 'PRODUCTO_DE_MENU':
        return 'El precio y los usos del menú se cambian en Menú semanal → Menú normal y extra.'
      case 'CATEGORIA_RESERVADA':
        return 'Esa categoría es solo para el menú normal y el menú extra.'
    }
  }
  return 'No se pudo guardar el producto.'
}

interface Formulario {
  codigo: string
  nombre: string
  categoriaId: string
  unidadBaseId: string
  usos: Record<Uso, boolean>
  precio: string
  costo: string
  controlaStock: boolean
  activo: boolean
  servicios: Servicio[]
}

const VACIO: Formulario = {
  codigo: '',
  nombre: '',
  categoriaId: '',
  unidadBaseId: '',
  usos: { vendible: false, insumo: false, subreceta: false },
  precio: '',
  costo: '',
  controlaStock: false,
  activo: true,
  servicios: [],
}

function desdeProducto(p: Producto): Formulario {
  return {
    codigo: p.codigo,
    nombre: p.nombre,
    categoriaId: p.categoriaId,
    unidadBaseId: p.unidadBaseId,
    usos: { vendible: p.esVendible, insumo: p.esInsumo, subreceta: p.esSubreceta },
    precio: p.precioVenta != null ? (p.precioVenta / 100).toFixed(2) : '',
    costo: p.costo != null ? String(Number(p.costo)) : '',
    controlaStock: p.controlaStock,
    activo: p.activo,
    servicios: [...(p.servicios as Servicio[])].sort(),
  }
}

export function PantallaProductoFormulario() {
  const { id } = useParams<{ id: string }>()
  const esNuevo = id === undefined
  const navigate = useNavigate()
  const { sesion } = useSesion()
  const token = sesion!.token

  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [unidades, setUnidades] = useState<Unidad[]>([])
  // Codigos ya usados: para sugerir el siguiente (SAN-015) sin chocar.
  const [codigos, setCodigos] = useState<string[]>([])
  const [existente, setExistente] = useState<Producto | null>(null)

  const [f, setF] = useState<Formulario>(VACIO)
  const [inicial, setInicial] = useState<Formulario>(VACIO)
  // Que campos ya toco el usuario: las sugerencias de un producto nuevo
  // (codigo, usos, unidad, stock) nunca pisan algo escrito a mano.
  const [tocado, setTocado] = useState({ codigo: false, usos: false, unidad: false, stock: false })

  const [guardando, setGuardando] = useState<'crear' | 'otro' | 'guardar' | null>(null)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [confirmandoBorrado, setConfirmandoBorrado] = useState(false)
  const [eliminando, setEliminando] = useState(false)
  const [errorEliminar, setErrorEliminar] = useState('')

  useEffect(() => {
    setCargando(true)
    setErrorCarga('')
    Promise.all([listarCategorias(token), listarUnidades(token), listarProductos(token)])
      .then(([cats, unids, prods]) => {
        setCategorias(cats)
        setUnidades(unids)
        setCodigos(prods.map((p) => p.codigo))
        if (!esNuevo) {
          const p = prods.find((x) => x.id === id)
          if (!p) throw new Error('no encontrado')
          setExistente(p)
          const datos = desdeProducto(p)
          setF(datos)
          setInicial(datos)
        } else {
          const und = unids.find((u) => u.codigo === 'und') ?? unids[0]
          const datos = { ...VACIO, unidadBaseId: und?.id ?? '' }
          setF(datos)
          setInicial(datos)
        }
      })
      .catch(() => setErrorCarga('No se pudo cargar el producto.'))
      .finally(() => setCargando(false))
  }, [id, esNuevo, token])

  const hojas = categorias.filter((c) => c.esHoja)
  const categoria = categorias.find((c) => c.id === f.categoriaId) ?? null
  const esFijo = categoria?.naturaleza === 'producto_fijo'
  const costoEditable = !existente || existente.origenCosto !== 'compra'
  // US-014: el menu normal y el extra son del sistema. La ficha solo cambia
  // nombre, unidad y control de stock; el precio va en Menu semanal, con
  // permiso de menu (el backend rechaza lo demas con PRODUCTO_DE_MENU).
  const esMenu = Boolean(existente?.tipoMenu)

  const gruposCategoria = Object.keys(TITULO_NATURALEZA)
    .filter((naturaleza) => naturaleza !== 'menu' || esMenu)
    .map((naturaleza) => ({ naturaleza, items: hojas.filter((c) => c.naturaleza === naturaleza) }))
    .filter((g) => g.items.length > 0)

  function cambiar(parcial: Partial<Formulario>) {
    setF((actual) => ({ ...actual, ...parcial }))
    setError('')
    setAviso('')
  }

  function elegirCategoria(categoriaId: string) {
    const cat = categorias.find((c) => c.id === categoriaId)
    const parcial: Partial<Formulario> = { categoriaId }
    if (esNuevo && cat) {
      if (!tocado.codigo) parcial.codigo = siguienteCodigo(prefijoDe(cat.nombre), codigos)
      if (!tocado.usos) {
        const sugeridos = USOS_SUGERIDOS[cat.naturaleza] ?? []
        parcial.usos = { vendible: false, insumo: false, subreceta: false }
        sugeridos.forEach((u) => (parcial.usos![u] = true))
        if (!tocado.stock) parcial.controlaStock = sugeridos.includes('insumo')
      }
      if (!tocado.unidad) {
        const codigoUnidad = UNIDAD_SUGERIDA[cat.naturaleza] ?? 'und'
        const u = unidades.find((x) => x.codigo === codigoUnidad)
        if (u) parcial.unidadBaseId = u.id
      }
    }
    cambiar(parcial)
  }

  function alternarUso(u: Uso) {
    setTocado((t) => ({ ...t, usos: true }))
    const usos = { ...f.usos, [u]: !f.usos[u] }
    const parcial: Partial<Formulario> = { usos }
    if (esNuevo && !tocado.stock) parcial.controlaStock = usos.insumo
    // Un plato que solo se vende se cuenta por unidad, no en kg ni en litros
    // (US-010: su receta se reparte en porciones, sin rendimiento propio).
    if (esNuevo && !tocado.unidad && usos.vendible && !usos.insumo && !usos.subreceta) {
      const und = unidades.find((x) => x.codigo === 'und')
      if (und) parcial.unidadBaseId = und.id
    }
    cambiar(parcial)
  }

  function alternarServicio(s: Servicio) {
    const servicios = f.servicios.includes(s) ? f.servicios.filter((x) => x !== s) : [...f.servicios, s].sort()
    cambiar({ servicios })
  }

  const precioNum = f.usos.vendible && f.precio !== '' ? parseFloat(f.precio) : null
  const costoNum = f.usos.insumo && f.costo !== '' ? parseFloat(f.costo) : null
  const precioValido = precioNum != null && !Number.isNaN(precioNum) && precioNum >= 0
  const costoValido = costoNum == null || (!Number.isNaN(costoNum) && costoNum >= 0)
  const m = margen(precioValido ? precioNum : null, costoValido ? costoNum : null)
  const primerUso = USOS.find((u) => f.usos[u.clave])?.clave

  const pendientes = useMemo(() => {
    const lista: { ok: boolean; texto: string }[] = [
      { ok: f.nombre.trim() !== '', texto: 'Ponerle nombre' },
      { ok: !!categoria, texto: 'Elegir la categoría' },
      { ok: FORMATO_CODIGO.test(f.codigo), texto: 'Código válido' },
      { ok: USOS.some((u) => f.usos[u.clave]), texto: 'Marcar para qué se usa' },
    ]
    if (f.usos.vendible && !esMenu) lista.push({ ok: precioValido, texto: 'Precio de venta' })
    if (f.usos.vendible && esFijo) lista.push({ ok: f.servicios.length > 0, texto: 'Al menos un servicio' })
    if (f.usos.insumo && f.costo !== '') lista.push({ ok: costoValido, texto: 'Costo válido' })
    return lista
  }, [f, categoria, esFijo, esMenu, precioValido, costoValido])
  const faltan = pendientes.filter((p) => !p.ok).length
  const listo = faltan === 0
  const sucio = JSON.stringify(f) !== JSON.stringify(inicial)

  async function guardar(modo: 'crear' | 'otro' | 'guardar') {
    if (!listo) {
      setError(`Falta${faltan === 1 ? ' 1 dato' : `n ${faltan} datos`}: ${pendientes.filter((p) => !p.ok).map((p) => p.texto.toLowerCase()).join(', ')}.`)
      return
    }
    setGuardando(modo)
    setError('')
    const datos: DatosProducto = {
      codigo: f.codigo.trim(),
      nombre: f.nombre.trim(),
      categoriaId: f.categoriaId,
      unidadBaseId: f.unidadBaseId,
      esVendible: f.usos.vendible,
      esInsumo: f.usos.insumo,
      esSubreceta: f.usos.subreceta,
      precioVenta: f.usos.vendible && precioNum != null ? Math.round(precioNum * 100) : null,
      controlaStock: f.controlaStock,
    }
    try {
      let productoId = id
      if (esNuevo) {
        productoId = (await crearProducto(token, datos)).id
      } else {
        await actualizarProducto(token, id!, datos)
      }
      // Costo manual: solo si es insumo, editable (no viene de una compra)
      // y cambio respecto de lo guardado -- reenviarlo sin cambios chocaria
      // con COSTO_YA_CONFIRMADO_POR_COMPRA por las puras.
      if (f.usos.insumo && costoEditable && costoNum != null && f.costo !== inicial.costo) {
        await registrarCostoManual(token, productoId!, costoNum)
      }
      // Servicios: solo existen para categorias "producto_fijo"; el backend
      // rechaza cualquier otra (AsignarServiciosProducto).
      if (esFijo && f.usos.vendible && JSON.stringify(f.servicios) !== JSON.stringify(inicial.servicios)) {
        await asignarServiciosProducto(token, productoId!, f.servicios)
      }
      if (!esNuevo && f.activo !== inicial.activo) {
        if (f.activo) await activarProducto(token, productoId!)
        else await desactivarProducto(token, productoId!)
      }

      if (modo === 'otro') {
        const lista = [...codigos, datos.codigo]
        setCodigos(lista)
        const cat = categoria!
        const nuevo: Formulario = {
          ...VACIO,
          categoriaId: f.categoriaId,
          unidadBaseId: f.unidadBaseId,
          usos: f.usos,
          controlaStock: f.controlaStock,
          servicios: f.servicios,
          codigo: siguienteCodigo(prefijoDe(cat.nombre), lista),
        }
        setF(nuevo)
        setInicial(nuevo)
        setTocado({ codigo: false, usos: false, unidad: false, stock: false })
        setAviso(`«${datos.nombre}» creado. Sigue con el siguiente.`)
        document.getElementById('prod-nombre')?.focus()
      } else {
        navigate('/admin/catalogo/productos')
      }
    } catch (e) {
      setError(mensajeError(e))
    } finally {
      setGuardando(null)
    }
  }

  async function confirmarEliminar() {
    setEliminando(true)
    setErrorEliminar('')
    try {
      await eliminarProducto(token, id!)
      navigate('/admin/catalogo/productos')
    } catch (e) {
      setErrorEliminar(
        e instanceof ErrorApi && e.cuerpo?.error === 'TIENE_HISTORIAL'
          ? 'No se puede eliminar: ya tiene presentaciones de compra. Desactívalo en su lugar (interruptor de abajo).'
          : 'No se pudo eliminar el producto.',
      )
    } finally {
      setEliminando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  const unidadBase = unidades.find((u) => u.id === f.unidadBaseId)

  return (
    <section className="panel-admin prod-form">
      <div className="panel-admin-encabezado">
        <div>
          <button type="button" className="prod-volver" onClick={() => navigate('/admin/catalogo/productos')}>
            <IconoFlechaIzquierda width={14} height={14} aria-hidden="true" /> Productos
          </button>
          <div className="prod-form-titulo">
            <h1>{esNuevo ? 'Nuevo producto' : 'Editar producto'}</h1>
            {!esNuevo && (
              <span className={`prod-pastilla ${inicial.activo ? 'prod-pastilla-exito' : 'prod-pastilla-inactivo'}`}>
                {inicial.activo ? 'Activo' : 'Inactivo'}
              </span>
            )}
          </div>
        </div>
        {!esNuevo && sucio && (
          <span className="prod-sin-guardar">
            <span aria-hidden="true" /> Cambios sin guardar
          </span>
        )}
      </div>

      <div className="prod-form-grid">
        <div className="prod-form-secciones">
          <section className="prod-seccion">
            <h2>Identificación</h2>
            <p>Cómo se busca y cómo aparece en comandas y etiquetas.</p>
            <div className="prod-fila-campos prod-fila-codigo">
              <div className="prod-campo">
                <label htmlFor="prod-codigo">Código</label>
                <input
                  id="prod-codigo"
                  className="prod-input prod-input-mono"
                  value={f.codigo}
                  disabled={!esNuevo}
                  placeholder="—"
                  onChange={(e) => {
                    setTocado((t) => ({ ...t, codigo: true }))
                    cambiar({ codigo: e.target.value.toUpperCase() })
                  }}
                />
                <span className="prod-ayuda">
                  {!esNuevo ? 'El código no se cambia una vez creado.' : categoria ? 'Sugerido según la categoría.' : 'Se sugiere al elegir categoría.'}
                </span>
              </div>
              <div className="prod-campo">
                <label htmlFor="prod-nombre">Nombre</label>
                <input
                  id="prod-nombre"
                  className="prod-input"
                  value={f.nombre}
                  placeholder="Ej. Sándwich de pollo"
                  autoFocus={esNuevo}
                  onChange={(e) => cambiar({ nombre: e.target.value })}
                />
              </div>
            </div>
          </section>

          <section className="prod-seccion">
            <h2>Clasificación</h2>
            <p>Solo categorías sin subcategorías pueden tener productos.</p>
            <div className="prod-fila-campos prod-fila-clasificacion">
              <div className="prod-campo">
                <label htmlFor="prod-categoria">Categoría</label>
                <select
                  id="prod-categoria"
                  className={`prod-input prod-select ${f.categoriaId ? '' : 'prod-select-vacio'}`}
                  value={f.categoriaId}
                  disabled={esMenu}
                  onChange={(e) => elegirCategoria(e.target.value)}
                >
                  <option value="">Elige una categoría</option>
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
                <span className="prod-ayuda">
                  {categoria ? (
                    <>
                      Tipo: <strong>{TITULO_NATURALEZA[categoria.naturaleza]}</strong>
                    </>
                  ) : (
                    'Decide qué datos se piden más abajo.'
                  )}
                </span>
              </div>
              <div className="prod-campo">
                <label htmlFor="prod-unidad">Unidad base</label>
                <select
                  id="prod-unidad"
                  className="prod-input prod-select"
                  value={f.unidadBaseId}
                  onChange={(e) => {
                    setTocado((t) => ({ ...t, unidad: true }))
                    cambiar({ unidadBaseId: e.target.value })
                  }}
                >
                  {unidades.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.nombre} ({u.codigo})
                    </option>
                  ))}
                </select>
                <span className="prod-ayuda">En la que se cuenta el stock.</span>
              </div>
            </div>
          </section>

          <section className="prod-seccion">
            <h2>¿Para qué se usa?</h2>
            <p>
              {esNuevo && categoria && !tocado.usos
                ? `Marcamos lo usual para ${categoria.nombre}. Puedes marcar más de uno.`
                : 'Puedes marcar más de uno: el arroz se compra, entra en recetas y también se vende.'}
            </p>
            <div className="prod-usos" role="group" aria-label="Usos">
              {USOS.map((u) => (
                <button
                  key={u.clave}
                  type="button"
                  role="checkbox"
                  aria-checked={f.usos[u.clave]}
                  className={`prod-uso prod-uso-${u.clave} ${f.usos[u.clave] ? 'prod-uso-on' : ''}`}
                  disabled={esMenu}
                  onClick={() => alternarUso(u.clave)}
                >
                  <span className="prod-uso-cabecera">
                    <IconoUso uso={u.clave} />
                    <span className="prod-uso-check" aria-hidden="true">
                      <IconoCheck width={13} height={13} strokeWidth={3} />
                    </span>
                  </span>
                  <strong>{u.titulo}</strong>
                  <span>{u.descripcion}</span>
                </button>
              ))}
            </div>
          </section>

          {f.usos.vendible && (
            <section className="prod-seccion">
              <h2>Venta</h2>
              <p>Lo que paga el cliente.</p>
              <div className="prod-campo prod-campo-corto">
                <label htmlFor="prod-precio">Precio de venta</label>
                <div className="prod-moneda">
                  <span>S/</span>
                  <input
                    id="prod-precio"
                    className="prod-input prod-input-monto"
                    inputMode="decimal"
                    placeholder="0.00"
                    value={f.precio}
                    disabled={esMenu}
                    onChange={(e) => cambiar({ precio: e.target.value.replace(',', '.') })}
                  />
                </div>
                {esMenu && (
                  <span className="prod-ayuda">
                    Es el precio del {existente?.tipoMenu === 'extra' ? 'menú extra' : 'menú normal'}: se cambia en Menú
                    semanal → Menú normal y extra.
                  </span>
                )}
              </div>
              {esFijo ? (
                <div className="prod-subseccion">
                  <span id="prod-servicios" className="prod-etiqueta">
                    ¿En qué servicios se ofrece?
                  </span>
                  <div className="prod-servicios" role="group" aria-labelledby="prod-servicios">
                    {SERVICIOS.map((s) => (
                      <button
                        key={s}
                        type="button"
                        aria-pressed={f.servicios.includes(s)}
                        className={`prod-servicio ${f.servicios.includes(s) ? 'prod-servicio-on' : ''}`}
                        onClick={() => alternarServicio(s)}
                      >
                        {ETIQUETA_SERVICIO[s]}
                      </button>
                    ))}
                  </div>
                </div>
              ) : (
                categoria && (
                  <p className="prod-nota-caja">
                    Los servicios solo se eligen en productos de categorías "Producto fijo". Los del menú del día salen en
                    almuerzo, según la programación semanal.
                  </p>
                )
              )}
            </section>
          )}

          {f.usos.insumo && (
            <section className="prod-seccion">
              <div className="prod-seccion-cabecera">
                <div>
                  <h2>Compra y costo</h2>
                  <p>Cuánto cuesta y en qué empaques se compra.</p>
                </div>
                {existente?.costo != null && (
                  <span className={`prod-pastilla ${costoEditable ? 'prod-pastilla-aviso' : 'prod-pastilla-exito'}`}>
                    {costoEditable ? 'Costo estimado' : 'Costo de compra'}
                  </span>
                )}
              </div>
              <div className="prod-campo prod-campo-corto">
                <label htmlFor="prod-costo">Costo por unidad base</label>
                <div className="prod-moneda">
                  <span>S/</span>
                  <input
                    id="prod-costo"
                    className="prod-input prod-input-monto"
                    inputMode="decimal"
                    placeholder="0.00"
                    value={f.costo}
                    disabled={!costoEditable}
                    onChange={(e) => cambiar({ costo: e.target.value.replace(',', '.') })}
                  />
                </div>
                <span className="prod-ayuda">
                  {costoEditable
                    ? 'Opcional. Se reemplaza con la primera compra registrada.'
                    : `Viene de la compra del ${formatearFecha(existente?.fechaUltimaCompra)}. No se edita a mano.`}
                </span>
              </div>
              <div className="prod-subseccion">
                {esNuevo ? (
                  <>
                    <span className="prod-etiqueta">Presentaciones de compra</span>
                    <p className="prod-nota-caja">Se agregan después de crearlo (saco, caja, paquete).</p>
                  </>
                ) : (
                  <Presentaciones token={token} productoId={id!} unidadBase={unidadBase} />
                )}
              </div>
            </section>
          )}

          <section className="prod-seccion prod-seccion-fila">
            <div>
              <h2>Controla stock</h2>
              <p>Descuenta del inventario al vender o usar en recetas.</p>
            </div>
            <Interruptor
              activo={f.controlaStock}
              etiqueta="Controla stock"
              onCambiar={() => {
                setTocado((t) => ({ ...t, stock: true }))
                cambiar({ controlaStock: !f.controlaStock })
              }}
            />
          </section>

          {!esNuevo && !esMenu && (
            <section className="prod-seccion prod-seccion-fila prod-seccion-peligro">
              <div>
                <h2>Eliminar producto</h2>
                <p>Solo si nunca se compró. Si ya tiene historial, desactívalo.</p>
              </div>
              <Boton
                variante="secundario"
                className="prod-boton-peligro"
                onClick={() => {
                  setErrorEliminar('')
                  setConfirmandoBorrado(true)
                }}
              >
                <IconoBorrar width={15} height={15} aria-hidden="true" /> Eliminar
              </Boton>
            </section>
          )}
        </div>

        <aside className="prod-form-lateral" aria-label="Vista previa">
          <div className="prod-preview">
            <div className="prod-preview-cabecera">
              <Mosaico nombre={f.nombre || '?'} uso={primerUso} grande />
              <div>
                <strong className={f.nombre.trim() ? '' : 'prod-vacio'}>{f.nombre.trim() || 'Producto sin nombre'}</strong>
                <span className="prod-sub">
                  <span className="prod-codigo">{f.codigo || '—'}</span> ·{' '}
                  {categoria ? rutaCategoria(categoria.id, categorias) : 'Sin categoría'}
                </span>
              </div>
            </div>
            <div className="prod-preview-usos">
              {primerUso ? (
                USOS.filter((u) => f.usos[u.clave]).map((u) => <PastillaUso key={u.clave} uso={u.clave} />)
              ) : (
                <span className="prod-nota">Sin usos marcados</span>
              )}
            </div>
            <div className="prod-cifras prod-cifras-preview">
              <div>
                <span>Precio</span>
                <strong>{soles(precioValido ? precioNum : null)}</strong>
              </div>
              <div>
                <span>Costo</span>
                <strong>{soles(costoValido ? costoNum : null)}</strong>
              </div>
              <div>
                <span>Margen</span>
                <strong className={`prod-texto-${tonoMargen(m)}`}>{m == null ? '—' : `${m}%`}</strong>
              </div>
            </div>
          </div>

          {!esNuevo && existente && (
            <FotoDelProducto token={token} productoId={existente.id} imagenInicial={existente.imagen ?? null} />
          )}

          {esNuevo ? (
            <div className="prod-tarjeta-lateral">
              <h3>Para crearlo falta</h3>
              {pendientes.map((p) => (
                <div key={p.texto} className={`prod-check ${p.ok ? 'prod-check-ok' : ''}`}>
                  <span aria-hidden="true">
                    <IconoCheck width={11} height={11} strokeWidth={3.5} />
                  </span>
                  {p.texto}
                </div>
              ))}
            </div>
          ) : (
            <div className="prod-tarjeta-lateral">
              <h3>Historial</h3>
              <div className="prod-dato">
                <span>Creado</span>
                <strong>{formatearFecha(existente?.creadoEn)}</strong>
              </div>
              <div className="prod-dato">
                <span>Última edición</span>
                <strong>{formatearFecha(existente?.actualizadoEn)}</strong>
              </div>
              <div className="prod-dato">
                <span>Última compra</span>
                <strong>{existente?.fechaUltimaCompra ? formatearFecha(existente.fechaUltimaCompra) : 'Sin compras'}</strong>
              </div>
            </div>
          )}
        </aside>
      </div>

      <div className="prod-form-pie">
        {esNuevo ? (
          <span className={`prod-pie-estado ${listo ? 'prod-texto-bueno' : ''}`}>
            {aviso || (listo ? 'Todo listo para crear.' : `Falta${faltan === 1 ? ' 1 dato' : `n ${faltan} datos`} para crear.`)}
          </span>
        ) : esMenu ? (
          <span className="prod-pie-estado">Producto del sistema: siempre activo, no se elimina.</span>
        ) : (
          <label className="prod-pie-activo">
            <Interruptor activo={f.activo} etiqueta="Producto activo" onCambiar={() => cambiar({ activo: !f.activo })} />
            <span>{f.activo ? 'Activo: aparece en ventas y compras' : 'Inactivo: oculto en registros nuevos'}</span>
          </label>
        )}
        {error && (
          <span className="prod-pie-error" role="alert">
            {error}
          </span>
        )}
        <div className="prod-espaciador" />
        <Boton variante="secundario" onClick={() => navigate('/admin/catalogo/productos')}>
          Cancelar
        </Boton>
        {esNuevo ? (
          <>
            <Boton variante="secundario" disabled={!listo || guardando !== null} cargando={guardando === 'otro'} onClick={() => guardar('otro')}>
              Crear y agregar otro
            </Boton>
            <Boton disabled={!listo || guardando !== null} cargando={guardando === 'crear'} onClick={() => guardar('crear')}>
              Crear producto
            </Boton>
          </>
        ) : (
          <Boton disabled={!sucio || guardando !== null} cargando={guardando === 'guardar'} onClick={() => guardar('guardar')}>
            Guardar cambios
          </Boton>
        )}
      </div>

      <ModalDeConfirmacion
        abierto={confirmandoBorrado}
        titulo="Eliminar producto"
        mensaje={`¿Eliminar «${inicial.nombre}»? Esta acción no se puede deshacer.`}
        textoConfirmar="Eliminar"
        peligro
        confirmando={eliminando}
        error={errorEliminar || undefined}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setConfirmandoBorrado(false)}
      />
    </section>
  )
}

/** Presentaciones de compra de un producto ya creado: lista + alta en linea. */
function Presentaciones({ token, productoId, unidadBase }: { token: string; productoId: string; unidadBase?: Unidad }) {
  const [lista, setLista] = useState<Presentacion[] | null>(null)
  const [agregando, setAgregando] = useState(false)
  const [nombre, setNombre] = useState('')
  const [factor, setFactor] = useState('')
  const [predeterminada, setPredeterminada] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  function cargar() {
    listarPresentaciones(token, productoId)
      .then(setLista)
      .catch(() => setLista([]))
  }
  useEffect(cargar, [token, productoId])

  async function guardar() {
    const n = parseFloat(factor.replace(',', '.'))
    if (!nombre.trim() || Number.isNaN(n) || n <= 0 || !unidadBase) {
      setError('Pon un nombre y cuántas unidades base trae (mayor que 0).')
      return
    }
    setGuardando(true)
    setError('')
    try {
      await crearPresentacion(token, {
        productoId,
        nombre: nombre.trim(),
        unidadId: unidadBase.id,
        factor: n,
        esPredeterminada: predeterminada || (lista?.length ?? 0) === 0,
      })
      setNombre('')
      setFactor('')
      setPredeterminada(false)
      setAgregando(false)
      cargar()
    } catch (e) {
      setError(
        e instanceof ErrorApi && e.cuerpo?.error === 'PREDETERMINADA_DUPLICADA'
          ? 'Ya hay una presentación predeterminada.'
          : 'No se pudo agregar la presentación.',
      )
    } finally {
      setGuardando(false)
    }
  }

  return (
    <>
      <div className="prod-seccion-cabecera">
        <span className="prod-etiqueta">Presentaciones de compra</span>
        {!agregando && (
          <button type="button" className="prod-enlace" onClick={() => setAgregando(true)}>
            <IconoMas width={14} height={14} aria-hidden="true" /> Agregar presentación
          </button>
        )}
      </div>
      {lista === null ? (
        <span className="prod-nota">Cargando…</span>
      ) : lista.length === 0 && !agregando ? (
        <p className="prod-nota-caja">Todavía no tiene. Ej.: «Saco 50 kg», «Caja x 12».</p>
      ) : (
        lista.map((pr) => (
          <div key={pr.id} className="prod-presentacion">
            <strong>{pr.nombre}</strong>
            <span>
              = {pr.factor} {unidadBase?.codigo ?? ''}
            </span>
            {pr.esPredeterminada && <span className="prod-pastilla prod-pastilla-exito">Predeterminada</span>}
          </div>
        ))
      )}
      {agregando && (
        <div className="prod-presentacion-nueva">
          <input
            className="prod-input"
            aria-label="Nombre de la presentación"
            placeholder="Ej. Saco 50 kg"
            value={nombre}
            autoFocus
            onChange={(e) => setNombre(e.target.value)}
          />
          <div className="prod-moneda prod-factor">
            <input
              className="prod-input prod-input-monto"
              aria-label={`Cuántos ${unidadBase?.codigo ?? ''} trae`}
              inputMode="decimal"
              placeholder="50"
              value={factor}
              onChange={(e) => setFactor(e.target.value)}
            />
            <span className="prod-sufijo">{unidadBase?.codigo}</span>
          </div>
          <label className="prod-casilla">
            <input type="checkbox" checked={predeterminada} onChange={(e) => setPredeterminada(e.target.checked)} />
            Predeterminada
          </label>
          <Boton variante="secundario" onClick={() => setAgregando(false)}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={guardar}>
            Agregar
          </Boton>
          {error && <p className="campo-error prod-presentacion-error">{error}</p>}
        </div>
      )}
    </>
  )
}
