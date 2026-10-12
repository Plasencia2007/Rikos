import { useEffect, useMemo, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoBusqueda,
  Insignia,
  ModalDeConfirmacion,
} from '../../comun/componentes'
import {
  aprobarAbonoDigital,
  listarAbonos,
  rechazarAbonoDigital,
  type FilaAbono,
} from '../../api/cliente'
import { enSoles } from './dinero'
import { ModalComprobanteAbono } from './ModalComprobanteAbono'
import { DialogoRechazarAbono } from './DialogoRechazarAbono'

type EstadoFiltro = 'pendientes' | 'aprobados' | 'rechazados' | 'todos'

const PESTANAS: { id: EstadoFiltro; titulo: string }[] = [
  { id: 'pendientes', titulo: 'Pendientes' },
  { id: 'aprobados', titulo: 'Aprobados' },
  { id: 'rechazados', titulo: 'Rechazados' },
  { id: 'todos', titulo: 'Todos los registros' },
]

function tonoEstado(estado: string): 'activo' | 'inactivo' | 'pendiente' {
  switch (estado) {
    case 'aprobado':
      return 'activo'
    case 'rechazado':
      return 'inactivo'
    default:
      return 'pendiente'
  }
}

export function PantallaAbonosPendientes() {
  const { sesion } = useSesion()
  const token = sesion!.token

  const [filas, setFilas] = useState<FilaAbono[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [pestana, setPestana] = useState<EstadoFiltro>('pendientes')
  const [avisoExito, setAvisoExito] = useState<string | null>(null)

  // Modales
  const [abonoParaVer, setAbonoParaVer] = useState<FilaAbono | null>(null)
  const [abonoParaAprobar, setAbonoParaAprobar] = useState<FilaAbono | null>(null)
  const [abonoParaRechazar, setAbonoParaRechazar] = useState<FilaAbono | null>(null)
  const [procesandoAccion, setProcesandoAccion] = useState(false)

  function cargar() {
    setCargando(true)
    setError('')
    listarAbonos(token)
      .then(setFilas)
      .catch((err) => {
        if (err?.estado === 403) {
          setError('No tienes permiso para acceder al módulo de abonos (se requiere pensionistas.ver).')
        } else {
          setError('No se pudieron cargar los abonos de pensionistas.')
        }
      })
      .finally(() => setCargando(false))
  }

  useEffect(() => {
    cargar()
  }, [token])

  // Métricas
  const metricas = useMemo(() => {
    const pendientes = filas.filter((f) => f.estado === 'pendiente')
    const aprobados = filas.filter((f) => f.estado === 'aprobado')
    const rechazados = filas.filter((f) => f.estado === 'rechazado')
    const montoPendiente = pendientes.reduce((acc, f) => acc + f.monto, 0)
    return {
      pendientesCount: pendientes.length,
      montoPendiente,
      aprobadosCount: aprobados.length,
      rechazadosCount: rechazados.length,
    }
  }, [filas])

  // Filtrado de filas
  const filasFiltradas = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return filas.filter((f) => {
      // Filtro de pestaña
      if (pestana === 'pendientes' && f.estado !== 'pendiente') return false
      if (pestana === 'aprobados' && f.estado !== 'aprobado') return false
      if (pestana === 'rechazados' && f.estado !== 'rechazado') return false

      // Filtro de búsqueda
      if (!q) return true
      const coincideNombre = f.pensionistaNombre.toLowerCase().includes(q)
      const coincideDni = f.pensionistaDni.includes(q)
      const coincideOp = f.numeroOperacion ? f.numeroOperacion.toLowerCase().includes(q) : false
      const coincideBanco = f.entidadBancaria ? f.entidadBancaria.toLowerCase().includes(q) : false
      const coincideMedio = f.medio.toLowerCase().includes(q)
      return coincideNombre || coincideDni || coincideOp || coincideBanco || coincideMedio
    })
  }, [filas, pestana, busqueda])

  // Acción de Aprobación (CA2)
  async function confirmarAprobacion() {
    if (!abonoParaAprobar) return
    setProcesandoAccion(true)
    try {
      const res = await aprobarAbonoDigital(token, abonoParaAprobar.id)
      setAvisoExito(
        `Abono de ${enSoles(abonoParaAprobar.monto)} para ${res.pensionistaNombre} aprobado con éxito. Saldo actual: ${enSoles(
          res.nuevoSaldo,
        )}`,
      )
      setAbonoParaAprobar(null)
      cargar()
    } catch (err: any) {
      if (err?.estado === 403) {
        setError('No cuentas con el permiso "pensionistas.administrar" para aprobar abonos.')
      } else {
        setError(err?.mensaje || 'Error al aprobar el abono digital.')
      }
    } finally {
      setProcesandoAccion(false)
    }
  }

  // Acción de Rechazo (CA3)
  async function confirmarRechazo(motivoId: string, observacion: string) {
    if (!abonoParaRechazar) return
    setProcesandoAccion(true)
    try {
      await rechazarAbonoDigital(token, abonoParaRechazar.id, { motivoId, observacion })
      setAvisoExito(
        `Abono de ${enSoles(abonoParaRechazar.monto)} para ${abonoParaRechazar.pensionistaNombre} ha sido rechazado formalmente.`,
      )
      setAbonoParaRechazar(null)
      cargar()
    } catch (err: any) {
      if (err?.estado === 403) {
        setError('No cuentas con el permiso "pensionistas.administrar" para rechazar abonos.')
      } else {
        setError(err?.mensaje || 'Error al rechazar el abono digital.')
      }
    } finally {
      setProcesandoAccion(false)
    }
  }

  return (
    <section className="panel-admin" aria-labelledby="titulo-abonos">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Pensionistas</p>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <h1 id="titulo-abonos" style={{ margin: 0 }}>
              Abonos de pensionistas
            </h1>
            {metricas.pendientesCount > 0 && (
              <Insignia tono="pendiente">
                {metricas.pendientesCount} pendientes de revisión
              </Insignia>
            )}
          </div>
          <p className="panel-admin-subtitulo">
            Cotejo, aprobación y rechazo centralizado de abonos digitales (Yape, Plin, Transferencias bancarias).
          </p>
        </div>
      </div>

      {avisoExito && (
        <div style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
          <Aviso tono="exito">{avisoExito}</Aviso>
          <button
            type="button"
            onClick={() => setAvisoExito(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#6B7280', fontSize: '0.85rem' }}
          >
            ✕
          </button>
        </div>
      )}

      {cargando ? (
        <EstadoCargando mensaje="Cargando abonos de pensionistas..." />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : (
        <>
          {/* Tarjetas KPI de Resumen */}
          <div className="cta-resumen">
            <div className="panel-admin-tarjeta cta-cifra">
              <span className="cta-etiqueta">Pendientes de revisión</span>
              <strong style={{ color: metricas.pendientesCount > 0 ? '#D97706' : '#374151' }}>
                {metricas.pendientesCount}
              </strong>
              <span className="cta-nota">Total: {enSoles(metricas.montoPendiente)}</span>
            </div>
            <div className="panel-admin-tarjeta cta-cifra">
              <span className="cta-etiqueta">Aprobados</span>
              <strong style={{ color: '#059669' }}>{metricas.aprobadosCount}</strong>
              <span className="cta-nota">Acreditados en cuentas corrientes</span>
            </div>
            <div className="panel-admin-tarjeta cta-cifra">
              <span className="cta-etiqueta">Rechazados</span>
              <strong style={{ color: '#DC2626' }}>{metricas.rechazadosCount}</strong>
              <span className="cta-nota">Sin efecto en cuentas corrientes</span>
            </div>
          </div>

          <div className="prod-tabla-tarjeta">
            {/* Barra superior con buscador y pestañas */}
            <div className="prod-barra-filtros">
              <label className="prod-buscador">
                <IconoBusqueda width={15} height={15} aria-hidden="true" />
                <input
                  type="text"
                  aria-label="Buscar abono"
                  placeholder="Buscar por DNI, nombre, N° de operación o banco..."
                  value={busqueda}
                  onChange={(e) => setBusqueda(e.target.value)}
                />
              </label>

              <div className="prod-espaciador" />

              <div className="prod-pills" role="tablist">
                {PESTANAS.map((p) => {
                  const activa = pestana === p.id
                  return (
                    <button
                      key={p.id}
                      type="button"
                      role="tab"
                      aria-selected={activa}
                      className={`prod-pill ${activa ? 'prod-pill-activa' : ''}`}
                      onClick={() => setPestana(p.id)}
                    >
                      {p.titulo}
                      {p.id === 'pendientes' && metricas.pendientesCount > 0 && (
                        <span
                          style={{
                            marginLeft: '6px',
                            background: activa ? '#B45309' : '#F59E0B',
                            color: '#FFFFFF',
                            borderRadius: '10px',
                            padding: '1px 6px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                          }}
                        >
                          {metricas.pendientesCount}
                        </span>
                      )}
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Listado / Tabla */}
            {filasFiltradas.length === 0 ? (
              <div style={{ padding: '32px 16px' }}>
                <EstadoVacio
                  titulo={
                    pestana === 'pendientes'
                      ? 'No hay abonos pendientes de revisión'
                      : 'No se encontraron abonos'
                  }
                  descripcion={
                    busqueda
                      ? 'No hay resultados que coincidan con la búsqueda actual.'
                      : pestana === 'pendientes'
                      ? '¡Excelente! Todos los abonos digitales registrados han sido atendidos.'
                      : 'No se registran operaciones en esta categoría.'
                  }
                />
              </div>
            ) : (
              <table className="tabla-admin" style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid #E5E7EB', color: '#6B7280', fontSize: '0.82rem' }}>
                    <th style={{ padding: '12px 16px' }}>Fecha y Hora</th>
                    <th style={{ padding: '12px 16px' }}>Pensionista</th>
                    <th style={{ padding: '12px 16px' }}>Local</th>
                    <th style={{ padding: '12px 16px' }}>Medio / Banco</th>
                    <th style={{ padding: '12px 16px' }}>N° Operación</th>
                    <th style={{ padding: '12px 16px', textAlign: 'right' }}>Monto</th>
                    <th style={{ padding: '12px 16px', textAlign: 'center' }}>Estado</th>
                    <th style={{ padding: '12px 16px', textAlign: 'right' }}>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filasFiltradas.map((f) => {
                    const esPendiente = f.estado === 'pendiente'
                    return (
                      <tr
                        key={f.id}
                        style={{
                          borderBottom: '1px solid #F3F4F6',
                          backgroundColor: esPendiente ? '#FFFDF8' : 'transparent',
                        }}
                      >
                        <td style={{ padding: '14px 16px', fontSize: '0.85rem', color: '#4B5563', whiteSpace: 'nowrap' }}>
                          {new Date(f.creadoEn).toLocaleString('es-PE', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </td>
                        <td style={{ padding: '14px 16px' }}>
                          <div style={{ fontWeight: 600, color: '#111827', fontSize: '0.9rem' }}>
                            {f.pensionistaNombre}
                          </div>
                          <div style={{ fontSize: '0.8rem', color: '#6B7280' }}>DNI: {f.pensionistaDni}</div>
                        </td>
                        <td style={{ padding: '14px 16px', fontSize: '0.85rem', color: '#4B5563' }}>
                          {f.localNombre}
                        </td>
                        <td style={{ padding: '14px 16px', fontSize: '0.85rem' }}>
                          <span style={{ fontWeight: 500, color: '#111827' }}>{f.medio}</span>
                          {f.entidadBancaria && (
                            <span style={{ color: '#6B7280', fontSize: '0.8rem', display: 'block' }}>
                              {f.entidadBancaria}
                            </span>
                          )}
                        </td>
                        <td style={{ padding: '14px 16px', fontSize: '0.88rem' }}>
                          {f.numeroOperacion ? (
                            <code
                              style={{
                                background: '#F3F4F6',
                                padding: '2px 6px',
                                borderRadius: '4px',
                                fontSize: '0.85rem',
                                color: '#1F2937',
                              }}
                            >
                              {f.numeroOperacion}
                            </code>
                          ) : (
                            <span style={{ color: '#9CA3AF', fontSize: '0.8rem' }}>s/n</span>
                          )}
                        </td>
                        <td
                          style={{
                            padding: '14px 16px',
                            textAlign: 'right',
                            fontWeight: 700,
                            fontSize: '0.95rem',
                            color: '#111827',
                            whiteSpace: 'nowrap',
                          }}
                        >
                          {enSoles(f.monto)}
                        </td>
                        <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                          <Insignia tono={tonoEstado(f.estado)}>
                            {f.estado.toUpperCase()}
                          </Insignia>
                          {f.estado === 'rechazado' && f.motivoRechazoDescripcion && (
                            <div
                              style={{
                                fontSize: '0.75rem',
                                color: '#DC2626',
                                marginTop: '4px',
                                maxWidth: '160px',
                                margin: '4px auto 0',
                              }}
                              title={f.observacionRechazo || undefined}
                            >
                              {f.motivoRechazoDescripcion}
                            </div>
                          )}
                        </td>
                        <td style={{ padding: '14px 16px', textAlign: 'right', whiteSpace: 'nowrap' }}>
                          <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
                            <button
                              type="button"
                              onClick={() => setAbonoParaVer(f)}
                              style={{
                                background: '#FFFFFF',
                                border: '1px solid #D1D5DB',
                                borderRadius: '6px',
                                padding: '6px 10px',
                                fontSize: '0.8rem',
                                color: '#374151',
                                cursor: 'pointer',
                              }}
                              title="Ver ficha bancaria y comprobante"
                            >
                              Ver ficha
                            </button>

                            {esPendiente && (
                              <>
                                <button
                                  type="button"
                                  onClick={() => setAbonoParaRechazar(f)}
                                  style={{
                                    background: '#FFFFFF',
                                    border: '1px solid #F87171',
                                    borderRadius: '6px',
                                    padding: '6px 10px',
                                    fontSize: '0.8rem',
                                    color: '#DC2626',
                                    cursor: 'pointer',
                                    fontWeight: 600,
                                  }}
                                  title="Rechazar abono digital con motivo"
                                >
                                  Rechazar
                                </button>
                                <button
                                  type="button"
                                  onClick={() => setAbonoParaAprobar(f)}
                                  style={{
                                    background: '#059669',
                                    border: '1px solid #059669',
                                    borderRadius: '6px',
                                    padding: '6px 12px',
                                    fontSize: '0.8rem',
                                    color: '#FFFFFF',
                                    cursor: 'pointer',
                                    fontWeight: 600,
                                  }}
                                  title="Aprobar y acreditar a cuenta corriente"
                                >
                                  Aprobar
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {/* Modal de Detalle / Comprobante */}
      <ModalComprobanteAbono
        abono={abonoParaVer}
        abierto={Boolean(abonoParaVer)}
        onCerrar={() => setAbonoParaVer(null)}
        onAprobar={(abono) => setAbonoParaAprobar(abono)}
        onRechazar={(abono) => setAbonoParaRechazar(abono)}
      />

      {/* Modal de Confirmación para Aprobar (CA2) */}
      <ModalDeConfirmacion
        abierto={Boolean(abonoParaAprobar)}
        titulo="¿Aprobar abono digital?"
        mensaje={
          abonoParaAprobar
            ? `Se acreditará ${enSoles(abonoParaAprobar.monto)} a la cuenta corriente de ${
                abonoParaAprobar.pensionistaNombre
              } (DNI ${abonoParaAprobar.pensionistaDni}). La operación es irreversible.`
            : ''
        }
        textoConfirmar="Aprobar abono"
        confirmando={procesandoAccion}
        onConfirmar={confirmarAprobacion}
        onCancelar={() => setAbonoParaAprobar(null)}
      />

      {/* Diálogo de Rechazo con Motivos Formales (CA3) */}
      <DialogoRechazarAbono
        abono={abonoParaRechazar}
        abierto={Boolean(abonoParaRechazar)}
        token={token}
        confirmando={procesandoAccion}
        onConfirmar={confirmarRechazo}
        onCancelar={() => setAbonoParaRechazar(null)}
      />
    </section>
  )
}
