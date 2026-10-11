import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  IconoBorrar,
  IconoFlechaIzquierda,
  IconoMas,
  Select,
} from '../../comun/componentes'
import {
  actualizarReceta,
  crearReceta,
  ErrorApi,
  listarConversiones,
  listarProductos,
  listarRecetas,
  listarUnidades,
  obtenerReceta,
  type ConversionUnidad,
  type Producto,
  type RecetaDetalle,
  type Unidad,
} from '../../api/cliente'
import { margen, soles, tonoMargen } from './productoComun'
import { cantidad } from './PantallaRecetas'

interface Fila {
  clave: number
  productoId: string
  cantidad: string
  unidadId: string
}

interface ErrorFila {
  productoId: string
  mensaje: string
}

function mensajeGeneral(codigo: string | undefined): string {
  switch (codigo) {
    case 'PRODUCTO_NO_ADMITE_RECETA':
      return 'Solo un producto vendible o una subreceta puede tener receta.'
    case 'RECETA_YA_EXISTE':
      return 'Ese producto ya tiene receta. Búscala en la lista y edítala.'
    case 'SIN_INGREDIENTES':
      return 'Agrega al menos un ingrediente.'
    case 'RENDIMIENTO_INVALIDO':
      return 'El rendimiento tiene que ser mayor que cero.'
    case 'PORCIONES_INVALIDAS':
      return 'Las porciones tienen que ser un número entero mayor que cero.'
    case 'NO_ENCONTRADO':
    case 'PRODUCTO_NO_ENCONTRADO':
      return 'La receta o su producto ya no existe. Vuelve a la lista y recarga.'
    default:
      return 'No se pudo guardar la receta.'
  }
}

function mensajeDeFila(codigo: string | undefined, unidadBase: string): string {
  switch (codigo) {
    case 'SIN_CONVERSION':
      return `No hay conversión de esta unidad a ${unidadBase} para este producto. Agrégala en Unidades y presentaciones.`
    case 'CICLO':
      return 'Esta subreceta ya usa, directa o indirectamente, la receta que estás armando.'
    case 'INGREDIENTE_INVALIDO':
      return 'Este producto no es insumo ni subreceta, o está desactivado.'
    case 'INGREDIENTE_REPETIDO':
      return 'Este ingrediente ya está en la lista.'
    case 'CANTIDAD_INVALIDA':
      return 'La cantidad tiene que ser mayor que cero.'
    default:
      return 'Revisa este ingrediente.'
  }
}

/**
 * US-010. El rendimiento va siempre en la unidad base del producto: asi
 * una subreceta se costea por unidad base igual que un insumo (decision
 * del sprint 4). Los costos los calcula el backend al guardar; mientras se
 * edita, los de la ultima version guardada quedan marcados como viejos.
 */
