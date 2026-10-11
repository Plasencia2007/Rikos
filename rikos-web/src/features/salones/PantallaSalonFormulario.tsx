import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft, Save } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Aviso, Boton, EstadoCargando, EstadoError, Interruptor } from '../../comun/componentes'
import {
  ErrorApi,
  actualizarSalon,
  cambiarEstadoSalon,
  crearSalon,
  obtenerSalon,
  type ColorDeSalon,
  type IconoDeSalon,
  type Salon,
} from '../../api/cliente'
import { COLORES_DE_SALON, EtiquetaDeSalon, ICONOS_DE_SALON, mensajeDeError } from './salonesVisual'

const COLOR_PISO_HEX = /^#[0-9A-Fa-f]{6}$/

/** Crear o editar un salón: nombre, descripción, icono y color, y el tamaño y color de piso de su plano. */
export function PantallaSalonFormulario() {
  const { salonId } = useParams<{ salonId: string }>()
  const esNuevo = salonId === undefined
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [cargando, setCargando] = useState(!esNuevo)
  const [errorCarga, setErrorCarga] = useState('')
  const [original, setOriginal] = useState<Salon | null>(null)
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [icono, setIcono] = useState<IconoDeSalon>('terraza')
  const [color, setColor] = useState<ColorDeSalon>('ambar')
  const [ancho, setAncho] = useState('1200')
  const [alto, setAlto] = useState('800')
  const [colorPiso, setColorPiso] = useState('#F3EEDF')
  const [activo, setActivo] = useState(true)
  const [errores, setErrores] = useState<{ nombre?: string; plano?: string; piso?: string }>({})
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    if (esNuevo) return
    obtenerSalon(token, salonId!)
      .then((s) => {
        setOriginal(s)
        setNombre(s.nombre)
        setDescripcion(s.descripcion ?? '')
        setIcono(s.icono)
        setColor(s.color)
        setAncho(String(s.anchoPlano))
        setAlto(String(s.altoPlano))
        setColorPiso(s.colorPiso)
        setActivo(s.activo)
      })
      .catch(() => setErrorCarga('No se pudo cargar el salón.'))
      .finally(() => setCargando(false))
  }, [esNuevo, salonId, token])

  async function guardar() {
    const nuevos: typeof errores = {}
    if (!nombre.trim()) nuevos.nombre = 'Escribe el nombre del salón.'
    const w = Number(ancho)
    const h = Number(alto)
    if (!Number.isInteger(w) || !Number.isInteger(h) || w <= 0 || h <= 0 || w > 10000 || h > 10000) {
      nuevos.plano = 'El ancho y el alto deben ser enteros entre 1 y 10000.'
    }
    if (!COLOR_PISO_HEX.test(colorPiso)) nuevos.piso = 'Usa un color como #F3EEDF.'
    setErrores(nuevos)
    setError('')
    if (Object.keys(nuevos).length > 0) return

    setGuardando(true)
    try {
      const datos = {
        nombre: nombre.trim(),
        descripcion: descripcion.trim() || null,
        icono,
        color,
        anchoPlano: w,
        altoPlano: h,
        colorPiso,
      }
      let id = salonId
      if (esNuevo) id = (await crearSalon(token, datos)).id
      else {
        await actualizarSalon(token, salonId!, datos)
        if (original && original.activo !== activo) await cambiarEstadoSalon(token, salonId!, activo)
      }
      navigate(esNuevo ? `/admin/configuracion/salones/${id}` : '/admin/configuracion/salones')
    } catch (e) {
      const codigo = e instanceof ErrorApi ? e.cuerpo?.error : undefined
      if (codigo === 'NOMBRE_DUPLICADO' || codigo === 'NOMBRE_INVALIDO') setErrores({ nombre: mensajeDeError(codigo, '') })
      else setError(mensajeDeError(codigo, 'No se pudo guardar el salón.'))
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  const volver = () => navigate('/admin/configuracion/salones')
  const c = COLORES_DE_SALON.find((x) => x.valor === color)!

  return (
    <section className="panel-admin sal-pagina">
      <div className="sal-encabezado-salon">
        <button type="button" className="as-volver" onClick={volver} aria-label="Volver a Salones">
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
            <span>{esNuevo ? 'Nuevo salón' : original?.nombre}</span>
          </nav>
          <div>
            <h1>{esNuevo ? 'Nuevo salón' : 'Editar salón'}</h1>
          </div>
          <p>Define la zona; las mesas se agregan después, dentro del salón.</p>
        </div>
      </div>

      <div className="sal-form-cuerpo">
        <div className="sal-form-columna">
          <section className="as-tarjeta" aria-labelledby="sal-info">
            <h2 id="sal-info" className="as-h2">
              Información general
            </h2>
            <label className="rf-campo">
              <span>
                Nombre del salón <span className="as-req">*</span>
              </span>
              <input
                type="text"
                placeholder="Ej. Terraza, Salón principal, Barra"
                value={nombre}
                className={errores.nombre ? 'invalido' : ''}
                aria-invalid={Boolean(errores.nombre) || undefined}
                onChange={(e) => {
                  setNombre(e.target.value)
                  setErrores((x) => ({ ...x, nombre: undefined }))
                }}
              />
              {errores.nombre && <span className="campo-error">{errores.nombre}</span>}
            </label>
            <label className="rf-campo">
              <span>
                Descripción <span className="as-suave">(opcional)</span>
              </span>
              <textarea
                rows={3}
                maxLength={250}
                placeholder="¿Cómo es esta zona? Lo verá el personal."
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
              />
            </label>
            <hr className="as-separador" />
            <div className="rf-campo">
              <span>Icono</span>
              <div className="sal-iconos" role="radiogroup" aria-label="Icono del salón">
                {ICONOS_DE_SALON.map(({ valor, etiqueta, Icono }) => (
                  <button
                    key={valor}
                    type="button"
                    role="radio"
                    aria-checked={icono === valor}
                    aria-label={etiqueta}
                    title={etiqueta}
                    className={`sal-opcion-icono ${icono === valor ? 'elegido' : ''}`}
                    onClick={() => setIcono(valor)}
                  >
                    <Icono size={24} strokeWidth={1.6} />
                  </button>
                ))}
              </div>
            </div>
            <div className="rf-campo">
              <span>Color</span>
              <div className="sal-colores" role="radiogroup" aria-label="Color del salón">
                {COLORES_DE_SALON.map((x) => (
                  <button
                    key={x.valor}
                    type="button"
                    role="radio"
                    aria-checked={color === x.valor}
                    aria-label={x.etiqueta}
                    className={`rf-punto ${color === x.valor ? 'elegido' : ''}`}
                    style={{ background: x.fuerte, ['--rf-color' as string]: x.fuerte, width: 28, height: 28 }}
                    onClick={() => setColor(x.valor)}
                  />
                ))}
              </div>
            </div>
          </section>

          <section className="as-tarjeta" aria-labelledby="sal-plano">
            <div className="as-titulo-bloque">
              <h2 id="sal-plano" className="as-h2">
                Plano del salón
              </h2>
              <span className="as-ayuda">El tamaño del lienzo donde ubicarás las mesas en el editor de plano visual.</span>
            </div>
            <div className="sal-form-fila">
              <label className="rf-campo">
                <span>
                  Ancho (px) <span className="as-req">*</span>
                </span>
                <input type="text" inputMode="numeric" value={ancho} onChange={(e) => setAncho(e.target.value)} />
              </label>
              <label className="rf-campo">
                <span>
                  Alto (px) <span className="as-req">*</span>
                </span>
                <input type="text" inputMode="numeric" value={alto} onChange={(e) => setAlto(e.target.value)} />
              </label>
              <label className="rf-campo">
                <span>Color del piso</span>
                <div className={`sal-color-piso ${errores.piso ? 'invalido' : ''}`}>
                  <input type="color" aria-label="Elegir color del piso" value={COLOR_PISO_HEX.test(colorPiso) ? colorPiso : '#F3EEDF'} onChange={(e) => setColorPiso(e.target.value.toUpperCase())} />
                  <input type="text" aria-label="Color del piso en hexadecimal" value={colorPiso} onChange={(e) => setColorPiso(e.target.value)} />
                </div>
              </label>
            </div>
            {errores.plano && <span className="campo-error">{errores.plano}</span>}
            {errores.piso && <span className="campo-error">{errores.piso}</span>}
            <span className="as-ayuda">El color del piso solo afecta el fondo del plano.</span>
          </section>
        </div>

        <aside className="sal-form-lateral" aria-label="Acciones">
          <section className="rf-resumen sal-acciones-tarjeta">
            <Boton onClick={() => void guardar()} cargando={guardando}>
              <Save size={18} aria-hidden="true" /> Guardar salón
            </Boton>
            <Boton variante="secundario" onClick={volver} disabled={guardando}>
              Cancelar
            </Boton>
            {!esNuevo && (
              <>
                <hr className="as-separador" />
                <div className="sal-estado-form">
                  <div>
                    <strong>Estado activo</strong>
                    <span>Aparece en el mapa del POS</span>
                  </div>
                  <Interruptor etiqueta="Estado activo" checked={activo} onChange={(e) => setActivo(e.target.checked)} />
                </div>
              </>
            )}
            {error && <Aviso tono="error">{error}</Aviso>}
          </section>
          <section className="sal-vista-previa">
            <span>ASÍ SE VERÁ EN EL MAPA</span>
            <div>
              <EtiquetaDeSalon icono={icono} color={color} />
              <div>
                <strong>{nombre.trim() || 'Nombre del salón'}</strong>
                <span style={{ color: c.fuerte }}>{original ? `${original.mesas} mesas · ${original.capacidad} personas` : 'Sin mesas todavía'}</span>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </section>
  )
}
