import { useState, useEffect, useMemo } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  listarCompras,
  confirmarCompra,
  listarExistencias,
  type Compra,
} from '../../api/cliente'
import { DialogoFormularioCompra } from './DialogoFormularioCompra'

interface Props {
  token?: string
  localId?: string
  localNombre?: string
}

export function PantallaCompras(props: Props) {
  const { sesion } = useSesion()
  const token = props.token ?? sesion?.token ?? ''
  const localId = props.localId ?? sesion?.localActivoId ?? sesion?.locales[0]?.id ?? ''
  const localNombre = props.localNombre ?? sesion?.locales.find(l => l.id === localId)?.nombre ?? 'Local Activo'
  const [compras, setCompras] = useState<Compra[]>([])
  const [cargando, setCargando] = useState(true)
  const [filtroEstado, setFiltroEstado] = useState<'todas' | 'borrador' | 'confirmada'>('todas')
  const [busqueda, setBusqueda] = useState('')
  const [modalNuevoAbierto, setModalNuevoAbierto] = useState(false)
  const [productosCatalogo, setProductosCatalogo] = useState<
    Array<{ id: string; codigo: string; nombre: string; unidadCodigo: string }>
  >([])

  const cargarDatos = async () => {
    try {
      setCargando(true)
      const data = await listarCompras(token, localId)
      setCompras(data)
    } catch (e) {
      console.error('Error al cargar compras:', e)
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargarDatos()
    // Cargar productos del catálogo del local para el modal
    listarExistencias(token, localId)
      .then(filas => {
        setProductosCatalogo(
          filas.map(f => ({
            id: f.productoId,
            codigo: f.codigo,
            nombre: f.nombre,
            unidadCodigo: f.unidadCodigo,
          })),
        )
      })
      .catch(e => console.error('Error al cargar catálogo de productos:', e))
  }, [token, localId])

  const handleConfirmar = async (compraId: string) => {
    if (!window.confirm('¿Confirmar esta compra e ingresar su stock a los almacenes del local?')) return
    try {
      await confirmarCompra(token, localId, compraId)
      await cargarDatos()
    } catch (e: any) {
      alert(e?.cuerpo?.error || e?.message || 'Error al confirmar la compra')
    }
  }

  const comprasFiltradas = useMemo(() => {
    return compras.filter(c => {
      if (filtroEstado !== 'todas' && c.estado !== filtroEstado) {
        return false
      }
      if (busqueda.trim()) {
        const q = busqueda.toLowerCase()
        const prov = (c.proveedorRazonSocial || '').toLowerCase()
        const ruc = (c.proveedorRuc || '').toLowerCase()
        const doc = `${c.serie || ''}-${c.numero || ''}`.toLowerCase()
        if (!prov.includes(q) && !ruc.includes(q) && !doc.includes(q)) {
          return false
        }
      }
      return true
    })
  }, [compras, filtroEstado, busqueda])

  const kpis = useMemo(() => {
    const total = compras.length
    const borradores = compras.filter(c => c.estado === 'borrador').length
    const confirmadas = compras.filter(c => c.estado === 'confirmada').length
    const totalInvertidoCentimos = compras
      .filter(c => c.estado === 'confirmada')
      .reduce((sum, c) => sum + (c.total || 0), 0)

    return {
      total,
      borradores,
      confirmadas,
      totalInvertidoSoles: (totalInvertidoCentimos / 100).toFixed(2),
    }
  }, [compras])

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
            Registro y Control de Compras
          </h1>
          <p style={{ margin: '0.25rem 0 0', color: '#6b7280', fontSize: '0.875rem' }}>
            Ingreso de mercadería con comprobante, aumento de inventario y actualización de costos de última compra para{' '}
            <strong>{localNombre}</strong> (ADM-18 / US-050).
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
            onClick={() => setModalNuevoAbierto(true)}
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
            + Registrar Compra
          </button>
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
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#6b7280' }}>Total Registros</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#111827', marginTop: '0.25rem' }}>{kpis.total}</div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.25rem' }}>Histórico en este local</div>
        </div>

        <div
          onClick={() => setFiltroEstado('borrador')}
          style={{
            backgroundColor: '#fff',
            padding: '1.25rem',
            borderRadius: '10px',
            border: `1px solid ${filtroEstado === 'borrador' ? '#f59e0b' : '#e5e7eb'}`,
            cursor: 'pointer',
          }}
        >
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#d97706' }}>📝 En Borrador</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#d97706', marginTop: '0.25rem' }}>{kpis.borradores}</div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.25rem' }}>Pendientes de confirmación</div>
        </div>

        <div
          onClick={() => setFiltroEstado('confirmada')}
          style={{
            backgroundColor: '#fff',
            padding: '1.25rem',
            borderRadius: '10px',
            border: `1px solid ${filtroEstado === 'confirmada' ? '#10b981' : '#e5e7eb'}`,
            cursor: 'pointer',
          }}
        >
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#059669' }}>✅ Confirmadas</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#059669', marginTop: '0.25rem' }}>{kpis.confirmadas}</div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.25rem' }}>Ingresadas al inventario</div>
        </div>

        <div style={{ backgroundColor: '#fff', padding: '1.25rem', borderRadius: '10px', border: '1px solid #e5e7eb' }}>
          <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#6b7280' }}>Total Invertido (S/)</div>
          <div style={{ fontSize: '1.875rem', fontWeight: 700, color: '#047857', marginTop: '0.25rem' }}>
            S/ {kpis.totalInvertidoSoles}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '0.25rem' }}>Compras confirmadas</div>
        </div>
      </div>

      {/* Barra de filtros y buscador */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '1rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {(['todas', 'confirmada', 'borrador'] as const).map(estado => (
            <button
              key={estado}
              onClick={() => setFiltroEstado(estado)}
              style={{
                padding: '0.5rem 1rem',
                borderRadius: '8px',
                border: '1px solid',
                borderColor: filtroEstado === estado ? '#059669' : '#d1d5db',
                backgroundColor: filtroEstado === estado ? '#ecfdf5' : '#fff',
                color: filtroEstado === estado ? '#047857' : '#374151',
                fontWeight: filtroEstado === estado ? 600 : 400,
                fontSize: '0.875rem',
                cursor: 'pointer',
                textTransform: 'capitalize',
              }}
            >
              {estado === 'todas' ? 'Todas' : estado === 'confirmada' ? 'Confirmadas' : 'Borradores'}
            </button>
          ))}
        </div>

        <input
          type="text"
          placeholder="Buscar por proveedor o comprobante..."
          value={busqueda}
          onChange={e => setBusqueda(e.target.value)}
          style={{
            padding: '0.5rem 0.75rem',
            borderRadius: '8px',
            border: '1px solid #d1d5db',
            fontSize: '0.875rem',
            minWidth: '280px',
          }}
        />
      </div>

      {/* Tabla de compras */}
      <div
        style={{
          backgroundColor: '#fff',
          borderRadius: '10px',
          border: '1px solid #e5e7eb',
          overflow: 'hidden',
          boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
        }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
          <thead>
            <tr style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', color: '#4b5563' }}>
              <th style={{ padding: '0.75rem 1rem' }}>Fecha</th>
              <th style={{ padding: '0.75rem 1rem' }}>Comprobante</th>
              <th style={{ padding: '0.75rem 1rem' }}>Proveedor</th>
              <th style={{ padding: '0.75rem 1rem' }}>Ítems</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Total (S/)</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'center' }}>Estado</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {cargando ? (
              <tr>
                <td colSpan={7} style={{ padding: '2rem', textAlign: 'center', color: '#9ca3af' }}>
                  Cargando compras...
                </td>
              </tr>
            ) : comprasFiltradas.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ padding: '2rem', textAlign: 'center', color: '#9ca3af' }}>
                  No se encontraron compras registradas con los filtros actuales.
                </td>
              </tr>
            ) : (
              comprasFiltradas.map(c => {
                const totalSoles = ((c.total || 0) / 100).toFixed(2)
                const docLabel =
                  c.tipoDocumento === 'sin_documento'
                    ? 'Sin comprobante'
                    : `${c.tipoDocumento.toUpperCase()} ${c.serie || ''}-${c.numero || ''}`

                return (
                  <tr key={c.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '0.75rem 1rem', whiteSpace: 'nowrap' }}>{c.fechaDocumento}</td>
                    <td style={{ padding: '0.75rem 1rem', fontWeight: 500, color: '#111827' }}>{docLabel}</td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <div style={{ fontWeight: 500, color: '#111827' }}>{c.proveedorRazonSocial}</div>
                      {c.proveedorRuc && <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>RUC: {c.proveedorRuc}</div>}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', color: '#4b5563' }}>
                      {c.detalles?.length || 0} producto(s)
                    </td>
                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right', fontWeight: 600, color: '#111827' }}>
                      S/ {totalSoles}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', textAlign: 'center' }}>
                      <span
                        style={{
                          display: 'inline-block',
                          padding: '0.25rem 0.6rem',
                          borderRadius: '9999px',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          backgroundColor: c.estado === 'confirmada' ? '#def7ec' : '#fef3c7',
                          color: c.estado === 'confirmada' ? '#03543f' : '#92400e',
                        }}
                      >
                        {c.estado === 'confirmada' ? 'Confirmada' : 'Borrador'}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem', alignItems: 'center' }}>
                        {c.estado === 'borrador' && (
                          <button
                            onClick={() => handleConfirmar(c.id)}
                            style={{
                              padding: '0.35rem 0.75rem',
                              backgroundColor: '#059669',
                              color: '#fff',
                              border: 'none',
                              borderRadius: '6px',
                              fontSize: '0.75rem',
                              fontWeight: 600,
                              cursor: 'pointer',
                            }}
                          >
                            Confirmar
                          </button>
                        )}
                        {c.rutaAdjunto && (
                          <a
                            href={`/api/locales/${localId}/compras/${c.id}/adjunto`}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{
                              padding: '0.35rem 0.5rem',
                              backgroundColor: '#f3f4f6',
                              color: '#374151',
                              border: '1px solid #d1d5db',
                              borderRadius: '6px',
                              fontSize: '0.75rem',
                              fontWeight: 500,
                              textDecoration: 'none',
                            }}
                          >
                            📎 Adjunto
                          </a>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Modal de formulario de registro */}
      <DialogoFormularioCompra
        abierto={modalNuevoAbierto}
        token={token}
        localId={localId}
        productosCatalogo={productosCatalogo}
        alCerrar={() => setModalNuevoAbierto(false)}
        alGuardarExitoso={() => {
          setModalNuevoAbierto(false)
          cargarDatos()
        }}
      />
    </div>
  )
}
