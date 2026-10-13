import { useEffect, useMemo, useState } from 'react'
import { Modal, Boton, Campo, Aviso } from '../../comun/componentes'
import {
  registrarAjusteInventario,
  listarMotivos,
  type Motivo,
  type SolicitudRegistroAjuste,
  type DetalleAjuste,
  type FilaExistencia,
} from '../../api/cliente'

interface Props {
  abierto: boolean
  token: string
  localId: string
  existencias: FilaExistencia[]
  productoInicialId?: string
  onGuardado: (ajuste: DetalleAjuste) => void
  onCerrar: () => void
}

type ModoAjuste = 'variacion' | 'stock_final'

export function ModalRegistrarAjuste({
  abierto,
  token,
  localId,
  existencias,
  productoInicialId,
  onGuardado,
  onCerrar,
}: Props) {
  const [productoId, setProductoId] = useState(productoInicialId ?? '')
  const [modo, setModo] = useState<ModoAjuste>('variacion')
  const [signoVariacion, setSignoVariacion] = useState<'entrada' | 'salida'>('salida')
  const [cantidadVariacion, setCantidadVariacion] = useState<string>('')
  const [stockContado, setStockContado] = useState<string>('')
  const [motivoId, setMotivoId] = useState('')
  const [observacion, setObservacion] = useState('')
  const [motivos, setMotivos] = useState<Motivo[]>([])
  const [cargandoMotivos, setCargandoMotivos] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  // Cargar motivos configurables para ajuste_inventario
  useEffect(() => {
    if (!abierto || !token) return
    async function cargar() {
      try {
        setCargandoMotivos(true)
        const lista = await listarMotivos(token, 'ajuste_inventario')
        const activos = lista.filter((m) => m.activo)
        setMotivos(activos)
        if (activos.length > 0 && !motivoId) {
          setMotivoId(activos[0].id)
        }
      } catch {
        // En caso de fallo, mantener lista vacía
      } finally {
        setCargandoMotivos(false)
      }
    }
    cargar()
  }, [abierto, token])

  // Al abrir o cambiar productoInicialId, reiniciar formulario
  useEffect(() => {
    if (abierto) {
      setError('')
      setCantidadVariacion('')
      setStockContado('')
      setObservacion('')
      setModo('variacion')
      setSignoVariacion('salida')
      if (productoInicialId) {
        setProductoId(productoInicialId)
      } else if (existencias.length > 0 && !productoId) {
        setProductoId(existencias[0].productoId)
      }
    }
  }, [abierto, productoInicialId, existencias])

  // Producto seleccionado actual
  const productoActual = useMemo(() => {
    return existencias.find((e) => e.productoId === productoId) ?? null
  }, [existencias, productoId])

  const unidad = productoActual?.unidadCodigo || productoActual?.unidadNombre || 'UND'
  const stockActual = productoActual ? productoActual.stockActual : 0

  // Motivo seleccionado
  const motivoActual = useMemo(() => {
    return motivos.find((m) => m.id === motivoId) ?? null
  }, [motivos, motivoId])

  const requiereObservacion = motivoActual?.requiereObservacion ?? false

  // Cálculo en tiempo real de la variación y del stock proyectado
  const calculo = useMemo(() => {
    let delta = 0
    let stockNuevo = stockActual

    if (modo === 'variacion') {
      const cant = Number(cantidadVariacion)
      if (!isNaN(cant) && cant > 0) {
        delta = signoVariacion === 'entrada' ? cant : -cant
        stockNuevo = stockActual + delta
      }
    } else {
      // Modo conteo físico / stock final
      const final = Number(stockContado)
      if (!isNaN(final) && stockContado.trim() !== '') {
        delta = final - stockActual
        stockNuevo = final
      }
    }

    return {
      delta,
      stockNuevo,
      hayCambio: Math.abs(delta) > 0.000001,
      esNegativo: stockNuevo < 0,
    }
  }, [modo, signoVariacion, cantidadVariacion, stockContado, stockActual])

  async function manejarSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')

    if (!productoId) {
      setError('Debe seleccionar un producto.')
      return
    }

    if (!motivoId) {
      setError('Debe seleccionar un motivo de ajuste válido.')
      return
    }

    if (requiereObservacion && (!observacion || !observacion.trim())) {
      setError('El motivo seleccionado exige ingresar una observación detallada.')
      return
    }

    let payload: SolicitudRegistroAjuste

    if (modo === 'variacion') {
      const cant = Number(cantidadVariacion)
      if (isNaN(cant) || cant <= 0) {
        setError('Ingrese una cantidad válida mayor a cero.')
        return
      }
      payload = {
        productoId,
        motivoId,
        tipoAjuste: signoVariacion === 'entrada' ? 'INCREMENTO' : 'DISMINUCION',
        cantidad: cant,
        observacion: observacion.trim() || null,
      }
    } else {
      if (stockContado.trim() === '' || isNaN(Number(stockContado))) {
        setError('Ingrese el stock físico contado.')
        return
      }
      const final = Number(stockContado)
      if (Math.abs(final - stockActual) < 0.000001) {
        setError('El stock contado es igual al stock actual registrado. No hay variación a ajustar.')
        return
      }
      payload = {
        productoId,
        motivoId,
        tipoAjuste: 'REEMPLAZO_STOCK',
        stockFinal: final,
        observacion: observacion.trim() || null,
      }
    }

    try {
      setGuardando(true)
      const ajuste = await registrarAjusteInventario(token, localId, payload)
      onGuardado(ajuste)
      onCerrar()
    } catch (err: any) {
      let mensaje =
        err?.cuerpo?.error ||
        err?.cuerpo?.mensaje ||
        err?.message ||
        'Error al registrar el ajuste de inventario.'

      if (mensaje === 'CANTIDAD_INVALIDA') {
        mensaje = 'La cantidad a ajustar no es válida. Debe ser mayor a 0.'
      } else if (mensaje === 'OBSERVACION_REQUERIDA') {
        mensaje = 'El motivo seleccionado exige ingresar una observación detallada.'
      } else if (mensaje === 'SIN_VARIACION') {
        mensaje = 'El conteo físico coincide con el stock registrado. No hay diferencia que ajustar.'
      } else if (mensaje === 'MOTIVO_NO_ENCONTRADO' || mensaje === 'MOTIVO_INACTIVO') {
        mensaje = 'El motivo seleccionado no está disponible o ha sido desactivado.'
      }

      setError(mensaje)
    } finally {
      setGuardando(false)
    }
  }

  if (!abierto) return null

  return (
    <Modal
      abierto={abierto}
      titulo="Registrar Ajuste de Inventario"
      onCerrar={onCerrar}
      pie={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', width: '100%' }}>
          <Boton variante="secundario" onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton
            variante="primario"
            onClick={manejarSubmit}
            cargando={guardando}
            disabled={!calculo.hayCambio || guardando}
          >
            Confirmar y Aplicar Ajuste
          </Boton>
        </div>
      }
    >
      <form onSubmit={manejarSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {error && <Aviso tono="error">{error}</Aviso>}

        {/* Selector de Producto */}
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
            Producto a ajustar <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <select
            value={productoId}
            onChange={(e) => setProductoId(e.target.value)}
            disabled={guardando}
            style={{
              width: '100%',
              padding: '0.6rem 0.75rem',
              borderRadius: '0.375rem',
              border: '1px solid #cbd5e1',
              fontSize: '0.925rem',
              backgroundColor: '#ffffff',
              color: '#0f172a',
            }}
          >
            <option value="">-- Seleccionar producto --</option>
            {existencias.map((e) => (
              <option key={e.productoId} value={e.productoId}>
                [{e.codigo}] {e.nombre} — Stock: {e.stockActual} {e.unidadCodigo || e.unidadNombre}
              </option>
            ))}
          </select>
        </div>

        {/* Tarjeta de Stock Actual Registrado */}
        {productoActual && (
          <div
            style={{
              backgroundColor: '#f8fafc',
              padding: '0.75rem 1rem',
              borderRadius: '0.5rem',
              border: '1px solid #e2e8f0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <div>
              <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>
                Stock Actual en Sistema
              </div>
              <div style={{ fontSize: '0.85rem', color: '#334155', marginTop: '0.15rem' }}>
                Categoría: <strong>{productoActual.categoriaNombre}</strong>
              </div>
            </div>
            <div
              style={{
                fontSize: '1.2rem',
                fontWeight: 700,
                color: stockActual < 0 ? '#ef4444' : stockActual === 0 ? '#64748b' : '#059669',
              }}
            >
              {stockActual} <span style={{ fontSize: '0.85rem', fontWeight: 500 }}>{unidad}</span>
            </div>
          </div>
        )}

        {/* Modo de Ajuste: Pestañas Switch */}
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
            Método de captura
          </label>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: '0.5rem',
              backgroundColor: '#f1f5f9',
              padding: '0.25rem',
              borderRadius: '0.5rem',
            }}
          >
            <button
              type="button"
              onClick={() => setModo('variacion')}
              style={{
                padding: '0.5rem',
                borderRadius: '0.375rem',
                border: 'none',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer',
                backgroundColor: modo === 'variacion' ? '#ffffff' : 'transparent',
                color: modo === 'variacion' ? '#0f172a' : '#64748b',
                boxShadow: modo === 'variacion' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                transition: 'all 0.15s ease',
              }}
            >
              Por Variación (+ / -)
            </button>
            <button
              type="button"
              onClick={() => setModo('stock_final')}
              style={{
                padding: '0.5rem',
                borderRadius: '0.375rem',
                border: 'none',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer',
                backgroundColor: modo === 'stock_final' ? '#ffffff' : 'transparent',
                color: modo === 'stock_final' ? '#0f172a' : '#64748b',
                boxShadow: modo === 'stock_final' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                transition: 'all 0.15s ease',
              }}
            >
              Por Conteo Físico Real
            </button>
          </div>
        </div>

        {/* Campos según el modo */}
        {modo === 'variacion' ? (
          <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: '0.75rem', alignItems: 'flex-start' }}>
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
                Signo
              </label>
              <select
                value={signoVariacion}
                onChange={(e) => setSignoVariacion(e.target.value as 'entrada' | 'salida')}
                style={{
                  width: '100%',
                  padding: '0.6rem 0.75rem',
                  borderRadius: '0.375rem',
                  border: '1px solid #cbd5e1',
                  fontSize: '0.925rem',
                  fontWeight: 600,
                  color: signoVariacion === 'entrada' ? '#059669' : '#dc2626',
                  backgroundColor: signoVariacion === 'entrada' ? '#f0fdf4' : '#fef2f2',
                }}
              >
                <option value="salida">- Salida / Merma</option>
                <option value="entrada">+ Entrada / Sobrante</option>
              </select>
            </div>
            <div>
              <Campo
                etiqueta={`Cantidad a variar (${unidad})`}
                type="number"
                step="any"
                min="0.0001"
                placeholder="Ej. 2.50"
                value={cantidadVariacion}
                onChange={(e) => setCantidadVariacion(e.target.value)}
              />
            </div>
          </div>
        ) : (
          <div>
            <Campo
              etiqueta={`Stock contado físico actual (${unidad})`}
              ayuda="El sistema calculará automáticamente la diferencia con el stock registrado."
              type="number"
              step="any"
              placeholder="Ej. 18.00"
              value={stockContado}
              onChange={(e) => setStockContado(e.target.value)}
            />
          </div>
        )}

        {/* Tarjeta de Resumen Proyectado */}
        {calculo.hayCambio && (
          <div
            style={{
              padding: '0.85rem 1rem',
              borderRadius: '0.5rem',
              backgroundColor: calculo.delta > 0 ? '#f0fdf4' : '#fff7ed',
              border: `1px solid ${calculo.delta > 0 ? '#bbf7d0' : '#fed7aa'}`,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.875rem', color: '#475569' }}>Diferencia a aplicar:</span>
              <span
                style={{
                  fontWeight: 700,
                  color: calculo.delta > 0 ? '#166534' : '#c2410c',
                  fontSize: '0.95rem',
                }}
              >
                {calculo.delta > 0 ? `+${calculo.delta}` : calculo.delta} {unidad}
              </span>
            </div>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginTop: '0.4rem',
                paddingTop: '0.4rem',
                borderTop: '1px dashed #cbd5e1',
              }}
            >
              <span style={{ fontSize: '0.875rem', fontWeight: 600, color: '#334155' }}>
                Stock resultante nuevo:
              </span>
              <span
                style={{
                  fontWeight: 700,
                  fontSize: '1.05rem',
                  color: calculo.esNegativo ? '#dc2626' : '#059669',
                }}
              >
                {calculo.stockNuevo} {unidad}
              </span>
            </div>

            {calculo.esNegativo && (
              <div
                style={{
                  marginTop: '0.6rem',
                  fontSize: '0.8rem',
                  color: '#b45309',
                  backgroundColor: '#fef3c7',
                  padding: '0.5rem',
                  borderRadius: '0.375rem',
                  lineHeight: 1.4,
                }}
              >
                ⚠️ <strong>Aviso RN-057:</strong> El ajuste dejará el stock en saldo negativo. Se registrará correctamente para reflejar la realidad física sin bloquear operaciones.
              </div>
            )}
          </div>
        )}

        {/* Selector de Motivo */}
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
            Motivo de ajuste <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <select
            value={motivoId}
            onChange={(e) => setMotivoId(e.target.value)}
            disabled={guardando || cargandoMotivos}
            style={{
              width: '100%',
              padding: '0.6rem 0.75rem',
              borderRadius: '0.375rem',
              border: '1px solid #cbd5e1',
              fontSize: '0.925rem',
              backgroundColor: '#ffffff',
              color: '#0f172a',
            }}
          >
            {motivos.length === 0 ? (
              <option value="">{cargandoMotivos ? 'Cargando motivos...' : 'Sin motivos disponibles'}</option>
            ) : (
              motivos.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.descripcion} {m.requiereObservacion ? '(requiere detalle obligatorio)' : ''}
                </option>
              ))
            )}
          </select>
        </div>

        {/* Campo de Observación */}
        <div>
          <Campo
            etiqueta={`Observación ${requiereObservacion ? '*' : '(opcional)'}`}
            ayuda={
              requiereObservacion
                ? 'Obligatoria para este motivo (ej. detalles de merma, rotura o sobrante detectado).'
                : 'Información adicional sobre el origen o causas de la discrepancia.'
            }
            placeholder={
              requiereObservacion
                ? 'Ingrese el detalle obligatorio de la causa del ajuste...'
                : 'Detalle opcional...'
            }
            value={observacion}
            onChange={(e) => setObservacion(e.target.value)}
          />
        </div>
      </form>
    </Modal>
  )
}
