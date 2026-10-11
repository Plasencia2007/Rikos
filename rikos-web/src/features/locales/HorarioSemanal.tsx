import { useEffect, useImperativeHandle, useState, type Ref } from 'react'
import { ChevronDown, ChevronRight, ChevronUp, CircleMinus, Copy, Plus } from 'lucide-react'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  IconoBorrar,
  Modal,
  ModalDeConfirmacion,
  Select,
} from '../../comun/componentes'
import {
  DIAS_SEMANA,
  ErrorApi,
  SERVICIOS,
  crearDiaEspecial,
  eliminarDiaEspecial,
  guardarHorarios,
  obtenerHorarios,
  type DiaEspecial,
  type DiaSemana,
  type FranjaDiaEspecial,
  type Servicio,
  type TurnoServicio,
} from '../../api/cliente'

export const ETIQUETA_DIA: Record<DiaSemana, string> = {
  lunes: 'Lunes', martes: 'Martes', miercoles: 'Miércoles', jueves: 'Jueves',
  viernes: 'Viernes', sabado: 'Sábado', domingo: 'Domingo',
}
export const ETIQUETA_SERVICIO: Record<Servicio, string> = { desayuno: 'Desayuno', almuerzo: 'Almuerzo', cena: 'Cena' }

export interface TurnoEdicion extends TurnoServicio {
  clave: string
}

let contador = 0
const claveNueva = () => `nuevo-${++contador}`

/** "12:00 – 22:00 · 2 turnos": desde el primer inicio hasta el último fin del día. */
function resumenDelDia(turnos: TurnoEdicion[]): string {
  if (turnos.length === 0) return 'Cerrado'
  const inicios = turnos.map((t) => t.horaInicio).sort()
  const fines = turnos.map((t) => t.horaFin).sort()
  const cantidad = `${turnos.length} ${turnos.length === 1 ? 'turno' : 'turnos'}`
  return `${inicios[0]} – ${fines[fines.length - 1]} · ${cantidad}`
}

function fechaLegible(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number)
  return new Date(a, m - 1, d).toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' }).replace('.', '')
}

export interface ManejadorHorario {
  /** Guarda el horario semanal. Devuelve false si falló (el error queda visible en la pantalla). */
  guardar: () => Promise<boolean>
}

interface Props {
  token: string
  localId: string
  ref?: Ref<ManejadorHorario>
  /** Se avisa al padre para la vista previa lateral. */
  onCambiarTurnos?: (turnos: TurnoEdicion[]) => void
}

