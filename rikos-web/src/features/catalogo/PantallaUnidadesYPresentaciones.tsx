import { useEffect, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, Campo, Select, Tabla, Tabs, type ColumnaTabla, type Pestana } from '../../comun/componentes'
import {
  listarUnidades,
  crearUnidad,
  listarProductos,
  listarPresentaciones,
  crearPresentacion,
  listarConversiones,
  crearConversion,
  type Unidad,
  type Producto,
  type Presentacion,
  type ConversionUnidad,
} from '../../api/cliente'

const PESTANAS: Pestana[] = [
  { id: 'unidades', etiqueta: 'Unidades' },
  { id: 'presentaciones', etiqueta: 'Presentaciones de compra' },
  { id: 'conversiones', etiqueta: 'Conversiones' },
]

export function PantallaUnidadesYPresentaciones() {
  const { sesion } = useSesion()
  const token = sesion!.token

  const [pestana, setPestana] = useState('unidades')
  const [unidades, setUnidades] = useState<Unidad[]>([])
  const [productos, setProductos] = useState<Producto[]>([])
  const [productoId, setProductoId] = useState('')
  const [presentaciones, setPresentaciones] = useState<Presentacion[]>([])
  const [conversiones, setConversiones] = useState<ConversionUnidad[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  function cargarBase() {
    setCargando(true)
    setError('')
    Promise.all([listarUnidades(token), listarProductos(token)])
      .then(([u, p]) => {
        setUnidades(u)
        setProductos(p.filter((x) => x.esInsumo))
      })
      .catch(() => setError('No se pudieron cargar las unidades.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargarBase, [token])

  useEffect(() => {
    if (!productoId) return
    listarPresentaciones(token, productoId).then(setPresentaciones)
    listarConversiones(token, productoId).then(setConversiones)
  }, [productoId, token])

  // ---- Unidades -----------------------------------------------------
  const [codigoUnidad, setCodigoUnidad] = useState('')
  const [nombreUnidad, setNombreUnidad] = useState('')
  const [tipoUnidad, setTipoUnidad] = useState('masa')

  async function guardarUnidad() {
    await crearUnidad(token, { codigo: codigoUnidad, nombre: nombreUnidad, tipo: tipoUnidad })
    setCodigoUnidad('')
    setNombreUnidad('')
    cargarBase()
  }

  const columnasUnidades: ColumnaTabla<Unidad>[] = [
    { encabezado: 'Código', render: (u) => u.codigo },
    { encabezado: 'Nombre', render: (u) => u.nombre },
    { encabezado: 'Tipo', render: (u) => u.tipo },
  ]

  // ---- Presentaciones -------------------------------------------------
  const [nombrePresentacion, setNombrePresentacion] = useState('')
  const [unidadPresentacionId, setUnidadPresentacionId] = useState('')
  const [factorPresentacion, setFactorPresentacion] = useState('')
  const [esPredeterminada, setEsPredeterminada] = useState(false)

  async function guardarPresentacion() {
    await crearPresentacion(token, {
      productoId,
      nombre: nombrePresentacion,
      unidadId: unidadPresentacionId,
      factor: parseFloat(factorPresentacion),
      esPredeterminada,
    })
    setNombrePresentacion('')
    setFactorPresentacion('')
    listarPresentaciones(token, productoId).then(setPresentaciones)
  }

  const productoSeleccionado = productos.find((p) => p.id === productoId)
  const unidadBaseSeleccionada = unidades.find((u) => u.id === productoSeleccionado?.unidadBaseId)

  // ---- Conversiones ----------------------------------------------------
  const [unidadOrigenId, setUnidadOrigenId] = useState('')
  const [unidadDestinoId, setUnidadDestinoId] = useState('')
  const [factorConversion, setFactorConversion] = useState('')

  async function guardarConversion() {
    await crearConversion(token, {
      productoId,
      unidadOrigenId,
      unidadDestinoId,
      factor: parseFloat(factorConversion),
    })
    setFactorConversion('')
    listarConversiones(token, productoId).then(setConversiones)
  }

  return (
    <section className="panel-admin">
      <h1>Unidades y presentaciones</h1>
      <Tabs pestanas={PESTANAS} activa={pestana} onCambiar={setPestana} />

      {pestana === 'unidades' && (
        <div className="panel-admin-tarjeta">
          <div className="panel-admin-fila-formulario">
            <Campo etiqueta="Código" value={codigoUnidad} onChange={(e) => setCodigoUnidad(e.target.value)} />
            <Campo etiqueta="Nombre" value={nombreUnidad} onChange={(e) => setNombreUnidad(e.target.value)} />
            <Select etiqueta="Tipo" value={tipoUnidad} onChange={(e) => setTipoUnidad(e.target.value)}>
              <option value="masa">Masa</option>
              <option value="volumen">Volumen</option>
              <option value="unidad">Unidad</option>
            </Select>
            <Boton onClick={guardarUnidad}>Agregar unidad</Boton>
          </div>
          <Tabla
            columnas={columnasUnidades}
            filas={unidades}
            obtenerClave={(u) => u.id}
            cargando={cargando}
            error={error}
          />
        </div>
      )}

      {(pestana === 'presentaciones' || pestana === 'conversiones') && (
        <div className="panel-admin-tarjeta">
          <Select etiqueta="Producto (insumo)" value={productoId} onChange={(e) => setProductoId(e.target.value)}>
            <option value="">— Elegir —</option>
            {productos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombre}
              </option>
            ))}
          </Select>

          {productoId && pestana === 'presentaciones' && (
            <>
              <div className="panel-admin-fila-formulario">
                <Campo
                  etiqueta="Nombre"
                  placeholder="Saco 50 kg"
                  value={nombrePresentacion}
                  onChange={(e) => setNombrePresentacion(e.target.value)}
                />
                <Select
                  etiqueta="Unidad"
                  value={unidadPresentacionId}
                  onChange={(e) => setUnidadPresentacionId(e.target.value)}
                >
                  <option value="">— Elegir —</option>
                  {unidades.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.nombre}
                    </option>
                  ))}
                </Select>
                <Campo
                  etiqueta="Factor"
                  type="number"
                  step="0.000001"
                  value={factorPresentacion}
                  onChange={(e) => setFactorPresentacion(e.target.value)}
                />
                <label className="opcion">
                  <input
                    type="checkbox"
                    className="opcion-control"
                    checked={esPredeterminada}
                    onChange={(e) => setEsPredeterminada(e.target.checked)}
                  />
                  <span className="opcion-etiqueta">Predeterminada</span>
                </label>
                <Boton onClick={guardarPresentacion}>Agregar</Boton>
              </div>
              {unidadPresentacionId && factorPresentacion && unidadBaseSeleccionada && (
                <p className="campo-ayuda">
                  1 {nombrePresentacion || 'presentación'} = {factorPresentacion} {unidadBaseSeleccionada.codigo}
                </p>
              )}
              <ul className="lista-simple">
                {presentaciones.map((p) => (
                  <li key={p.id}>
                    {p.nombre} — factor {p.factor} {p.esPredeterminada && '(predeterminada)'}
                  </li>
                ))}
              </ul>
            </>
          )}

          {productoId && pestana === 'conversiones' && (
            <>
              <div className="panel-admin-fila-formulario">
                <Select etiqueta="De" value={unidadOrigenId} onChange={(e) => setUnidadOrigenId(e.target.value)}>
                  <option value="">— Elegir —</option>
                  {unidades.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.nombre}
                    </option>
                  ))}
                </Select>
                <Select etiqueta="A" value={unidadDestinoId} onChange={(e) => setUnidadDestinoId(e.target.value)}>
                  <option value="">— Elegir —</option>
                  {unidades.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.nombre}
                    </option>
                  ))}
                </Select>
                <Campo
                  etiqueta="Factor"
                  type="number"
                  step="0.000001"
                  value={factorConversion}
                  onChange={(e) => setFactorConversion(e.target.value)}
                />
                <Boton onClick={guardarConversion}>Agregar</Boton>
              </div>
              {unidadOrigenId && unidadDestinoId && factorConversion && (
                <p className="campo-ayuda">
                  1 {unidades.find((u) => u.id === unidadOrigenId)?.codigo} = {factorConversion}{' '}
                  {unidades.find((u) => u.id === unidadDestinoId)?.codigo}
                </p>
              )}
              <ul className="lista-simple">
                {conversiones.map((c) => (
                  <li key={c.id}>
                    {unidades.find((u) => u.id === c.unidadOrigenId)?.codigo} →{' '}
                    {unidades.find((u) => u.id === c.unidadDestinoId)?.codigo}: {c.factor}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </section>
  )
}
