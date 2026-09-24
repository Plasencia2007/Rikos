import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, Campo, Checkbox, EstadoCargando, EstadoError, Select } from '../../comun/componentes'
import {
  listarCategorias,
  listarUnidades,
  listarProductos,
  crearProducto,
  actualizarProducto,
  registrarCostoManual,
  type Categoria,
  type Unidad,
  type Producto,
  type DatosProducto,
} from '../../api/cliente'

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
  const [productoExistente, setProductoExistente] = useState<Producto | null>(null)

  const [codigo, setCodigo] = useState('')
  const [nombre, setNombre] = useState('')
  const [categoriaId, setCategoriaId] = useState('')
  const [unidadBaseId, setUnidadBaseId] = useState('')
  const [esVendible, setEsVendible] = useState(false)
  const [esInsumo, setEsInsumo] = useState(false)
  const [esSubreceta, setEsSubreceta] = useState(false)
  const [precioVentaSoles, setPrecioVentaSoles] = useState('')
  const [controlaStock, setControlaStock] = useState(true)
  const [costoInicial, setCostoInicial] = useState('')

  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setCargando(true)
    setErrorCarga('')
    Promise.all([
      listarCategorias(token),
      listarUnidades(token),
      esNuevo ? Promise.resolve([]) : listarProductos(token),
    ])
      .then(([cats, unids, productos]) => {
        setCategorias(cats)
        setUnidades(unids)
        if (!esNuevo) {
          const p = productos.find((x) => x.id === id)
          if (!p) throw new Error('producto no encontrado')
          setProductoExistente(p)
          setCodigo(p.codigo)
          setNombre(p.nombre)
          setCategoriaId(p.categoriaId)
          setUnidadBaseId(p.unidadBaseId)
          setEsVendible(p.esVendible)
          setEsInsumo(p.esInsumo)
          setEsSubreceta(p.esSubreceta)
          setPrecioVentaSoles(p.precioVenta != null ? (p.precioVenta / 100).toFixed(2) : '')
          setControlaStock(p.controlaStock)
          // Precarga el costo actual, sea manual o ya confirmado por
          // compra -- el bloque de costo-manual mas abajo lo muestra
          // deshabilitado si origenCosto es 'compra', pero el valor debe
          // verse en cualquier caso, no solo cuando es editable.
          if (p.costo != null) setCostoInicial(String(p.costo))
        }
      })
      .catch(() => setErrorCarga('No se pudo cargar el producto.'))
      .finally(() => setCargando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, esNuevo, token])

  async function guardar() {
    setError('')
    if (esVendible && !precioVentaSoles) {
      setError('El precio de venta es obligatorio para un producto vendible.')
      return
    }
    setGuardando(true)
    try {
      const datos: DatosProducto = {
        codigo,
        nombre,
        categoriaId,
        unidadBaseId,
        esVendible,
        esInsumo,
        esSubreceta,
        precioVenta: esVendible ? Math.round(parseFloat(precioVentaSoles) * 100) : null,
        controlaStock,
      }
      let idFinal = id
      if (esNuevo) {
        const creado = await crearProducto(token, datos)
        idFinal = creado.id
      } else {
        await actualizarProducto(token, id!, datos)
      }
      // Solo reenvia el costo si de verdad es editable a mano -- si ya
      // viene de una compra confirmada (origenCosto === 'compra'), el
      // campo esta deshabilitado en la UI y no debe reenviarse: el
      // backend lo rechazaria con COSTO_YA_CONFIRMADO_POR_COMPRA aunque
      // el usuario no haya tocado nada, solo por guardar el resto del
      // formulario.
      const costoEditable = !productoExistente || productoExistente.origenCosto !== 'compra'
      if (esInsumo && costoEditable && costoInicial && idFinal) {
        await registrarCostoManual(token, idFinal, parseFloat(costoInicial))
      }
      navigate('/admin/catalogo/productos')
    } catch {
      setError('No se pudo guardar el producto. Revisa que el código no esté repetido.')
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  const categoriasHoja = categorias.filter((c) => c.esHoja)
  const costoEsManualEditable = !productoExistente || productoExistente.origenCosto !== 'compra'

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>{esNuevo ? 'Nuevo producto' : 'Editar producto'}</h1>
        <Boton onClick={guardar} cargando={guardando}>
          {esNuevo ? 'Crear producto' : 'Guardar cambios'}
        </Boton>
      </div>

      <div className="panel-admin-tarjeta">
        <Campo etiqueta="Código" value={codigo} onChange={(e) => setCodigo(e.target.value)} required />
        <Campo etiqueta="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} required />

        <Select
          etiqueta="Categoría (hoja)"
          value={categoriaId}
          onChange={(e) => setCategoriaId(e.target.value)}
          required
        >
          <option value="">— Elegir —</option>
          {categoriasHoja.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </Select>

        <Select
          etiqueta="Unidad base"
          value={unidadBaseId}
          onChange={(e) => setUnidadBaseId(e.target.value)}
          required
        >
          <option value="">— Elegir —</option>
          {unidades.map((u) => (
            <option key={u.id} value={u.id}>
              {u.nombre} ({u.codigo})
            </option>
          ))}
        </Select>

        <div className="panel-admin-checkboxes">
          <Checkbox etiqueta="Es vendible" checked={esVendible} onChange={(e) => setEsVendible(e.target.checked)} />
          <Checkbox etiqueta="Es insumo" checked={esInsumo} onChange={(e) => setEsInsumo(e.target.checked)} />
          <Checkbox
            etiqueta="Es subreceta"
            checked={esSubreceta}
            onChange={(e) => setEsSubreceta(e.target.checked)}
          />
        </div>

        {esVendible && (
          <Campo
            etiqueta="Precio de venta (S/)"
            type="number"
            step="0.01"
            value={precioVentaSoles}
            onChange={(e) => setPrecioVentaSoles(e.target.value)}
            required
          />
        )}

        <Checkbox
          etiqueta="Controla stock"
          checked={controlaStock}
          onChange={(e) => setControlaStock(e.target.checked)}
        />

        {esInsumo && (
          <div className="panel-admin-tarjeta-anidada">
            <Campo
              etiqueta="Costo inicial (S/, por unidad base)"
              type="number"
              step="0.000001"
              value={costoInicial}
              onChange={(e) => setCostoInicial(e.target.value)}
              disabled={!costoEsManualEditable}
              ayuda={
                costoEsManualEditable
                  ? 'Costo estimado: se reemplaza automáticamente con la primera compra registrada de este producto.'
                  : 'Este costo ya viene de una compra confirmada y no se puede editar a mano.'
              }
            />
          </div>
        )}

        {error && <p className="campo-error">{error}</p>}
      </div>
    </section>
  )
}
