import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoFlechaIzquierda,
  Modal,
  Select,
} from '../../comun/componentes'
import {
  ErrorApi,
  cargarSaldoInicial,
  fijarTopeDeCredito,
  listarMotivos,
  obtenerCuenta,
  obtenerPensionista,
  registrarAjusteDeCuenta,
  type CuentaDePensionista,
  type DetallePensionista,
  type Motivo,
  type MovimientoDeCuenta,
  type SignoMovimiento,
  type TipoMovimientoCuenta,
} from '../../api/cliente'
import { aCentimos, desdeCentimos, enSoles } from './dinero'

/**
 * US-018: la cuenta corriente de un pensionista. Una sola cuenta, valida en
 * todos los locales: cada movimiento dice donde ocurrio. El tope es un
 * RECORDATORIO para cobrar la deuda, nunca un limite: por eso el texto nunca
 * dice "excedido" ni "bloqueado". El saldo inicial lo carga solo la oficina
 * central (pensionistas.administrar), una vez; ajustar y fijar el tope pide
 * pensionistas.modificar; el resto solo ve.
 */

const CONCEPTO: Record<TipoMovimientoCuenta, string> = {
  consumo: 'Consumo',
  abono: 'Abono',
  ajuste: 'Ajuste',
  arrastre: 'Arrastre del mes anterior',
  saldo_inicial: 'Saldo inicial',
}

const AVISO_TOPE: Record<string, string> = {
  PLAN_SIN_PRECIO:
    'No se pudo calcular el tope: al plan le falta el precio de alguna comida que este pensionista suele tomar.',
  SIN_DIAS_DE_ATENCION:
    'No se pudo calcular el tope: ningún local tiene horarios de atención cargados para este mes.',
}

