import { useState } from 'react'
import { Boton, Campo, Modal } from '../../comun/componentes'
import { configurarStockMinimo, type FilaExistencia } from '../../api/cliente'

interface Props {
  existencia: FilaExistencia | null
  abierto: boolean
  token: string
  localId: string
  onGuardado: (actualizada: FilaExistencia) => void
  onCerrar: () => void
}

export function DialogoStockMinimo({
  existencia,
  abierto,
  token,
  localId,
  onGuardado,
  onCerrar,
}: Props) {
  const [valor, setValor] = useState<string>(
    existencia?.stockMinimo != null ? String(existencia.stockMinimo) : '',
  )
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  if (!existencia || !abierto) return null

  const unidad = existencia.unidadCodigo || existencia.unidadNombre

  async function manejarGuardar(e?: React.FormEvent) {
    if (e) e.preventDefault()
    if (!existencia) return
    setError('')

    let nuevoMinimo: number | null = null
    if (valor.trim() !== '') {
      const num = Number(valor.trim())
      if (isNaN(num) || num < 0) {
        setError('El stock mínimo debe ser un número mayor o igual a 0.')
        return
      }
      nuevoMinimo = num
    }

    try {
      setGuardando(true)
      const actualizada = await configurarStockMinimo(
        token,
        localId,
        existencia.productoId,
        nuevoMinimo,
      )
      onGuardado(actualizada)
      onCerrar()
    } catch (err: any) {
      setError(err?.cuerpo?.error ?? 'Error al guardar el stock mínimo.')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Modal
      abierto={abierto}
      titulo="Configurar Stock Mínimo"
      onCerrar={onCerrar}
      pie={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', width: '100%' }}>
          <Boton variante="secundario" onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton variante="primario" onClick={manejarGuardar} cargando={guardando}>
            Guardar Umbral
          </Boton>
        </div>
      }
    >
      <form onSubmit={manejarGuardar} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div>
          <div style={{ fontSize: '0.875rem', color: '#64748b' }}>
            Producto:
          </div>
          <div style={{ fontSize: '1.125rem', fontWeight: 600, color: '#0f172a' }}>
            {existencia.nombre}
          </div>
          <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '0.15rem' }}>
            Código: <span style={{ fontFamily: 'monospace' }}>{existencia.codigo}</span> | Categoría: {existencia.categoriaNombre}
          </div>
        </div>

        <div style={{ backgroundColor: '#f8fafc', padding: '0.75rem 1rem', borderRadius: '0.5rem', border: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '0.875rem', color: '#64748b' }}>Stock actual registrado:</span>
          <span style={{ fontWeight: 700, fontSize: '1rem', color: existencia.stockActual < 0 ? '#ef4444' : '#059669' }}>
            {existencia.stockActual} {unidad}
          </span>
        </div>

        <Campo
          etiqueta={`Stock Mínimo en unidad base (${unidad})`}
          ayuda="Al descender a esta cantidad o menos, el sistema emitirá una alerta visual sin bloquear ventas ni despachos (RN-051)."
          error={error}
          type="number"
          step="any"
          min="0"
          placeholder="Ej: 10.00"
          value={valor}
          onChange={(e) => setValor(e.target.value)}
          sufijo={
            valor !== '' ? (
              <button
                type="button"
                onClick={() => setValor('')}
                title="Limpiar stock mínimo"
                style={{
                  border: 'none',
                  background: 'transparent',
                  color: '#64748b',
                  cursor: 'pointer',
                  fontSize: '0.8rem',
                  padding: '0 0.5rem',
                  fontWeight: 600,
                }}
              >
                Sin mínimo
              </button>
            ) : undefined
          }
        />
      </form>
    </Modal>
  )
}
