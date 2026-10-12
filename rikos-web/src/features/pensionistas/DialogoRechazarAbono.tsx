import { useEffect, useState } from 'react'
import { Boton, EstadoCargando, Modal, Select } from '../../comun/componentes'
import { listarMotivos, type FilaAbono, type Motivo } from '../../api/cliente'
import { enSoles } from './dinero'

interface Props {
  abono: FilaAbono | null
  abierto: boolean
  token: string
  confirmando?: boolean
  onConfirmar: (motivoId: string, observacion: string) => void
  onCancelar: () => void
}

export function DialogoRechazarAbono({
  abono,
  abierto,
  token,
  confirmando = false,
  onConfirmar,
  onCancelar,
}: Props) {
  const [motivos, setMotivos] = useState<Motivo[]>([])
  const [cargandoMotivos, setCargandoMotivos] = useState(false)
  const [motivoId, setMotivoId] = useState('')
  const [observacion, setObservacion] = useState('')
  const [errorLocal, setErrorLocal] = useState('')

  useEffect(() => {
    if (!abierto) {
      setMotivoId('')
      setObservacion('')
      setErrorLocal('')
      return
    }

    setCargandoMotivos(true)
    listarMotivos(token, 'rechazo_abono')
      .then((lista) => {
        setMotivos(lista)
        if (lista.length > 0 && !motivoId) {
          setMotivoId(lista[0].id)
        }
      })
      .catch(() => {
        setErrorLocal('No se pudo cargar el catálogo de motivos de rechazo.')
      })
      .finally(() => setCargandoMotivos(false))
  }, [abierto, token])

  if (!abono) return null

  const motivoSeleccionado = motivos.find((m) => m.id === motivoId)
  const observacionObligatoria = Boolean(motivoSeleccionado?.requiereObservacion)
  const puedeConfirmar =
    Boolean(motivoId) && (!observacionObligatoria || observacion.trim().length > 0) && !confirmando

  function manejarConfirmacion() {
    if (!puedeConfirmar) return
    onConfirmar(motivoId, observacion.trim())
  }

  return (
    <Modal
      abierto={abierto}
      titulo="Rechazar Abono Digital"
      onCerrar={onCancelar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCancelar} disabled={confirmando}>
            Cancelar
          </Boton>
          <Boton
            variante="peligro"
            disabled={!puedeConfirmar}
            cargando={confirmando}
            onClick={manejarConfirmacion}
          >
            Confirmar rechazo
          </Boton>
        </>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {/* Resumen del abono que se va a rechazar */}
        <div
          style={{
            padding: '12px 14px',
            backgroundColor: '#FEF2F2',
            border: '1px solid #FECACA',
            borderRadius: '8px',
            fontSize: '0.88rem',
            color: '#991B1B',
          }}
        >
          <div style={{ fontWeight: 600 }}>
            {abono.pensionistaNombre} (DNI: {abono.pensionistaDni})
          </div>
          <div style={{ marginTop: '4px', fontSize: '0.82rem', color: '#B91C1C' }}>
            Monto: <strong>{enSoles(abono.monto)}</strong> · {abono.medio} (Op: {abono.numeroOperacion || 's/n'})
          </div>
          <p style={{ marginTop: '6px', fontSize: '0.78rem', color: '#7F1D1D', margin: '6px 0 0' }}>
            El rechazo no afectará la deuda ni el saldo del pensionista (CA3 / RN-092).
          </p>
        </div>

        {cargandoMotivos ? (
          <EstadoCargando mensaje="Cargando motivos de rechazo..." />
        ) : errorLocal ? (
          <div style={{ color: '#DC2626', fontSize: '0.88rem' }}>{errorLocal}</div>
        ) : (
          <>
            <Select
              etiqueta="Motivo de rechazo (formal)"
              value={motivoId}
              onChange={(e) => setMotivoId(e.target.value)}
            >
              {motivos.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.descripcion}{m.requiereObservacion ? ' (exige detalle)' : ''}
                </option>
              ))}
            </Select>

            <div className={`campo ${observacionObligatoria && observacion.trim().length === 0 ? 'campo-invalido' : ''}`}>
              <label htmlFor="obs-rechazo-abono" className="campo-etiqueta">
                {observacionObligatoria
                  ? 'Observación / Detalle adicional (obligatorio)'
                  : 'Observación (opcional)'}
              </label>
              <textarea
                id="obs-rechazo-abono"
                className="campo-control"
                value={observacion}
                onChange={(e) => setObservacion(e.target.value)}
                placeholder="Explica la discrepancia bancaria, monto o datos incompletos..."
                rows={3}
              />
              {observacionObligatoria && observacion.trim().length === 0 && (
                <p className="campo-error">Este motivo exige ingresar un detalle u observación.</p>
              )}
            </div>
          </>
        )}
      </div>
    </Modal>
  )
}
