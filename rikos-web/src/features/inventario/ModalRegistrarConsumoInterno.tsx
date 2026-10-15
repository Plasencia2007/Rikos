import { useEffect, useMemo, useState } from 'react'
import { Modal, Boton, Aviso } from '../../comun/componentes'
import {
  listarUsuarios,
  registrarConsumoInterno,
  type Usuario,
  type FilaExistencia,
  type FilaConsumoInterno,
  type RegistroConsumoInternoComando,
} from '../../api/cliente'

interface Props {
  abierto: boolean
  token: string
  localId: string
  existencias: FilaExistencia[]
  onGuardado: (consumo: FilaConsumoInterno) => void
  onCerrar: () => void
}

export function ModalRegistrarConsumoInterno({
  abierto,
  token,
  localId,
  existencias,
  onGuardado,
  onCerrar,
}: Props) {
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [cargandoUsuarios, setCargandoUsuarios] = useState(false)
  const [usuarioId, setUsuarioId] = useState('')
  const [productoId, setProductoId] = useState('')
  const [servicio, setServicio] = useState('almuerzo')
  const [cantidad, setCantidad] = useState('1')
  const [fecha, setFecha] = useState(new Date().toISOString().slice(0, 10))
  const [turno, setTurno] = useState('dia')
  const [esSegundaComida, setEsSegundaComida] = useState(false)
  const [supervisorPin, setSupervisorPin] = useState('')
  const [observacion, setObservacion] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!abierto || !token) return
    setError('')
    setCantidad('1')
    setServicio('almuerzo')
    setTurno('dia')
    setEsSegundaComida(false)
    setSupervisorPin('')
    setObservacion('')

    async function cargar() {
      try {
        setCargandoUsuarios(true)
        const lista = await listarUsuarios(token)
        const activos = lista.filter((u) => u.activo)
        setUsuarios(activos)
        if (activos.length > 0 && !usuarioId) {
          setUsuarioId(activos[0].id)
        }
      } catch {
        // En caso de error manteniendo lista previa
      } finally {
        setCargandoUsuarios(false)
      }
    }
    cargar()

    if (existencias.length > 0 && !productoId) {
      setProductoId(existencias[0].productoId)
    }
  }, [abierto, token, existencias])

  // Producto seleccionado
  const productoActual = useMemo(() => {
    return existencias.find((e) => e.productoId === productoId) ?? null
  }, [existencias, productoId])

  // Trabajador seleccionado
  const usuarioActual = useMemo(() => {
    return usuarios.find((u) => u.id === usuarioId) ?? null
  }, [usuarios, usuarioId])

  if (!abierto) return null

  const valorNumCantidad = parseFloat(cantidad) || 0

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    setError('')

    if (!usuarioId) {
      setError('Debe seleccionar un trabajador/personal beneficiario.')
      return
    }

    if (!productoId) {
      setError('Debe seleccionar el plato o alimento consumido.')
      return
    }

    if (isNaN(valorNumCantidad) || valorNumCantidad <= 0) {
      setError('La cantidad consumida debe ser mayor a cero.')
      return
    }

    if (esSegundaComida && !supervisorPin.trim() && !observacion.trim()) {
      setError('La 2da comida en el mismo turno requiere ingresar el PIN del supervisor o una justificación aprobada.')
      return
    }

    try {
      setGuardando(true)
      const comando: RegistroConsumoInternoComando = {
        usuarioId,
        fecha,
        servicio,
        turno,
        productoId,
        cantidad: valorNumCantidad,
        observacion: observacion.trim() || undefined,
        supervisorPin: supervisorPin.trim() || undefined,
      }

      const creado = await registrarConsumoInterno(token, localId, comando)
      onGuardado(creado)
      onCerrar()
    } catch (err: any) {
      const msg = err?.cuerpo?.error || err.message || 'Error al registrar el consumo interno.'
      setError(msg)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Modal
      abierto={abierto}
      onCerrar={onCerrar}
      titulo="Registrar Comida de Personal (US-051)"
      pie={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', width: '100%' }}>
          <Boton variante="secundario" onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton variante="primario" onClick={() => handleSubmit()} cargando={guardando}>
            Confirmar Consumo Interno
          </Boton>
        </div>
      }
    >
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {error && <Aviso tono="error">{error}</Aviso>}

        {/* Trabajador Beneficiario */}
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
            Personal Beneficiario <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <select
            value={usuarioId}
            onChange={(e) => setUsuarioId(e.target.value)}
            disabled={guardando || cargandoUsuarios}
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
            {usuarios.length === 0 ? (
              <option value="">{cargandoUsuarios ? 'Cargando personal...' : 'Sin trabajadores encontrados'}</option>
            ) : (
              usuarios.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.nombres} {u.apellidos} (@{u.nombreUsuario})
                </option>
              ))
            )}
          </select>
          {usuarioActual && (
            <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
              Rol / Cargos: {usuarioActual.roles?.map((r) => r.nombre).join(', ') || 'Personal operativo'}
            </div>
          )}
        </div>

        {/* Plato o Comida Preparada */}
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
            Plato o Preparación Consumida <span style={{ color: '#ef4444' }}>*</span>
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
            <option value="">-- Seleccionar plato o comida --</option>
            {existencias.map((e) => (
              <option key={e.productoId} value={e.productoId}>
                [{e.codigo}] {e.nombre} — Stock: {e.stockActual} {e.unidadCodigo || e.unidadNombre}
              </option>
            ))}
          </select>
          {productoActual && (
            <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
              Stock actual en almacén: <strong>{productoActual.stockActual} {productoActual.unidadCodigo || productoActual.unidadNombre}</strong>
            </div>
          )}
        </div>

        {/* Fila: Fecha y Servicio */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
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
              Fecha de Consumo
            </label>
            <input
              type="date"
              value={fecha}
              onChange={(e) => setFecha(e.target.value)}
              style={{
                width: '100%',
                padding: '0.55rem 0.75rem',
                borderRadius: '0.375rem',
                border: '1px solid #cbd5e1',
                fontSize: '0.875rem',
                backgroundColor: '#ffffff',
                color: '#0f172a',
              }}
            />
          </div>

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
              Servicio
            </label>
            <select
              value={servicio}
              onChange={(e) => setServicio(e.target.value)}
              style={{
                width: '100%',
                padding: '0.55rem 0.75rem',
                borderRadius: '0.375rem',
                border: '1px solid #cbd5e1',
                fontSize: '0.875rem',
                backgroundColor: '#ffffff',
                color: '#0f172a',
              }}
            >
              <option value="desayuno">☕ Desayuno</option>
              <option value="almuerzo">🍲 Almuerzo</option>
              <option value="cena">🌙 Cena</option>
              <option value="refrigerio">🥪 Refrigerio / Colación</option>
            </select>
          </div>
        </div>

        {/* Fila: Turno y Cantidad */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
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
              Turno
            </label>
            <select
              value={turno}
              onChange={(e) => setTurno(e.target.value)}
              style={{
                width: '100%',
                padding: '0.55rem 0.75rem',
                borderRadius: '0.375rem',
                border: '1px solid #cbd5e1',
                fontSize: '0.875rem',
                backgroundColor: '#ffffff',
                color: '#0f172a',
              }}
            >
              <option value="dia">Día / Mañana</option>
              <option value="tarde">Tarde</option>
              <option value="noche">Noche</option>
            </select>
          </div>

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
              Cantidad / Porciones
            </label>
            <input
              type="number"
              step="0.5"
              min="0.5"
              value={cantidad}
              onChange={(e) => setCantidad(e.target.value)}
              style={{
                width: '100%',
                padding: '0.55rem 0.75rem',
                borderRadius: '0.375rem',
                border: '1px solid #cbd5e1',
                fontSize: '0.875rem',
                backgroundColor: '#ffffff',
                color: '#0f172a',
              }}
            />
          </div>
        </div>

        {/* Checkbox de 2da Comida o Excepción */}
        <div
          style={{
            backgroundColor: esSegundaComida ? '#fffbeb' : '#f8fafc',
            border: `1px solid ${esSegundaComida ? '#fde68a' : '#e2e8f0'}`,
            borderRadius: '0.5rem',
            padding: '0.75rem',
          }}
        >
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.875rem', fontWeight: 600, color: '#334155' }}>
            <input
              type="checkbox"
              checked={esSegundaComida}
              onChange={(e) => setEsSegundaComida(e.target.checked)}
            />
            <span>¿Es 2da comida en el mismo turno o requiere autorización de supervisor?</span>
          </label>

          {esSegundaComida && (
            <div style={{ marginTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <div style={{ fontSize: '0.75rem', color: '#b45309' }}>
                ⚠️ La política de personal contempla 1 comida libre de cobro por turno. Para segundas raciones se requiere PIN o autorización de supervisor.
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, color: '#334155', marginBottom: '0.25rem' }}>
                  PIN de Supervisor (4 dígitos)
                </label>
                <input
                  type="password"
                  maxLength={6}
                  value={supervisorPin}
                  onChange={(e) => setSupervisorPin(e.target.value)}
                  placeholder="Ingrese PIN de supervisor..."
                  style={{
                    width: '100%',
                    padding: '0.5rem 0.75rem',
                    borderRadius: '0.375rem',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.875rem',
                    backgroundColor: '#ffffff',
                    color: '#0f172a',
                  }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Observaciones */}
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
            Observación adicional (opcional)
          </label>
          <textarea
            rows={2}
            value={observacion}
            onChange={(e) => setObservacion(e.target.value)}
            placeholder="Notas sobre el plato servido o justificación de turno extendido..."
            style={{
              width: '100%',
              padding: '0.5rem 0.75rem',
              borderRadius: '0.375rem',
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
