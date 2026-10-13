import { useState, useEffect, useMemo } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { listarProduccion, type DetalleProduccion } from '../../api/cliente'
import { ModalRegistrarProduccion } from './ModalRegistrarProduccion'

interface Props {
  token?: string
  localId?: string
  localNombre?: string
}

export function PantallaProduccion(props: Props) {
  const { sesion } = useSesion()
  const token = props.token ?? sesion?.token ?? ''
  const localId = props.localId ?? sesion?.localActivoId ?? sesion?.locales[0]?.id ?? ''
  const localNombre = props.localNombre ?? sesion?.locales.find(l => l.id === localId)?.nombre ?? 'Local Activo'

  const [producciones, setProducciones] = useState<DetalleProduccion[]>([])
  const [cargando, setCargando] = useState(true)
  const [fechaFiltro, setFechaFiltro] = useState('')
  const [filtroTipo, setFiltroTipo] = useState<'todas' | 'subrecetas' | 'platos'>('todas')
  const [modalAbierto, setModalAbierto] = useState(false)

  const cargarDatos = async () => {
    try {
      setCargando(true)
      const data = await listarProduccion(token, localId, fechaFiltro || undefined)
      setProducciones(data)
    } catch (e) {
      console.error('Error al cargar producciones:', e)
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    if (localId) {
      cargarDatos()
    }
  }, [token, localId, fechaFiltro])

  const produccionesFiltradas = useMemo(() => {
    return producciones.filter(p => {
      if (filtroTipo === 'subrecetas') return p.esSubreceta
      if (filtroTipo === 'platos') return !p.esSubreceta
      return true
    })
  }, [producciones, filtroTipo])

  const kpis = useMemo(() => {
    const totalTandas = producciones.length
    const totalPorcionesPrep = producciones.reduce((acc, p) => acc + (p.porcionesPreparadas || 0), 0)
    const totalPorcionesVend = producciones.reduce((acc, p) => acc + (p.porcionesVendidas || 0), 0)
    const totalSubrecetas = producciones.filter(p => p.esSubreceta).length

    return {
      totalTandas,
      totalPorcionesPrep,
      totalPorcionesVend,
      totalSubrecetas,
    }
  }, [producciones])

  return (
    <div style={{ padding: '2rem', maxWidth: '1280px', margin: '0 auto', color: '#1f2937' }}>
      {/* Encabezado */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: '2rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 700, margin: 0, color: '#111827' }}>
            Producción de Cocina y Subrecetas
          </h1>
          <p style={{ margin: '0.25rem 0 0', color: '#6b7280', fontSize: '0.875rem' }}>
            Transformación de insumos en subrecetas y seguimiento de porciones para{' '}
            <strong>{localNombre}</strong> (ADM-19 / US-048).
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button
            onClick={cargarDatos}
            style={{
              padding: '0.625rem 1rem',
              backgroundColor: '#fff',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              fontSize: '0.875rem',
              fontWeight: 500,
              cursor: 'pointer',
              color: '#374151',
            }}
          >
            Actualizar
          </button>
          <button
            onClick={() => setModalAbierto(true)}
            id="btn-nueva-produccion"
            style={{
              padding: '0.625rem 1.25rem',
              backgroundColor: '#059669',
              color: '#fff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '0.875rem',
              fontWeight: 600,
              cursor: 'pointer',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
            }}
          >
            + Registrar Producción
          </button>
        </div>
      </div>

      {/* Banner Caso Crítico 11 */}
      <div
        style={{
          backgroundColor: '#eff6ff',
          border: '1px solid #bfdbfe',
          borderRadius: '10px',
          padding: '1rem 1.25rem',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          color: '#1e40af',
          fontSize: '0.875rem',
        }}
      >
        <span style={{ fontSize: '1.25rem' }}>ℹ️</span>
        <div>
          <strong>Regla de negocio (Caso Crítico 11 / CA2):</strong> Las subrecetas registradas aquí ingresan al inventario y descuentan sus insumos de una sola vez. Al vender un plato final que las contenga, el sistema descontará la subreceta directamente sin volver a descontar los insumos ya consumidos.
        </div>
      </div>

      {/* Tarjetas KPI */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1rem',
          marginBottom: '2rem',
        }}
      >
        <div style={{ backgroundColor: '#fff', padding: '1.25rem', borderRadius: '10px', border: '1px solid #e5e7eb' }}>
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#6b7280' }}>Total Tandas</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#111827', marginTop: '0.25rem' }}>
            {kpis.totalTandas}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.25rem' }}>Lotes registrados en cocina</div>
        </div>

        <div style={{ backgroundColor: '#fff', padding: '1.25rem', borderRadius: '10px', border: '1px solid #e5e7eb' }}>
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#059669' }}>🍲 Subrecetas Producidas</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#059669', marginTop: '0.25rem' }}>
            {kpis.totalSubrecetas}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.25rem' }}>Salsas, bases y preparaciones</div>
        </div>

        <div style={{ backgroundColor: '#fff', padding: '1.25rem', borderRadius: '10px', border: '1px solid #e5e7eb' }}>
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#2563eb' }}>🍽️ Porciones Preparadas</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#2563eb', marginTop: '0.25rem' }}>
            {kpis.totalPorcionesPrep}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.25rem' }}>Rendimiento total cocinado</div>
        </div>

        <div style={{ backgroundColor: '#fff', padding: '1.25rem', borderRadius: '10px', border: '1px solid #e5e7eb' }}>
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#d97706' }}>🏷️ Porciones Vendidas (CA3)</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#d97706', marginTop: '0.25rem' }}>
            {kpis.totalPorcionesVend}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.25rem' }}>Descontadas por comanda</div>
        </div>
      </div>

      {/* Barra de Filtros */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: '#fff',
          padding: '1rem',
          borderRadius: '10px',
          border: '1px solid #e5e7eb',
          marginBottom: '1.5rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            onClick={() => setFiltroTipo('todas')}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: '6px',
              fontSize: '0.8125rem',
              fontWeight: 600,
              cursor: 'pointer',
              border: 'none',
              backgroundColor: filtroTipo === 'todas' ? '#111827' : '#f3f4f6',
              color: filtroTipo === 'todas' ? '#fff' : '#4b5563',
            }}
          >
            Todas ({producciones.length})
          </button>
          <button
            onClick={() => setFiltroTipo('subrecetas')}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: '6px',
              fontSize: '0.8125rem',
              fontWeight: 600,
              cursor: 'pointer',
              border: 'none',
              backgroundColor: filtroTipo === 'subrecetas' ? '#059669' : '#f3f4f6',
              color: filtroTipo === 'subrecetas' ? '#fff' : '#4b5563',
            }}
          >
            🍲 Solo Subrecetas ({kpis.totalSubrecetas})
          </button>
          <button
            onClick={() => setFiltroTipo('platos')}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: '6px',
              fontSize: '0.8125rem',
              fontWeight: 600,
              cursor: 'pointer',
              border: 'none',
              backgroundColor: filtroTipo === 'platos' ? '#2563eb' : '#f3f4f6',
              color: filtroTipo === 'platos' ? '#fff' : '#4b5563',
            }}
          >
            🍽️ Platos de Menú ({producciones.length - kpis.totalSubrecetas})
          </button>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label style={{ fontSize: '0.8125rem', color: '#6b7280', fontWeight: 500 }}>Filtrar por fecha:</label>
          <input
            type="date"
            value={fechaFiltro}
            onChange={e => setFechaFiltro(e.target.value)}
            style={{
              padding: '0.45rem 0.75rem',
              border: '1px solid #d1d5db',
              borderRadius: '6px',
              fontSize: '0.8125rem',
            }}
          />
          {fechaFiltro && (
            <button
              onClick={() => setFechaFiltro('')}
              style={{
                background: 'none',
                border: 'none',
                color: '#6b7280',
                cursor: 'pointer',
                fontSize: '0.8125rem',
                textDecoration: 'underline',
              }}
            >
              Limpiar
            </button>
          )}
        </div>
      </div>

      {/* Tabla de Producciones */}
      <div
        style={{
          backgroundColor: '#fff',
          borderRadius: '10px',
          border: '1px solid #e5e7eb',
          overflow: 'hidden',
          boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
        }}
      >
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', color: '#4b5563' }}>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Fecha / Servicio</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Producto Producido</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Tipo</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'center' }}>
                  Porciones (Plan / Prep / Vend)
                </th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'right' }}>Rendimiento</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'right' }}>Costo Real Unit.</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Registrado Por</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={7} style={{ padding: '3rem', textAlign: 'center', color: '#6b7280' }}>
                    Cargando registros de producción...
                  </td>
                </tr>
              ) : produccionesFiltradas.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ padding: '3rem', textAlign: 'center', color: '#6b7280' }}>
                    No se encontraron registros de producción para los filtros seleccionados.
                  </td>
                </tr>
              ) : (
                produccionesFiltradas.map(p => (
                  <tr key={p.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <div style={{ fontWeight: 600, color: '#111827' }}>{p.fecha}</div>
                      <div style={{ fontSize: '0.75rem', color: '#6b7280', textTransform: 'capitalize' }}>
                        {p.servicio || 'General'}
                      </div>
                    </td>

                    <td style={{ padding: '0.75rem 1rem' }}>
                      <div style={{ fontWeight: 600, color: '#111827' }}>{p.productoNombre}</div>
                      <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>{p.productoCodigo}</div>
                    </td>

                    <td style={{ padding: '0.75rem 1rem' }}>
                      {p.esSubreceta ? (
                        <span
                          style={{
                            backgroundColor: '#d1fae5',
                            color: '#065f46',
                            padding: '0.25rem 0.5rem',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                          }}
                        >
                          🍲 Subreceta
                        </span>
                      ) : (
                        <span
                          style={{
                            backgroundColor: '#dbeafe',
                            color: '#1e40af',
                            padding: '0.25rem 0.5rem',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                          }}
                        >
                          🍽️ Plato Final
                        </span>
                      )}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', textAlign: 'center' }}>
                      <div style={{ fontWeight: 600, color: '#111827' }}>
                        {p.porcionesPlanificadas != null ? p.porcionesPlanificadas : '-'} /{' '}
                        <span style={{ color: '#059669' }}>{p.porcionesPreparadas}</span> /{' '}
                        <span style={{ color: '#d97706' }}>{p.porcionesVendidas}</span>
                      </div>
                      {p.porcionesPreparadas > 0 && (
                        <div
                          style={{
                            width: '100px',
                            height: '6px',
                            backgroundColor: '#e5e7eb',
                            borderRadius: '9999px',
                            margin: '0.25rem auto 0',
                            overflow: 'hidden',
                          }}
                        >
                          <div
                            style={{
                              width: `${Math.min(100, (p.porcionesVendidas / p.porcionesPreparadas) * 100)}%`,
                              height: '100%',
                              backgroundColor: '#d97706',
                            }}
                          />
                        </div>
                      )}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right', fontWeight: 600, color: '#111827' }}>
                      {p.rendimientoObtenido} {p.unidadMedidaSimbolo}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                      <div style={{ fontWeight: 600, color: '#059669' }}>
                        S/ {(p.costoUnitarioEstimado || 0).toFixed(2)}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                        Total: S/ {(p.costoTotalLote || 0).toFixed(2)}
                      </div>
                    </td>

                    <td style={{ padding: '0.75rem 1rem', color: '#4b5563', fontSize: '0.8125rem' }}>
                      {p.registradoPorNombre || 'Sistema'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal para Registrar Producción */}
      <ModalRegistrarProduccion
        abierto={modalAbierto}
        onCerrar={() => setModalAbierto(false)}
        onProduccionRegistrada={cargarDatos}
        token={token}
        localId={localId}
      />
    </div>
  )
}
