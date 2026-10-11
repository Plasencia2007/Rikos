import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  Checkbox,
  EstadoCargando,
  EstadoError,
  IconoFlechaIzquierda,
  Insignia,
  Modal,
  ModalDeConfirmacion,
  Select,
} from '../../comun/componentes'
import {
  ErrorApi,
  activarPensionista,
  actualizarPensionista,
  crearPensionista,
  desactivarPensionista,
  listarPlanes,
  obtenerPensionista,
  regenerarPinPensionista,
  type ComidaHabitual,
  type DatosPensionista,
  type DetallePensionista,
  type PlanTarifario,
} from '../../api/cliente'

/**
 * US-016: alta y edicion de la ficha, con su responsable (obligatorio).
 *
 * El PIN lo genera el sistema y se muestra UNA sola vez, al crear la ficha o
 * al generar uno nuevo: no hay forma de verlo despues (RN-090). La contrasena
 * del portal no se pide todavia (RF-027). Solo con conexion: dos locales sin
 * internet podrian dar el mismo DNI a personas distintas.
 */

const COMIDAS: { valor: ComidaHabitual; titulo: string }[] = [
  { valor: 'desayuno', titulo: 'Desayuno' },
  { valor: 'almuerzo', titulo: 'Almuerzo' },
  { valor: 'cena', titulo: 'Cena' },
]

interface Formulario {
  dni: string
  nombres: string
  apellidos: string
  telefono: string
  correo: string
  institucion: string
  codigoEstudiante: string
  carrera: string
  ciclo: string
  planId: string
  comidas: ComidaHabitual[]
  frecuenciaAbono: string
  rNombre: string
  rDni: string
  rTelefono: string
  rCorreo: string
  rParentesco: string
  rDireccion: string
}

const VACIO: Formulario = {
  dni: '',
  nombres: '',
  apellidos: '',
  telefono: '',
  correo: '',
  institucion: '',
  codigoEstudiante: '',
  carrera: '',
  ciclo: '',
  planId: '',
  comidas: ['almuerzo'],
  frecuenciaAbono: '',
  rNombre: '',
  rDni: '',
  rTelefono: '',
  rCorreo: '',
  rParentesco: '',
  rDireccion: '',
}

function desde(d: DetallePensionista): Formulario {
  const r = d.responsable
  return {
    dni: d.dni,
    nombres: d.nombres,
    apellidos: d.apellidos,
    telefono: d.telefono ?? '',
    correo: d.correo ?? '',
    institucion: d.institucion ?? '',
    codigoEstudiante: d.codigoEstudiante ?? '',
    carrera: d.carrera ?? '',
    ciclo: d.ciclo ?? '',
    planId: d.planId,
    comidas: d.comidas,
    frecuenciaAbono: d.frecuenciaAbono ?? '',
    rNombre: r?.nombreCompleto ?? '',
    rDni: r?.dni ?? '',
    rTelefono: r?.telefono ?? '',
    rCorreo: r?.correo ?? '',
    rParentesco: r?.parentesco ?? '',
    rDireccion: r?.direccion ?? '',
  }
}

const opcional = (t: string) => t.trim() || null

function datosDe(f: Formulario): DatosPensionista {
  return {
    dni: f.dni.trim(),
    nombres: f.nombres.trim(),
    apellidos: f.apellidos.trim(),
    telefono: opcional(f.telefono),
    correo: opcional(f.correo),
    institucion: opcional(f.institucion),
    codigoEstudiante: opcional(f.codigoEstudiante),
    carrera: opcional(f.carrera),
    ciclo: opcional(f.ciclo),
    planId: f.planId,
    comidas: f.comidas,
    frecuenciaAbono: opcional(f.frecuenciaAbono),
    responsable: {
      nombreCompleto: f.rNombre.trim(),
      dni: f.rDni.trim(),
      telefono: f.rTelefono.trim(),
      correo: f.rCorreo.trim(),
      parentesco: f.rParentesco.trim(),
      direccion: f.rDireccion.trim(),
    },
  }
}

