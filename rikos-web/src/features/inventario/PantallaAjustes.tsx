import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoMas,
} from '../../comun/componentes'
import {
  listarAjustesInventario,
  listarExistencias,
  type DetalleAjuste,
  type FilaExistencia,
} from '../../api/cliente'
import { ModalRegistrarAjuste } from './ModalRegistrarAjuste'

export function PantallaAjustes() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const productoUrlParam = searchParams.get('productoId') ?? ''

  const { sesion } = useSesion()
  const token = sesion?.token ?? ''
  const localId = sesion?.localActivoId ?? sesion?.locales[0]?.id ?? ''

  const [ajustes, setAjustes] = useState<DetalleAjuste[]>([])
  const [existencias, setExistencias] = useState<FilaExistencia[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  // Filtros
  const [productoFiltro, setProductoFiltro] = useState(productoUrlParam)
  const [busquedaTexto, setBusquedaTexto] = useState('')
  const [fechaDesde, setFechaDesde] = useState('')
  const [fechaHasta, setFechaHasta] = useState('')

  // Modal de registro
  const [modalAbierto, setModalAbierto] = useState(false)
  const [productoSeleccionadoModal, setProductoSeleccionadoModal] = useState(productoUrlParam)

  const nombreLocal = useMemo(() => {
    return sesion?.locales.find((l) => l.id === localId)?.nombre ?? 'Local'
  }, [sesion, localId])

  async function cargarDatos() {
    if (!token || !localId) return
    try {
      setCargando(true)
      setError('')

      const [listaAjustes, listaExistencias] = await Promise.all([
        listarAjustesInventario(token, localId, {
          productoId: productoFiltro || undefined,
          desde: fechaDesde || undefined,
          hasta: fechaHasta || undefined,
        }),
        listarExistencias(token, localId),
      ])

      setAjustes(listaAjustes)
      setExistencias(listaExistencias)
    } catch {
      setError('No se pudo cargar el historial de ajustes de inventario.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargarDatos()
  }, [token, localId, productoFiltro, fechaDesde, fechaHasta])

  // Filtrado en memoria por texto de búsqueda (código, nombre, motivo, usuario u observación)
  const ajustesFiltrados = useMemo(() => {
    const q = busquedaTexto.toLowerCase().trim()
    if (!q) return ajustes
    return ajustes.filter(
      (a) =>
        a.productoCodigo?.toLowerCase().includes(q) ||
        a.productoNombre?.toLowerCase().includes(q) ||
        a.motivoDescripcion?.toLowerCase().includes(q) ||
        a.registradoPorNombre?.toLowerCase().includes(q) ||
        a.observacion?.toLowerCase().includes(q),
    )
  }, [ajustes, busquedaTexto])

  // Métricas y KPIs
  const kpis = useMemo(() => {
    const total = ajustesFiltrados.length
    let entradas = 0
    let salidas = 0
    let totalEntradasCant = 0
    let totalSalidasCant = 0

    for (const a of ajustesFiltrados) {
      if (a.signo === 'entrada') {
        entradas++
        totalEntradasCant += a.cantidad
      } else {
        salidas++
        totalSalidasCant += a.cantidad
      }
    }

    return {
      total,
      entradas,
      salidas,
      totalEntradasCant,
      totalSalidasCant,
      diferenciaNeta: totalEntradasCant - totalSalidasCant,
    }
  }, [ajustesFiltrados])

  function formatearFecha(isoString: string) {
    if (!isoString) return '-'
    const d = new Date(isoString)
    return d.toLocaleString('es-PE', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  }


  return (
    <div style={{ padding: '1.5rem', maxWidth: '1400px', margin: '0 auto' }}>
      {/* Encabezado */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: '1.75rem', fontWeight: 700, color: 'var(--color-texto, #0f172a)' }}>
            Ajustes de Inventario
          </h1>
          <p style={{ margin: '0.25rem 0 0 0', color: 'var(--color-texto-secundario, #64748b)', fontSize: '0.95rem' }}>
            Registro de mermas, roturas, sobrantes y correcciones físicas con motivo obligatorio para <strong>{nombreLocal}</strong> (US-045).
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Boton
            variante="secundario"
            onClick={() => navigate('/admin/inventario/existencias')}
          >
            Ver Existencias
          </Boton>
          <Boton
            variante="secundario"
            onClick={() => navigate('/admin/inventario/kardex')}
          >
            Ver Kardex
          </Boton>
          <Boton
            variante="secundario"
            onClick={cargarDatos}
            disabled={cargando}
          >
            Actualizar
          </Boton>
          <Boton
            variante="primario"
            onClick={() => {
              setProductoSeleccionadoModal(productoFiltro || '')
              setModalAbierto(true)
            }}
          >
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
              <IconoMas /> Nuevo Ajuste
            </span>
          </Boton>
        </div>
      </div>

      {/* KPI Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        {/* KPI: Total Registros */}
        <div
          style={{
            backgroundColor: 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            padding: '1.15rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            border: '1px solid var(--color-borde, #e2e8f0)',
          }}
        >
          <div style={{ fontSize: '0.85rem', color: '#64748b', fontWeight: 500 }}>
            Total de Ajustes
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#0f172a', marginTop: '0.25rem' }}>
            {kpis.total}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Movimientos registrados
          </div>
        </div>

        {/* KPI: Entradas / Sobrantes */}
        <div
          style={{
            backgroundColor: 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            padding: '1.15rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            border: '1px solid #bbf7d0',
          }}
        >
          <div style={{ fontSize: '0.85rem', color: '#166534', fontWeight: 600 }}>
            + Entradas / Sobrantes
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#15803d', marginTop: '0.25rem' }}>
            {kpis.entradas}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#166534', marginTop: '0.25rem' }}>
            {kpis.totalEntradasCant.toFixed(2)} uds. agregadas
          </div>
        </div>

        {/* KPI: Salidas / Mermas */}
        <div
          style={{
            backgroundColor: 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            padding: '1.15rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            border: '1px solid #fecaca',
          }}
        >
          <div style={{ fontSize: '0.85rem', color: '#991b1b', fontWeight: 600 }}>
            - Salidas / Mermas
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#b91c1c', marginTop: '0.25rem' }}>
            {kpis.salidas}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#991b1b', marginTop: '0.25rem' }}>
            {kpis.totalSalidasCant.toFixed(2)} uds. descontadas
          </div>
        </div>

        {/* KPI: Variación Neta */}
        <div
          style={{
            backgroundColor: 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            padding: '1.15rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            border: '1px solid var(--color-borde, #e2e8f0)',
          }}
        >
          <div style={{ fontSize: '0.85rem', color: '#475569', fontWeight: 500 }}>
            Impacto Neto
          </div>
          <div
            style={{
              fontSize: '1.75rem',
              fontWeight: 700,
              color: kpis.diferenciaNeta < 0 ? '#b91c1c' : kpis.diferenciaNeta > 0 ? '#15803d' : '#64748b',
              marginTop: '0.25rem',
            }}
          >
            {kpis.diferenciaNeta > 0 ? `+${kpis.diferenciaNeta.toFixed(2)}` : kpis.diferenciaNeta.toFixed(2)}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Balance neto de unidades
          </div>
        </div>
      </div>

      {/* Barra de Filtros */}
      <div
        style={{
          backgroundColor: 'var(--color-superficie, #ffffff)',
          borderRadius: '0.75rem',
          padding: '1rem 1.25rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
          border: '1px solid var(--color-borde, #e2e8f0)',
          marginBottom: '1.5rem',
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '1rem',
          alignItems: 'flex-end',
        }}
      >
        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
            Filtrar por Producto
          </label>
          <select
            value={productoFiltro}
            onChange={(e) => setProductoFiltro(e.target.value)}
            style={{
              width: '100%',
              padding: '0.55rem 0.75rem',
              borderRadius: '0.375rem',
              border: '1px solid #cbd5e1',
              fontSize: '0.9rem',
              backgroundColor: '#ffffff',
            }}
          >
            <option value="">-- Todos los productos --</option>
            {existencias.map((e) => (
              <option key={e.productoId} value={e.productoId}>
                [{e.codigo}] {e.nombre}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '0.35rem' }}>
            Búsqueda de texto
          </label>
          <input
            type="text"
            placeholder="Buscar por motivo, usuario, obs..."
            value={busquedaTexto}
            onChange={(e) => setBusquedaTexto(e.target.value)}
            style={{
              width: '100%',
              padding: '0.55rem 0.75rem',
              borderRadius: '0.375rem',
              border: '1px solid #cbd5e1',
              fontSize: '0.9rem',
            }}
          />
        </div>

        <div>
          <Campo
            etiqueta="Desde"
            type="date"
            value={fechaDesde}
            onChange={(e) => setFechaDesde(e.target.value)}
          />
        </div>

        <div>
          <Campo
            etiqueta="Hasta"
            type="date"
            value={fechaHasta}
            onChange={(e) => setFechaHasta(e.target.value)}
          />
        </div>

        {(productoFiltro || busquedaTexto || fechaDesde || fechaHasta) && (
          <div>
            <Boton
              variante="secundario"
              onClick={() => {
                setProductoFiltro('')
                setBusquedaTexto('')
                setFechaDesde('')
                setFechaHasta('')
              }}
              style={{ width: '100%', height: '40px' }}
            >
              Limpiar filtros
            </Boton>
          </div>
        )}
      </div>

      {/* Contenido Principal: Tabla o Estados */}
      {cargando ? (
        <EstadoCargando mensaje="Cargando historial de ajustes de inventario..." />
      ) : error ? (
        <EstadoError
          mensaje={error}
          accion={
            <Boton variante="secundario" onClick={cargarDatos}>
              Reintentar
            </Boton>
          }
        />
      ) : ajustesFiltrados.length === 0 ? (
        <EstadoVacio
          titulo="No se encontraron ajustes de inventario"
          descripcion={
            busquedaTexto || productoFiltro || fechaDesde || fechaHasta
              ? 'No hay registros con los filtros aplicados. Intenta restablecer los filtros de búsqueda.'
              : 'Aún no se han registrado ajustes de inventario para este local. Usa el botón "Nuevo Ajuste" para reportar una merma o sobrante.'
          }
          accion={
            <Boton
              variante="primario"
              onClick={() => {
                setProductoSeleccionadoModal(productoFiltro || '')
                setModalAbierto(true)
              }}
            >
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                <IconoMas /> Registrar Primer Ajuste
              </span>
            </Boton>
          }
        />
      ) : (
        <div
          style={{
            backgroundColor: 'var(--color-superficie, #ffffff)',
            borderRadius: '0.75rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            border: '1px solid var(--color-borde, #e2e8f0)',
            overflow: 'hidden',
          }}
        >
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569' }}>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Fecha / Hora</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Producto</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'center' }}>Tipo</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'right' }}>Cantidad</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'center' }}>Stock Ant. → Nuevo</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Motivo</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Observación</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Registrado Por</th>
                </tr>
              </thead>
              <tbody>
                {ajustesFiltrados.map((a) => {
                  const esEntrada = a.signo === 'entrada'
                  const stockNegativo = a.stockNuevo < 0

                  return (
                    <tr
                      key={a.id}
                      style={{
                        borderBottom: '1px solid #f1f5f9',
                        transition: 'background-color 0.15s ease',
                      }}
                    >
                      {/* Fecha / Hora */}
                      <td style={{ padding: '0.85rem 1rem', whiteSpace: 'nowrap', color: '#64748b', fontSize: '0.85rem' }}>
                        {formatearFecha(a.ocurridoEn)}
                      </td>

                      {/* Producto */}
                      <td style={{ padding: '0.85rem 1rem' }}>
                        <div style={{ fontWeight: 600, color: '#0f172a' }}>{a.productoNombre}</div>
                        <div style={{ fontSize: '0.785rem', color: '#64748b', fontFamily: 'monospace' }}>
                          {a.productoCodigo}
                        </div>
                      </td>

                      {/* Tipo / Signo Badge */}
                      <td style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            padding: '0.25rem 0.6rem',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: esEntrada ? '#dcfce7' : '#fee2e2',
                            color: esEntrada ? '#15803d' : '#991b1b',
                            border: `1px solid ${esEntrada ? '#bbf7d0' : '#fecaca'}`,
                          }}
                        >
                          {esEntrada ? '+ Entrada' : '- Salida / Merma'}
                        </span>
                      </td>

                      {/* Cantidad */}
                      <td
                        style={{
                          padding: '0.85rem 1rem',
                          textAlign: 'right',
                          fontWeight: 700,
                          color: esEntrada ? '#15803d' : '#b91c1c',
                        }}
                      >
                        {esEntrada ? `+${a.cantidad}` : `-${a.cantidad}`}{' '}
                        <span style={{ fontSize: '0.75rem', fontWeight: 400, color: '#64748b' }}>
                          {a.unidadMedida}
                        </span>
                      </td>

                      {/* Stock Ant -> Nuevo */}
                      <td style={{ padding: '0.85rem 1rem', textAlign: 'center', whiteSpace: 'nowrap' }}>
                        <span style={{ color: '#64748b' }}>{a.stockAnterior}</span>
                        <span style={{ margin: '0 0.35rem', color: '#94a3b8' }}>→</span>
                        <span
                          style={{
                            fontWeight: 700,
                            color: stockNegativo ? '#dc2626' : '#0f172a',
                          }}
                        >
                          {a.stockNuevo} {a.unidadMedida}
                        </span>
                        {stockNegativo && (
                          <span
                            title="Stock negativo según RN-057"
                            style={{ marginLeft: '0.35rem', fontSize: '0.8rem', cursor: 'help' }}
                          >
                            ⚠️
                          </span>
                        )}
                      </td>

                      {/* Motivo */}
                      <td style={{ padding: '0.85rem 1rem', color: '#1e293b', fontWeight: 500 }}>
                        {a.motivoDescripcion || 'Ajuste regular'}
                      </td>

                      {/* Observación */}
                      <td style={{ padding: '0.85rem 1rem', color: '#64748b', fontSize: '0.85rem', maxWidth: '240px' }}>
                        {a.observacion || <span style={{ color: '#cbd5e1', fontStyle: 'italic' }}>Sin obs.</span>}
                      </td>

                      {/* Registrado Por */}
                      <td style={{ padding: '0.85rem 1rem', color: '#475569', fontSize: '0.85rem', whiteSpace: 'nowrap' }}>
                        {a.registradoPorNombre || 'Sistema'}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal Registrar Ajuste */}
      <ModalRegistrarAjuste
        abierto={modalAbierto}
        token={token}
        localId={localId}
        existencias={existencias}
        productoInicialId={productoSeleccionadoModal}
        onGuardado={() => {
          cargarDatos()
        }}
        onCerrar={() => setModalAbierto(false)}
      />
    </div>
  )
}
