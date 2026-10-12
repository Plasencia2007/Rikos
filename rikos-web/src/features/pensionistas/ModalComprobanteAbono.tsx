import { useState } from 'react'
import { Boton, Modal } from '../../comun/componentes'
import type { FilaAbono } from '../../api/cliente'
import { enSoles } from './dinero'

interface Props {
  abono: FilaAbono | null
  abierto: boolean
  onCerrar: () => void
  onAprobar?: (abono: FilaAbono) => void
  onRechazar?: (abono: FilaAbono) => void
}

function colorMedio(medio: string): { bg: string; color: string; label: string } {
  const m = medio.toLowerCase()
  if (m.includes('yape')) return { bg: '#742284', color: '#ffffff', label: 'Yape' }
  if (m.includes('plin')) return { bg: '#00BFA5', color: '#ffffff', label: 'Plin' }
  if (m.includes('transferencia') || m.includes('deposito')) return { bg: '#002A8F', color: '#ffffff', label: 'Transferencia' }
  if (m.includes('tarjeta')) return { bg: '#1E293B', color: '#ffffff', label: 'Tarjeta' }
  return { bg: '#475569', color: '#ffffff', label: medio.toUpperCase() }
}

export function ModalComprobanteAbono({
  abono,
  abierto,
  onCerrar,
  onAprobar,
  onRechazar,
}: Props) {
  const [copiado, setCopiado] = useState(false)

  if (!abono) return null

  const chip = colorMedio(abono.medio)
  const esPendiente = abono.estado === 'pendiente'

  function copiarNumeroOperacion() {
    if (!abono?.numeroOperacion) return
    navigator.clipboard.writeText(abono.numeroOperacion).then(() => {
      setCopiado(true)
      setTimeout(() => setCopiado(false), 2000)
    })
  }

  const fechaFormateada = new Date(abono.creadoEn).toLocaleString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })

  return (
    <Modal
      abierto={abierto}
      titulo="Comprobante y Cotejo de Abono"
      onCerrar={onCerrar}
      pie={
        <div style={{ display: 'flex', width: '100%', justifyContent: 'space-between', alignItems: 'center' }}>
          <Boton variante="secundario" onClick={onCerrar}>
            Cerrar
          </Boton>
          {esPendiente && (
            <div style={{ display: 'flex', gap: '8px' }}>
              {onRechazar && (
                <Boton
                  variante="peligro"
                  onClick={() => {
                    onCerrar()
                    onRechazar(abono)
                  }}
                >
                  Rechazar abono
                </Boton>
              )}
              {onAprobar && (
                <Boton
                  variante="primario"
                  onClick={() => {
                    onCerrar()
                    onAprobar(abono)
                  }}
                >
                  Aprobar abono
                </Boton>
              )}
            </div>
          )}
        </div>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {/* Banner de Estado */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '12px 16px',
            background: esPendiente ? '#FEF3C7' : abono.estado === 'aprobado' ? '#D1FAE5' : '#FEE2E2',
            borderRadius: '8px',
            border: `1px solid ${
              esPendiente ? '#FCD34D' : abono.estado === 'aprobado' ? '#6EE7B7' : '#FCA5A5'
            }`,
          }}
        >
          <div>
            <span style={{ fontSize: '0.82rem', color: '#4B5563', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Estado del abono
            </span>
            <div style={{ fontWeight: 600, fontSize: '1rem', textTransform: 'capitalize' }}>
              {abono.estado}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <span style={{ fontSize: '0.82rem', color: '#4B5563' }}>Monto registrado</span>
            <div style={{ fontWeight: 700, fontSize: '1.25rem', color: '#166534' }}>
              {enSoles(abono.monto)}
            </div>
          </div>
        </div>

        {/* Ficha Bancaria */}
        <div
          style={{
            border: '1px solid #E5E7EB',
            borderRadius: '8px',
            padding: '16px',
            background: '#F9FAFB',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.85rem', color: '#6B7280' }}>Medio de Pago</span>
            <span
              style={{
                backgroundColor: chip.bg,
                color: chip.color,
                padding: '4px 10px',
                borderRadius: '6px',
                fontSize: '0.85rem',
                fontWeight: 600,
              }}
            >
              {chip.label}
              {abono.entidadBancaria ? ` · ${abono.entidadBancaria}` : ''}
            </span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.85rem', color: '#6B7280' }}>N° de Operación</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <code
                style={{
                  background: '#E0E7FF',
                  color: '#3730A3',
                  padding: '3px 8px',
                  borderRadius: '4px',
                  fontSize: '0.95rem',
                  fontWeight: 600,
                  letterSpacing: '0.05em',
                }}
              >
                {abono.numeroOperacion || 'Sin número'}
              </code>
              {abono.numeroOperacion && (
                <button
                  type="button"
                  onClick={copiarNumeroOperacion}
                  style={{
                    background: copiado ? '#10B981' : '#FFFFFF',
                    color: copiado ? '#FFFFFF' : '#374151',
                    border: '1px solid #D1D5DB',
                    borderRadius: '4px',
                    padding: '3px 8px',
                    fontSize: '0.78rem',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                  title="Copiar N° de Operación"
                >
                  {copiado ? '✓ Copiado' : 'Copiar'}
                </button>
              )}
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.85rem', color: '#6B7280' }}>Pensionista</span>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontWeight: 600, color: '#111827' }}>{abono.pensionistaNombre}</div>
              <div style={{ fontSize: '0.82rem', color: '#6B7280' }}>DNI: {abono.pensionistaDni}</div>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.85rem', color: '#6B7280' }}>Local Emisor</span>
            <span style={{ fontWeight: 500, color: '#111827' }}>{abono.localNombre}</span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.85rem', color: '#6B7280' }}>Fecha y Hora</span>
            <span style={{ fontSize: '0.85rem', color: '#374151' }}>{fechaFormateada}</span>
          </div>
        </div>

        {/* Motivo de rechazo si aplica */}
        {abono.estado === 'rechazado' && (
          <div
            style={{
              padding: '12px 14px',
              backgroundColor: '#FEF2F2',
              border: '1px solid #F87171',
              borderRadius: '6px',
            }}
          >
            <div style={{ fontWeight: 600, color: '#991B1B', fontSize: '0.88rem' }}>
              Motivo de Rechazo: {abono.motivoRechazoDescripcion || 'No especificado'}
            </div>
            {abono.observacionRechazo && (
              <div style={{ fontSize: '0.82rem', color: '#7F1D1D', marginTop: '4px' }}>
                Obs: {abono.observacionRechazo}
              </div>
            )}
            {abono.revisadoEn && (
              <div style={{ fontSize: '0.78rem', color: '#9CA3AF', marginTop: '6px' }}>
                Revisado el {new Date(abono.revisadoEn).toLocaleString('es-PE')}
              </div>
            )}
          </div>
        )}

        {/* Imagen del comprobante adjunto si existe */}
        {abono.comprobanteUrl ? (
          <div>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#374151', display: 'block', marginBottom: '6px' }}>
              Imagen del comprobante / voucher
            </span>
            <div
              style={{
                border: '1px solid #E5E7EB',
                borderRadius: '8px',
                overflow: 'hidden',
                textAlign: 'center',
                background: '#111827',
                padding: '8px',
              }}
            >
              <img
                src={abono.comprobanteUrl}
                alt="Comprobante de abono digital"
                style={{
                  maxWidth: '100%',
                  maxHeight: '360px',
                  objectFit: 'contain',
                  borderRadius: '4px',
                }}
              />
            </div>
          </div>
        ) : (
          <div
            style={{
              padding: '10px 14px',
              backgroundColor: '#EFF6FF',
              borderRadius: '6px',
              fontSize: '0.82rem',
              color: '#1E40AF',
              border: '1px solid #BFDBFE',
            }}
          >
            ℹ️ Coteje el <strong>N° de Operación ({abono.numeroOperacion || 's/n'})</strong> directamente con el extracto o notificación bancaria de la app del restaurante antes de aprobar.
          </div>
        )}
      </div>
    </Modal>
  )
}