/** El error, con el campo al que pertenece cuando se sabe, para marcarlo en la pantalla. */
function errorDeApi(e: unknown): { campo: keyof Formulario | null; mensaje: string } {
  if (e instanceof ErrorApi) {
    switch (e.cuerpo?.error) {
      case 'DNI_INVALIDO':
        return { campo: 'dni', mensaje: 'El DNI tiene que tener exactamente 8 dígitos.' }
      case 'DNI_DUPLICADO':
        return { campo: 'dni', mensaje: 'Ya hay un pensionista con ese DNI. Un DNI solo puede tener una cuenta.' }
      case 'NOMBRES_OBLIGATORIOS':
        return { campo: 'nombres', mensaje: 'Los nombres y los apellidos son obligatorios.' }
      case 'PLAN_NO_ENCONTRADO':
        return { campo: 'planId', mensaje: 'Elige un plan tarifario.' }
      case 'SIN_COMIDAS':
        return { campo: null, mensaje: 'Marca al menos una comida habitual: con ella se calcula su tope de crédito.' }
      case 'RESPONSABLE_OBLIGATORIO':
      case 'RESPONSABLE_INCOMPLETO':
        return {
          campo: 'rNombre',
          mensaje: 'El responsable es obligatorio: nombre, DNI, teléfono, correo, parentesco y dirección.',
        }
      case 'RESPONSABLE_DNI_INVALIDO':
        return { campo: 'rDni', mensaje: 'El DNI del responsable tiene que tener exactamente 8 dígitos.' }
      case 'NO_ENCONTRADO':
        return { campo: null, mensaje: 'Este pensionista ya no existe. Vuelve a la lista.' }
    }
    if (e.estado === 403) return { campo: null, mensaje: 'Tus permisos no alcanzan para esto.' }
  }
  return { campo: null, mensaje: 'No se pudo guardar. Revisa la conexión: las fichas solo se guardan con internet.' }
}

