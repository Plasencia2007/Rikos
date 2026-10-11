import { useEffect, useState } from 'react'
import { Aviso, Boton, Campo, EstadoCargando, Modal, Select } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import {
  cerrarTurnoDeCaja,
  listarMotivos,
  reabrirCobro,
  resumenDelTurno,
  type CierreDeCaja,
  type CobroDelTurno,
  type Motivo,
  type ResumenDeTurno,
} from '../../api/cliente'
import { mensajeDeCaja, soles } from './mensajes'

/**
 * US-036: cerrar el turno. Se ve el efectivo ESPERADO antes de contar (RN-067); el cajero escribe lo que contó y la diferencia sale
 * al instante. Una diferencia (desde S/ 0,01, a favor o en contra) avisa, pero nunca impide cerrar (RN-070). Un cierre no se modifica.
 */
export function DialogoCierre({
  token,
  onCerrar,
  onCerrado,
}: {
  token: string
  onCerrar: () => void
  /** Despues de ver el resultado del cierre. */
  onCerrado: () => void
}) {
  const [resumen, setResumen] = useState<ResumenDeTurno | null>(null)
  const [contado, setContado] = useState('')
  const [observacion, setObservacion] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [aviso, setAviso] = useState('')
  const [cierre, setCierre] = useState<CierreDeCaja | null>(null)

  useEffect(() => {
    resumenDelTurno(token)
      .then(setResumen)
      .catch((e) => setAviso(mensajeDeCaja(e, 'No se pudo calcular el turno.')))
  }, [token])

  const centimos = soles(contado)
  const valido = centimos !== null && !Number.isNaN(centimos)
  const diferencia = resumen && valido && centimos !== null ? centimos - resumen.efectivoEsperado : null

  async function cerrar() {
    if (!valido || centimos === null) return
    setTrabajando(true)
    setAviso('')
    try {
      setCierre(await cerrarTurnoDeCaja(token, centimos, observacion.trim() || null))
    } catch (e) {
      setAviso(mensajeDeCaja(e, 'No se pudo cerrar el turno. Inténtalo de nuevo.'))
    } finally {
      setTrabajando(false)
    }
  }

  if (cierre) {
    return (
      <Modal abierto titulo="Turno cerrado" onCerrar={onCerrado} pie={<Boton onClick={onCerrado}>Listo</Boton>}>
        <div className="caja-cierre-resultado" role="status">
          <dl className="caja-totales">
            <div>
              <dt>Esperado</dt>
              <dd>{enSoles(cierre.efectivoEsperado)}</dd>
            </div>
            <div>
              <dt>Contado</dt>
              <dd>{enSoles(cierre.efectivoContado)}</dd>
            </div>
            <div className="caja-total-grande">
              <dt>Diferencia</dt>
              <dd>{cierre.diferencia === 0 ? 'Sin diferencia' : `${cierre.diferencia > 0 ? '+' : '−'} ${enSoles(Math.abs(cierre.diferencia))}`}</dd>
            </div>
          </dl>
          {cierre.alerta ? (
            <Aviso tono="error">
              {cierre.diferencia < 0 ? 'Falta dinero en la caja.' : 'Sobra dinero en la caja.'} El cierre quedó registrado con la diferencia y no se puede modificar.
            </Aviso>
          ) : (
            <Aviso>La caja cuadra. El cierre quedó registrado y no se puede modificar.</Aviso>
          )}
        </div>
      </Modal>
    )
  }

  return (
    <Modal
      abierto
      titulo="Cerrar turno"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={trabajando} disabled={!resumen || !valido} onClick={cerrar}>
            Cerrar turno
          </Boton>
        </>
      }
    >
      {!resumen && !aviso && <EstadoCargando />}
      {aviso && <Aviso tono="error">{aviso}</Aviso>}
      {resumen && (
        <div className="ped-dialogo">
          <dl className="caja-totales">
            <div>
              <dt>Fondo inicial</dt>
              <dd>{enSoles(resumen.fondoInicial)}</dd>
            </div>
            <div>
              <dt>Cobrado en efectivo</dt>
              <dd>{enSoles(resumen.efectivoCobrado)}</dd>
            </div>
            <div className="caja-total-grande">
              <dt>Efectivo esperado en caja</dt>
              <dd>{enSoles(resumen.efectivoEsperado)}</dd>
            </div>
            <div>
              <dt>Yape · Plin · Tarjeta</dt>
              <dd>
                {enSoles(resumen.totalYape)} · {enSoles(resumen.totalPlin)} · {enSoles(resumen.totalTarjeta)}
              </dd>
            </div>
          </dl>
          <Campo
            etiqueta="Efectivo contado (S/)"
            inputMode="decimal"
            placeholder="0.00"
            value={contado}
            error={contado !== '' && !valido ? 'Escribe un monto válido.' : undefined}
            onChange={(e) => setContado(e.target.value)}
          />
          {diferencia !== null && (
            <p className={`caja-diferencia ${diferencia !== 0 ? 'caja-falta' : ''}`} role="status">
              {diferencia === 0
                ? 'La caja cuadra'
                : diferencia < 0
                  ? `Faltan ${enSoles(-diferencia)}`
                  : `Sobran ${enSoles(diferencia)}`}
            </p>
          )}
          <Campo
            etiqueta="Observación (opcional)"
            value={observacion}
            maxLength={500}
            onChange={(e) => setObservacion(e.target.value)}
          />
        </div>
      )}
    </Modal>
  )
}

