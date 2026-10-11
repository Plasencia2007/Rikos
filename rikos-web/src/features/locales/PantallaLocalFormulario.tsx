import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { ArrowRight, Check } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Aviso, Boton, EstadoCargando, EstadoError } from '../../comun/componentes'
import { ErrorApi, actualizarLocal, crearLocal, obtenerLocal, type Local } from '../../api/cliente'
import { esCodigoValido, sugerirCodigo } from './codigoLocal'
import {
  COLOR_HEX,
  FORMULARIO_VACIO,
  datosParaGuardar,
  erroresDeContacto,
  formularioDesdeLocal,
  type FormularioLocal,
} from './formularioLocal'
import { MarcoAsistente, PASOS, type IdPaso } from './MarcoAsistente'
import { LateralDatos, PasoDatos } from './PasoDatos'
import { LateralContacto, PasoContacto } from './PasoContacto'
import { LateralOperacion, PasoOperacion } from './PasoOperacion'
import { LateralAspecto, PasoAspecto } from './PasoAspecto'
import type { ManejadorHorario, TurnoEdicion } from './HorarioSemanal'

const MENSAJE_CODIGO = 'Solo minúsculas, números y guiones. Sin espacios, tildes ni ñ.'

interface EstadoDeNavegacion {
  ubicacion?: { latitud: number | null; longitud: number | null }
}

/**
 * Asistente de 4 pasos para crear y editar un local: Datos, Contacto,
 * Operación y Aspecto. El local se crea al terminar el paso 1 (los demás
 * pasos trabajan sobre su id), y desde ahí cada paso guarda con un PUT que
 * reemplaza el local entero -- por eso hay una sola copia `form` con todo lo
 * editable, compartida por los cuatro pasos.
 */