const FORMATO_FECHA = new Intl.DateTimeFormat('es-PE', {
  timeZone: 'America/Lima',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

function mensajeDe(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    switch (error.cuerpo?.error) {
      case 'SALDO_INICIAL_YA_REGISTRADO':
        return 'Esta cuenta ya tiene su saldo inicial. Si hubo un error, corrígelo con un ajuste.'
      case 'FECHA_FUTURA':
        return 'La fecha de corte no puede ser futura.'
      case 'IMPORTE_INVALIDO':
        return 'El importe tiene que ser mayor que cero.'
      case 'MOTIVO_OBLIGATORIO':
      case 'MOTIVO_INVALIDO':
        return 'Elige un motivo de ajuste de cuenta que esté activo.'
      case 'OBSERVACION_OBLIGATORIA':
        return 'Ese motivo pide una observación.'
      case 'SIN_LOCAL_ACTIVO':
        return 'Elige primero el local en el que estás trabajando.'
      case 'LIMITE_INVALIDO':
        return 'El tope fijado tiene que ser mayor que cero.'
      case 'NO_ENCONTRADO':
        return 'No se encontró la cuenta.'
    }
    if (error.estado === 403) return 'No tienes permiso para hacer esto.'
  }
  return porDefecto
}

type Dialogo = 'saldo' | 'ajuste' | 'tope' | null

export function PantallaCuentaPensionista() {
  const { id } = useParams<{ id: string }>()
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()
  const puedeAjustar = tienePermiso('pensionistas.modificar')
  const puedeSaldoInicial = tienePermiso('pensionistas.administrar')

  const [pensionista, setPensionista] = useState<DetallePensionista | null>(null)
  const [cuenta, setCuenta] = useState<CuentaDePensionista | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const [filtroLocal, setFiltroLocal] = useState('')

  const cargar = useCallback(() => {
    if (!id) return
    setCargando(true)
    setError('')
    Promise.all([obtenerPensionista(token, id), obtenerCuenta(token, id)])
      .then(([p, c]) => {
        setPensionista(p)
        setCuenta(c)
      })
      .catch((e) =>
        setError(e instanceof ErrorApi && e.estado === 404 ? 'No existe ese pensionista.' : 'No se pudo cargar la cuenta.'),
      )
      .finally(() => setCargando(false))
  }, [id, token])

  useEffect(cargar, [cargar])

  const locales = useMemo(() => {
    const vistos = new Map<string, string>()
    cuenta?.movimientos.forEach((m) => vistos.set(m.localId, m.localNombre ?? m.localId))
    return [...vistos.entries()]
  }, [cuenta])

  const movimientos = useMemo(
    () => (cuenta?.movimientos ?? []).filter((m) => !filtroLocal || m.localId === filtroLocal),
    [cuenta, filtroLocal],
  )

  if (cargando && !cuenta) return <EstadoCargando />
  if (error || !cuenta || !pensionista) {
    return (
      <EstadoError
        mensaje={error || 'No se pudo cargar la cuenta.'}
        accion={<Boton onClick={() => navigate('/admin/pensionistas')}>Volver a Pensionistas</Boton>}
      />
    )
  }

  const { resumen } = cuenta
  const tieneSaldoInicial = cuenta.movimientos.some((m) => m.tipo === 'saldo_inicial')
  const tope = resumen.tope

  function alGuardar(nueva: CuentaDePensionista) {
    setCuenta(nueva)
    setDialogo(null)
  }

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">
            <button type="button" className="receta-volver" onClick={() => navigate(`/admin/pensionistas/${id}`)}>
              <IconoFlechaIzquierda width={14} height={14} aria-hidden="true" /> Ficha del pensionista
            </button>
          </p>
          <h1>Cuenta de {`${pensionista.nombres} ${pensionista.apellidos}`.trim()}</h1>
          <p className="panel-admin-subtitulo">
            Una sola cuenta para todos los locales. Cada movimiento dice dónde ocurrió.
          </p>
        </div>
        <div className="cta-acciones">
          {puedeSaldoInicial && !tieneSaldoInicial && (
            <Boton variante="secundario" onClick={() => setDialogo('saldo')}>
              Cargar saldo inicial
            </Boton>
          )}
          {puedeAjustar && <Boton onClick={() => setDialogo('ajuste')}>Registrar ajuste</Boton>}
        </div>
      </div>

      {resumen.superaTope && tope.monto != null && (
        <Aviso>
          Debe {enSoles(resumen.deuda)} y superó su tope de {enSoles(tope.monto)}: recuérdale el pago. No se le
          impide consumir.
        </Aviso>
      )}
      {!puedeAjustar && <Aviso tono="informacion">Puedes ver esta cuenta, pero no modificarla con tus permisos.</Aviso>}

      <div className="cta-resumen">
        <div className="panel-admin-tarjeta cta-cifra">
          <span className="cta-etiqueta">Debe</span>
          <strong className={resumen.deuda > 0 ? 'cta-deuda' : ''}>{enSoles(resumen.deuda)}</strong>
        </div>
        <div className="panel-admin-tarjeta cta-cifra">
          <span className="cta-etiqueta">Saldo a favor</span>
          <strong className={resumen.saldoAFavor > 0 ? 'cta-favor' : ''}>{enSoles(resumen.saldoAFavor)}</strong>
        </div>
        <div className="panel-admin-tarjeta cta-cifra">
          <span className="cta-etiqueta">
            Tope de crédito {tope.origen === 'fijado' ? '(fijado a mano)' : '(calculado)'}
          </span>
          <strong>{tope.monto == null ? 'Sin calcular' : enSoles(tope.monto)}</strong>
          {tope.origen === 'calculado' && tope.monto != null && (
            <span className="cta-nota">{tope.comidasContadas} comidas en lo que resta del mes</span>
          )}
          {puedeAjustar && (
            <button type="button" className="plan-historial-boton" onClick={() => setDialogo('tope')}>
              Cambiar tope
            </button>
          )}
        </div>
        <div className="panel-admin-tarjeta cta-cifra">
          <span className="cta-etiqueta">Crédito disponible</span>
          <strong>{resumen.creditoDisponible == null ? '—' : enSoles(resumen.creditoDisponible)}</strong>
        </div>
      </div>
      {tope.aviso && <Aviso tono="informacion">{AVISO_TOPE[tope.aviso]}</Aviso>}

      <div className="prod-tabla-tarjeta">
        <div className="prod-barra-filtros">
          <strong>Movimientos</strong>
          <div className="prod-espaciador" />
          {locales.length > 1 && (
            <Select
              etiqueta="Local"
              aria-label="Filtrar por local"
              value={filtroLocal}
              onChange={(e) => setFiltroLocal(e.target.value)}
            >
              <option value="">Todos los locales</option>
              {locales.map(([lid, nombre]) => (
                <option key={lid} value={lid}>
                  {nombre}
                </option>
              ))}
            </Select>
          )}
        </div>
        <div className="prod-tabla-scroll">
          {movimientos.length === 0 ? (
            <EstadoVacio
              titulo="Sin movimientos todavía"
              descripcion={
                puedeSaldoInicial && !tieneSaldoInicial
                  ? 'Si el pensionista ya debía o tenía saldo a favor, carga su saldo inicial.'
                  : 'Aquí aparecerán sus consumos, abonos y ajustes.'
              }
            />
          ) : (
            <table className="prod-tabla">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Concepto</th>
                  <th>Local</th>
                  <th>Registró</th>
                  <th className="cta-num">Cargo</th>
                  <th className="cta-num">Crédito</th>
                </tr>
              </thead>
              <tbody>
                {movimientos.map((m) => (
                  <FilaMovimiento key={m.id} m={m} />
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {dialogo === 'saldo' && (
        <DialogoSaldoInicial
          token={token}
          pensionistaId={pensionista.id}
          onCerrar={() => setDialogo(null)}
          onGuardado={alGuardar}
        />
      )}
      {dialogo === 'ajuste' && (
        <DialogoAjuste
          token={token}
          pensionistaId={pensionista.id}
          onCerrar={() => setDialogo(null)}
          onGuardado={alGuardar}
        />
      )}
      {dialogo === 'tope' && (
        <DialogoTope
          token={token}
          pensionistaId={pensionista.id}
          fijadoActual={tope.origen === 'fijado' ? tope.monto : null}
          onCerrar={() => setDialogo(null)}
          onGuardado={alGuardar}
        />
      )}
    </section>
  )
}

function FilaMovimiento({ m }: { m: MovimientoDeCuenta }) {
  const detalle = [m.motivo, m.descripcion].filter(Boolean).join(' · ')
  return (
    <tr className="prod-fila">
      <td>{FORMATO_FECHA.format(new Date(m.ocurridoEn))}</td>
      <td>
        {CONCEPTO[m.tipo]}
        {detalle && <span className="cta-nota"> {detalle}</span>}
      </td>
      <td>{m.localNombre ?? <span className="prod-vacio">—</span>}</td>
      <td>{m.registradoPorNombre ?? <span className="prod-vacio">—</span>}</td>
      <td className="cta-num">{m.signo === 'cargo' ? enSoles(m.importe) : ''}</td>
      <td className="cta-num">{m.signo === 'credito' ? enSoles(m.importe) : ''}</td>
    </tr>
  )
}

interface PropsDialogo {
  token: string
  pensionistaId: string
  onCerrar: () => void
  onGuardado: (c: CuentaDePensionista) => void
}

function SelectorDeSigno({ valor, onCambio, cargo, credito }: {
  valor: SignoMovimiento
  onCambio: (s: SignoMovimiento) => void
  cargo: string
  credito: string
}) {
  return (
    <Select etiqueta="Tipo" value={valor} onChange={(e) => onCambio(e.target.value as SignoMovimiento)}>
      <option value="cargo">{cargo}</option>
      <option value="credito">{credito}</option>
    </Select>
  )
}

function DialogoSaldoInicial({ token, pensionistaId, onCerrar, onGuardado }: PropsDialogo) {
  const [signo, setSigno] = useState<SignoMovimiento>('cargo')
  const [importe, setImporte] = useState('')
  const [fecha, setFecha] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  function guardar() {
    const centimos = aCentimos(importe)
    if (centimos == null || Number.isNaN(centimos)) {
      setError('Escribe un importe mayor que cero, por ejemplo 50 o 50.50.')
      return
    }
    setGuardando(true)
    setError('')
    cargarSaldoInicial(token, pensionistaId, {
      importe: centimos,
      signo,
      fechaCorte: fecha || null,
      descripcion: descripcion.trim() || null,
    })
      .then(onGuardado)
      .catch((e) => setError(mensajeDe(e, 'No se pudo cargar el saldo inicial.')))
      .finally(() => setGuardando(false))
  }

  return (
    <Modal
      abierto
      titulo="Cargar saldo inicial"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={guardar}>
            Cargar saldo inicial
          </Boton>
        </>
      }
    >
      <div className="cta-dialogo">
        <p>
          Lo que este pensionista debía o tenía a favor al empezar en el sistema. Se carga una sola vez; un error se
          corrige con un ajuste.
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}
        <SelectorDeSigno valor={signo} onCambio={setSigno} cargo="Debe (deuda)" credito="Tiene a favor" />
        <Campo etiqueta="Importe (S/)" inputMode="decimal" value={importe} onChange={(e) => setImporte(e.target.value)} />
        <Campo
          etiqueta="Fecha de corte"
          type="date"
          ayuda="Hasta cuándo llega el cuaderno. Si la dejas vacía, es hoy."
          value={fecha}
          onChange={(e) => setFecha(e.target.value)}
        />
        <Campo
          etiqueta="Nota"
          maxLength={250}
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
        />
      </div>
    </Modal>
  )
}

function DialogoAjuste({ token, pensionistaId, onCerrar, onGuardado }: PropsDialogo) {
  const [motivos, setMotivos] = useState<Motivo[] | null>(null)
  const [errorMotivos, setErrorMotivos] = useState(false)
  const [signo, setSigno] = useState<SignoMovimiento>('credito')
  const [importe, setImporte] = useState('')
  const [motivoId, setMotivoId] = useState('')
  const [observacion, setObservacion] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    listarMotivos(token, 'ajuste_cuenta')
      .then((lista) => setMotivos(lista.filter((m) => m.activo)))
      .catch(() => setErrorMotivos(true))
  }, [token])

  const motivo = motivos?.find((m) => m.id === motivoId)

  function guardar() {
    const centimos = aCentimos(importe)
    if (centimos == null || Number.isNaN(centimos)) {
      setError('Escribe un importe mayor que cero, por ejemplo 3 o 3.50.')
      return
    }
    if (!motivoId) {
      setError('Elige el motivo del ajuste.')
      return
    }
    if (motivo?.requiereObservacion && !observacion.trim()) {
      setError('Ese motivo pide una observación.')
      return
    }
    setGuardando(true)
    setError('')
    registrarAjusteDeCuenta(token, pensionistaId, {
      importe: centimos,
      signo,
      motivoId,
      observacion: observacion.trim() || null,
    })
      .then(onGuardado)
      .catch((e) => setError(mensajeDe(e, 'No se pudo registrar el ajuste.')))
      .finally(() => setGuardando(false))
  }

  return (
    <Modal
      abierto
      titulo="Registrar ajuste"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={guardar} disabled={motivos == null}>
            Registrar ajuste
          </Boton>
        </>
      }
    >
      <div className="cta-dialogo">
        <p>
          Un ajuste corrige la cuenta sin borrar nada: queda como un movimiento más, con su motivo y en la bitácora.
        </p>
        {errorMotivos && <Aviso tono="error">No se pudieron cargar los motivos de ajuste.</Aviso>}
        {motivos?.length === 0 && (
          <Aviso>No hay motivos de ajuste de cuenta activos. Créalos en Configuración → Motivos.</Aviso>
        )}
        {error && <Aviso tono="error">{error}</Aviso>}
        <SelectorDeSigno valor={signo} onCambio={setSigno} cargo="Cargo (debe más)" credito="Crédito (debe menos)" />
        <Campo etiqueta="Importe (S/)" inputMode="decimal" value={importe} onChange={(e) => setImporte(e.target.value)} />
        <Select etiqueta="Motivo" value={motivoId} onChange={(e) => setMotivoId(e.target.value)}>
          <option value="">Elige un motivo</option>
          {(motivos ?? []).map((m) => (
            <option key={m.id} value={m.id}>
              {m.descripcion}
            </option>
          ))}
        </Select>
        <Campo
          etiqueta={motivo?.requiereObservacion ? 'Observación (obligatoria)' : 'Observación'}
          maxLength={250}
          value={observacion}
          onChange={(e) => setObservacion(e.target.value)}
        />
      </div>
    </Modal>
  )
}

