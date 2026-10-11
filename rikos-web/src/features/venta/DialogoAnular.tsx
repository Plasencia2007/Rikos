import { useEffect, useState } from 'react'
import { Aviso, Boton, Campo, EstadoCargando, EstadoVacio, Modal, Select } from '../../comun/componentes'
import { listarMotivos, type Motivo } from '../../api/cliente'

/**
 * US-031: anular una linea que ya salio a cocina. Pide un motivo de los configurables (tipo "anulacion") y, si el
 * motivo lo exige, una nota. No se cobra; queda marcada "anulada despues de comandar" porque la receta ya se gasto.
 */
export function DialogoAnular({
  token,
  producto,
  trabajando,
  onAnular,
  onCerrar,
}: {
  token: string
  producto: string
  trabajando: boolean
  onAnular: (motivoId: string, observacion: string | null) => void
  onCerrar: () => void
}) {
  const [motivos, setMotivos] = useState<Motivo[] | null>(null)
  const [errorDeCarga, setErrorDeCarga] = useState('')
  const [motivoId, setMotivoId] = useState('')
  const [nota, setNota] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    listarMotivos(token, 'anulacion')
      .then((lista) => setMotivos(lista.filter((m) => m.activo)))
      .catch(() => setErrorDeCarga('No se pudieron cargar los motivos. Revisa la conexión e inténtalo otra vez.'))
  }, [token])

  const elegido = motivos?.find((m) => m.id === motivoId)

  function confirmar() {
    if (!elegido) return setError('Elige el motivo de la anulación.')
    if (elegido.requiereObservacion && nota.trim() === '') return setError('Este motivo pide que escribas una nota.')
    onAnular(elegido.id, nota.trim() || null)
  }

  return (
    <Modal
      abierto
      titulo="Anular producto"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton variante="peligro" cargando={trabajando} disabled={!motivos || motivos.length === 0} onClick={confirmar}>
            Anular
          </Boton>
        </>
      }
    >
      <div className="ped-dialogo">
        <p>
          Vas a anular <strong>{producto}</strong>. Ya salió a cocina: no se cobra, pero queda registrado como pérdida.
        </p>
        {errorDeCarga && <Aviso tono="error">{errorDeCarga}</Aviso>}
        {error && <Aviso tono="error">{error}</Aviso>}
        {!motivos && !errorDeCarga && <EstadoCargando />}
        {motivos && motivos.length === 0 && (
          <EstadoVacio
            titulo="No hay motivos de anulación"
            descripcion="Pídele al administrador que cree al menos uno en Configuración → Motivos (tipo anulación)."
          />
        )}
        {motivos && motivos.length > 0 && (
          <>
            <Select
              etiqueta="Motivo"
              value={motivoId}
              onChange={(e) => {
                setError('')
                setMotivoId(e.target.value)
              }}
            >
              <option value="">Elige un motivo…</option>
              {motivos.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.descripcion}
                </option>
              ))}
            </Select>
            <Campo
              etiqueta={elegido?.requiereObservacion ? 'Nota (obligatoria)' : 'Nota (opcional)'}
              maxLength={250}
              value={nota}
              onChange={(e) => setNota(e.target.value)}
            />
          </>
        )}
      </div>
    </Modal>
  )
}
