import { useState, useEffect, useMemo, useCallback } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  consultarKardex,
  listarExistencias,
  type PaginaKardex,
  type FiltroKardexConsulta,
} from '../../api/cliente'
import {
  History,
  RotateCw,
  Filter,
  Package,
  ArrowUpRight,
  ArrowDownRight,
  ChevronLeft,
  ChevronRight,
  X,
  FileSpreadsheet,
  ArrowLeft,
} from 'lucide-react'

interface Props {
  token?: string
  localId?: string
  localNombre?: string
}

export function PantallaKardex(props: Props) {
  const { sesion } = useSesion()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const token = props.token ?? sesion?.token ?? ''
  const localId = props.localId ?? sesion?.localActivoId ?? sesion?.locales[0]?.id ?? ''
  const localNombre =
    props.localNombre ?? sesion?.locales.find((l) => l.id === localId)?.nombre ?? 'Local Activo'

  // Param de producto inicial desde URL (?productoId=...)
  const paramProductoId = searchParams.get('productoId') ?? ''

  // Estados de filtros
  const [filtroProducto, setFiltroProducto] = useState(paramProductoId)
  const [filtroOrigen, setFiltroOrigen] = useState('todos')
  const [filtroDesde, setFiltroDesde] = useState('')
  const [filtroHasta, setFiltroHasta] = useState('')
  const [orden, setOrden] = useState<'desc' | 'asc'>('desc')
  const [pagina, setPagina] = useState(0)
  const [tamano, setTamano] = useState(25)

  // Datos
  const [kardex, setKardex] = useState<PaginaKardex | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Productos para el selector
  const [productos, setProductos] = useState<
    Array<{ id: string; codigo: string; nombre: string; unidadCodigo: string }>
  >([])

  // Sincronizar filtroProducto si cambia el query param en la URL
  useEffect(() => {
    if (paramProductoId) {
      setFiltroProducto(paramProductoId)
    }
  }, [paramProductoId])

  // Cargar catálogo de productos del local
  useEffect(() => {
    if (!token || !localId) return
    listarExistencias(token, localId)
      .then((existencias) => {
        setProductos(
          existencias.map((e) => ({
            id: e.productoId,
            codigo: e.codigo,
            nombre: e.nombre,
            unidadCodigo: e.unidadCodigo,
          }))
        )
      })
      .catch((err) => console.error('Error al cargar catálogo de productos:', err))
  }, [token, localId])

  // Cargar movimientos del Kardex
  const cargarKardex = useCallback(async () => {
    if (!token || !localId) return
    setCargando(true)
    setError(null)
    try {
      const filtros: FiltroKardexConsulta = {
        productoId: filtroProducto || undefined,
        origen: filtroOrigen !== 'todos' ? filtroOrigen : undefined,
        desde: filtroDesde || undefined,
        hasta: filtroHasta || undefined,
        orden,
        pagina,
        tamano,
      }
      const respuesta = await consultarKardex(token, localId, filtros)
      setKardex(respuesta)
    } catch (e: any) {
      console.error('Error al consultar Kardex:', e)
      setError(e.message ?? 'No se pudo cargar el Kardex de inventario.')
    } finally {
      setCargando(false)
    }
  }, [token, localId, filtroProducto, filtroOrigen, filtroDesde, filtroHasta, orden, pagina, tamano])

  useEffect(() => {
    cargarKardex()
  }, [cargarKardex])

  // Limpiar filtros
  const limpiarFiltros = () => {
    setFiltroProducto('')
    setFiltroOrigen('todos')
    setFiltroDesde('')
    setFiltroHasta('')
    setOrden('desc')
    setPagina(0)
    setSearchParams({})
  }

  // Producto seleccionado actualmente para mostrar en cabecera si hay filtro
  const productoFiltrado = useMemo(() => {
    if (!filtroProducto) return null
    return productos.find((p) => p.id === filtroProducto) ?? null
  }, [filtroProducto, productos])

  // Formato de badge de origen
  const renderBadgeOrigen = (origen: string) => {
    const estilos: Record<string, { etiqueta: string; bg: string; color: string; border: string }> = {
      compra: { etiqueta: 'Compra', bg: '#eff6ff', color: '#1d4ed8', border: '#bfdbfe' },
      venta: { etiqueta: 'Venta', bg: '#faf5ff', color: '#7e22ce', border: '#e9d5ff' },
      produccion: { etiqueta: 'Producción', bg: '#ecfdf5', color: '#047857', border: '#a7f3d0' },
      ajuste: { etiqueta: 'Ajuste', bg: '#fffbeb', color: '#b45309', border: '#fde68a' },
      transferencia: { etiqueta: 'Transferencia', bg: '#ecfeff', color: '#0e7490', border: '#a5f3fc' },
      conteo: { etiqueta: 'Conteo Físico', bg: '#fdf4ff', color: '#a21caf', border: '#f5d0fe' },
      consumo_interno: { etiqueta: 'Consumo Interno', bg: '#eef2ff', color: '#4338ca', border: '#c7d2fe' },
      anulacion_post_comanda: { etiqueta: 'Anulación Comanda', bg: '#fff1f2', color: '#be123c', border: '#fecdd3' },
    }
    const config = estilos[origen.toLowerCase()] ?? {
      etiqueta: origen,
      bg: '#f1f5f9',
      color: '#475569',
      border: '#cbd5e1',
    }
    return (
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          padding: '0.2rem 0.55rem',
          borderRadius: '9999px',
          fontSize: '0.75rem',
          fontWeight: 600,
          backgroundColor: config.bg,
          color: config.color,
          border: `1px solid ${config.border}`,
          whiteSpace: 'nowrap',
        }}
      >
        {config.etiqueta}
      </span>
    )
  }

  const formatearFecha = (iso: string) => {
    if (!iso) return '-'
    const d = new Date(iso)
    return d.toLocaleString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const formatearCantidad = (val: number, simbolo: string) => {
    const dec = simbolo === 'und' ? 0 : 3
    return `${val.toLocaleString('es-PE', { minimumFractionDigits: dec, maximumFractionDigits: dec })} ${simbolo}`
  }

  return (
    <div
      style={{
        width: '100%',
        maxWidth: '100%',
        minWidth: 0,
        boxSizing: 'border-box',
        padding: '1.5rem',
        margin: '0 auto',
        fontFamily: 'inherit',
      }}
    >
      {/* Cabecera de la pantalla */}
      <div
        style={{
          width: '100%',
          boxSizing: 'border-box',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1.5rem',
          borderBottom: '1px solid #e2e8f0',
          paddingBottom: '1.25rem',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
            <button
              type="button"
              onClick={() => navigate('/admin/inventario/existencias')}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '0.35rem',
                borderRadius: '0.375rem',
                border: '1px solid #cbd5e1',
                background: '#ffffff',
                color: '#475569',
                cursor: 'pointer',
              }}
              title="Volver a Existencias"
            >
              <ArrowLeft style={{ width: '1rem', height: '1rem' }} />
            </button>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.35rem',
                fontSize: '0.75rem',
                fontWeight: 700,
                color: '#4f46e5',
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
              }}
            >
              <History style={{ width: '0.9rem', height: '0.9rem' }} />
              Inventario y Operaciones · ADM-17
            </span>
          </div>

          <h1 style={{ margin: 0, fontSize: '1.75rem', fontWeight: 700, color: '#0f172a', letterSpacing: '-0.02em' }}>
            Kardex de Inventario
          </h1>
          <p style={{ margin: '0.25rem 0 0 0', color: '#64748b', fontSize: '0.925rem' }}>
            Libro de movimientos inmutable con origen rastreable, balance continuo y auditoría en{' '}
            <strong style={{ color: '#334155' }}>{localNombre}</strong>.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <button
            type="button"
            onClick={() => cargarKardex()}
            disabled={cargando}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '0.5rem 1rem',
              borderRadius: '0.5rem',
              border: '1px solid #cbd5e1',
              backgroundColor: '#ffffff',
              color: '#334155',
              fontWeight: 600,
              fontSize: '0.875rem',
              cursor: cargando ? 'not-allowed' : 'pointer',
              boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
              opacity: cargando ? 0.6 : 1,
            }}
          >
            <RotateCw style={{ width: '1rem', height: '1rem', animation: cargando ? 'spin 1s linear infinite' : 'none' }} />
            <span>Refrescar</span>
          </button>
        </div>
      </div>

      {/* Banner si hay un producto específico filtrado */}
      {productoFiltrado && (
        <div
          style={{
            width: '100%',
            maxWidth: '100%',
            minWidth: 0,
            boxSizing: 'border-box',
            backgroundColor: '#eef2ff',
            border: '1px solid #c7d2fe',
            borderRadius: '0.75rem',
            padding: '1rem 1.25rem',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '0.75rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                padding: '0.5rem',
                backgroundColor: '#e0e7ff',
                borderRadius: '0.5rem',
                color: '#4338ca',
                display: 'flex',
              }}
            >
              <Package style={{ width: '1.25rem', height: '1.25rem' }} />
            </div>
            <div>
              <div style={{ fontSize: '0.7rem', fontWeight: 700, color: '#4f46e5', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Filtrado por producto
              </div>
              <div style={{ fontSize: '1rem', fontWeight: 700, color: '#1e1b4b' }}>
                {productoFiltrado.nombre}{' '}
                <span
                  style={{
                    fontSize: '0.75rem',
                    fontFamily: 'monospace',
                    backgroundColor: '#ffffff',
                    padding: '0.15rem 0.5rem',
                    borderRadius: '0.375rem',
                    border: '1px solid #c7d2fe',
                    color: '#4338ca',
                    marginLeft: '0.25rem',
                  }}
                >
                  {productoFiltrado.codigo}
                </span>
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              setFiltroProducto('')
              setSearchParams({})
            }}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              padding: '0.4rem 0.85rem',
              borderRadius: '0.5rem',
              border: '1px solid #c7d2fe',
              backgroundColor: '#ffffff',
              color: '#4338ca',
              fontSize: '0.8rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            <X style={{ width: '0.85rem', height: '0.85rem' }} />
            <span>Ver todos los productos</span>
          </button>
        </div>
      )}

      {/* Tarjeta de Filtros Combinables */}
      <div
        style={{
          width: '100%',
          maxWidth: '100%',
          minWidth: 0,
          boxSizing: 'border-box',
          backgroundColor: '#ffffff',
          border: '1px solid #e2e8f0',
          borderRadius: '0.75rem',
          padding: '1.25rem',
          marginBottom: '1.5rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.4rem',
            fontSize: '0.75rem',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
            color: '#64748b',
            marginBottom: '1rem',
          }}
        >
          <Filter style={{ width: '0.9rem', height: '0.9rem', color: '#4f46e5' }} />
          <span>Filtros combinables</span>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '1rem',
          }}
        >
          {/* Selector de producto */}
          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
              Producto / Insumo
            </label>
            <select
              value={filtroProducto}
              onChange={(e) => {
                setFiltroProducto(e.target.value)
                setPagina(0)
                if (e.target.value) {
                  setSearchParams({ productoId: e.target.value })
                } else {
                  setSearchParams({})
                }
              }}
              style={{
                width: '100%',
                padding: '0.5rem 0.75rem',
                borderRadius: '0.5rem',
                border: '1px solid #cbd5e1',
                backgroundColor: '#ffffff',
                color: '#1e293b',
                fontSize: '0.875rem',
                boxSizing: 'border-box',
                outline: 'none',
              }}
            >
              <option value="">Todos los productos</option>
              {productos.map((p) => (
                <option key={p.id} value={p.id}>
                  [{p.codigo}] {p.nombre}
                </option>
              ))}
            </select>
          </div>

          {/* Selector de origen */}
          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
              Origen del movimiento
            </label>
            <select
              value={filtroOrigen}
              onChange={(e) => {
                setFiltroOrigen(e.target.value)
                setPagina(0)
              }}
              style={{
                width: '100%',
                padding: '0.5rem 0.75rem',
                borderRadius: '0.5rem',
                border: '1px solid #cbd5e1',
                backgroundColor: '#ffffff',
                color: '#1e293b',
                fontSize: '0.875rem',
                boxSizing: 'border-box',
                outline: 'none',
              }}
            >
              <option value="todos">Todos los orígenes</option>
              <option value="compra">Compra</option>
              <option value="venta">Venta</option>
              <option value="produccion">Producción</option>
              <option value="ajuste">Ajuste de inventario</option>
              <option value="transferencia">Transferencia entre locales</option>
              <option value="conteo">Conteo físico</option>
              <option value="consumo_interno">Consumo interno</option>
              <option value="anulacion_post_comanda">Anulación post comanda</option>
            </select>
          </div>

          {/* Fecha desde */}
          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
              Desde
            </label>
            <input
              type="date"
              value={filtroDesde}
              onChange={(e) => {
                setFiltroDesde(e.target.value)
                setPagina(0)
              }}
              style={{
                width: '100%',
                padding: '0.5rem 0.75rem',
                borderRadius: '0.5rem',
                border: '1px solid #cbd5e1',
                backgroundColor: '#ffffff',
                color: '#1e293b',
                fontSize: '0.875rem',
                boxSizing: 'border-box',
                outline: 'none',
              }}
            />
          </div>

          {/* Fecha hasta */}
          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
              Hasta
            </label>
            <input
              type="date"
              value={filtroHasta}
              onChange={(e) => {
                setFiltroHasta(e.target.value)
                setPagina(0)
              }}
              style={{
                width: '100%',
                padding: '0.5rem 0.75rem',
                borderRadius: '0.5rem',
                border: '1px solid #cbd5e1',
                backgroundColor: '#ffffff',
                color: '#1e293b',
                fontSize: '0.875rem',
                boxSizing: 'border-box',
                outline: 'none',
              }}
            />
          </div>

          {/* Orden cronológico */}
          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
              Orden cronológico
            </label>
            <select
              value={orden}
              onChange={(e) => {
                setOrden(e.target.value as 'desc' | 'asc')
                setPagina(0)
              }}
              style={{
                width: '100%',
                padding: '0.5rem 0.75rem',
                borderRadius: '0.5rem',
                border: '1px solid #cbd5e1',
                backgroundColor: '#ffffff',
                color: '#1e293b',
                fontSize: '0.875rem',
                boxSizing: 'border-box',
                outline: 'none',
              }}
            >
              <option value="desc">Más recientes primero (DESC)</option>
              <option value="asc">Más antiguos primero (ASC)</option>
            </select>
          </div>
        </div>

        {/* Resumen de filtro y botón limpiar */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginTop: '1rem',
            paddingTop: '0.75rem',
            borderTop: '1px solid #f1f5f9',
            fontSize: '0.8rem',
          }}
        >
          <span style={{ color: '#64748b' }}>
            Total de movimientos encontrados: <strong style={{ color: '#0f172a' }}>{kardex?.totalElementos ?? 0}</strong>
          </span>

          {(filtroProducto || filtroOrigen !== 'todos' || filtroDesde || filtroHasta || orden !== 'desc') && (
            <button
              type="button"
              onClick={limpiarFiltros}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.25rem',
                border: 'none',
                background: 'transparent',
                color: '#4f46e5',
                fontWeight: 600,
                fontSize: '0.8rem',
                cursor: 'pointer',
              }}
            >
              <X style={{ width: '0.85rem', height: '0.85rem' }} />
              <span>Limpiar filtros</span>
            </button>
          )}
        </div>
      </div>

      {/* Mensaje de error */}
      {error && (
        <div
          style={{
            padding: '1rem 1.25rem',
            backgroundColor: '#fef2f2',
            border: '1px solid #fecaca',
            borderRadius: '0.75rem',
            color: '#b91c1c',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '1.5rem',
          }}
        >
          <span>{error}</span>
          <button
            type="button"
            onClick={() => cargarKardex()}
            style={{
              padding: '0.35rem 0.75rem',
              borderRadius: '0.375rem',
              backgroundColor: '#b91c1c',
              color: '#ffffff',
              border: 'none',
              fontSize: '0.75rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Reintentar
          </button>
        </div>
      )}

      {/* Tabla del Kardex */}
      <div
        style={{
          width: '100%',
          maxWidth: '100%',
          minWidth: 0,
          boxSizing: 'border-box',
          backgroundColor: '#ffffff',
          borderRadius: '0.75rem',
          border: '1px solid #e2e8f0',
          overflow: 'hidden',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
        }}
      >
        <div style={{ overflowX: 'auto', width: '100%', WebkitOverflowScrolling: 'touch' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
                <th style={{ padding: '0.75rem 0.85rem' }}>Fecha / Hora</th>
                <th style={{ padding: '0.75rem 0.85rem' }}>Producto</th>
                <th style={{ padding: '0.75rem 0.85rem' }}>Origen</th>
                <th style={{ padding: '0.75rem 0.85rem', textAlign: 'right' }}>Movimiento</th>
                <th style={{ padding: '0.75rem 0.85rem', textAlign: 'center' }}>Variación Stock</th>
                <th style={{ padding: '0.75rem 0.85rem', textAlign: 'right' }}>Costo Unit.</th>
                <th style={{ padding: '0.75rem 0.85rem', textAlign: 'right' }}>Costo Total</th>
                <th style={{ padding: '0.75rem 0.85rem' }}>Referencia / Motivo</th>
                <th style={{ padding: '0.75rem 0.85rem' }}>Registrado Por</th>
              </tr>
            </thead>
            <tbody>
              {cargando && !kardex ? (
                <tr>
                  <td colSpan={9} style={{ padding: '3rem 1rem', textAlign: 'center', color: '#64748b' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem' }}>
                      <RotateCw style={{ width: '1.5rem', height: '1.5rem', animation: 'spin 1s linear infinite', color: '#4f46e5' }} />
                      <span>Cargando movimientos del Kardex...</span>
                    </div>
                  </td>
                </tr>
              ) : !kardex?.elementos.length ? (
                <tr>
                  <td colSpan={9} style={{ padding: '4rem 1rem', textAlign: 'center', color: '#64748b' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.75rem' }}>
                      <FileSpreadsheet style={{ width: '2.5rem', height: '2.5rem', color: '#cbd5e1' }} />
                      <div>
                        <p style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: '#334155' }}>
                          No se encontraron movimientos
                        </p>
                        <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.8rem', color: '#94a3b8' }}>
                          Prueba cambiando los filtros de producto, origen o rango de fechas.
                        </p>
                      </div>
                    </div>
                  </td>
                </tr>
              ) : (
                kardex.elementos.map((mov) => {
                  const esEntrada = mov.signo.toLowerCase() === 'entrada'
                  const colorSigno = esEntrada ? '#059669' : '#dc2626'

                  return (
                    <tr
                      key={mov.id}
                      style={{ borderBottom: '1px solid #f1f5f9', transition: 'background-color 0.15s ease' }}
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#f8fafc')}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                    >
                      {/* Fecha y hora */}
                      <td style={{ padding: '0.75rem 0.85rem', whiteSpace: 'nowrap' }}>
                        <div style={{ fontWeight: 600, color: '#1e293b' }}>{formatearFecha(mov.ocurridoEn)}</div>
                        <div style={{ fontSize: '0.75rem', color: '#94a3b8', fontFamily: 'monospace', marginTop: '0.15rem' }}>
                          reg: {new Date(mov.registradoEn).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}
                        </div>
                      </td>

                      {/* Producto */}
                      <td style={{ padding: '0.75rem 0.85rem', minWidth: '170px' }}>
                        <div style={{ fontWeight: 600, color: '#0f172a', whiteSpace: 'nowrap' }}>{mov.productoNombre}</div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', color: '#64748b', marginTop: '0.2rem' }}>
                          <span style={{ fontFamily: 'monospace', backgroundColor: '#f1f5f9', padding: '0.1rem 0.35rem', borderRadius: '0.25rem', border: '1px solid #e2e8f0', whiteSpace: 'nowrap' }}>
                            {mov.productoCodigo}
                          </span>
                          {mov.categoriaNombre && <span style={{ whiteSpace: 'nowrap' }}>· {mov.categoriaNombre}</span>}
                        </div>
                      </td>

                      {/* Origen */}
                      <td style={{ padding: '0.75rem 0.85rem', whiteSpace: 'nowrap' }}>
                        {renderBadgeOrigen(mov.origen)}
                      </td>

                      {/* Movimiento (Signo & Cantidad) */}
                      <td style={{ padding: '0.75rem 0.85rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                        <div
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.2rem',
                            fontWeight: 700,
                            color: colorSigno,
                          }}
                        >
                          {esEntrada ? (
                            <ArrowUpRight style={{ width: '1rem', height: '1rem' }} />
                          ) : (
                            <ArrowDownRight style={{ width: '1rem', height: '1rem' }} />
                          )}
                          <span>
                            {esEntrada ? '+' : '-'}
                            {formatearCantidad(mov.cantidadBase, mov.unidadMedidaSimbolo)}
                          </span>
                        </div>
                        {mov.cantidad !== mov.cantidadBase && (
                          <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                            Pres.: {mov.cantidad}
                          </div>
                        )}
                      </td>

                      {/* Variación de Stock */}
                      <td style={{ padding: '0.75rem 0.85rem', textAlign: 'center', whiteSpace: 'nowrap', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                        <span style={{ color: '#64748b' }}>{mov.stockAnterior.toFixed(2)}</span>
                        <span style={{ color: '#94a3b8', margin: '0 0.35rem' }}>→</span>
                        <span
                          style={{
                            fontWeight: 700,
                            color: mov.stockNuevo < 0 ? '#dc2626' : '#0f172a',
                          }}
                        >
                          {mov.stockNuevo.toFixed(2)} {mov.unidadMedidaSimbolo}
                        </span>
                      </td>

                      {/* Costo Unitario */}
                      <td style={{ padding: '0.75rem 0.85rem', textAlign: 'right', whiteSpace: 'nowrap', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                        {mov.costoUnitario !== null ? (
                          <span style={{ color: '#334155' }}>
                            S/ {mov.costoUnitario.toFixed(2)}
                          </span>
                        ) : (
                          <span style={{ color: '#94a3b8' }}>-</span>
                        )}
                      </td>

                      {/* Costo Total */}
                      <td style={{ padding: '0.75rem 0.85rem', textAlign: 'right', whiteSpace: 'nowrap', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                        {mov.costoTotal !== null ? (
                          <span style={{ fontWeight: 700, color: '#047857' }}>
                            S/ {mov.costoTotal.toFixed(2)}
                          </span>
                        ) : (
                          <span style={{ color: '#94a3b8' }}>-</span>
                        )}
                      </td>

                      {/* Referencia / Motivo */}
                      <td style={{ padding: '0.75rem 0.85rem', maxWidth: '260px' }}>
                        {mov.referenciaTipo && (
                          <div style={{ fontSize: '0.75rem', fontFamily: 'monospace', color: '#4338ca' }}>
                            {mov.referenciaTipo}: {mov.referenciaId ? mov.referenciaId.substring(0, 12) + '...' : '-'}
                          </div>
                        )}
                        {mov.motivoDescripcion && (
                          <div style={{ fontSize: '0.75rem', color: '#b45309', fontWeight: 600 }}>
                            Motivo: {mov.motivoDescripcion}
                          </div>
                        )}
                        {mov.observacion && (
                          <div
                            style={{
                              fontSize: '0.75rem',
                              color: '#64748b',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                            }}
                            title={mov.observacion}
                          >
                            {mov.observacion}
                          </div>
                        )}
                        {!mov.referenciaTipo && !mov.motivoDescripcion && !mov.observacion && (
                          <span style={{ color: '#94a3b8', fontSize: '0.75rem' }}>-</span>
                        )}
                      </td>

                      {/* Registrado por */}
                      <td style={{ padding: '0.75rem 0.85rem', whiteSpace: 'nowrap', fontSize: '0.8rem', color: '#334155' }}>
                        {mov.registradoPorNombre || 'Sistema'}
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Barra de paginación */}
        {kardex && kardex.totalElementos > 0 && (
          <div
            style={{
              width: '100%',
              boxSizing: 'border-box',
              padding: '0.85rem 1.25rem',
              backgroundColor: '#f8fafc',
              borderTop: '1px solid #e2e8f0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: '0.75rem',
              fontSize: '0.8rem',
              color: '#64748b',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <span>
                Mostrando <strong>{pagina * tamano + 1}</strong> a{' '}
                <strong>{Math.min((pagina + 1) * tamano, kardex.totalElementos)}</strong> de{' '}
                <strong>{kardex.totalElementos}</strong> movimientos
              </span>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <span>Por página:</span>
                <select
                  value={tamano}
                  onChange={(e) => {
                    setTamano(Number(e.target.value))
                    setPagina(0)
                  }}
                  style={{
                    backgroundColor: '#ffffff',
                    border: '1px solid #cbd5e1',
                    borderRadius: '0.375rem',
                    padding: '0.2rem 0.5rem',
                    fontSize: '0.75rem',
                    color: '#1e293b',
                    outline: 'none',
                  }}
                >
                  <option value={10}>10</option>
                  <option value={25}>25</option>
                  <option value={50}>50</option>
                  <option value={100}>100</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button
                type="button"
                onClick={() => setPagina((p) => Math.max(0, p - 1))}
                disabled={pagina === 0 || cargando}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  padding: '0.35rem 0.5rem',
                  borderRadius: '0.375rem',
                  border: '1px solid #cbd5e1',
                  backgroundColor: '#ffffff',
                  color: '#334155',
                  cursor: pagina === 0 || cargando ? 'not-allowed' : 'pointer',
                  opacity: pagina === 0 || cargando ? 0.4 : 1,
                }}
                title="Página anterior"
              >
                <ChevronLeft style={{ width: '1rem', height: '1rem' }} />
              </button>
              <span style={{ fontWeight: 600, color: '#0f172a', padding: '0 0.5rem' }}>
                Página {pagina + 1} de {kardex.totalPaginas || 1}
              </span>
              <button
                type="button"
                onClick={() => setPagina((p) => Math.min((kardex.totalPaginas || 1) - 1, p + 1))}
                disabled={pagina >= (kardex.totalPaginas || 1) - 1 || cargando}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  padding: '0.35rem 0.5rem',
                  borderRadius: '0.375rem',
                  border: '1px solid #cbd5e1',
                  backgroundColor: '#ffffff',
                  color: '#334155',
                  cursor: pagina >= (kardex.totalPaginas || 1) - 1 || cargando ? 'not-allowed' : 'pointer',
                  opacity: pagina >= (kardex.totalPaginas || 1) - 1 || cargando ? 0.4 : 1,
                }}
                title="Página siguiente"
              >
                <ChevronRight style={{ width: '1rem', height: '1rem' }} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