export function PantallaRecetaFormulario() {
  const { id } = useParams()
  const esNueva = !id
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()
  const soloLectura = esNueva ? !tienePermiso('recetas.crear') : !tienePermiso('recetas.modificar')

  const [productos, setProductos] = useState<Producto[]>([])
  const [unidades, setUnidades] = useState<Unidad[]>([])
  const [conRecetaIds, setConRecetaIds] = useState<Set<string>>(new Set())
  const [detalle, setDetalle] = useState<RecetaDetalle | null>(null)
  const [conversiones, setConversiones] = useState<Record<string, ConversionUnidad[]>>({})
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')

  const [productoId, setProductoId] = useState('')
  const [rendimiento, setRendimiento] = useState('')
  const [porciones, setPorciones] = useState('1')
  const [filas, setFilas] = useState<Fila[]>([])
  const [sucio, setSucio] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [errorFila, setErrorFila] = useState<ErrorFila | null>(null)
  const [guardadoOk, setGuardadoOk] = useState(false)
  const siguienteClave = useRef(1)

  function nuevaFila(): Fila {
    return { clave: siguienteClave.current++, productoId: '', cantidad: '', unidadId: '' }
  }

  function traerConversiones(ids: string[]) {
    const faltan = ids.filter((pid) => pid && !(pid in conversiones))
    faltan.forEach((pid) =>
      listarConversiones(token, pid)
        .then((lista) => setConversiones((actual) => ({ ...actual, [pid]: lista })))
        .catch(() => setConversiones((actual) => ({ ...actual, [pid]: [] }))),
    )
  }

  function llenarDesde(d: RecetaDetalle) {
    setDetalle(d)
    setProductoId(d.productoId)
    setRendimiento(String(Number(d.rendimiento)))
    setPorciones(String(d.porciones))
    setFilas(
      d.ingredientes.map((i) => ({
        clave: siguienteClave.current++,
        productoId: i.productoId,
        cantidad: String(Number(i.cantidad)),
        unidadId: i.unidadId,
      })),
    )
    setSucio(false)
  }

  function cargar() {
    setCargando(true)
    setErrorCarga('')
    Promise.all([
      listarProductos(token),
      listarUnidades(token),
      esNueva ? listarRecetas(token) : Promise.resolve([]),
      esNueva ? Promise.resolve(null) : obtenerReceta(token, id!),
    ])
      .then(([p, u, r, d]) => {
        setProductos(p)
        setUnidades(u)
        setConRecetaIds(new Set(r.map((x) => x.productoId)))
        if (d) {
          llenarDesde(d)
          traerConversiones(d.ingredientes.map((i) => i.productoId))
        } else {
          setFilas([nuevaFila()])
        }
      })
      .catch((e) =>
        setErrorCarga(
          e instanceof ErrorApi && e.estado === 404 ? 'Esta receta no existe.' : 'No se pudo cargar la receta.',
        ),
      )
      .finally(() => setCargando(false))
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(cargar, [token, id])

  const productoPorId = useMemo(() => new Map(productos.map((p) => [p.id, p])), [productos])
  const unidadPorId = useMemo(() => new Map(unidades.map((u) => [u.id, u])), [unidades])

  const producto = productoPorId.get(productoId)
  const unidadBase = producto ? unidadPorId.get(producto.unidadBaseId) : undefined
  /**
   * El rendimiento solo importa en una subreceta: dice cuanto sale (1.5 kg
   * de aderezo) para costear la parte que otra receta usa (300 g). En un
   * plato que solo se vende no cambia nada -- el costo por porcion es total
   * / porciones -- y pedirlo confundia ("un plato rinde 2 litros").
   */
  const pideRendimiento = Boolean(producto?.esSubreceta)

  const candidatosReceta = productos
    .filter((p) => p.activo && (p.esVendible || p.esSubreceta) && !conRecetaIds.has(p.id))
    .sort((a, b) => a.nombre.localeCompare(b.nombre))

  const candidatosIngrediente = productos
    .filter((p) => p.activo && (p.esInsumo || p.esSubreceta) && p.id !== productoId)
    .sort((a, b) => a.nombre.localeCompare(b.nombre))
  const insumos = candidatosIngrediente.filter((p) => p.esInsumo)
  const subrecetas = candidatosIngrediente.filter((p) => !p.esInsumo && p.esSubreceta)

  /** La base del producto, mas las unidades a las que tiene una conversion con su base (en cualquier sentido). */
  function unidadesPara(pid: string): Unidad[] {
    const p = productoPorId.get(pid)
    if (!p) return []
    const ids = new Set([p.unidadBaseId])
    for (const c of conversiones[pid] ?? []) {
      if (c.unidadOrigenId === p.unidadBaseId) ids.add(c.unidadDestinoId)
      if (c.unidadDestinoId === p.unidadBaseId) ids.add(c.unidadOrigenId)
    }
    return [...ids].map((uid) => unidadPorId.get(uid)).filter((u): u is Unidad => Boolean(u))
  }

  function marcarCambio() {
    setSucio(true)
    setGuardadoOk(false)
  }

  function cambiarFila(clave: number, cambios: Partial<Fila>) {
    setFilas((actual) => actual.map((f) => (f.clave === clave ? { ...f, ...cambios } : f)))
    if (errorFila && cambios.productoId !== undefined) setErrorFila(null)
    marcarCambio()
  }

  function elegirIngrediente(clave: number, pid: string) {
    const p = productoPorId.get(pid)
    cambiarFila(clave, { productoId: pid, unidadId: p?.unidadBaseId ?? '' })
    traerConversiones([pid])
  }

  function validar(): string {
    if (!productoId) return 'Elige el producto de la receta.'
    if (pideRendimiento && !(Number(rendimiento) > 0)) return 'El rendimiento tiene que ser mayor que cero.'
    if (!Number.isInteger(Number(porciones)) || Number(porciones) <= 0) {
      return 'Las porciones tienen que ser un número entero mayor que cero.'
    }
    const completas = filas.filter((f) => f.productoId)
    if (completas.length === 0) return 'Agrega al menos un ingrediente.'
    for (const f of completas) {
      if (!(Number(f.cantidad) > 0)) {
        setErrorFila({ productoId: f.productoId, mensaje: 'La cantidad tiene que ser mayor que cero.' })
        return 'Revisa las cantidades de los ingredientes.'
      }
    }
    return ''
  }

  async function guardar() {
    setError('')
    setErrorFila(null)
    setGuardadoOk(false)
    const problema = validar()
    if (problema) {
      setError(problema)
      return
    }
    const datos = {
      // Un plato no tiene rendimiento propio: sale en porciones, y su costo
      // por porcion es total / porciones. El backend igual lo exige (> 0),
      // asi que se manda el mismo numero de porciones.
      rendimiento: pideRendimiento ? Number(rendimiento) : Number(porciones),
      porciones: Number(porciones),
      ingredientes: filas
        .filter((f) => f.productoId)
        .map((f) => ({ productoId: f.productoId, cantidad: Number(f.cantidad), unidadId: f.unidadId })),
    }
    setGuardando(true)
    try {
      const resultado = esNueva
        ? await crearReceta(token, { ...datos, productoId })
        : await actualizarReceta(token, id!, datos)
      llenarDesde(resultado)
      setGuardadoOk(true)
      if (esNueva) navigate(`/admin/catalogo/recetas/${resultado.id}`, { replace: true })
    } catch (e) {
      const cuerpo = e instanceof ErrorApi ? e.cuerpo : undefined
      if (cuerpo?.productoId) {
        const base = unidadPorId.get(productoPorId.get(cuerpo.productoId)?.unidadBaseId ?? '')?.codigo ?? 'su unidad base'
        setErrorFila({ productoId: cuerpo.productoId, mensaje: mensajeDeFila(cuerpo.error, base) })
        setError('Hay un ingrediente con problemas. Está marcado en la lista.')
      } else {
        setError(mensajeGeneral(cuerpo?.error))
      }
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) {
    return (
      <EstadoError
        mensaje={errorCarga}
        accion={<Boton onClick={() => navigate('/admin/catalogo/recetas')}>Volver a Recetas</Boton>}
      />
    )
  }

  const costosVisibles = Boolean(detalle?.costosVisibles)
  const lineaPorProducto = new Map((detalle?.ingredientes ?? []).map((i) => [i.productoId, i]))
  const sinCosto = (detalle?.ingredientes ?? []).filter((i) => i.costoUnitario == null)
  const precio = producto?.esVendible && producto.precioVenta != null ? producto.precioVenta / 100 : null
  const costoPorPorcion = detalle?.costoPorPorcion ?? null
  const m = !sucio ? margen(precio, costoPorPorcion) : null
  const titulo = esNueva ? 'Nueva receta' : (detalle?.productoNombre ?? 'Receta')

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">
            <button type="button" className="receta-volver" onClick={() => navigate('/admin/catalogo/recetas')}>
              <IconoFlechaIzquierda width={14} height={14} aria-hidden="true" /> Recetas
            </button>
          </p>
          <h1>{titulo}</h1>
          {!esNueva && detalle && (
            <p className="panel-admin-subtitulo">
              {detalle.esSubreceta ? 'Subreceta' : 'Plato vendible'} · <span className="prod-codigo">{detalle.productoCodigo}</span> ·
              versión {detalle.version + 1}
            </p>
          )}
        </div>
        {!soloLectura && (
          <Boton cargando={guardando} onClick={guardar} disabled={!esNueva && !sucio}>
            {esNueva ? 'Guardar receta' : 'Guardar cambios'}
          </Boton>
        )}
      </div>

      {soloLectura && (
        <Aviso tono="informacion">Puedes ver esta receta, pero no modificarla con tus permisos.</Aviso>
      )}
      {error && <Aviso tono="error">{error}</Aviso>}
      {guardadoOk && !error && <Aviso tono="exito">Receta guardada. Los costos ya están recalculados.</Aviso>}

      <div className="receta-grilla">
        <div className="panel-admin-tarjeta receta-tarjeta">
          <h2 className="receta-titulo-seccion">Qué produce</h2>
          {esNueva && candidatosReceta.length === 0 ? (
            <Aviso tono="informacion">
              <p>
                No hay productos para armar una receta: todos los platos vendibles y subrecetas ya tienen la suya.
                Para una receta nueva, primero crea el producto (marcado como vendible o como subreceta).
              </p>
              <Boton variante="secundario" onClick={() => navigate('/admin/catalogo/productos/nuevo')}>
                Crear producto
              </Boton>
            </Aviso>
          ) : esNueva ? (
            <Select
              etiqueta="Producto"
              value={productoId}
              disabled={soloLectura}
              onChange={(e) => {
                setProductoId(e.target.value)
                marcarCambio()
              }}
              ayuda={
                candidatosReceta.length === 0
                  ? 'Todos los productos vendibles y subrecetas ya tienen receta.'
                  : 'Un plato vendible o una subreceta que todavía no tenga receta.'
              }
            >
              <option value="">Elige un producto…</option>
              {candidatosReceta.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nombre} ({p.esSubreceta ? 'subreceta' : 'vendible'})
                </option>
              ))}
            </Select>
          ) : null}
          <div className="receta-fila-campos">
            {pideRendimiento && (
            <Campo
              etiqueta="Rendimiento"
              type="number"
              min="0"
              step="any"
              inputMode="decimal"
              value={rendimiento}
              disabled={soloLectura || !producto}
              sufijo={unidadBase?.codigo}
              ayuda={unidadBase ? `Cuánto sale de una preparación, en ${unidadBase.nombre.toLowerCase()}.` : undefined}
              onChange={(e) => {
                setRendimiento(e.target.value)
                marcarCambio()
              }}
            />
            )}
            <Campo
              etiqueta="Porciones"
              type="number"
              min="1"
              step="1"
              inputMode="numeric"
              value={porciones}
              disabled={soloLectura || !producto}
              ayuda={
                pideRendimiento || !producto
                  ? 'En cuántas porciones se reparte.'
                  : 'Cuántos platos salen con estas cantidades. El costo por porción es el total entre ellos.'
              }
              onChange={(e) => {
                setPorciones(e.target.value)
                marcarCambio()
              }}
            />
          </div>
        </div>

        <div className="panel-admin-tarjeta receta-tarjeta receta-resumen" aria-live="polite">
          <h2 className="receta-titulo-seccion">Costo</h2>
          {esNueva && !detalle ? (
            <p className="prod-nota">Se calcula al guardar, con el último costo de cada insumo.</p>
          ) : !costosVisibles ? (
            <p className="prod-nota">Tus permisos no incluyen ver costos de recetas.</p>
          ) : (
            <>
              <div className="prod-cifras">
                <div>
                  <span>Por porción</span>
                  <strong>{detalle?.costoIncompleto ? '—' : soles(costoPorPorcion)}</strong>
                </div>
                <div>
                  <span>Total</span>
                  <strong>{detalle?.costoIncompleto ? '—' : soles(detalle?.costoTotal)}</strong>
                </div>
                {precio != null ? (
                  <div>
                    <span>Margen</span>
                    <strong className={`prod-texto-${tonoMargen(m)}`}>{m == null ? '—' : `${m}%`}</strong>
                  </div>
                ) : detalle?.esSubreceta && detalle.costoTotal != null ? (
                  <div>
                    <span>Por {detalle.unidadBaseCodigo}</span>
                    <strong>{soles(detalle.costoTotal / detalle.rendimiento)}</strong>
                  </div>
                ) : null}
              </div>
              {sucio && <p className="prod-nota">Hay cambios sin guardar: estos costos son de la versión guardada.</p>}
              {detalle?.costoIncompleto && (
                <Aviso>
                  Falta el costo de {sinCosto.map((i) => i.productoNombre).join(', ')}. Cárgalo en Productos para ver el
                  costo de esta receta.
                </Aviso>
              )}
            </>
          )}
        </div>
      </div>

      <div className="prod-tabla-tarjeta">
        <div className="prod-barra-filtros">
          <h2 className="receta-titulo-seccion">Ingredientes</h2>
          <div className="prod-espaciador" />
          {!soloLectura && (
            <Boton
              variante="secundario"
              disabled={!producto}
              onClick={() => {
                setFilas((actual) => [...actual, nuevaFila()])
                marcarCambio()
              }}
            >
              <IconoMas width={15} height={15} aria-hidden="true" /> Agregar ingrediente
            </Boton>
          )}
        </div>
        <div className="prod-tabla-scroll">
          {!producto ? (
            <p className="prod-nota receta-sin-producto">Elige primero el producto de la receta.</p>
          ) : (
            <table className="prod-tabla receta-tabla">
              <thead>
                <tr>
                  <th>Ingrediente</th>
                  <th className="prod-num">Cantidad</th>
                  <th>Unidad</th>
                  {costosVisibles && <th className="prod-num">Costo</th>}
                  {!soloLectura && (
                    <th className="tabla-columna-acciones">
                      <span className="visualmente-oculto">Quitar</span>
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {filas.map((f, indice) => {
                  const ingrediente = productoPorId.get(f.productoId)
                  const opcionesUnidad = unidadesPara(f.productoId)
                  const linea = !sucio ? lineaPorProducto.get(f.productoId) : undefined
                  const conError = errorFila && errorFila.productoId === f.productoId
                  const usados = new Set(filas.filter((x) => x.clave !== f.clave).map((x) => x.productoId))
                  return (
                    <tr key={f.clave} className={conError ? 'receta-fila-error' : ''}>
                      <td>
                        <select
                          className="campo-control campo-select"
                          aria-label={`Ingrediente ${indice + 1}`}
                          aria-invalid={conError || undefined}
                          value={f.productoId}
                          disabled={soloLectura}
                          onChange={(e) => elegirIngrediente(f.clave, e.target.value)}
                        >
                          <option value="">Elige un insumo o subreceta…</option>
                          {/* En edicion, un ingrediente que ya no es candidato (desactivado) igual se muestra. */}
                          {ingrediente && !candidatosIngrediente.includes(ingrediente) && (
                            <option value={ingrediente.id}>{ingrediente.nombre}</option>
                          )}
                          <optgroup label="Insumos">
                            {insumos.map((p) => (
                              <option key={p.id} value={p.id} disabled={usados.has(p.id)}>
                                {p.nombre}
                              </option>
                            ))}
                          </optgroup>
                          <optgroup label="Subrecetas">
                            {subrecetas.map((p) => (
                              <option key={p.id} value={p.id} disabled={usados.has(p.id)}>
                                {p.nombre}
                              </option>
                            ))}
                          </optgroup>
                        </select>
                        {conError && <p className="campo-error">{errorFila!.mensaje}</p>}
                      </td>
                      <td className="prod-num">
                        <input
                          className="campo-control receta-cantidad"
                          type="number"
                          min="0"
                          step="any"
                          inputMode="decimal"
                          aria-label={`Cantidad del ingrediente ${indice + 1}`}
                          value={f.cantidad}
                          disabled={soloLectura || !f.productoId}
                          onChange={(e) => cambiarFila(f.clave, { cantidad: e.target.value })}
                        />
                      </td>
                      <td>
                        <select
                          className="campo-control campo-select"
                          aria-label={`Unidad del ingrediente ${indice + 1}`}
                          value={f.unidadId}
                          disabled={soloLectura || !f.productoId}
                          onChange={(e) => cambiarFila(f.clave, { unidadId: e.target.value })}
                        >
                          {opcionesUnidad.length === 0 && <option value="">—</option>}
                          {opcionesUnidad.map((u) => (
                            <option key={u.id} value={u.id}>
                              {u.codigo}
                            </option>
                          ))}
                        </select>
                      </td>
                      {costosVisibles && (
                        <td className="prod-num">
                          {linea == null ? (
                            <span className="prod-vacio">—</span>
                          ) : linea.costoLinea == null ? (
                            <span className="prod-estimado">sin costo</span>
                          ) : (
                            <span title={`${soles(linea.costoUnitario)} por ${linea.unidadBaseCodigo}`}>
                              {soles(linea.costoLinea)}
                            </span>
                          )}
                        </td>
                      )}
                      {!soloLectura && (
                        <td className="tabla-columna-acciones">
                          <button
                            type="button"
                            className="prod-icono-boton"
                            aria-label={`Quitar ingrediente ${indice + 1}`}
                            onClick={() => {
                              setFilas((actual) => actual.filter((x) => x.clave !== f.clave))
                              marcarCambio()
                            }}
                          >
                            <IconoBorrar width={15} height={15} aria-hidden="true" />
                          </button>
                        </td>
                      )}
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </div>
        {producto && (
          <div className="prod-pie">
            <span>
              {filas.filter((f) => f.productoId).length} ingredientes ·{' '}
              {pideRendimiento
                ? `rinde ${cantidad(Number(rendimiento) || 0)} ${unidadBase?.codigo ?? ''}`
                : `${Number(porciones) || 0} porciones`}
            </span>
            <span>Si una unidad no aparece, falta su conversión en Unidades y presentaciones</span>
          </div>
        )}
      </div>
    </section>
  )
}