function DialogoTope({ token, pensionistaId, fijadoActual, onCerrar, onGuardado }: PropsDialogo & {
  fijadoActual: number | null
}) {
  const [valor, setValor] = useState(desdeCentimos(fijadoActual))
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  function enviar(limite: number | null) {
    setGuardando(true)
    setError('')
    fijarTopeDeCredito(token, pensionistaId, limite)
      .then(onGuardado)
      .catch((e) => setError(mensajeDe(e, 'No se pudo cambiar el tope.')))
      .finally(() => setGuardando(false))
  }

  function fijar() {
    const centimos = aCentimos(valor)
    if (centimos == null || Number.isNaN(centimos)) {
      setError('Escribe un tope mayor que cero, o usa "Que se calcule solo".')
      return
    }
    enviar(centimos)
  }

  return (
    <Modal
      abierto
      titulo="Tope de crédito"
      onCerrar={onCerrar}
      pie={
        <>
          {fijadoActual != null && (
            <Boton variante="secundario" cargando={guardando} onClick={() => enviar(null)}>
              Que se calcule solo
            </Boton>
          )}
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={fijar}>
            Fijar tope
          </Boton>
        </>
      }
    >
      <div className="cta-dialogo">
        <p>
          El tope es solo un recordatorio para cobrar la deuda: nunca impide que el pensionista consuma. Por defecto se
          calcula con los días de atención del mes y las comidas que toma.
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}
        <Campo etiqueta="Tope fijado (S/)" inputMode="decimal" value={valor} onChange={(e) => setValor(e.target.value)} />
      </div>
    </Modal>
  )
}
