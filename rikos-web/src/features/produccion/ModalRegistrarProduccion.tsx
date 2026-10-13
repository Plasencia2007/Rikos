import { useState, useEffect, useMemo } from 'react'
import {
  obtenerPrecargaProduccion,
  registrarProduccion,
  type PrecargaProduccion,
  type ProductoConReceta,
} from '../../api/cliente'

interface Props {
  abierto: boolean
  onCerrar: () => void
  onProduccionRegistrada: () => void
  token: string
  localId: string
}

export function ModalRegistrarProduccion({
  abierto,
  onCerrar,
  onProduccionRegistrada,
  token,
  localId,
}: Props) {
  const [fecha, setFecha] = useState(() => new Date().toISOString().split('T')[0])
  const [servicio, setServicio] = useState<'almuerzo' | 'cena' | 'todo_el_dia'>('almuerzo')
  const [precarga, setPrecarga] = useState<PrecargaProduccion | null>(null)
  const [cargandoPrecarga, setCargandoPrecarga] = useState(false)
  const [productoSeleccionadoId, setProductoSeleccionadoId] = useState('')

  const [porcionesPlanificadas, setPorcionesPlanificadas] = useState<number | ''>('')
  const [porcionesPreparadas, setPorcionesPreparadas] = useState<number | ''>('')
  const [rendimientoObtenido, setRendimientoObtenido] = useState<number | ''>('')

  const [guardando, setGuardando] = useState(false)
  const [errorGuardado, setErrorGuardado] = useState<string | null>(null)

  // Cargar datos de precarga cada vez que cambia fecha o servicio
  useEffect(() => {
    if (!abierto || !localId) return
    setCargandoPrecarga(true)
    obtenerPrecargaProduccion(token, localId, fecha, servicio)
      .then(data => {
        setPrecarga(data)
        if (data.productos.length > 0 && !productoSeleccionadoId) {
          setProductoSeleccionadoId(data.productos[0].id)
        }
      })
      .catch(err => console.error('Error al obtener precarga de producción:', err))
      .finally(() => setCargandoPrecarga(false))
  }, [abierto, localId, fecha, servicio, token])

  const productoActual: ProductoConReceta | undefined = useMemo(() => {
    if (!precarga) return undefined
    return precarga.productos.find(p => p.id === productoSeleccionadoId)
  }, [precarga, productoSeleccionadoId])

  // Al seleccionar un producto, precargar porciones de la programación del menú (D2)
  useEffect(() => {
    if (!productoActual || !precarga) return

    const planificadas = precarga.porcionesPlanificadasPorProductoId[productoActual.id]
    if (planificadas != null && planificadas > 0) {
      setPorcionesPlanificadas(planificadas)
      setPorcionesPreparadas(planificadas)
      // Escalar rendimiento inicial al planificado
      const rendBase = productoActual.rendimientoBase || 1
      const porcBase = productoActual.porcionesBase || 1
      const rendInicial = Number(((planificadas / porcBase) * rendBase).toFixed(2))
      setRendimientoObtenido(rendInicial)
    } else {
      setPorcionesPlanificadas('')
      setPorcionesPreparadas(productoActual.porcionesBase || 10)
      setRendimientoObtenido(productoActual.rendimientoBase || 5)
    }
  }, [productoActual, precarga])

  // Cálculo del factor de escala y consumo de insumos (D3)
  const { factor, insumosCalculados, costoLoteTotal, costoPorPorcion, tieneStockInsuficiente } = useMemo(() => {
    if (!productoActual || !productoActual.rendimientoBase || Number(rendimientoObtenido) <= 0) {
      return {
        factor: 1,
        insumosCalculados: [],
        costoLoteTotal: 0,
        costoPorPorcion: 0,
        tieneStockInsuficiente: false,
      }
    }

    const rend = Number(rendimientoObtenido)
    const factorEscala = rend / productoActual.rendimientoBase
    let costoTotal = 0
    let saldoNegativo = false

    const insumos = (productoActual.ingredientes || []).map(ing => {
      const cantidadConsumida = Number((ing.cantidadBase * factorEscala).toFixed(4))
      const stockActual = ing.stockActual || 0
      const stockProyectado = Number((stockActual - cantidadConsumida).toFixed(4))
      const costoInsumo = Number((cantidadConsumida * (ing.costoUnitario || 0)).toFixed(2))
      costoTotal += costoInsumo

      if (stockProyectado < 0) {
        saldoNegativo = true
      }

      return {
        ...ing,
        cantidadConsumida,
        stockProyectado,
        costoInsumo,
        insuficiente: stockProyectado < 0,
      }
    })

    const prep = Number(porcionesPreparadas) || 1
    return {
      factor: factorEscala,
      insumosCalculados: insumos,
      costoLoteTotal: Number(costoTotal.toFixed(2)),
      costoPorPorcion: Number((costoTotal / prep).toFixed(2)),
      tieneStockInsuficiente: saldoNegativo,
    }
  }, [productoActual, rendimientoObtenido, porcionesPreparadas])

  if (!abierto) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!productoActual) return

    const prep = Number(porcionesPreparadas)
    const rend = Number(rendimientoObtenido)

    if (isNaN(prep) || prep <= 0) {
      setErrorGuardado('Las porciones preparadas deben ser mayores a 0')
      return
    }
    if (isNaN(rend) || rend <= 0) {
      setErrorGuardado('El rendimiento obtenido debe ser mayor a 0')
      return
    }

    try {
      setGuardando(true)
      setErrorGuardado(null)

      await registrarProduccion(token, localId, {
        productoId: productoActual.id,
        recetaId: productoActual.recetaId,
        fecha,
        servicio,
        porcionesPlanificadas: porcionesPlanificadas === '' ? null : Number(porcionesPlanificadas),
        porcionesPreparadas: prep,
        rendimientoObtenido: rend,
      })

      onProduccionRegistrada()
      onCerrar()
    } catch (err: any) {
      console.error('Error al registrar producción:', err)
      setErrorGuardado(err?.cuerpo?.error || 'No se pudo registrar la producción. Verifique los datos.')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 50,
        padding: '1rem',
      }}
    >
      <div
        style={{
          backgroundColor: '#fff',
          borderRadius: '12px',
          width: '100%',
          maxWidth: '780px',
          maxHeight: '92vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
          overflow: 'hidden',
        }}
      >
        {/* Encabezado */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            borderBottom: '1px solid #e5e7eb',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            backgroundColor: '#f9fafb',
          }}
        >
          <div>
            <h2 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700, color: '#111827' }}>
              Registrar Producción de Cocina
            </h2>
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.8125rem', color: '#6b7280' }}>
              Descuenta insumos e ingresa el rendimiento real al inventario (US-048)
            </p>
          </div>
          <button
            onClick={onCerrar}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '1.5rem',
              color: '#9ca3af',
              cursor: 'pointer',
              padding: '0.25rem',
            }}
          >
            ×
          </button>
        </div>

        {/* Contenido scrolleable */}
        <form onSubmit={handleSubmit} style={{ overflowY: 'auto', padding: '1.5rem', flex: 1 }}>
          {errorGuardado && (
            <div
              style={{
                backgroundColor: '#fee2e2',
                border: '1px solid #f87171',
                color: '#b91c1c',
                padding: '0.75rem 1rem',
                borderRadius: '8px',
                fontSize: '0.875rem',
                marginBottom: '1.25rem',
              }}
            >
              {errorGuardado}
            </div>
          )}

          {/* Fila 1: Fecha y Servicio */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.25rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, color: '#374151', marginBottom: '0.25rem' }}>
                Fecha de Producción
              </label>
              <input
                type="date"
                value={fecha}
                onChange={e => setFecha(e.target.value)}
                required
                style={{
                  width: '100%',
                  padding: '0.5rem 0.75rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, color: '#374151', marginBottom: '0.25rem' }}>
                Servicio
              </label>
              <select
                value={servicio}
                onChange={e => setServicio(e.target.value as any)}
                style={{
                  width: '100%',
                  padding: '0.5rem 0.75rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                }}
              >
                <option value="almuerzo">Almuerzo</option>
                <option value="cena">Cena</option>
                <option value="todo_el_dia">Todo el día / General</option>
              </select>
            </div>
          </div>

          {/* Fila 2: Selección de Subreceta o Plato */}
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, color: '#374151', marginBottom: '0.25rem' }}>
              Producto o Subreceta a Producir
            </label>
            <select
              value={productoSeleccionadoId}
              onChange={e => setProductoSeleccionadoId(e.target.value)}
              disabled={cargandoPrecarga || !precarga?.productos.length}
              style={{
                width: '100%',
                padding: '0.625rem 0.75rem',
                border: '1px solid #d1d5db',
                borderRadius: '6px',
                fontSize: '0.875rem',
                backgroundColor: cargandoPrecarga ? '#f3f4f6' : '#fff',
                fontWeight: 500,
              }}
            >
              {cargandoPrecarga && <option>Cargando recetas del catálogo...</option>}
              {!cargandoPrecarga && (!precarga || precarga.productos.length === 0) && (
                <option>No hay productos con recetas activas configuradas</option>
              )}

              {/* Subrecetas primero */}
              <optgroup label="🍲 Subrecetas de Cocina (Salsas, Bases, Rellenos)">
                {precarga?.productos
                  .filter(p => p.esSubreceta)
                  .map(p => (
                    <option key={p.id} value={p.id}>
                      [{p.codigo}] {p.nombre} (Receta base: {p.rendimientoBase} {p.unidadMedidaSimbolo})
                    </option>
                  ))}
              </optgroup>

              {/* Platos del menú */}
              <optgroup label="🍽️ Platos con Receta (Menú y Carta)">
                {precarga?.productos
                  .filter(p => !p.esSubreceta)
                  .map(p => (
                    <option key={p.id} value={p.id}>
                      [{p.codigo}] {p.nombre} (Receta base: {p.porcionesBase} porciones)
                    </option>
                  ))}
              </optgroup>
            </select>
          </div>

          {/* Fila 3: Porciones y Rendimiento */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: '1rem',
              backgroundColor: '#f9fafb',
              padding: '1rem',
              borderRadius: '8px',
              border: '1px solid #e5e7eb',
              marginBottom: '1.25rem',
            }}
          >
            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#4b5563', marginBottom: '0.25rem' }}>
                Porciones Planificadas
                {precarga?.porcionesPlanificadasPorProductoId[productoActual?.id ?? ''] != null && (
                  <span style={{ color: '#059669', marginLeft: '0.25rem' }}>✓ Del menú</span>
                )}
              </label>
              <input
                type="number"
                min="0"
                value={porcionesPlanificadas}
                onChange={e => setPorcionesPlanificadas(e.target.value === '' ? '' : Number(e.target.value))}
                placeholder="Opcional"
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                  backgroundColor: '#fff',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#111827', marginBottom: '0.25rem' }}>
                Porciones Preparadas *
              </label>
              <input
                type="number"
                min="1"
                required
                value={porcionesPreparadas}
                onChange={e => {
                  const val = e.target.value === '' ? '' : Number(e.target.value)
                  setPorcionesPreparadas(val)
                  // Autocalcular rendimiento sugerido
                  if (val && productoActual?.porcionesBase && productoActual?.rendimientoBase) {
                    const rendSugerido = Number(((val / productoActual.porcionesBase) * productoActual.rendimientoBase).toFixed(2))
                    setRendimientoObtenido(rendSugerido)
                  }
                }}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                  backgroundColor: '#fff',
                  fontWeight: 600,
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#111827', marginBottom: '0.25rem' }}>
                Rendimiento Obtenido * ({productoActual?.unidadMedidaSimbolo || 'und'})
              </label>
              <input
                type="number"
                step="0.0001"
                min="0.0001"
                required
                value={rendimientoObtenido}
                onChange={e => setRendimientoObtenido(e.target.value === '' ? '' : Number(e.target.value))}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                  backgroundColor: '#fff',
                  fontWeight: 600,
                }}
              />
            </div>
          </div>

          {/* Previsualización en Vivo de Descuento de Insumos (D3, D6) */}
          <div style={{ marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
              <span style={{ fontSize: '0.875rem', fontWeight: 600, color: '#111827' }}>
                Insumos a Descontar (Factor de Escala: {factor.toFixed(2)}x)
              </span>
              <span style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                Costo estimado del lote: <strong style={{ color: '#059669' }}>S/ {costoLoteTotal.toFixed(2)}</strong> (S/ {costoPorPorcion.toFixed(2)}/porc)
              </span>
            </div>

            {tieneStockInsuficiente && (
              <div
                style={{
                  backgroundColor: '#fffbeb',
                  border: '1px solid #fde68a',
                  color: '#b45309',
                  padding: '0.625rem 0.875rem',
                  borderRadius: '6px',
                  fontSize: '0.75rem',
                  marginBottom: '0.75rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <span>⚠️</span>
                <span>
                  Uno o más insumos no tienen suficiente existencia. Se permitirá registrar la producción dejando stock negativo con alerta per <strong>RN-057</strong>.
                </span>
              </div>
            )}

            <div style={{ border: '1px solid #e5e7eb', borderRadius: '8px', overflow: 'hidden' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8125rem' }}>
                <thead>
                  <tr style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', color: '#4b5563', textAlign: 'left' }}>
                    <th style={{ padding: '0.5rem 0.75rem' }}>Insumo</th>
                    <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>A Consumir</th>
                    <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Stock Actual</th>
                    <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Saldo Proyectado</th>
                    <th style={{ padding: '0.5rem 0.75rem', textAlign: 'right' }}>Costo Est.</th>
                  </tr>
                </thead>
                <tbody>
                  {insumosCalculados.length === 0 ? (
                    <tr>
                      <td colSpan={5} style={{ padding: '1rem', textAlign: 'center', color: '#9ca3af' }}>
                        No hay ingredientes en la receta de este producto.
                      </td>
                    </tr>
                  ) : (
                    insumosCalculados.map(ing => (
                      <tr key={ing.productoId} style={{ borderBottom: '1px solid #f3f4f6' }}>
                        <td style={{ padding: '0.5rem 0.75rem', fontWeight: 500 }}>{ing.productoNombre}</td>
                        <td style={{ padding: '0.5rem 0.75rem', textAlign: 'right', fontWeight: 600, color: '#dc2626' }}>
                          -{ing.cantidadConsumida} {ing.unidadMedidaSimbolo}
                        </td>
                        <td style={{ padding: '0.5rem 0.75rem', textAlign: 'right', color: '#6b7280' }}>
                          {ing.stockActual} {ing.unidadMedidaSimbolo}
                        </td>
                        <td
                          style={{
                            padding: '0.5rem 0.75rem',
                            textAlign: 'right',
                            fontWeight: 600,
                            color: ing.insuficiente ? '#d97706' : '#059669',
                          }}
                        >
                          {ing.stockProyectado} {ing.unidadMedidaSimbolo}
                          {ing.insuficiente && ' ⚠️'}
                        </td>
                        <td style={{ padding: '0.5rem 0.75rem', textAlign: 'right', color: '#374151' }}>
                          S/ {ing.costoInsumo.toFixed(2)}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Banner explicativo de Caso Crítico 11 */}
          <div
            style={{
              backgroundColor: '#eff6ff',
              border: '1px solid #bfdbfe',
              borderRadius: '8px',
              padding: '0.75rem 1rem',
              fontSize: '0.75rem',
              color: '#1e40af',
              lineHeight: 1.5,
            }}
          >
            <strong>Regla de negocio (Caso Crítico 11 / CA2):</strong> Al confirmar esta tanda se descontarán los insumos listados arriba y se ingresarán{' '}
            <strong>{rendimientoObtenido || 0} {productoActual?.unidadMedidaSimbolo || ''}</strong> de {productoActual?.nombre}. Cuando un plato final que use esta subreceta se comande o venda, se descontará directamente del stock de esta subreceta sin volver a consumir los insumos ya gastados.
          </div>
        </form>

        {/* Pie con botones */}
        <div
          style={{
            padding: '1rem 1.5rem',
            borderTop: '1px solid #e5e7eb',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '0.75rem',
            backgroundColor: '#f9fafb',
          }}
        >
          <button
            type="button"
            onClick={onCerrar}
            disabled={guardando}
            style={{
              padding: '0.5rem 1rem',
              backgroundColor: '#fff',
              border: '1px solid #d1d5db',
              borderRadius: '6px',
              fontSize: '0.875rem',
              fontWeight: 500,
              cursor: 'pointer',
              color: '#374151',
            }}
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={guardando || !productoActual}
            style={{
              padding: '0.5rem 1.25rem',
              backgroundColor: guardando ? '#9ca3af' : '#059669',
              color: '#fff',
              border: 'none',
              borderRadius: '6px',
              fontSize: '0.875rem',
              fontWeight: 600,
              cursor: guardando ? 'not-allowed' : 'pointer',
            }}
          >
            {guardando ? 'Registrando...' : 'Confirmar Producción'}
          </button>
        </div>
      </div>
    </div>
  )
}