export function PantallaLocalFormulario() {
  const { id, paso: pasoUrl } = useParams<{ id: string; paso: string }>()
  const esNuevo = id === undefined
  const paso: IdPaso = PASOS.some((p) => p.id === pasoUrl) ? (pasoUrl as IdPaso) : 'datos'
  const navigate = useNavigate()
  const ubicacion = useLocation()
  const { sesion, refrescarLocales } = useSesion()
  const token = sesion!.token

  const [cargando, setCargando] = useState(!esNuevo)
  const [errorCarga, setErrorCarga] = useState('')
  const [local, setLocal] = useState<Local | null>(null)
  const [form, setForm] = useState<FormularioLocal>(FORMULARIO_VACIO)
  const [codigoTocado, setCodigoTocado] = useState(false)
  const [errores, setErrores] = useState<Record<string, string | undefined>>({})
  const [guardando, setGuardando] = useState(false)
  const [errorGuardar, setErrorGuardar] = useState('')
  const [guardadoOk, setGuardadoOk] = useState(false)
  const [turnos, setTurnos] = useState<TurnoEdicion[]>([])
  const horarioRef = useRef<ManejadorHorario>(null)

  useEffect(() => {
    if (esNuevo) return
    setCargando(true)
    obtenerLocal(token, id!)
      .then((l) => {
        setLocal(l)
        const desdeServidor = formularioDesdeLocal(l)
        // El punto del mapa marcado antes de crear el local viaja por el estado de navegación:
        // el POST de crear no lo lleva, se guarda con el primer PUT (paso Contacto).
        const pendiente = (ubicacion.state as EstadoDeNavegacion | null)?.ubicacion
        setForm(
          pendiente && desdeServidor.latitud == null
            ? { ...desdeServidor, latitud: pendiente.latitud, longitud: pendiente.longitud }
            : desdeServidor,
        )
      })
      .catch(() => setErrorCarga('No se pudo cargar este local.'))
      .finally(() => setCargando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [esNuevo, id, token])

  function cambiar(cambios: Partial<FormularioLocal>) {
    setGuardadoOk(false)
    setErrorGuardar('')
    setForm((f) => ({ ...f, ...cambios }))
  }

  function cambiarNombre(nombre: string) {
    setErrores((e) => ({ ...e, nombre: undefined }))
    setForm((f) => ({ ...f, nombre, codigo: esNuevo && !codigoTocado ? sugerirCodigo(nombre) : f.codigo }))
  }

  function cambiarCodigo(codigo: string) {
    setCodigoTocado(true)
    setErrores((e) => ({ ...e, codigo: undefined }))
    setForm((f) => ({ ...f, codigo }))
  }

  const irAPaso = (p: IdPaso) => navigate(`/admin/locales/${id}/editar/${p}`)
  const volverALista = () => navigate('/admin/locales')

  async function guardarLocal(): Promise<boolean> {
    if (!local) return false
    try {
      const actualizado = await actualizarLocal(token, local.id, datosParaGuardar(form))
      setLocal(actualizado)
      setGuardadoOk(true)
      return true
    } catch (excepcion) {
      setErrorGuardar(
        excepcion instanceof ErrorApi && excepcion.estado === 400
          ? 'Faltan datos obligatorios. Revisa el teléfono y el correo en el paso Contacto.'
          : 'No se pudo guardar el local.',
      )
      return false
    }
  }

  async function continuarDatos() {
    const nuevos: typeof errores = {}
    if (!form.nombre.trim()) nuevos.nombre = 'Ingresa el nombre del local.'
    if (esNuevo && !esCodigoValido(form.codigo)) nuevos.codigo = form.codigo ? MENSAJE_CODIGO : 'Ingresa el código del local.'
    if (!form.direccion.trim()) nuevos.direccion = 'Ingresa la dirección del local.'
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return

    setGuardando(true)
    setErrorGuardar('')
    try {
      if (esNuevo) {
        const creado = await crearLocal(token, { codigo: form.codigo, nombre: form.nombre, direccion: form.direccion })
        await refrescarLocales()
        const estado: EstadoDeNavegacion = { ubicacion: { latitud: form.latitud, longitud: form.longitud } }
        navigate(`/admin/locales/${creado.id}/editar/contacto`, { replace: true, state: estado })
        return
      }
      // Un local antiguo puede no tener teléfono ni correo todavía: el PUT los exige,
      // así que los cambios de este paso se guardan junto con el paso Contacto.
      if (Object.keys(erroresDeContacto(form)).length === 0) {
        if (!(await guardarLocal())) return
      }
      irAPaso('contacto')
    } catch (excepcion) {
      if (excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'CODIGO_DUPLICADO') {
        setErrores({ codigo: 'Ya existe un local con ese código.' })
      } else if (excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'CODIGO_FORMATO_INVALIDO') {
        setErrores({ codigo: MENSAJE_CODIGO })
      } else {
        setErrorGuardar('No se pudo crear el local.')
      }
    } finally {
      setGuardando(false)
    }
  }

  async function continuarContacto() {
    const nuevos = erroresDeContacto(form)
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return
    setGuardando(true)
    setErrorGuardar('')
    if (await guardarLocal()) irAPaso('operacion')
    setGuardando(false)
  }

  async function continuarOperacion() {
    const min = Number(form.tiempoMinEntregaMin)
    const max = Number(form.tiempoMaxEntregaMin)
    const nuevos: typeof errores = {}
    if (form.deliveryHabilitado && form.tiempoMinEntregaMin && form.tiempoMaxEntregaMin && max < min) {
      nuevos.tiempos = 'El tiempo máximo no puede ser menor que el mínimo.'
    }
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return
    setGuardando(true)
    setErrorGuardar('')
    if ((await guardarLocal()) && (await horarioRef.current?.guardar())) irAPaso('aspecto')
    setGuardando(false)
  }

  async function finalizar() {
    if (form.colorPrincipal && !COLOR_HEX.test(form.colorPrincipal)) {
      setErrorGuardar('El color principal no es válido.')
      return
    }
    setGuardando(true)
    setErrorGuardar('')
    if (await guardarLocal()) {
      await refrescarLocales()
      volverALista()
    }
    setGuardando(false)
  }

  if (!esNuevo && cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  const indice = PASOS.findIndex((p) => p.id === paso)
  const anterior = indice > 0 ? PASOS[indice - 1].id : null
  const nombreLocal = local?.nombre ?? form.nombre

  const encabezados: Record<IdPaso, { titulo: string; subtitulo: string; ruta: string[] }> = {
    datos: esNuevo
      ? {
          titulo: 'Crear local',
          subtitulo: 'Primero agrega la información necesaria para identificar la sede.',
          ruta: ['Locales', 'Nuevo local'],
        }
      : {
          titulo: 'Datos del local',
          subtitulo: 'Actualiza la información que identifica la sede.',
          ruta: ['Locales', nombreLocal, 'Datos'],
        },
    contacto: {
      titulo: 'Contacto del local',
      subtitulo: 'Agrega los medios de contacto y redes sociales para que tus clientes puedan encontrarte.',
      ruta: ['Locales', nombreLocal, 'Contacto'],
    },
    operacion: {
      titulo: 'Configurar operación',
      subtitulo: 'Define cómo recibes pedidos y cuándo atiende este local.',
      ruta: ['Locales', nombreLocal, 'Operación'],
    },
    aspecto: {
      titulo: 'Identidad del local',
      subtitulo: 'Personaliza cómo se presenta este local en el menú digital.',
      ruta: ['Locales', nombreLocal, 'Aspecto'],
    },
  }

  const continuar = { datos: continuarDatos, contacto: continuarContacto, operacion: continuarOperacion, aspecto: finalizar }[paso]
  const textoContinuar = {
    datos: esNuevo ? 'Crear local y continuar' : 'Guardar y continuar',
    contacto: 'Guardar y continuar',
    operacion: 'Guardar y continuar',
    aspecto: 'Finalizar configuración',
  }[paso]

  const pie = (
    <>
      {paso === 'datos' ? (
        <Boton variante="secundario" onClick={volverALista}>
          Cancelar
        </Boton>
      ) : (
        <Boton variante="secundario" onClick={() => irAPaso(anterior!)}>
          Volver
        </Boton>
      )}
      <span className="as-pie-relleno" />
      {paso === 'datos' && esNuevo && (
        <span className="as-pie-nota">Se creará el local para poder completar los demás ajustes.</span>
      )}
      {guardadoOk && !esNuevo && paso !== 'datos' && (
        <span className="as-pie-guardado" role="status">
          <Check size={18} strokeWidth={2.4} aria-hidden="true" /> Cambios guardados
        </span>
      )}
      {paso === 'aspecto' && (
        <button type="button" className="as-enlace as-pie-omitir" onClick={volverALista}>
          Omitir por ahora
        </button>
      )}
      <Boton onClick={continuar} cargando={guardando}>
        {textoContinuar}{' '}
        {paso === 'aspecto' ? <Check size={17} aria-hidden="true" /> : <ArrowRight size={17} aria-hidden="true" />}
      </Boton>
    </>
  )

  const lateral = {
    datos: <LateralDatos />,
    contacto: <LateralContacto form={form} />,
    operacion: <LateralOperacion form={form} turnos={turnos} />,
    aspecto: <LateralAspecto form={form} />,
  }[paso]

  return (
    <MarcoAsistente
      paso={paso}
      pasosHabilitados={!esNuevo}
      {...encabezados[paso]}
      onIrAPaso={irAPaso}
      onVolverALista={volverALista}
      lateral={lateral}
      pie={pie}
    >
      {paso === 'datos' && (
        <PasoDatos
          form={form}
          esNuevo={esNuevo}
          errores={errores}
          onCambiar={cambiar}
          onCambiarNombre={cambiarNombre}
          onCambiarCodigo={cambiarCodigo}
        />
      )}
      {paso === 'contacto' && <PasoContacto form={form} errores={errores} onCambiar={cambiar} />}
      {paso === 'operacion' && local && (
        <PasoOperacion
          token={token}
          localId={local.id}
          form={form}
          errores={errores}
          horarioRef={horarioRef}
          onCambiar={cambiar}
          onCambiarTurnos={setTurnos}
        />
      )}
      {paso === 'aspecto' && <PasoAspecto form={form} onCambiar={cambiar} />}
      {errorGuardar && <Aviso tono="error">{errorGuardar}</Aviso>}
    </MarcoAsistente>
  )
}
