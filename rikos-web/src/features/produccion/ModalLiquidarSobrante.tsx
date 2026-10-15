import { useEffect, useState } from 'react'
import { Modal, Boton, Aviso } from '../../comun/componentes'
import {
  registrarSobranteProduccion,
  listarMotivos,
  type Motivo,
  type DetalleProduccion,
  type LiquidarSobranteComando,
} from '../../api/cliente'

interface Props {
  abierto: boolean
  produccion: DetalleProduccion | null
  token: string
  localId: string
  onGuardado: (produccionActualizada: DetalleProduccion) => void
  onCerrar: () => void
}

type DestinoSobrante = 'conservar' | 'personal' | 'perdida' | 'reutilizacion' | 'otro'

export function ModalLiquidarSobrante({
  abierto,
  produccion,
  token,
  localId,
  onGuardado,
  onCerrar,
}: Props) {
  const [sobrante, setSobrante] = useState<string>('')
  const [destino, setDestino] = useState<DestinoSobrante>('conservar')
  const [motivoId, setMotivoId] = useState('')
  const [observacion, setObservacion] = useState('')
  const [motivos, setMotivos] = useState<Motivo[]>([])
  const [cargandoMotivos, setCargandoMotivos] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!abierto || !produccion) return
    setSobrante('')
    setDestino('conservar')
    setMotivoId('')
    setObservacion('')
    setError('')

    async function cargar() {
      try {
        setCargandoMotivos(true)
        const lista = await listarMotivos(token, 'sobrante_produccion').catch(() =>
          listarMotivos(token, 'ajuste_inventario')
        )
        const activos = lista.filter((m) => m.activo)
        setMotivos(activos)
        if (activos.length > 0) {
          setMotivoId(activos[0].id)
        }
      } catch {
        // En caso de fallo o lista vacía
      } finally {
        setCargandoMotivos(false)
      }
    }
    cargar()
  }, [abierto, produccion, token])

  if (!abierto || !produccion) return null

  const requiereSalidaInventario = ['perdida', 'personal', 'otro'].includes(destino)
  const rendimientoMax = produccion.rendimientoObtenido ?? 0
  const valorNumSobrante = parseFloat(sobrante) || 0

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    setError('')

    if (isNaN(valorNumSobrante) || valorNumSobrante <= 0) {
      setError('La cantidad de sobrante debe ser un número mayor a cero.')
      return
    }

    if (valorNumSobrante > rendimientoMax) {
      setError(
        `El sobrante (${valorNumSobrante} ${produccion.unidadMedidaSimbolo}) no puede exceder el rendimiento obtenido (${rendimientoMax} ${produccion.unidadMedidaSimbolo}).`
      )
      return
    }

    if (destino === 'perdida' && !observacion.trim() && !motivoId) {
      setError('Por favor indique un motivo o detalle de la merma/pérdida.')
      return
    }

    try {
      setGuardando(true)
      const comando: LiquidarSobranteComando = {
        sobrante: valorNumSobrante,
        destino,
        motivoId: motivoId || undefined,
        observacion: observacion.trim() || undefined,
      }

      const actualizada = await registrarSobranteProduccion(token, localId, produccion.id, comando)
      onGuardado(actualizada)
      onCerrar()
    } catch (err: any) {
      const msg = err?.cuerpo?.error || err.message || 'Error al liquidar sobrante de producción.'
      setError(msg)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Modal
      abierto={abierto}
      onCerrar={onCerrar}
      titulo="Liquidar Sobrante de Producción (US-049)"
      pie={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', width: '100%' }}>
          <Boton variante="secundario" onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton variante="primario" onClick={() => handleSubmit()} cargando={guardando}>
            Confirmar Destino de Sobrante
          </Boton>
        </div>
      }
    >
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {error && <Aviso tono="error">{error}</Aviso>}

        {/* Resumen de la Tanda */}
        <div
          style={{
            backgroundColor: '#f9fafb',
            border: '1px solid #e5e7eb',
            borderRadius: '8px',
            padding: '1rem',
            fontSize: '0.875rem',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span style={{ color: '#6b7280' }}>Producto Producido:</span>
            <strong style={{ color: '#111827' }}>
              {produccion.productoNombre} ({produccion.productoCodigo})
            </strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span style={{ color: '#6b7280' }}>Fecha / Servicio:</span>
            <span>
              {produccion.fecha} — {produccion.servicio || 'General'}
            </span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: '#6b7280' }}>Rendimiento Total Obtenido:</span>
            <strong style={{ color: '#059669' }}>
              {produccion.rendimientoObtenido} {produccion.unidadMedidaSimbolo}
            </strong>
          </div>
        </div>

        {/* Cantidad Sobrante */}
        <div>
          <label
            style={{
              display: 'block',
              fontSize: '0.875rem',
              fontWeight: 600,
              color: '#334155',
              marginBottom: '0.35rem',
            }}
          >
            Cantidad Sobrante ({produccion.unidadMedidaSimbolo}) <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <input
            type="number"
            step="0.001"
            min="0.001"
            max={rendimientoMax}
            value={sobrante}
            onChange={(e) => setSobrante(e.target.value)}
            placeholder={`Ej. 2.500 ${produccion.unidadMedidaSimbolo}`}
            required
            autoFocus
            style={{
              width: '100%',
              padding: '0.625rem 0.875rem',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              fontSize: '1rem',
              backgroundColor: '#fff',
              color: '#0f172a',
            }}
          />
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Máximo disponible: {rendimientoMax} {produccion.unidadMedidaSimbolo}
          </div>
        </div>

        {/* Selector de Destino */}
        <div>
          <label
            style={{
              display: 'block',
              fontSize: '0.875rem',
              fontWeight: 600,
              color: '#334155',
              marginBottom: '0.35rem',
            }}
          >
            Destino del Sobrante
          </label>
          <select
            value={destino}
            onChange={(e) => setDestino(e.target.value as DestinoSobrante)}
            style={{
              width: '100%',
              padding: '0.625rem 0.875rem',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              fontSize: '0.9375rem',
              backgroundColor: '#fff',
              color: '#0f172a',
            }}
          >
            <option value="conservar">📦 Conservar en stock (Disponible para servicio siguiente)</option>
            <option value="reutilizacion">🔄 Reutilización (Aprovechable como subreceta o ingrediente)</option>
            <option value="personal">👥 Consumo de Personal (Registrar salida de existencias)</option>
            <option value="perdida">⚠️ Merma / Pérdida (Descartar y descontar de existencias)</option>
            <option value="otro">ℹ️ Otro destino (Descuenta existencias con observación)</option>
          </select>
        </div>

        {/* Explicación de impacto en Inventario */}
        {requiereSalidaInventario ? (
          <div
            style={{
              backgroundColor: '#fef2f2',
              border: '1px solid #fecaca',
              borderRadius: '6px',
              padding: '0.75rem',
              fontSize: '0.8125rem',
              color: '#991b1b',
            }}
          >
            📉 <strong>Impacto en Existencias:</strong> Al elegir <em>{destino}</em>, se registrará un
            movimiento de <strong>salida</strong> en el Kardex y se descontará la existencia física del
            almacén.
          </div>
        ) : (
          <div
            style={{
              backgroundColor: '#f0fdf4',
              border: '1px solid #bbf7d0',
              borderRadius: '6px',
              padding: '0.75rem',
              fontSize: '0.8125rem',
              color: '#166534',
            }}
          >
            ✨ <strong>Existencias intactas:</strong> El sobrante permanecerá registrado en el almacén del local
            para su consumo o venta posterior.
          </div>
        )}

        {/* Motivo (si aplica o hay catálogo de motivos) */}
        {motivos.length > 0 && (
          <div>
            <label
              style={{
                display: 'block',
                fontSize: '0.875rem',
                fontWeight: 600,
                color: '#334155',
                marginBottom: '0.35rem',
              }}
            >
              Motivo del Destino
            </label>
            <select
              value={motivoId}
              onChange={(e) => setMotivoId(e.target.value)}
              disabled={cargandoMotivos}
              style={{
                width: '100%',
                padding: '0.625rem 0.875rem',
                borderRadius: '6px',
                border: '1px solid #cbd5e1',
                fontSize: '0.875rem',
                backgroundColor: '#fff',
                color: '#0f172a',
              }}
            >
              <option value="">-- Sin motivo específico --</option>
              {motivos.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.descripcion}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Observación adicional */}
        <div>
          <label
            style={{
              display: 'block',
              fontSize: '0.875rem',
              fontWeight: 600,
              color: '#334155',
              marginBottom: '0.35rem',
            }}
          >
            Observación / Justificación
          </label>
          <textarea
            rows={2}
            value={observacion}
            onChange={(e) => setObservacion(e.target.value)}
            placeholder="Detalles sobre el estado del alimento, almacenamiento o persona responsable..."
            style={{
              width: '100%',
              padding: '0.5rem 0.75rem',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              fontSize: '0.875rem',
              resize: 'vertical',
              color: '#0f172a',
            }}
          />
        </div>
      </form>
    </Modal>
  )
}
