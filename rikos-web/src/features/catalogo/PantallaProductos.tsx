import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, Campo, ModalDeConfirmacion, Select, Tabla, type ColumnaTabla } from '../../comun/componentes'
import {
  listarProductos,
  listarCategorias,
  desactivarProducto,
  activarProducto,
  type Producto,
  type Categoria,
} from '../../api/cliente'

export function PantallaProductos() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [productos, setProductos] = useState<Producto[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const [filtroNaturaleza, setFiltroNaturaleza] = useState('')
  const [filtroEstado, setFiltroEstado] = useState<'todos' | 'activos' | 'inactivos'>('activos')
  const [paraCambiarEstado, setParaCambiarEstado] = useState<Producto | null>(null)

  function cargar() {
    setCargando(true)
    setError('')
    Promise.all([listarProductos(token), listarCategorias(token)])
      .then(([p, c]) => {
        setProductos(p)
        setCategorias(c)
      })
      .catch(() => setError('No se pudieron cargar los productos.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const filtrados = useMemo(() => {
    return productos.filter((p) => {
      if (
        busqueda &&
        !p.nombre.toLowerCase().includes(busqueda.toLowerCase()) &&
        !p.codigo.toLowerCase().includes(busqueda.toLowerCase())
      ) {
        return false
      }
      if (filtroCategoria && p.categoriaId !== filtroCategoria) return false
      if (filtroNaturaleza === 'vendible' && !p.esVendible) return false
      if (filtroNaturaleza === 'insumo' && !p.esInsumo) return false
      if (filtroNaturaleza === 'subreceta' && !p.esSubreceta) return false
      if (filtroEstado === 'activos' && !p.activo) return false
      if (filtroEstado === 'inactivos' && p.activo) return false
      return true
    })
  }, [productos, busqueda, filtroCategoria, filtroNaturaleza, filtroEstado])

  async function confirmarCambioEstado() {
    if (!paraCambiarEstado) return
    if (paraCambiarEstado.activo) {
      await desactivarProducto(token, paraCambiarEstado.id)
    } else {
      await activarProducto(token, paraCambiarEstado.id)
    }
    setParaCambiarEstado(null)
    cargar()
  }

  const columnas: ColumnaTabla<Producto>[] = [
    { encabezado: 'Código', render: (p) => p.codigo },
    { encabezado: 'Nombre', render: (p) => p.nombre },
    { encabezado: 'Categoría', render: (p) => p.categoriaNombre },
    {
      encabezado: 'Naturaleza',
      render: (p) =>
        [p.esVendible && 'Vendible', p.esInsumo && 'Insumo', p.esSubreceta && 'Subreceta']
          .filter(Boolean)
          .join(' · ') || '—',
    },
    { encabezado: 'Estado', render: (p) => (p.activo ? 'Activo' : 'Inactivo') },
    {
      encabezado: 'Acciones',
      acciones: true,
      render: (p) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <Boton variante="secundario" onClick={() => navigate(`/admin/catalogo/productos/${p.id}/editar`)}>
            Editar
          </Boton>
          <Boton variante={p.activo ? 'peligro' : 'secundario'} onClick={() => setParaCambiarEstado(p)}>
            {p.activo ? 'Desactivar' : 'Activar'}
          </Boton>
        </div>
      ),
    },
  ]

  const hayCategoriasHoja = categorias.some((c) => c.esHoja)

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>Productos</h1>
        <Boton onClick={() => navigate('/admin/catalogo/productos/nuevo')} disabled={!hayCategoriasHoja}>
          Nuevo producto
        </Boton>
      </div>

      <div className="panel-admin-filtros">
        <Campo
          etiqueta="Buscar"
          placeholder="Código o nombre"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
        />
        <Select etiqueta="Categoría" value={filtroCategoria} onChange={(e) => setFiltroCategoria(e.target.value)}>
          <option value="">Todas</option>
          {categorias
            .filter((c) => c.esHoja)
            .map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
        </Select>
        <Select
          etiqueta="Naturaleza"
          value={filtroNaturaleza}
          onChange={(e) => setFiltroNaturaleza(e.target.value)}
        >
          <option value="">Todas</option>
          <option value="vendible">Vendible</option>
          <option value="insumo">Insumo</option>
          <option value="subreceta">Subreceta</option>
        </Select>
        <Select
          etiqueta="Estado"
          value={filtroEstado}
          onChange={(e) => setFiltroEstado(e.target.value as typeof filtroEstado)}
        >
          <option value="activos">Activos</option>
          <option value="inactivos">Inactivos</option>
          <option value="todos">Todos</option>
        </Select>
      </div>

      <Tabla
        columnas={columnas}
        filas={filtrados}
        obtenerClave={(p) => p.id}
        cargando={cargando}
        error={error}
        vacioTitulo={
          !hayCategoriasHoja && productos.length === 0
            ? 'Primero crea una categoría'
            : productos.length === 0
              ? 'Todavía no hay productos'
              : 'Nada coincide con el filtro'
        }
        vacioDescripcion={
          !hayCategoriasHoja && productos.length === 0
            ? 'Un producto necesita una categoría hoja para colgar de ella. Ve a Categorías y crea al menos una subcategoría.'
            : productos.length === 0
              ? 'Crea el primer producto del catálogo: elige su categoría, su unidad base y si es vendible, insumo o subreceta.'
              : 'Prueba con otro texto de búsqueda o quita algún filtro.'
        }
        vacioAccion={
          !hayCategoriasHoja && productos.length === 0 ? (
            <Boton onClick={() => navigate('/admin/catalogo/categorias')}>Ir a Categorías</Boton>
          ) : productos.length === 0 ? (
            <Boton onClick={() => navigate('/admin/catalogo/productos/nuevo')}>Nuevo producto</Boton>
          ) : undefined
        }
      />

      {paraCambiarEstado && (
        <ModalDeConfirmacion
          abierto
          titulo={paraCambiarEstado.activo ? 'Desactivar producto' : 'Activar producto'}
          mensaje={
            paraCambiarEstado.activo
              ? `"${paraCambiarEstado.nombre}" dejará de poder usarse en registros nuevos. Su historial se conserva.`
              : `"${paraCambiarEstado.nombre}" volverá a estar disponible para registros nuevos.`
          }
          onConfirmar={confirmarCambioEstado}
          onCancelar={() => setParaCambiarEstado(null)}
        />
      )}
    </section>
  )
}
