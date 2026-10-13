import { useState, useEffect } from 'react'
import {
  listarProveedores,
  listarPresentacionesProducto,
  registrarCompra,
  subirComprobanteCompra,
  type ProveedorCatalogo,
  type PresentacionCatalogo,
  type SolicitudRegistroCompra,
  type ItemRegistroCompra,
} from '../../api/cliente'

interface Props {
  abierto: boolean
  token: string
  localId: string
  productosCatalogo: Array<{
    id: string
    codigo: string
    nombre: string
    unidadCodigo: string
  }>
  alCerrar: () => void
  alGuardarExitoso: () => void
}

interface ItemFormulario {
  tempId: string
  productoId: string
  presentacionId: string
  cantidad: string
  precioUnitarioSoles: string
  presentacionesDisponibles: PresentacionCatalogo[]
}

export function DialogoFormularioCompra({
  abierto,
  token,
  localId,
  productosCatalogo,
  alCerrar,
  alGuardarExitoso,
}: Props) {
  const [proveedores, setProveedores] = useState<ProveedorCatalogo[]>([])
  const [proveedorId, setProveedorId] = useState('')
  const [tipoDocumento, setTipoDocumento] = useState<'factura' | 'boleta' | 'ticket' | 'sin_documento'>('factura')
  const [serie, setSerie] = useState('')
  const [numero, setNumero] = useState('')
  const [fechaDocumento, setFechaDocumento] = useState(new Date().toISOString().split('T')[0])
  const [observacion, setObservacion] = useState('')
  const [archivoAdjunto, setArchivoAdjunto] = useState<File | null>(null)

  const [items, setItems] = useState<ItemFormulario[]>([
    {
      tempId: 'item-1',
      productoId: '',
      presentacionId: '',
      cantidad: '1',
      precioUnitarioSoles: '',
      presentacionesDisponibles: [],
    },
  ])

  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (abierto) {
      listarProveedores(token)
        .then(lista => {
          const activos = lista.filter(p => p.activo)
          setProveedores(activos)
          if (activos.length > 0 && !proveedorId) {
            setProveedorId(activos[0].id)
          }
        })
        .catch(err => console.error('Error al cargar proveedores:', err))
    }
  }, [abierto, token])

  if (!abierto) return null

  const handleCambioProducto = async (index: number, nuevoProdId: string) => {
    const nuevos = [...items]
    nuevos[index].productoId = nuevoProdId
    nuevos[index].presentacionId = ''
    nuevos[index].presentacionesDisponibles = []

    if (nuevoProdId) {
      try {
        const pres = await listarPresentacionesProducto(token, nuevoProdId)
        nuevos[index].presentacionesDisponibles = pres.filter(p => p.activo)
        const pred = pres.find(p => p.esPredeterminada && p.activo)
        if (pred) {
          nuevos[index].presentacionId = pred.id
        }
      } catch (err) {
        console.error('Error al listar presentaciones:', err)
      }
    }
    setItems(nuevos)
  }

  const handleAgregarFila = () => {
    setItems([
      ...items,
      {
        tempId: 'item-' + Date.now(),
        productoId: '',
        presentacionId: '',
        cantidad: '1',
        precioUnitarioSoles: '',
        presentacionesDisponibles: [],
      },
    ])
  }

  const handleEliminarFila = (index: number) => {
    if (items.length <= 1) return
    setItems(items.filter((_, i) => i !== index))
  }

  const calcularTotalSoles = () => {
    return items.reduce((acc, item) => {
      const cant = parseFloat(item.cantidad) || 0
      const precio = parseFloat(item.precioUnitarioSoles) || 0
      return acc + cant * precio
    }, 0)
  }

  const guardar = async (confirmarInmediatamente: boolean) => {
    setError(null)
    if (!proveedorId) {
      setError('Selecciona un proveedor')
      return
    }
    if (tipoDocumento !== 'sin_documento') {
      if (!serie.trim() || !numero.trim()) {
        setError('Serie y número son obligatorios para este comprobante')
        return
      }
    }
    if (items.length === 0 || !items.some(i => i.productoId)) {
      setError('Agrega al menos un producto a la compra')
      return
    }

    const detallesValidados: ItemRegistroCompra[] = []
    for (const item of items) {
      if (!item.productoId) continue
      const cant = parseFloat(item.cantidad)
      if (isNaN(cant) || cant <= 0) {
        setError('Cada ítem debe tener una cantidad mayor a cero')
        return
      }
      const precioSoles = parseFloat(item.precioUnitarioSoles)
      if (isNaN(precioSoles) || precioSoles < 0) {
        setError('Cada ítem debe tener un precio unitario válido')
        return
      }
      detallesValidados.push({
        productoId: item.productoId,
        presentacionId: item.presentacionId || null,
        cantidad: cant,
        precioUnitario: Math.round(precioSoles * 100),
      })
    }

    const payload: SolicitudRegistroCompra = {
      proveedorId,
      tipoDocumento,
      serie: tipoDocumento !== 'sin_documento' ? serie.trim().toUpperCase() : null,
      numero: tipoDocumento !== 'sin_documento' ? numero.trim() : null,
      fechaDocumento,
      observacion: observacion.trim() || null,
      confirmarInmediatamente,
      detalles: detallesValidados,
    }

    try {
      setCargando(true)
      const guardada = await registrarCompra(token, localId, payload)
      if (archivoAdjunto) {
        try {
          await subirComprobanteCompra(token, localId, guardada.id, archivoAdjunto)
        } catch (e) {
          console.warn('No se pudo subir comprobante tras registrar compra:', e)
        }
      }
      alGuardarExitoso()
    } catch (e: any) {
      const msg = e?.cuerpo?.error || e?.message || 'Error al registrar compra'
      setError(msg)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0,0,0,0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 9999,
        padding: '1rem',
      }}
    >
      <div
        style={{
          backgroundColor: '#fff',
          borderRadius: '12px',
          width: '100%',
          maxWidth: '850px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
        }}
      >
        {/* Cabecera */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            borderBottom: '1px solid #e5e7eb',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <div>
            <h2 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 600, color: '#111827' }}>
              Registrar Compra de Inventario
            </h2>
            <p style={{ margin: 0, fontSize: '0.875rem', color: '#6b7280' }}>
              Ingreso físico de mercadería, actualización de stock y costo de reposición (ADM-18).
            </p>
          </div>
          <button
            onClick={alCerrar}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '1.5rem',
              cursor: 'pointer',
              color: '#9ca3af',
            }}
          >
            ×
          </button>
        </div>

        {/* Contenido con scroll */}
        <div style={{ padding: '1.5rem', overflowY: 'auto', flex: 1 }}>
          {error && (
            <div
              style={{
                backgroundColor: '#fee2e2',
                color: '#991b1b',
                padding: '0.75rem',
                borderRadius: '6px',
                marginBottom: '1rem',
                fontSize: '0.875rem',
              }}
            >
              ⚠️ {error}
            </div>
          )}

          {/* Formulario cabecera */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1rem',
              marginBottom: '1.5rem',
              backgroundColor: '#f9fafb',
              padding: '1rem',
              borderRadius: '8px',
            }}
          >
            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#374151', marginBottom: 4 }}>
                Proveedor *
              </label>
              <select
                value={proveedorId}
                onChange={e => setProveedorId(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                }}
              >
                {proveedores.map(p => (
                  <option key={p.id} value={p.id}>
                    {p.razonSocial} {p.ruc ? `(${p.ruc})` : ''}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#374151', marginBottom: 4 }}>
                Tipo Comprobante *
              </label>
              <select
                value={tipoDocumento}
                onChange={e => setTipoDocumento(e.target.value as any)}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                }}
              >
                <option value="factura">Factura</option>
                <option value="boleta">Boleta</option>
                <option value="ticket">Ticket</option>
                <option value="sin_documento">Sin documento</option>
              </select>
            </div>

            {tipoDocumento !== 'sin_documento' && (
              <>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#374151', marginBottom: 4 }}>
                    Serie *
                  </label>
                  <input
                    type="text"
                    value={serie}
                    onChange={e => setSerie(e.target.value)}
                    placeholder="Ej. F001"
                    maxLength={10}
                    style={{
                      width: '100%',
                      padding: '0.5rem',
                      border: '1px solid #d1d5db',
                      borderRadius: '6px',
                      fontSize: '0.875rem',
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#374151', marginBottom: 4 }}>
                    Número *
                  </label>
                  <input
                    type="text"
                    value={numero}
                    onChange={e => setNumero(e.target.value)}
                    placeholder="Ej. 000123"
                    maxLength={20}
                    style={{
                      width: '100%',
                      padding: '0.5rem',
                      border: '1px solid #d1d5db',
                      borderRadius: '6px',
                      fontSize: '0.875rem',
                    }}
                  />
                </div>
              </>
            )}

            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#374151', marginBottom: 4 }}>
                Fecha Documento *
              </label>
              <input
                type="date"
                value={fechaDocumento}
                onChange={e => setFechaDocumento(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                }}
              />
            </div>

            <div style={{ gridColumn: 'span 2' }}>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#374151', marginBottom: 4 }}>
                Archivo Comprobante (PDF o Imagen)
              </label>
              <input
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,.webp"
                onChange={e => setArchivoAdjunto(e.target.files ? e.target.files[0] : null)}
                style={{
                  fontSize: '0.875rem',
                  color: '#4b5563',
                }}
              />
            </div>

            <div style={{ gridColumn: 'span 2' }}>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#374151', marginBottom: 4 }}>
                Observación / Glosa (opcional)
              </label>
              <input
                type="text"
                value={observacion}
                onChange={e => setObservacion(e.target.value)}
                placeholder="Notas de recepción, condición de entrega, etc."
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                }}
              />
            </div>
          </div>

          {/* Tabla de ítems de compra */}
          <div style={{ marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: '#1f2937' }}>
                Detalle de Productos Comprados
              </h3>
              <button
                type="button"
                onClick={handleAgregarFila}
                style={{
                  backgroundColor: '#f3f4f6',
                  color: '#1f2937',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  padding: '0.35rem 0.75rem',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                + Agregar Producto
              </button>
            </div>

            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', textAlign: 'left' }}>
                  <th style={{ padding: '0.5rem' }}>Producto</th>
                  <th style={{ padding: '0.5rem', width: '180px' }}>Presentación</th>
                  <th style={{ padding: '0.5rem', width: '100px' }}>Cantidad</th>
                  <th style={{ padding: '0.5rem', width: '120px' }}>Precio Unit. (S/)</th>
                  <th style={{ padding: '0.5rem', width: '100px', textAlign: 'right' }}>Subtotal</th>
                  <th style={{ padding: '0.5rem', width: '40px' }}></th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, idx) => {
                  const cant = parseFloat(item.cantidad) || 0
                  const precio = parseFloat(item.precioUnitarioSoles) || 0
                  const subtotal = cant * precio

                  return (
                    <tr key={item.tempId} style={{ borderBottom: '1px solid #f3f4f6' }}>
                      <td style={{ padding: '0.5rem' }}>
                        <select
                          value={item.productoId}
                          onChange={e => handleCambioProducto(idx, e.target.value)}
                          style={{
                            width: '100%',
                            padding: '0.4rem',
                            border: '1px solid #d1d5db',
                            borderRadius: '4px',
                            fontSize: '0.8125rem',
                          }}
                        >
                          <option value="">-- Seleccionar producto --</option>
                          {productosCatalogo.map(p => (
                            <option key={p.id} value={p.id}>
                              {p.nombre} ({p.codigo})
                            </option>
                          ))}
                        </select>
                      </td>

                      <td style={{ padding: '0.5rem' }}>
                        <select
                          value={item.presentacionId}
                          onChange={e => {
                            const nuevos = [...items]
                            nuevos[idx].presentacionId = e.target.value
                            setItems(nuevos)
                          }}
                          disabled={!item.productoId}
                          style={{
                            width: '100%',
                            padding: '0.4rem',
                            border: '1px solid #d1d5db',
                            borderRadius: '4px',
                            fontSize: '0.8125rem',
                          }}
                        >
                          <option value="">Unidad base</option>
                          {item.presentacionesDisponibles.map(p => (
                            <option key={p.id} value={p.id}>
                              {p.nombre} (x{p.factor})
                            </option>
                          ))}
                        </select>
                      </td>

                      <td style={{ padding: '0.5rem' }}>
                        <input
                          type="number"
                          step="0.001"
                          min="0.001"
                          value={item.cantidad}
                          onChange={e => {
                            const nuevos = [...items]
                            nuevos[idx].cantidad = e.target.value
                            setItems(nuevos)
                          }}
                          style={{
                            width: '100%',
                            padding: '0.4rem',
                            border: '1px solid #d1d5db',
                            borderRadius: '4px',
                            fontSize: '0.8125rem',
                          }}
                        />
                      </td>

                      <td style={{ padding: '0.5rem' }}>
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          placeholder="0.00"
                          value={item.precioUnitarioSoles}
                          onChange={e => {
                            const nuevos = [...items]
                            nuevos[idx].precioUnitarioSoles = e.target.value
                            setItems(nuevos)
                          }}
                          style={{
                            width: '100%',
                            padding: '0.4rem',
                            border: '1px solid #d1d5db',
                            borderRadius: '4px',
                            fontSize: '0.8125rem',
                          }}
                        />
                      </td>

                      <td style={{ padding: '0.5rem', textAlign: 'right', fontWeight: 600, color: '#111827' }}>
                        S/ {subtotal.toFixed(2)}
                      </td>

                      <td style={{ padding: '0.5rem', textAlign: 'center' }}>
                        <button
                          type="button"
                          onClick={() => handleEliminarFila(idx)}
                          disabled={items.length <= 1}
                          style={{
                            background: 'none',
                            border: 'none',
                            color: items.length <= 1 ? '#d1d5db' : '#ef4444',
                            cursor: items.length <= 1 ? 'not-allowed' : 'pointer',
                            fontSize: '1rem',
                            fontWeight: 'bold',
                          }}
                        >
                          🗑️
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {/* Resumen total */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              alignItems: 'baseline',
              gap: '1rem',
              padding: '1rem',
              backgroundColor: '#f3f4f6',
              borderRadius: '8px',
            }}
          >
            <span style={{ fontSize: '1rem', fontWeight: 600, color: '#374151' }}>TOTAL COMPRA:</span>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color: '#047857' }}>
              S/ {calcularTotalSoles().toFixed(2)}
            </span>
          </div>
        </div>

        {/* Botones de acción */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            borderTop: '1px solid #e5e7eb',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '0.75rem',
            backgroundColor: '#f9fafb',
            borderBottomLeftRadius: '12px',
            borderBottomRightRadius: '12px',
          }}
        >
          <button
            type="button"
            onClick={alCerrar}
            disabled={cargando}
            style={{
              padding: '0.625rem 1.25rem',
              border: '1px solid #d1d5db',
              borderRadius: '6px',
              backgroundColor: '#fff',
              color: '#374151',
              fontWeight: 500,
              cursor: 'pointer',
            }}
          >
            Cancelar
          </button>

          <button
            type="button"
            onClick={() => guardar(false)}
            disabled={cargando}
            style={{
              padding: '0.625rem 1.25rem',
              border: '1px solid #d1d5db',
              borderRadius: '6px',
              backgroundColor: '#f3f4f6',
              color: '#1f2937',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {cargando ? 'Guardando...' : 'Guardar Borrador'}
          </button>

          <button
            type="button"
            onClick={() => guardar(true)}
            disabled={cargando}
            style={{
              padding: '0.625rem 1.25rem',
              border: 'none',
              borderRadius: '6px',
              backgroundColor: '#059669',
              color: '#fff',
              fontWeight: 600,
              cursor: 'pointer',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
            }}
          >
            {cargando ? 'Confirmando...' : 'Confirmar e Ingresar Stock'}
          </button>
        </div>
      </div>
    </div>
  )
}
