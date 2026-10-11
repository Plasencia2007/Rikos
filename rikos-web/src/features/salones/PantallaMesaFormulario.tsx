import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft, Info, Save } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Aviso, Boton, EstadoCargando, EstadoError, Interruptor } from '../../comun/componentes'
import {
  ErrorApi,
  activarMesa,
  actualizarMesa,
  crearMesa,
  desactivarMesa,
  listarMesas,
  listarSalones,
  type FormaDeMesa,
  type Mesa,
  type Salon,
} from '../../api/cliente'
import { FORMAS_DE_MESA, mensajeDeError } from './salonesVisual'
import { VistaDeMesa } from './VistaDeMesa'

/** El siguiente número libre a partir de los que ya hay: "7" -> "8", "T07" -> "T08". */
export function siguienteNumero(existentes: string[]): string {
  let mejor: { prefijo: string; n: number; largo: number } | null = null
  for (const numero of existentes) {
    const m = /^(\D*)(\d+)$/.exec(numero)
    if (m && (!mejor || Number(m[2]) > mejor.n)) mejor = { prefijo: m[1], n: Number(m[2]), largo: m[2].length }
  }
  if (!mejor) return '1'
  const siguiente = String(mejor.n + 1).padStart(mejor.largo, '0')
  return `${mejor.prefijo}${siguiente}`.slice(0, 10)
}