export function PantallaPensionistaFormulario() {
  const { id } = useParams()
  const esNuevo = !id
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()
  const soloLectura = esNuevo ? !tienePermiso('pensionistas.crear') : !tienePermiso('pensionistas.modificar')
  const puedeDesactivar = tienePermiso('pensionistas.desactivar')

  const [f, setF] = useState<Formulario>(VACIO)
  const [inicial, setInicial] = useState<Formulario>(VACIO)
  const [existente, setExistente] = useState<DetallePensionista | null>(null)
  const [planes, setPlanes] = useState<PlanTarifario[]>([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<{ campo: keyof Formulario | null; mensaje: string } | null>(null)
  const [guardado, setGuardado] = useState(false)
  const [pin, setPin] = useState<{ valor: string; motivo: 'alta' | 'nuevo' } | null>(null)
  const [confirmandoPin, setConfirmandoPin] = useState(false)
  const [confirmandoEstado, setConfirmandoEstado] = useState(false)
  const [trabajando, setTrabajando] = useState(false)
  const [errorAccion, setErrorAccion] = useState('')

  useEffect(() => {
    setCargando(true)
    setErrorCarga('')
    Promise.all([listarPlanes(token), esNuevo ? Promise.resolve(null) : obtenerPensionista(token, id!)])
      .then(([p, d]) => {
        setPlanes(p)
        if (d) {
          setExistente(d)
          setF(desde(d))
          setInicial(desde(d))
        } else if (p.length === 1) {
          // Con un solo plan (lo normal hoy) no hay nada que elegir.
          setF((actual) => ({ ...actual, planId: p[0].id }))
        }
      })
      .catch((e) =>
        setErrorCarga(
          e instanceof ErrorApi && e.estado === 404 ? 'Este pensionista no existe.' : 'No se pudo cargar la ficha.',
        ),
      )
      .finally(() => setCargando(false))
  }, [token, id, esNuevo])

  const sucio = JSON.stringify(f) !== JSON.stringify(inicial)

  function cambiar(parcial: Partial<Formulario>) {
    setF((actual) => ({ ...actual, ...parcial }))
    setGuardado(false)
    setError(null)
  }

  function alternarComida(c: ComidaHabitual) {
    cambiar({ comidas: f.comidas.includes(c) ? f.comidas.filter((x) => x !== c) : [...f.comidas, c] })
  }

  function faltantes(): string[] {
    const lista: string[] = []
    if (!f.dni.trim()) lista.push('DNI')
    if (!f.nombres.trim() || !f.apellidos.trim()) lista.push('nombres y apellidos')
    if (!f.planId) lista.push('plan')
    if (f.comidas.length === 0) lista.push('al menos una comida')
    if (!f.rNombre.trim() || !f.rDni.trim() || !f.rTelefono.trim() || !f.rCorreo.trim() || !f.rParentesco.trim() ||
      !f.rDireccion.trim()) {
      lista.push('todos los datos del responsable')
    }
    return lista
  }

  async function guardar() {
    const faltan = faltantes()
    if (faltan.length > 0) {
      setError({ campo: null, mensaje: `Falta: ${faltan.join(', ')}.` })
      return
    }
    setGuardando(true)
    setError(null)
    try {
      if (esNuevo) {
        const creado = await crearPensionista(token, datosDe(f))
        setPin({ valor: creado.pin, motivo: 'alta' })
      } else {
        const d = await actualizarPensionista(token, id!, datosDe(f))
        setExistente(d)
        setInicial(desde(d))
        setF(desde(d))
        setGuardado(true)
      }
    } catch (e) {
      setError(errorDeApi(e))
    } finally {
      setGuardando(false)
    }
  }

  async function nuevoPin() {
    setTrabajando(true)
    setErrorAccion('')
    try {
      const r = await regenerarPinPensionista(token, id!)
      setConfirmandoPin(false)
      setPin({ valor: r.pin, motivo: 'nuevo' })
    } catch (e) {
      setErrorAccion(errorDeApi(e).mensaje)
    } finally {
      setTrabajando(false)
    }
  }

  async function cambiarEstado() {
    if (!existente) return
    setTrabajando(true)
    setErrorAccion('')
    try {
      if (existente.activo) await desactivarPensionista(token, id!)
      else await activarPensionista(token, id!)
      setExistente(await obtenerPensionista(token, id!))
      setConfirmandoEstado(false)
    } catch (e) {
      setErrorAccion(errorDeApi(e).mensaje)
    } finally {
      setTrabajando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) {
    return (
      <EstadoError
        mensaje={errorCarga}
        accion={<Boton onClick={() => navigate('/admin/pensionistas')}>Volver a Pensionistas</Boton>}
      />
    )
  }

  const errorEn = (campo: keyof Formulario) => (error?.campo === campo ? error.mensaje : undefined)
  const titulo = esNuevo ? 'Nuevo pensionista' : `${existente?.nombres ?? ''} ${existente?.apellidos ?? ''}`.trim()

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">
            <button type="button" className="receta-volver" onClick={() => navigate('/admin/pensionistas')}>
              <IconoFlechaIzquierda width={14} height={14} aria-hidden="true" /> Pensionistas
            </button>
          </p>
          <h1>{titulo}</h1>
          {existente && (
            <p className="panel-admin-subtitulo">
              <Insignia tono={existente.activo ? 'activo' : 'inactivo'} /> · DNI{' '}
              <span className="prod-codigo">{existente.dni}</span>
              {existente.planNombre ? ` · ${existente.planNombre}` : ''}
            </p>
          )}
        </div>
        <div className="cta-acciones">
          {existente && (
            <Boton variante="secundario" onClick={() => navigate(`/admin/pensionistas/${existente.id}/cuenta`)}>
              Ver cuenta
            </Boton>
          )}
          {!soloLectura && (
            <Boton cargando={guardando} onClick={guardar} disabled={!esNuevo && !sucio}>
              {esNuevo ? 'Dar de alta' : 'Guardar cambios'}
            </Boton>
          )}
        </div>
      </div>

      {soloLectura && <Aviso tono="informacion">Puedes ver esta ficha, pero no modificarla con tus permisos.</Aviso>}
      {existente && !existente.activo && (
        <Aviso>
          Este pensionista está inactivo: no puede sacar tickets ni consumir, pero conserva su cuenta y puede seguir
          abonando.
        </Aviso>
      )}
      {planes.length === 0 && (
        <Aviso tono="error">
          No hay planes tarifarios. Crea uno en Pensionistas → Planes tarifarios antes de dar de alta a alguien.
        </Aviso>
      )}
      {error && !error.campo && <Aviso tono="error">{error.mensaje}</Aviso>}
      {guardado && !error && <Aviso tono="exito">Ficha guardada.</Aviso>}

      <fieldset className="pen-form" disabled={soloLectura}>
        <div className="panel-admin-tarjeta pen-seccion">
          <h2>Datos personales</h2>
          <div className="pen-campos">
            <Campo
              etiqueta="DNI"
              inputMode="numeric"
              maxLength={8}
              value={f.dni}
              onChange={(e) => cambiar({ dni: e.target.value.replace(/\D/g, '') })}
              error={errorEn('dni')}
              ayuda="Único en toda la empresa."
            />
            <Campo etiqueta="Nombres" value={f.nombres} autoFocus={esNuevo} onChange={(e) => cambiar({ nombres: e.target.value })} error={errorEn('nombres')} />
            <Campo etiqueta="Apellidos" value={f.apellidos} onChange={(e) => cambiar({ apellidos: e.target.value })} />
            <Campo etiqueta="Teléfono" inputMode="tel" value={f.telefono} onChange={(e) => cambiar({ telefono: e.target.value })} />
            <Campo etiqueta="Correo" type="email" value={f.correo} onChange={(e) => cambiar({ correo: e.target.value })} />
          </div>
        </div>

        <div className="panel-admin-tarjeta pen-seccion">
          <h2>Estudios</h2>
          <div className="pen-campos">
            <Campo etiqueta="Universidad o instituto" value={f.institucion} onChange={(e) => cambiar({ institucion: e.target.value })} />
            <Campo etiqueta="Código de estudiante" value={f.codigoEstudiante} onChange={(e) => cambiar({ codigoEstudiante: e.target.value })} />
            <Campo etiqueta="Carrera" value={f.carrera} onChange={(e) => cambiar({ carrera: e.target.value })} />
            <Campo etiqueta="Ciclo o semestre (opcional)" value={f.ciclo} onChange={(e) => cambiar({ ciclo: e.target.value })} />
          </div>
        </div>

        <div className="panel-admin-tarjeta pen-seccion">
          <h2>Plan y comidas</h2>
          <div className="pen-campos">
            <Select etiqueta="Plan tarifario" value={f.planId} onChange={(e) => cambiar({ planId: e.target.value })} error={errorEn('planId')}>
              <option value="">Elige un plan…</option>
              {planes.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nombre}
                </option>
              ))}
            </Select>
            <Select
              etiqueta="Frecuencia habitual de abono (opcional)"
              value={f.frecuenciaAbono}
              onChange={(e) => cambiar({ frecuenciaAbono: e.target.value })}
            >
              <option value="">Sin declarar</option>
              <option value="semanal">Semanal</option>
              <option value="quincenal">Quincenal</option>
              <option value="mensual">Mensual</option>
            </Select>
          </div>
          <div>
            <span className="campo-etiqueta">Comidas habituales</span>
            <div className="pen-comidas">
              {COMIDAS.map((c) => (
                <Checkbox key={c.valor} etiqueta={c.titulo} checked={f.comidas.includes(c.valor)} onChange={() => alternarComida(c.valor)} />
              ))}
            </div>
            <p className="prod-nota">
              Con ellas se calcula su tope de crédito. No limitan lo que puede consumir: si pide otra comida, se le cobra igual.
            </p>
          </div>
        </div>

        <div className="panel-admin-tarjeta pen-seccion">
          <h2>Responsable</h2>
          <p className="prod-nota">Obligatorio. Es quien responde por la cuenta y quien recibe el acceso compartido al portal.</p>
          <div className="pen-campos">
            <Campo etiqueta="Nombre completo" value={f.rNombre} onChange={(e) => cambiar({ rNombre: e.target.value })} error={errorEn('rNombre')} />
            <Campo etiqueta="DNI" inputMode="numeric" maxLength={8} value={f.rDni} onChange={(e) => cambiar({ rDni: e.target.value.replace(/\D/g, '') })} error={errorEn('rDni')} />
            <Campo etiqueta="Teléfono" inputMode="tel" value={f.rTelefono} onChange={(e) => cambiar({ rTelefono: e.target.value })} />
            <Campo etiqueta="Correo" type="email" value={f.rCorreo} onChange={(e) => cambiar({ rCorreo: e.target.value })} />
            <Campo etiqueta="Parentesco o relación" value={f.rParentesco} placeholder="Ej. Madre" onChange={(e) => cambiar({ rParentesco: e.target.value })} />
            <Campo etiqueta="Dirección" value={f.rDireccion} onChange={(e) => cambiar({ rDireccion: e.target.value })} />
          </div>
        </div>
      </fieldset>

      {existente && (
        <div className="panel-admin-tarjeta pen-seccion">
          <h2>Acceso y estado</h2>
          {errorAccion && <Aviso tono="error">{errorAccion}</Aviso>}
          <div className="pen-acciones">
            {tienePermiso('pensionistas.modificar') && (
              <div>
                <Boton variante="secundario" onClick={() => setConfirmandoPin(true)}>
                  Generar PIN nuevo
                </Boton>
                <p className="prod-nota">
                  El PIN no se puede volver a ver. Si lo olvidó, se genera otro y el anterior deja de servir.
                </p>
              </div>
            )}
            {puedeDesactivar && (
              <div>
                <Boton variante={existente.activo ? 'peligro' : 'secundario'} onClick={() => setConfirmandoEstado(true)}>
                  {existente.activo ? 'Desactivar' : 'Reactivar'}
                </Boton>
                <p className="prod-nota">
                  {existente.activo
                    ? 'No podrá sacar tickets ni consumir. Su cuenta y su historial se conservan.'
                    : 'Vuelve a poder consumir, con la misma cuenta que tenía.'}
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {pin && (
        <Modal
          abierto
          titulo={pin.motivo === 'alta' ? 'Pensionista dado de alta' : 'PIN nuevo'}
          onCerrar={() => undefined}
          pie={
            <Boton
              onClick={() => {
                setPin(null)
                if (pin.motivo === 'alta') navigate('/admin/pensionistas')
              }}
            >
              Ya lo anoté
            </Boton>
          }
        >
          <p>
            Este es su PIN para el kiosco. Entrégaselo ahora: <strong>no se puede volver a ver.</strong>
          </p>
          <p className="pen-pin" aria-label={`PIN ${pin.valor.split('').join(' ')}`}>
            {pin.valor}
          </p>
          <p className="prod-nota">Si lo pierde, se genera uno nuevo desde su ficha.</p>
        </Modal>
      )}

      <ModalDeConfirmacion
        abierto={confirmandoPin}
        titulo="Generar un PIN nuevo"
        mensaje="El PIN actual dejará de servir y se mostrará el nuevo una sola vez. ¿Continuar?"
        textoConfirmar="Generar PIN"
        confirmando={trabajando}
        error={errorAccion || undefined}
        onConfirmar={nuevoPin}
        onCancelar={() => setConfirmandoPin(false)}
      />
      <ModalDeConfirmacion
        abierto={confirmandoEstado}
        titulo={existente?.activo ? 'Desactivar pensionista' : 'Reactivar pensionista'}
        mensaje={
          existente?.activo
            ? `¿Desactivar a ${existente?.nombres}? No podrá sacar tickets ni consumir, pero su cuenta y su historial se conservan.`
            : `¿Reactivar a ${existente?.nombres}? Seguirá con la misma cuenta que tenía.`
        }
        textoConfirmar={existente?.activo ? 'Desactivar' : 'Reactivar'}
        peligro={existente?.activo}
        confirmando={trabajando}
        error={errorAccion || undefined}
        onConfirmar={cambiarEstado}
        onCancelar={() => setConfirmandoEstado(false)}
      />
    </section>
  )
}
