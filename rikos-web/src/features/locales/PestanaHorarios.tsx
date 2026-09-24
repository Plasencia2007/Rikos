import { useEffect, useState } from 'react'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  IconoMas,
  IconoProhibido,
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

const ETIQUETA_DIA: Record<DiaSemana, string> = {
  lunes: 'Lunes', martes: 'Martes', miercoles: 'Miércoles', jueves: 'Jueves',
  viernes: 'Viernes', sabado: 'Sábado', domingo: 'Domingo',
}
const ETIQUETA_SERVICIO: Record<Servicio, string> = { desayuno: 'Desayuno', almuerzo: 'Almuerzo', cena: 'Cena' }

interface TurnoEdicion extends TurnoServicio {
  clave: string
}

let contador = 0
const claveNueva = () => `nuevo-${++contador}`

interface Props {
  token: string
  localId: string
}

export function PestanaHorarios({ token, localId }: Props) {
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [turnos, setTurnos] = useState<TurnoEdicion[]>([])
  const [diasEspeciales, setDiasEspeciales] = useState<DiaEspecial[]>([])
  const [copiandoDesde, setCopiandoDesde] = useState<DiaSemana | null>(null)
  const [diasDestino, setDiasDestino] = useState<DiaSemana[]>([])

  const [guardando, setGuardando] = useState(false)
  const [errorGuardar, setErrorGuardar] = useState('')
  const [guardadoOk, setGuardadoOk] = useState(false)

  const [modalDiaEspecialAbierto, setModalDiaEspecialAbierto] = useState(false)
  const [diaEspecialAEliminar, setDiaEspecialAEliminar] = useState<DiaEspecial | null>(null)
  const [eliminandoDiaEspecial, setEliminandoDiaEspecial] = useState(false)

  async function cargar() {
    setCargando(true)
    setError('')
    try {
      const datos = await obtenerHorarios(token, localId)
      setTurnos(datos.turnos.map((t) => ({ ...t, clave: t.id ?? claveNueva() })))
      setDiasEspeciales(datos.diasEspeciales)
    } catch {
      setError('No se pudo cargar el horario de este local.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    void cargar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [localId])

  function agregarTurno(dia: DiaSemana) {
    setGuardadoOk(false)
    setTurnos((actuales) => [
      ...actuales,
      { clave: claveNueva(), diaSemana: dia, servicio: 'almuerzo', horaInicio: '12:00', horaFin: '15:00' },
    ])
  }

  function actualizarTurno(clave: string, cambios: Partial<TurnoEdicion>) {
    setGuardadoOk(false)
    setTurnos((actuales) => actuales.map((t) => (t.clave === clave ? { ...t, ...cambios } : t)))
  }

  function quitarTurno(clave: string) {
    setGuardadoOk(false)
    setTurnos((actuales) => actuales.filter((t) => t.clave !== clave))
  }

  function abrirCopiar(dia: DiaSemana) {
    setCopiandoDesde(dia)
    setDiasDestino([])
  }

  function copiarA() {
    if (!copiandoDesde) return
    const turnosOrigen = turnos.filter((t) => t.diaSemana === copiandoDesde)
    setGuardadoOk(false)
    setTurnos((actuales) => [
      ...actuales.filter((t) => !diasDestino.includes(t.diaSemana)),
      ...diasDestino.flatMap((dia) =>
        turnosOrigen.map((t) => ({ ...t, clave: claveNueva(), diaSemana: dia, id: undefined })),
      ),
    ])
    setCopiandoDesde(null)
  }

  async function guardar() {
    setGuardando(true)
    setErrorGuardar('')
    setGuardadoOk(false)
    try {
      const guardados = await guardarHorarios(
        token,
        localId,
        turnos.map(({ clave: _clave, ...turno }) => turno),
      )
      setTurnos(guardados.map((t) => ({ ...t, clave: t.id ?? claveNueva() })))
      setGuardadoOk(true)
    } catch (excepcion) {
      setErrorGuardar(
        excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'FRANJAS_SOLAPADAS'
          ? 'Hay turnos que se solapan en el mismo día y servicio. Ajusta las horas antes de guardar.'
          : 'No se pudo guardar el horario.',
      )
    } finally {
      setGuardando(false)
    }
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
    <div className="pestana-horarios">
      <div className="horarios-semana">
        {DIAS_SEMANA.map((dia) => {
          const turnosDelDia = turnos.filter((t) => t.diaSemana === dia)
          return (
            <div className="horario-dia" key={dia}>
              <div className="horario-dia-encabezado">
                <span
                  className={`horario-dia-insignia ${turnosDelDia.length === 0 ? 'cerrado' : ''}`}
                  aria-hidden="true"
                >
                  {ETIQUETA_DIA[dia].charAt(0)}
                </span>
                <div>
                  <strong>{ETIQUETA_DIA[dia]}</strong>
                  <div className={`horario-dia-estado ${turnosDelDia.length === 0 ? 'cerrado' : 'abierto'}`}>
                    {turnosDelDia.length === 0 ? 'Cerrado' : 'Abierto'}
                  </div>
                </div>
              </div>

              {turnosDelDia.map((turno) => (
                <div className="horario-turno" key={turno.clave}>
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
                    className="horario-turno-quitar"
                    aria-label="Quitar turno"
                    onClick={() => quitarTurno(turno.clave)}
                  >
                    <IconoProhibido width={14} height={14} />
                  </button>
                </div>
              ))}

              <div className="horario-dia-acciones">
                <button type="button" className="boton-texto-chico" onClick={() => agregarTurno(dia)}>
                  <IconoMas width={13} height={13} /> Agregar turno
                </button>
                {turnosDelDia.length > 0 && (
                  <button type="button" className="boton-texto-chico" onClick={() => abrirCopiar(dia)}>
                    Copiar a otros días
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>

      {errorGuardar && <Aviso tono="error">{errorGuardar}</Aviso>}
      {guardadoOk && <Aviso tono="exito">Horario guardado.</Aviso>}

      <div className="horarios-guardar">
        <Boton onClick={guardar} cargando={guardando}>
          Guardar horario
        </Boton>
      </div>

      <hr className="separador" />

      <div className="dias-especiales">
        <div className="dias-especiales-encabezado">
          <h3>Días especiales y feriados</h3>
          <Boton variante="secundario" onClick={() => setModalDiaEspecialAbierto(true)}>
            <IconoMas width={14} height={14} /> Agregar día especial
          </Boton>
        </div>

        {diasEspeciales.length === 0 ? (
          <p className="dias-especiales-vacio">No hay feriados ni excepciones cargadas todavía.</p>
        ) : (
          <ul className="dias-especiales-lista">
            {diasEspeciales.map((d) => (
              <li key={d.id} className="dia-especial-fila">
                <div>
                  <strong>{d.fecha}</strong>
                  {d.descripcion && <span className="dia-especial-descripcion"> — {d.descripcion}</span>}
                  <div className="dia-especial-detalle">
                    {d.cerrado
                      ? 'Cerrado todo el día'
                      : d.franjas.map((f) => `${ETIQUETA_SERVICIO[f.servicio]} ${f.horaInicio}–${f.horaFin}`).join(' · ')}
                  </div>
                </div>
                <Boton variante="peligro" onClick={() => setDiaEspecialAEliminar(d)}>
                  Eliminar
                </Boton>
              </li>
            ))}
          </ul>
        )}
      </div>

      {copiandoDesde && (
        <Modal
          abierto
          titulo={`Copiar el horario de ${ETIQUETA_DIA[copiandoDesde]} a…`}
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
    </div>
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
              <IconoMas width={13} height={13} /> Agregar franja
            </button>
          </div>
        )}

        {error && <Aviso tono="error">{error}</Aviso>}
      </div>
    </Modal>
  )
}