/** US-035: reabrir un cobro (solo mientras el turno siga abierto): pide el motivo de reapertura. */
export function DialogoReabrir({
  token,
  cobro,
  onCerrar,
  onReabierto,
}: {
  token: string
  cobro: CobroDelTurno
  onCerrar: () => void
  onReabierto: () => void
}) {
  const [motivos, setMotivos] = useState<Motivo[] | null>(null)
  const [motivoId, setMotivoId] = useState('')
  const [observacion, setObservacion] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [aviso, setAviso] = useState('')

  useEffect(() => {
    listarMotivos(token, 'reapertura')
      .then((lista) => {
        const activos = lista.filter((m) => m.activo)
        setMotivos(activos)
        if (activos.length > 0) setMotivoId(activos[0].id)
      })
      .catch(() => setMotivos([]))
  }, [token])

  const motivo = motivos?.find((m) => m.id === motivoId)
  const faltaNota = Boolean(motivo?.requiereObservacion) && observacion.trim() === ''

  async function reabrir() {
    setTrabajando(true)
    setAviso('')
    try {
      await reabrirCobro(token, cobro.id, motivoId, observacion.trim() || null)
      onReabierto()
    } catch (e) {
      setAviso(mensajeDeCaja(e, 'No se pudo reabrir el cobro.'))
    } finally {
      setTrabajando(false)
    }
  }

  return (
    <Modal
      abierto
      titulo={`Reabrir el cobro del pedido ${cobro.pedidoNumero}`}
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton variante="peligro" cargando={trabajando} disabled={!motivoId || faltaNota} onClick={reabrir}>
            Reabrir cobro
          </Boton>
        </>
      }
    >
      {!motivos ? (
        <EstadoCargando />
      ) : motivos.length === 0 ? (
        <Aviso>No hay un motivo de reapertura habilitado. Pídele al administrador que cree uno en Configuración → Motivos.</Aviso>
      ) : (
        <div className="ped-dialogo">
          <p>
            Se devuelven los productos a «en cocina», el dinero de {enSoles(cobro.total)} sale de la caja y, si había un pensionista, se le
            devuelve su cargo. Los productos se pueden volver a cobrar.
          </p>
          {aviso && <Aviso tono="error">{aviso}</Aviso>}
          <Select etiqueta="Motivo" value={motivoId} onChange={(e) => setMotivoId(e.target.value)}>
            {motivos.map((m) => (
              <option key={m.id} value={m.id}>
                {m.descripcion}
              </option>
            ))}
          </Select>
          <Campo
            etiqueta={motivo?.requiereObservacion ? 'Observación (obligatoria)' : 'Observación (opcional)'}
            value={observacion}
            maxLength={250}
            onChange={(e) => setObservacion(e.target.value)}
          />
        </div>
      )}
    </Modal>
  )
}