/** Generar o editar una mesa: número, capacidad, forma, atributos y salón. */
export function PantallaMesaFormulario() {
  const { salonId, mesaId } = useParams<{ salonId: string; mesaId: string }>()
  const esNueva = mesaId === undefined
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [salones, setSalones] = useState<Salon[]>([])
  const [mesa, setMesa] = useState<Mesa | null>(null)
  const [salonElegido, setSalonElegido] = useState(salonId ?? '')
  const [numero, setNumero] = useState('')
  const [nombre, setNombre] = useState('')
  const [capacidad, setCapacidad] = useState('4')
  const [forma, setForma] = useState<FormaDeMesa>('cuadrada')
  const [vip, setVip] = useState(false)
  const [zonaExterior, setZonaExterior] = useState(false)
  const [zonaFumadores, setZonaFumadores] = useState(false)
  const [activa, setActiva] = useState(true)
  const [errores, setErrores] = useState<{ numero?: string; capacidad?: string }>({})
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    Promise.all([listarSalones(token), listarMesas(token, true)])
      .then(([ss, todas]) => {
        setSalones(ss)
        if (esNueva) {
          setNumero(siguienteNumero(todas.map((m) => m.numero)))
          return
        }
        const m = todas.find((x) => x.id === mesaId)
        if (!m) throw new Error('no encontrada')
        setMesa(m)
        setSalonElegido(m.salonId ?? salonId ?? '')
        setNumero(m.numero)
        setNombre(m.nombre ?? '')
        setCapacidad(m.capacidad != null ? String(m.capacidad) : '')
        setForma(m.forma)
        setVip(m.vip)
        setZonaExterior(m.zonaExterior)
        setZonaFumadores(m.zonaFumadores)
        setActiva(m.activo)
      })
      .catch(() => setErrorCarga('No se pudo cargar la mesa.'))
      .finally(() => setCargando(false))
  }, [esNueva, mesaId, salonId, token])

  async function guardar() {
    const nuevos: typeof errores = {}
    if (!numero.trim()) nuevos.numero = 'Escribe el número de la mesa.'
    else if (numero.trim().length > 10) nuevos.numero = 'Hasta 10 caracteres.'
    const cap = capacidad.trim() === '' ? null : Number(capacidad)
    if (cap != null && (!Number.isInteger(cap) || cap <= 0)) nuevos.capacidad = 'La capacidad debe ser un número mayor que cero.'
    setErrores(nuevos)
    setError('')
    if (Object.keys(nuevos).length > 0) return

    setGuardando(true)
    try {
      const datos = {
        numero: numero.trim(),
        nombre: nombre.trim() || null,
        capacidad: cap,
        salonId: salonElegido || null,
        forma,
        vip,
        zonaExterior,
        zonaFumadores,
        // Las medidas solo se tocan al editar en el plano; al cambiar la forma vuelven a las de la forma.
        ancho: mesa && mesa.forma === forma ? mesa.ancho : null,
        alto: mesa && mesa.forma === forma ? mesa.alto : null,
      }
      let id = mesaId
      if (esNueva) id = (await crearMesa(token, datos)).id
      else await actualizarMesa(token, mesaId!, datos)
      if ((esNueva && !activa) || (!esNueva && mesa && mesa.activo !== activa)) {
        if (activa) await activarMesa(token, id!)
        else await desactivarMesa(token, id!)
      }
      navigate(`/admin/configuracion/salones/${salonElegido || salonId}`)
    } catch (e) {
      const codigo = e instanceof ErrorApi ? e.cuerpo?.error : undefined
      if (codigo === 'NUMERO_DUPLICADO' || codigo === 'NUMERO_INVALIDO') setErrores({ numero: mensajeDeError(codigo, '') })
      else if (codigo === 'CAPACIDAD_INVALIDA') setErrores({ capacidad: mensajeDeError(codigo, '') })
      else setError(mensajeDeError(codigo, 'No se pudo guardar la mesa.'))
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  const salonActual = salones.find((s) => s.id === (salonElegido || salonId))
  const volver = () => navigate(`/admin/configuracion/salones/${salonId}`)

  return (
    <section className="panel-admin sal-pagina">
      <div className="sal-encabezado-salon">
        <button type="button" className="as-volver" onClick={volver} aria-label="Volver a las mesas del salón">
          <ChevronLeft size={18} aria-hidden="true" />
        </button>
        <div className="sal-encabezado-texto">
          <nav className="loc-migas" aria-label="Ruta">
            <Link to="/admin/configuracion" className="cfg-miga-enlace">
              Configuración
            </Link>
            <span aria-hidden="true">/</span>
            <Link to="/admin/configuracion/salones" className="cfg-miga-enlace">
              Salones y mesas
            </Link>
            <span aria-hidden="true">/</span>
            <Link to={`/admin/configuracion/salones/${salonId}`} className="cfg-miga-enlace">
              {salonActual?.nombre ?? 'Salón'}
            </Link>
            <span aria-hidden="true">/</span>
            <span>{esNueva ? 'Nueva mesa' : `Mesa ${mesa?.numero}`}</span>
          </nav>
          <div>
            <h1>{esNueva ? 'Generar mesa' : 'Editar mesa'}</h1>
          </div>
          <p>
            {esNueva ? 'Crea la mesa con su capacidad y forma. ' : 'Cambia los datos de la mesa. '}
            Estará en el salón <strong>{salonActual?.nombre ?? '—'}</strong>.
          </p>
        </div>
      </div>

      <div className="sal-form-cuerpo">
        <div className="sal-form-columna">
          <section className="as-tarjeta" aria-labelledby="mesa-info">
            <h2 id="mesa-info" className="as-h2">
              Información de la mesa
            </h2>
            <div className="sal-form-fila sal-dos">
              <label className="rf-campo">
                <span>Nombre o etiqueta</span>
                <input type="text" placeholder="Ej. Mesa 1, Ventana" maxLength={60} value={nombre} onChange={(e) => setNombre(e.target.value)} />
                <span className="as-ayuda">Opcional. Si lo dejas vacío, el mapa muestra el número.</span>
              </label>
              <label className="rf-campo">
                <span>
                  Número de mesa <span className="as-req">*</span>
                </span>
                <input
                  type="text"
                  placeholder="Ej. T07"
                  value={numero}
                  className={errores.numero ? 'invalido' : ''}
                  onChange={(e) => {
                    setNumero(e.target.value)
                    setErrores((x) => ({ ...x, numero: undefined }))
                  }}
                />
                {errores.numero ? <span className="campo-error">{errores.numero}</span> : <span className="as-ayuda">Único en el local. Sugerido: el siguiente libre.</span>}
              </label>
            </div>
            <div className="sal-form-fila sal-dos">
              <label className="rf-campo">
                <span>Capacidad (n.º de sillas)</span>
                <input
                  type="text"
                  inputMode="numeric"
                  value={capacidad}
                  className={errores.capacidad ? 'invalido' : ''}
                  onChange={(e) => {
                    setCapacidad(e.target.value)
                    setErrores((x) => ({ ...x, capacidad: undefined }))
                  }}
                />
                {errores.capacidad && <span className="campo-error">{errores.capacidad}</span>}
              </label>
              <div className="rf-campo">
                <span>Forma de la mesa</span>
                <div className="sal-formas" role="radiogroup" aria-label="Forma de la mesa">
                  {FORMAS_DE_MESA.map((f) => (
                    <button key={f.valor} type="button" role="radio" aria-checked={forma === f.valor} className={`sal-forma-opcion ${forma === f.valor ? 'elegida' : ''}`} onClick={() => setForma(f.valor)}>
                      {f.etiqueta}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            {salones.length > 1 && (
              <label className="rf-campo">
                <span>Salón</span>
                <select value={salonElegido} onChange={(e) => setSalonElegido(e.target.value)}>
                  {salones.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.nombre}
                      {s.activo ? '' : ' (inactivo)'}
                    </option>
                  ))}
                </select>
              </label>
            )}
          </section>

          <section className="as-tarjeta" aria-labelledby="mesa-rasgos">
            <div className="as-titulo-bloque">
              <h2 id="mesa-rasgos" className="as-h2">
                Zonificación y características
              </h2>
              <span className="as-ayuda">Marca si esta mesa tiene atributos o condiciones especiales por su ubicación.</span>
            </div>
            {[
              { t: 'Mesa exclusiva VIP', d: '¿Requiere consumo mínimo o pago extra?', v: vip, f: setVip },
              { t: 'Zona exterior (terraza)', d: 'Ubicada al aire libre.', v: zonaExterior, f: setZonaExterior },
              { t: 'Zona de fumadores', d: 'Permitido el uso de cigarrillo o vape.', v: zonaFumadores, f: setZonaFumadores },
            ].map((r) => (
              <div key={r.t} className="sal-rasgo">
                <div>
                  <strong>{r.t}</strong>
                  <span>{r.d}</span>
                </div>
                <Interruptor etiqueta={r.t} checked={r.v} onChange={(e) => r.f(e.target.checked)} />
              </div>
            ))}
          </section>
        </div>

        <aside className="sal-form-lateral" aria-label="Acciones">
          <section className="rf-resumen sal-acciones-tarjeta">
            <div className="sal-estado-form">
              <div>
                <strong>Estado activo</strong>
                <span>Visible para que el mozo la tome</span>
              </div>
              <Interruptor etiqueta="Estado activo" checked={activa} onChange={(e) => setActiva(e.target.checked)} />
            </div>
            <hr className="as-separador" />
            <Boton onClick={() => void guardar()} cargando={guardando}>
              <Save size={18} aria-hidden="true" /> Guardar mesa
            </Boton>
            <Boton variante="secundario" onClick={volver} disabled={guardando}>
              Volver a las mesas
            </Boton>
            {error && <Aviso tono="error">{error}</Aviso>}
          </section>
          <section className="sal-vista-previa">
            <span>ASÍ SE VERÁ EN EL MAPA</span>
            <div className="sal-vista-plano">
              <VistaDeMesa forma={forma} etiqueta={numero || '—'} sillas={Number(capacidad) || 0} />
            </div>
            <p className="as-ayuda">
              <Info size={14} aria-hidden="true" /> Al guardar, la mesa aparece en un lugar libre del plano; luego la acomodas en el editor de
              plano visual.
            </p>
          </section>
        </aside>
      </div>
    </section>
  )
}
