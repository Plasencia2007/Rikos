import { useEffect, useMemo, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoBusqueda,
} from '../../comun/componentes'
import {
  listarExistencias,
  obtenerResumenAlertasInventario,
  type FilaExistencia,
  type ResumenAlertasInventario,
} from '../../api/cliente'
import { DialogoStockMinimo } from './DialogoStockMinimo'

type FiltroTab = 'todos' | 'bajo_minimo' | 'negativo'

export function PantallaExistencias() {
  const { sesion } = useSesion()
  const token = sesion?.token ?? ''
  const localId = sesion?.localActivoId ?? sesion?.locales[0]?.id ?? ''

  const [existencias, setExistencias] = useState<FilaExistencia[]>([])
  const [resumen, setResumen] = useState<ResumenAlertasInventario | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [filtro, setFiltro] = useState<FiltroTab>('todos')

  // Estado para el modal de configuración de stock mínimo
  const [seleccionado, setSeleccionado] = useState<FilaExistencia | null>(null)
  const [modalAbierto, setModalAbierto] = useState(false)

  const nombreLocal = useMemo(() => {
    return sesion?.locales.find((l) => l.id === localId)?.nombre ?? 'Local'
  }, [sesion, localId])

  async function cargarDatos() {
    if (!token || !localId) return
    try {
      setCargando(true)
      setError('')
      const [lista, res] = await Promise.all([
        listarExistencias(token, localId, filtro === 'todos' ? undefined : filtro),
        obtenerResumenAlertasInventario(token, localId),
      ])
      setExistencias(lista)
      setResumen(res)
    } catch {
      setError('No se pudieron cargar las existencias de inventario.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargarDatos()
  }, [token, localId, filtro])

  const filasFiltradas = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    if (!q) return existencias
    return existencias.filter(
      (f) =>
        f.nombre.toLowerCase().includes(q) ||
        f.codigo.toLowerCase().includes(q) ||
        f.categoriaNombre.toLowerCase().includes(q),
    )
  }, [existencias, busqueda])

  function abrirConfiguracionMinimo(f: FilaExistencia) {
    setSeleccionado(f)
    setModalAbierto(true)
  }

  function alGuardarMinimo(actualizada: FilaExistencia) {
    setExistencias((prev) =>
      prev.map((item) => (item.productoId === actualizada.productoId ? actualizada : item)),
    )
    obtenerResumenAlertasInventario(token, localId)
      .then(setResumen)
      .catch(() => {})
  }

  const mensajeVacio = useMemo(() => {
    if (busqueda) return 'No se encontraron productos que coincidan con la búsqueda.'
    if (filtro === 'bajo_minimo') return 'Ningún producto se encuentra por debajo del stock mínimo.'
    if (filtro === 'negativo') return 'No hay productos con stock negativo en este local.'
    return 'No hay productos en el catálogo para mostrar existencias.'
  }, [busqueda, filtro])

  return (
    <div className="contenedor-pantalla" style={{ padding: '1.5rem', maxWidth: '1400px', margin: '0 auto' }}>
      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '1.75rem', fontWeight: 700, color: 'var(--color-texto, #0f172a)' }}>
            Existencias de Inventario
          </h1>
          <p style={{ margin: '0.25rem 0 0 0', color: 'var(--color-texto-secundario, #64748b)', fontSize: '0.95rem' }}>
            Control de stock físico, umbrales mínimos y alertas en tiempo real para <strong>{nombreLocal}</strong> (ADM-16).
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Boton
            variante="secundario"
            onClick={cargarDatos}
            disabled={cargando}
            aria-label="Actualizar datos"
          >
            Actualizar
          </Boton>
        </div>
      </div>

      {/* KPI Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        {/* Card: Total */}
        <div
          style={{
            backgroundColor: 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            padding: '1.25rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            border: '1px solid var(--color-borde, #e2e8f0)',
          }}
        >
          <div style={{ fontSize: '0.85rem', color: 'var(--color-texto-secundario, #64748b)', fontWeight: 500 }}>
            Total Productos
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--color-texto, #0f172a)', marginTop: '0.25rem' }}>
            {resumen ? resumen.totalProductos : '-'}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Catálogo activo en este local
          </div>
        </div>

        {/* Card: Bajo Mínimo */}
        <div
          onClick={() => setFiltro('bajo_minimo')}
          style={{
            backgroundColor: filtro === 'bajo_minimo' ? '#fffbeb' : 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            padding: '1.25rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            border: filtro === 'bajo_minimo' ? '2px solid #f59e0b' : '1px solid var(--color-borde, #e2e8f0)',
            cursor: 'pointer',
            transition: 'all 0.2s ease',
          }}
        >
          <div style={{ fontSize: '0.85rem', color: '#b45309', fontWeight: 600 }}>
            ⚠️ Bajo Stock Mínimo
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#d97706', marginTop: '0.25rem' }}>
            {resumen ? resumen.totalBajoMinimo : '-'}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#b45309', marginTop: '0.25rem' }}>
            Requieren reposición o compra
          </div>
        </div>

        {/* Card: Negativo */}
        <div
          onClick={() => setFiltro('negativo')}
          style={{
            backgroundColor: filtro === 'negativo' ? '#fef2f2' : 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            padding: '1.25rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            border: filtro === 'negativo' ? '2px solid #ef4444' : '1px solid var(--color-borde, #e2e8f0)',
            cursor: 'pointer',
            transition: 'all 0.2s ease',
          }}
        >
          <div style={{ fontSize: '0.85rem', color: '#b91c1c', fontWeight: 600 }}>
            🚨 Stock Negativo
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#dc2626', marginTop: '0.25rem' }}>
            {resumen ? resumen.totalNegativos : '-'}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#b91c1c', marginTop: '0.25rem' }}>
            Consumo previo a registro (RN-050)
          </div>
        </div>

        {/* Card: Sin Stock */}
        <div
          style={{
            backgroundColor: 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            padding: '1.25rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            border: '1px solid var(--color-borde, #e2e8f0)',
          }}
        >
          <div style={{ fontSize: '0.85rem', color: 'var(--color-texto-secundario, #64748b)', fontWeight: 500 }}>
            Sin Stock (0)
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#64748b', marginTop: '0.25rem' }}>
            {resumen ? resumen.totalSinStock : '-'}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            En cero sin umbral configurado
          </div>
        </div>
      </div>

      {/* Barra de Filtros y Búsqueda */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '1rem',
          marginBottom: '1rem',
          flexWrap: 'wrap',
        }}
      >
        {/* Pills de Filtrado */}
        <div style={{ display: 'flex', gap: '0.5rem', background: '#f1f5f9', padding: '0.25rem', borderRadius: '0.5rem' }}>
          <button
            type="button"
            onClick={() => setFiltro('todos')}
            style={{
              padding: '0.4rem 0.85rem',
              borderRadius: '0.375rem',
              border: 'none',
              background: filtro === 'todos' ? '#ffffff' : 'transparent',
              color: filtro === 'todos' ? '#0f172a' : '#64748b',
              fontWeight: filtro === 'todos' ? 600 : 500,
              boxShadow: filtro === 'todos' ? '0 1px 2px rgba(0,0,0,0.05)' : 'none',
              cursor: 'pointer',
              fontSize: '0.875rem',
            }}
          >
            Todos ({resumen ? resumen.totalProductos : 0})
          </button>
          <button
            type="button"
            onClick={() => setFiltro('bajo_minimo')}
            style={{
              padding: '0.4rem 0.85rem',
              borderRadius: '0.375rem',
              border: 'none',
              background: filtro === 'bajo_minimo' ? '#ffffff' : 'transparent',
              color: filtro === 'bajo_minimo' ? '#b45309' : '#64748b',
              fontWeight: filtro === 'bajo_minimo' ? 600 : 500,
              boxShadow: filtro === 'bajo_minimo' ? '0 1px 2px rgba(0,0,0,0.05)' : 'none',
              cursor: 'pointer',
              fontSize: '0.875rem',
            }}
          >
            Bajo mínimo ({resumen ? resumen.totalBajoMinimo : 0})
          </button>
          <button
            type="button"
            onClick={() => setFiltro('negativo')}
            style={{
              padding: '0.4rem 0.85rem',
              borderRadius: '0.375rem',
              border: 'none',
              background: filtro === 'negativo' ? '#ffffff' : 'transparent',
              color: filtro === 'negativo' ? '#dc2626' : '#64748b',
              fontWeight: filtro === 'negativo' ? 600 : 500,
              boxShadow: filtro === 'negativo' ? '0 1px 2px rgba(0,0,0,0.05)' : 'none',
              cursor: 'pointer',
              fontSize: '0.875rem',
            }}
          >
            Stock negativo ({resumen ? resumen.totalNegativos : 0})
          </button>
        </div>

        {/* Buscador */}
        <div style={{ position: 'relative', width: '320px', maxWidth: '100%' }}>
          <span style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }}>
            <IconoBusqueda />
          </span>
          <input
            type="text"
            placeholder="Buscar por código, nombre o categoría..."
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            style={{
              width: '100%',
              padding: '0.5rem 0.75rem 0.5rem 2.25rem',
              borderRadius: '0.5rem',
              border: '1px solid var(--color-borde, #cbd5e1)',
              fontSize: '0.875rem',
              backgroundColor: 'var(--color-superficie, #ffffff)',
            }}
          />
        </div>
      </div>

      {/* Contenido / Tabla */}
      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError
          mensaje={error}
          accion={
            <Boton variante="secundario" onClick={cargarDatos}>
              Reintentar
            </Boton>
          }
        />
      ) : filasFiltradas.length === 0 ? (
        <EstadoVacio
          titulo="Sin existencias"
          descripcion={mensajeVacio}
        />
      ) : (
        <div
          style={{
            backgroundColor: 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            border: '1px solid var(--color-borde, #e2e8f0)',
            overflowX: 'auto',
            boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
          }}
        >
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569' }}>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Código</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Producto</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Categoría</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Unidad Base</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'right' }}>Stock Actual</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'right' }}>Stock Mínimo</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'center' }}>Alerta</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'center' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filasFiltradas.map((f) => {
                const unidad = f.unidadCodigo || f.unidadNombre
                const esNegativo = f.stockActual < 0
                const esBajoMinimo = f.estadoAlerta === 'bajo_minimo'

                return (
                  <tr
                    key={f.productoId}
                    style={{
                      borderBottom: '1px solid #f1f5f9',
                      backgroundColor: esNegativo ? '#fff5f5' : esBajoMinimo ? '#fffdf7' : 'transparent',
                      transition: 'background-color 0.15s ease',
                    }}
                  >
                    {/* Código */}
                    <td style={{ padding: '0.85rem 1rem', fontFamily: 'monospace', color: '#64748b', fontSize: '0.825rem' }}>
                      {f.codigo}
                    </td>

                    {/* Producto */}
                    <td style={{ padding: '0.85rem 1rem', fontWeight: 600, color: '#0f172a' }}>
                      {f.nombre}
                    </td>

                    {/* Categoría */}
                    <td style={{ padding: '0.85rem 1rem', color: '#475569' }}>
                      {f.categoriaNombre}
                    </td>

                    {/* Unidad */}
                    <td style={{ padding: '0.85rem 1rem', color: '#475569' }}>
                      {f.unidadNombre} ({unidad})
                    </td>

                    {/* Stock Actual */}
                    <td
                      style={{
                        padding: '0.85rem 1rem',
                        textAlign: 'right',
                        fontWeight: 700,
                        fontSize: '0.95rem',
                        color: esNegativo ? '#dc2626' : esBajoMinimo ? '#d97706' : f.stockActual === 0 ? '#94a3b8' : '#059669',
                      }}
                    >
                      {f.stockActual} <span style={{ fontSize: '0.75rem', fontWeight: 400 }}>{unidad}</span>
                    </td>

                    {/* Stock Mínimo */}
                    <td style={{ padding: '0.85rem 1rem', textAlign: 'right', color: '#334155' }}>
                      {f.stockMinimo != null ? (
                        <span style={{ fontWeight: 600 }}>
                          {f.stockMinimo} <span style={{ fontSize: '0.75rem', fontWeight: 400 }}>{unidad}</span>
                        </span>
                      ) : (
                        <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Sin umbral</span>
                      )}
                    </td>

                    {/* Alerta Badge */}
                    <td style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>
                      {esNegativo ? (
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem',
                            padding: '0.25rem 0.6rem',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: '#fee2e2',
                            color: '#991b1b',
                            border: '1px solid #fecaca',
                          }}
                        >
                          ● Stock Negativo
                        </span>
                      ) : esBajoMinimo ? (
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem',
                            padding: '0.25rem 0.6rem',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: '#fef3c7',
                            color: '#92400e',
                            border: '1px solid #fde68a',
                          }}
                        >
                          ▲ Bajo Mínimo
                        </span>
                      ) : f.stockActual === 0 ? (
                        <span
                          style={{
                            display: 'inline-flex',
                            padding: '0.25rem 0.6rem',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 500,
                            backgroundColor: '#f1f5f9',
                            color: '#64748b',
                          }}
                        >
                          Sin stock
                        </span>
                      ) : (
                        <span
                          style={{
                            display: 'inline-flex',
                            padding: '0.25rem 0.6rem',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            backgroundColor: '#dcfce7',
                            color: '#166534',
                          }}
                        >
                          ✓ Normal
                        </span>
                      )}
                    </td>

                    {/* Acciones */}
                    <td style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>
                      <button
                        type="button"
                        onClick={() => abrirConfiguracionMinimo(f)}
                        style={{
                          padding: '0.35rem 0.75rem',
                          borderRadius: '0.375rem',
                          border: '1px solid #cbd5e1',
                          background: '#ffffff',
                          color: '#0284c7',
                          fontWeight: 600,
                          fontSize: '0.8rem',
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.background = '#f0f9ff')}
                        onMouseLeave={(e) => (e.currentTarget.style.background = '#ffffff')}
                      >
                        {f.stockMinimo != null ? 'Editar mínimo' : 'Fijar mínimo'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal de edición de stock mínimo */}
      <DialogoStockMinimo
        existencia={seleccionado}
        abierto={modalAbierto}
        token={token}
        localId={localId}
        onGuardado={alGuardarMinimo}
        onCerrar={() => setModalAbierto(false)}
      />
    </div>
  )
}
