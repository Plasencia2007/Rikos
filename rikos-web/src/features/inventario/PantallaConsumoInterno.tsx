import { useEffect, useMemo, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoMas,
} from '../../comun/componentes'
import {
  listarConsumosInternos,
  obtenerResumenConsumoInterno,
  listarExistencias,
  type FilaConsumoInterno,
  type ResumenConsumoInterno,
  type FilaExistencia,
} from '../../api/cliente'
import { ModalRegistrarConsumoInterno } from './ModalRegistrarConsumoInterno'

export function PantallaConsumoInterno() {
  const { sesion } = useSesion()
  const token = sesion?.token ?? ''
  const localId = sesion?.localActivoId ?? sesion?.locales[0]?.id ?? ''

  const [consumos, setConsumos] = useState<FilaConsumoInterno[]>([])
  const [resumen, setResumen] = useState<ResumenConsumoInterno | null>(null)
  const [existencias, setExistencias] = useState<FilaExistencia[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  // Filtros
  const [fechaFiltro, setFechaFiltro] = useState(new Date().toISOString().slice(0, 10))
  const [busqueda, setBusqueda] = useState('')
  const [modalAbierto, setModalAbierto] = useState(false)

  const nombreLocal = useMemo(() => {
    return sesion?.locales.find((l) => l.id === localId)?.nombre ?? 'Local Activo'
  }, [sesion, localId])

  async function cargarDatos() {
    if (!token || !localId) return
    try {
      setCargando(true)
      setError('')

      const [lista, res, ex] = await Promise.all([
        listarConsumosInternos(token, localId, fechaFiltro || undefined),
        obtenerResumenConsumoInterno(token, localId, fechaFiltro || undefined),
        listarExistencias(token, localId),
      ])

      setConsumos(lista)
      setResumen(res)
      setExistencias(ex)
    } catch {
      setError('No se pudo cargar el registro de consumos internos.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargarDatos()
  }, [token, localId, fechaFiltro])

  const consumosFiltrados = useMemo(() => {
    if (!busqueda.trim()) return consumos
    const q = busqueda.toLowerCase().trim()
    return consumos.filter(
      (c) =>
        c.usuarioNombre?.toLowerCase().includes(q) ||
        c.productoNombre?.toLowerCase().includes(q) ||
        c.productoCodigo?.toLowerCase().includes(q) ||
        c.servicio?.toLowerCase().includes(q)
    )
  }, [consumos, busqueda])

  return (
    <div style={{ padding: '1.5rem', maxWidth: '1400px', margin: '0 auto' }}>
      {/* Cabecera Principal */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: '1.5rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>
              Consumo Interno del Personal (US-051)
            </h1>
            <span
              style={{
                backgroundColor: '#f1f5f9',
                color: '#475569',
                padding: '0.2rem 0.5rem',
                borderRadius: '0.375rem',
                fontSize: '0.75rem',
                fontWeight: 600,
              }}
            >
              {nombreLocal}
            </span>
          </div>
          <p style={{ fontSize: '0.875rem', color: '#64748b', margin: '0.25rem 0 0 0' }}>
            Control de alimentación y comidas asignadas al equipo de trabajo por turno sin cobro de venta.
          </p>
        </div>

        <Boton variante="primario" onClick={() => setModalAbierto(true)}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
            <IconoMas /> Registrar Comida
          </span>
        </Boton>
      </div>

      {/* Tarjetas KPI */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '1.25rem',
            borderRadius: '0.75rem',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
          }}
        >
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#2563eb' }}>🍽️ Comidas del Día / Periodo</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#1e293b', marginTop: '0.25rem' }}>
            {resumen ? resumen.totalComidas : 0}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Raciones registradas y descontadas del almacén
          </div>
        </div>

        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '1.25rem',
            borderRadius: '0.75rem',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
          }}
        >
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#059669' }}>💰 Costo Contable Total</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#059669', marginTop: '0.25rem' }}>
            S/ {resumen ? Number(resumen.costoTotal || 0).toFixed(2) : '0.00'}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Gasto operativo de personal (sin cobro)
          </div>
        </div>

        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '1.25rem',
            borderRadius: '0.75rem',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
          }}
        >
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#d97706' }}>🛡️ Raciones Autorizadas / 2das</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#d97706', marginTop: '0.25rem' }}>
            {resumen ? resumen.totalConAutorizacion : 0}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Aprobadas bajo PIN o autorización de supervisor
          </div>
        </div>
      </div>

      {/* Barra de Filtros */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: '#ffffff',
          padding: '0.875rem 1.25rem',
          borderRadius: '0.5rem',
          border: '1px solid #e2e8f0',
          marginBottom: '1.5rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flex: 1, minWidth: '280px' }}>
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por personal, plato, servicio..."
            style={{
              width: '100%',
              maxWidth: '380px',
              padding: '0.5rem 0.75rem',
              borderRadius: '0.375rem',
              border: '1px solid #cbd5e1',
              fontSize: '0.875rem',
            }}
          />
          {busqueda && (
            <button
              onClick={() => setBusqueda('')}
              style={{
                background: 'none',
                border: 'none',
                color: '#64748b',
                cursor: 'pointer',
                fontSize: '0.8125rem',
                textDecoration: 'underline',
              }}
            >
              Limpiar
            </button>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label style={{ fontSize: '0.8125rem', color: '#64748b', fontWeight: 500 }}>Filtrar por fecha:</label>
          <input
            type="date"
            value={fechaFiltro}
            onChange={(e) => setFechaFiltro(e.target.value)}
            style={{
              padding: '0.45rem 0.65rem',
              borderRadius: '0.375rem',
              border: '1px solid #cbd5e1',
              fontSize: '0.8125rem',
            }}
          />
          {fechaFiltro && (
            <button
              onClick={() => setFechaFiltro('')}
              style={{
                background: 'none',
                border: 'none',
                color: '#2563eb',
                cursor: 'pointer',
                fontSize: '0.8125rem',
                textDecoration: 'underline',
              }}
            >
              Ver todas
            </button>
          )}
        </div>
      </div>

      {/* Contenido / Tabla */}
      {cargando ? (
        <EstadoCargando mensaje="Cargando historial de consumo interno..." />
      ) : error ? (
        <EstadoError
          mensaje={error}
          accion={
            <Boton variante="secundario" onClick={cargarDatos}>
              Reintentar
            </Boton>
          }
        />
      ) : consumosFiltrados.length === 0 ? (
        <EstadoVacio
          titulo="Sin consumos internos registrados"
          descripcion="No se encontraron registros de comidas para el filtro seleccionado. Utilice el botón superior para registrar consumos de los trabajadores."
        />
      ) : (
        <div
          style={{
            backgroundColor: '#ffffff',
            borderRadius: '0.5rem',
            border: '1px solid #e2e8f0',
            overflow: 'hidden',
            boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
          }}
        >
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569' }}>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Fecha / Servicio</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Personal Beneficiario</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Plato / Alimento</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'center' }}>Cantidad</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'right' }}>Costo Contable</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'center' }}>Autorización</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Observaciones</th>
                </tr>
              </thead>
              <tbody>
                {consumosFiltrados.map((c) => (
                  <tr key={c.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <div style={{ fontWeight: 600, color: '#0f172a' }}>{c.fecha}</div>
                      <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'capitalize' }}>
                        {c.servicio} {c.turno ? `• Turno ${c.turno}` : ''}
                      </div>
                    </td>

                    <td style={{ padding: '0.75rem 1rem' }}>
                      <div style={{ fontWeight: 600, color: '#0f172a' }}>{c.usuarioNombre}</div>
                    </td>

                    <td style={{ padding: '0.75rem 1rem' }}>
                      <div style={{ fontWeight: 600, color: '#0f172a' }}>{c.productoNombre}</div>
                      <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>{c.productoCodigo}</div>
                    </td>

                    <td style={{ padding: '0.75rem 1rem', textAlign: 'center', fontWeight: 600, color: '#0f172a' }}>
                      {c.cantidad} {c.unidadMedidaSimbolo || 'porc'}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                      <div style={{ fontWeight: 600, color: '#059669' }}>
                        S/ {Number(c.costoTotal || 0).toFixed(2)}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                        Unit: S/ {Number(c.costoUnitario || 0).toFixed(2)}
                      </div>
                    </td>

                    <td style={{ padding: '0.75rem 1rem', textAlign: 'center' }}>
                      {c.requirioAutorizacion ? (
                        <span
                          style={{
                            backgroundColor: '#fef3c7',
                            color: '#92400e',
                            padding: '0.2rem 0.5rem',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            display: 'inline-block',
                          }}
                        >
                          🛡️ {c.autorizadoPorNombre ? `Aprobado: ${c.autorizadoPorNombre}` : 'Supervisor'}
                        </span>
                      ) : (
                        <span
                          style={{
                            backgroundColor: '#dcfce7',
                            color: '#166534',
                            padding: '0.2rem 0.5rem',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            display: 'inline-block',
                          }}
                        >
                          ✓ 1ra ración libre
                        </span>
                      )}
                    </td>

                    <td style={{ padding: '0.75rem 1rem', color: '#64748b', fontSize: '0.8125rem' }}>
                      {c.observacion || '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal de Registro */}
      <ModalRegistrarConsumoInterno
        abierto={modalAbierto}
        token={token}
        localId={localId}
        existencias={existencias}
        onCerrar={() => setModalAbierto(false)}
        onGuardado={() => {
          cargarDatos()
        }}
      />
    </div>
  )
}