export function HorarioSemanal({ token, localId, ref, onCambiarTurnos }: Props) {
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [turnos, setTurnos] = useState<TurnoEdicion[]>([])
  const [diasEspeciales, setDiasEspeciales] = useState<DiaEspecial[]>([])
  const [abiertos, setAbiertos] = useState<Set<DiaSemana>>(new Set())
  const [copiandoDesde, setCopiandoDesde] = useState<DiaSemana | null>(null)
  const [diasDestino, setDiasDestino] = useState<DiaSemana[]>([])
  const [errorGuardar, setErrorGuardar] = useState('')

  const [modalDiaEspecialAbierto, setModalDiaEspecialAbierto] = useState(false)
  const [diaEspecialAEliminar, setDiaEspecialAEliminar] = useState<DiaEspecial | null>(null)
  const [eliminandoDiaEspecial, setEliminandoDiaEspecial] = useState(false)

  async function cargar(primeraVez = false) {
    if (primeraVez) setCargando(true)
    setError('')
    try {
      const datos = await obtenerHorarios(token, localId)
      const conClave = datos.turnos.map((t) => ({ ...t, clave: t.id ?? claveNueva() }))
      setDiasEspeciales(datos.diasEspeciales)
      if (primeraVez) {
        setTurnos(conClave)
        const primero = DIAS_SEMANA.find((d) => conClave.some((t) => t.diaSemana === d))
        setAbiertos(new Set(primero ? [primero] : []))
      }
    } catch {
      setError('No se pudo cargar el horario de este local.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    void cargar(true)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [localId])

  useEffect(() => {
    onCambiarTurnos?.(turnos)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [turnos])

  useImperativeHandle(ref, () => ({
    async guardar() {
      setErrorGuardar('')
      try {
        const guardados = await guardarHorarios(
          token,
          localId,
          // eslint-disable-next-line @typescript-eslint/no-unused-vars -- se descarta la clave de lista a propósito
          turnos.map(({ clave: _clave, ...turno }) => turno),
        )
        setTurnos(guardados.map((t) => ({ ...t, clave: t.id ?? claveNueva() })))
        return true
      } catch (excepcion) {
        setErrorGuardar(
          excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'FRANJAS_SOLAPADAS'
            ? 'Hay turnos que se solapan en el mismo día y servicio. Ajusta las horas antes de continuar.'
            : 'No se pudo guardar el horario.',
        )
        return false
      }
    },
  }))

  function alternarDia(dia: DiaSemana) {
    setAbiertos((actuales) => {
      const siguiente = new Set(actuales)
      if (siguiente.has(dia)) siguiente.delete(dia)
      else siguiente.add(dia)
      return siguiente
    })
  }

  function agregarTurno(dia: DiaSemana) {
    const delDia = turnos.filter((t) => t.diaSemana === dia)
    const usados = new Set(delDia.map((t) => t.servicio))
    const servicio = SERVICIOS.find((s) => !usados.has(s)) ?? 'almuerzo'
    const horas: Record<Servicio, [string, string]> = {
      desayuno: ['08:00', '11:00'], almuerzo: ['12:00', '15:00'], cena: ['18:00', '22:00'],
    }
    setTurnos((actuales) => [
      ...actuales,
      { clave: claveNueva(), diaSemana: dia, servicio, horaInicio: horas[servicio][0], horaFin: horas[servicio][1] },
    ])
    setAbiertos((actuales) => new Set(actuales).add(dia))
  }

  function actualizarTurno(clave: string, cambios: Partial<TurnoEdicion>) {
    setTurnos((actuales) => actuales.map((t) => (t.clave === clave ? { ...t, ...cambios } : t)))
  }

  function quitarTurno(clave: string) {
    setTurnos((actuales) => actuales.filter((t) => t.clave !== clave))
  }

  function abrirCopiar(dia?: DiaSemana) {
    const origen = dia ?? DIAS_SEMANA.find((d) => turnos.some((t) => t.diaSemana === d)) ?? 'lunes'
    setCopiandoDesde(origen)
    setDiasDestino([])
  }

  function copiarA() {
    if (!copiandoDesde) return
    const turnosOrigen = turnos.filter((t) => t.diaSemana === copiandoDesde)
    setTurnos((actuales) => [
      ...actuales.filter((t) => !diasDestino.includes(t.diaSemana)),
      ...diasDestino.flatMap((dia) =>
        turnosOrigen.map((t) => ({ ...t, clave: claveNueva(), diaSemana: dia, id: undefined })),
      ),
    ])
    setAbiertos((actuales) => new Set([...actuales, ...diasDestino]))
    setCopiandoDesde(null)
  }

  async function confirmarEliminarDiaEspecial() {
    if (!diaEspecialAEliminar) return
    setEliminandoDiaEspecial(true)
    try {
      await eliminarDiaEspecial(token, localId, diaEspecialAEliminar.id)
      setDiaEspecialAEliminar(null)
      await cargar()
    } finally {
      setEliminandoDiaEspecial(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (error) return <EstadoError mensaje={error} />

  return (
    <>
      <div className="as-fila-titulo">
        <div className="as-titulo-bloque">
          <h2 className="as-h2">Horarios de atención</h2>
          <span className="as-ayuda">Define los horarios en que tu local recibe pedidos.</span>
        </div>
        <button type="button" className="as-boton-borde" onClick={() => abrirCopiar()}>
          <Copy size={16} aria-hidden="true" /> Copiar horario a otros días
        </button>
      </div>

      <div className="hor-dias">
        {DIAS_SEMANA.map((dia) => {
          const delDia = turnos.filter((t) => t.diaSemana === dia)
          const abierto = abiertos.has(dia)
          const estaAbierto = delDia.length > 0
          return (
            <div className={`hor-dia ${abierto ? 'expandido' : ''}`} key={dia}>
              <button
                type="button"
                className="hor-dia-cabecera"
                aria-expanded={abierto}
                onClick={() => alternarDia(dia)}
              >
                <span className="hor-chev" aria-hidden="true">
                  {abierto ? <ChevronDown size={17} /> : <ChevronRight size={17} />}
                </span>
                <span className={`hor-letra ${estaAbierto ? '' : 'cerrado'}`} aria-hidden="true">
                  {ETIQUETA_DIA[dia] === 'Miércoles' ? 'X' : ETIQUETA_DIA[dia].charAt(0)}
                </span>
                <strong>{ETIQUETA_DIA[dia]}</strong>
                <span className={`as-pastilla ${estaAbierto ? 'abierto' : 'cerrado'}`}>
                  {estaAbierto ? 'Abierto' : 'Cerrado'}
                </span>
                <span className="hor-resumen">{resumenDelDia(delDia)}</span>
                <span className="hor-chev" aria-hidden="true">
                  {abierto ? <ChevronUp size={17} /> : <ChevronRight size={17} />}
                </span>
              </button>

              {abierto && (
                <div className="hor-dia-cuerpo">
                  {delDia.map((turno) => (
                    <div className="hor-turno" key={turno.clave}>
                      <select
                        aria-label={`Servicio del turno, ${ETIQUETA_DIA[dia]}`}
                        value={turno.servicio}
                        onChange={(e) => actualizarTurno(turno.clave, { servicio: e.target.value as Servicio })}
                      >
                        {SERVICIOS.map((s) => (
                          <option key={s} value={s}>
                            {ETIQUETA_SERVICIO[s]}
                          </option>
                        ))}
                      </select>
                      <input
                        type="time"
                        aria-label={`Hora de inicio, ${ETIQUETA_DIA[dia]}`}
                        value={turno.horaInicio}
                        onChange={(e) => actualizarTurno(turno.clave, { horaInicio: e.target.value })}
                      />
                      <span>a</span>
                      <input
                        type="time"
                        aria-label={`Hora de fin, ${ETIQUETA_DIA[dia]}`}
                        value={turno.horaFin}
                        onChange={(e) => actualizarTurno(turno.clave, { horaFin: e.target.value })}
                      />
                      <button
                        type="button"
                        className="hor-quitar"
                        aria-label={`Quitar turno de ${ETIQUETA_SERVICIO[turno.servicio]}, ${ETIQUETA_DIA[dia]}`}
                        onClick={() => quitarTurno(turno.clave)}
                      >
                        <CircleMinus size={20} aria-hidden="true" />
                      </button>
                    </div>
                  ))}
                  {delDia.length === 0 && (
                    <p className="as-ayuda">Este día el local no atiende. Agrega un turno para abrirlo.</p>
                  )}
                  <div className="hor-acciones">
                    <button type="button" className="as-enlace" onClick={() => agregarTurno(dia)}>
                      <Plus size={15} strokeWidth={2.2} aria-hidden="true" /> Agregar turno
                    </button>
                    {delDia.length > 0 && (
                      <button type="button" className="as-enlace" onClick={() => abrirCopiar(dia)}>
                        <Copy size={15} aria-hidden="true" /> Copiar a otros días
                      </button>
                    )}
                  </div>
                </div>
              )}
            </div>
          )
        })}
      </div>

      {errorGuardar && <Aviso tono="error">{errorGuardar}</Aviso>}

      <hr className="as-separador" />

      <div className="as-fila-titulo">
        <div className="as-titulo-bloque">
          <h2 className="as-h2">Días especiales y feriados</h2>
          <span className="as-ayuda">Estas fechas reemplazan el horario semanal.</span>
        </div>
        <button type="button" className="as-boton-borde" onClick={() => setModalDiaEspecialAbierto(true)}>
          <Plus size={16} strokeWidth={2.2} aria-hidden="true" /> Agregar día especial
        </button>
      </div>

      {diasEspeciales.length === 0 ? (
        <p className="as-vacio-linea">No hay feriados ni excepciones cargadas todavía.</p>
      ) : (
        <div className="hor-tabla" role="table" aria-label="Días especiales">
          <div className="hor-tabla-fila hor-tabla-cabecera" role="row">
            <span role="columnheader">Fecha</span>
            <span role="columnheader">Nombre</span>
            <span role="columnheader">Horario</span>
            <span role="columnheader">Estado</span>
            <span role="columnheader" className="hor-tabla-acc">Acciones</span>
          </div>
          {diasEspeciales.map((d) => (
            <div className="hor-tabla-fila" role="row" key={d.id}>
              <span role="cell">{fechaLegible(d.fecha)}</span>
              <strong role="cell">{d.descripcion || '—'}</strong>
              <span role="cell" className="as-suave">
                {d.cerrado || d.franjas.length === 0
                  ? '—'
                  : d.franjas.map((f) => `${ETIQUETA_SERVICIO[f.servicio]} ${f.horaInicio}–${f.horaFin}`).join(' · ')}
              </span>
              <span role="cell">
                <span className={`as-pastilla ${d.cerrado ? 'cerrado' : 'abierto'}`}>
                  {d.cerrado ? 'Cerrado' : 'Horario especial'}
                </span>
              </span>
              <span role="cell" className="hor-tabla-acc">
                <button
                  type="button"
                  className="as-icono-boton peligro"
                  aria-label={`Eliminar ${d.descripcion || fechaLegible(d.fecha)}`}
                  onClick={() => setDiaEspecialAEliminar(d)}
                >
                  <IconoBorrar size={16} aria-hidden="true" />
                </button>
              </span>
            </div>
          ))}
        </div>
      )}

      {copiandoDesde && (
        <Modal
          abierto
          titulo="Copiar horario a otros días"
          onCerrar={() => setCopiandoDesde(null)}
          pie={
            <>
              <Boton variante="secundario" onClick={() => setCopiandoDesde(null)}>
                Cancelar
              </Boton>
              <Boton onClick={copiarA} disabled={diasDestino.length === 0}>
                Copiar
              </Boton>
            </>
          }
        >
          <div className="formulario-admin">
            <Select
              etiqueta="Copiar desde"
              value={copiandoDesde}
              onChange={(e) => {
                setCopiandoDesde(e.target.value as DiaSemana)
                setDiasDestino([])
              }}
            >
              {DIAS_SEMANA.map((d) => (
                <option key={d} value={d}>
                  {ETIQUETA_DIA[d]}
                </option>
              ))}
            </Select>
            <div className="copiar-dias-opciones">
              {DIAS_SEMANA.filter((d) => d !== copiandoDesde).map((dia) => (
                <label key={dia} className="opcion">
                  <input
                    type="checkbox"
                    className="opcion-control"
                    checked={diasDestino.includes(dia)}
                    onChange={(e) =>
                      setDiasDestino((actuales) =>
                        e.target.checked ? [...actuales, dia] : actuales.filter((d) => d !== dia),
                      )
                    }
                  />
                  <span className="opcion-etiqueta">{ETIQUETA_DIA[dia]}</span>
                </label>
              ))}
            </div>
          </div>
        </Modal>
      )}

      {modalDiaEspecialAbierto && (
        <FormularioDiaEspecial
          token={token}
          localId={localId}
          onCerrar={() => setModalDiaEspecialAbierto(false)}
          onCreado={async () => {
            setModalDiaEspecialAbierto(false)
            await cargar()
          }}
        />
      )}

      <ModalDeConfirmacion
        abierto={diaEspecialAEliminar != null}
        titulo="Eliminar día especial"
        mensaje={`Vas a quitar "${diaEspecialAEliminar?.descripcion || diaEspecialAEliminar?.fecha}" del calendario. Ese día volverá a regirse por el horario semanal normal.`}
        textoConfirmar="Eliminar"
        peligro
        confirmando={eliminandoDiaEspecial}
        onConfirmar={confirmarEliminarDiaEspecial}
        onCancelar={() => setDiaEspecialAEliminar(null)}
      />
    </>
  )
}

function FormularioDiaEspecial({
  token,
  localId,
  onCerrar,
  onCreado,
}: {
  token: string
  localId: string
  onCerrar: () => void
  onCreado: () => void
}) {
  const [fecha, setFecha] = useState('')
  const [cerrado, setCerrado] = useState(true)
  const [descripcion, setDescripcion] = useState('')
  const [franjas, setFranjas] = useState<FranjaDiaEspecial[]>([])
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  function agregarFranja() {
    setFranjas((actuales) => [...actuales, { servicio: 'almuerzo', horaInicio: '12:00', horaFin: '15:00' }])
  }

  async function crear() {
    if (!fecha) {
      setError('Elige una fecha.')
      return
    }
    setGuardando(true)
    setError('')
    try {
      await crearDiaEspecial(token, localId, {
        fecha,
        cerrado,
        descripcion: descripcion || undefined,
        franjas: cerrado ? [] : franjas,
      })
      onCreado()
    } catch (excepcion) {
      setError(
        excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'FECHA_DUPLICADA'
          ? 'Ya hay un día especial cargado para esa fecha.'
          : excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'FRANJAS_SOLAPADAS'
            ? 'Las franjas de ese día se solapan entre sí.'
            : 'No se pudo guardar el día especial.',
      )
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Modal
      abierto
      titulo="Nuevo día especial"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton onClick={crear} cargando={guardando}>
            Guardar
          </Boton>
        </>
      }
    >
      <div className="formulario-admin">
        <Campo etiqueta="Fecha" type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} required />
        <Campo
          etiqueta="Descripción (opcional)"
          placeholder="Fiestas Patrias"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
        />
        <label className="opcion">
          <input
            type="checkbox"
            className="opcion-control"
            checked={cerrado}
            onChange={(e) => setCerrado(e.target.checked)}
          />
          <span className="opcion-etiqueta">El local no atiende ese día</span>
        </label>

        {!cerrado && (
          <div className="dia-especial-franjas">
            {franjas.map((franja, indice) => (
              <div className="horario-turno" key={indice}>
                <Select
                  etiqueta="Servicio"
                  value={franja.servicio}
                  onChange={(e) =>
                    setFranjas((actuales) =>
                      actuales.map((f, i) => (i === indice ? { ...f, servicio: e.target.value as Servicio } : f)),
                    )
                  }
                >
                  {SERVICIOS.map((s) => (
                    <option key={s} value={s}>
                      {ETIQUETA_SERVICIO[s]}
                    </option>
                  ))}
                </Select>
                <input
                  type="time"
                  value={franja.horaInicio}
                  onChange={(e) =>
                    setFranjas((actuales) =>
                      actuales.map((f, i) => (i === indice ? { ...f, horaInicio: e.target.value } : f)),
                    )
                  }
                />
                <span>a</span>
                <input
                  type="time"
                  value={franja.horaFin}
                  onChange={(e) =>
                    setFranjas((actuales) =>
                      actuales.map((f, i) => (i === indice ? { ...f, horaFin: e.target.value } : f)),
                    )
                  }
                />
              </div>
            ))}
            <button type="button" className="boton-texto-chico" onClick={agregarFranja}>
              <Plus size={13} aria-hidden="true" /> Agregar franja
            </button>
          </div>
        )}

        {error && <Aviso tono="error">{error}</Aviso>}
      </div>
    </Modal>
  )
}
