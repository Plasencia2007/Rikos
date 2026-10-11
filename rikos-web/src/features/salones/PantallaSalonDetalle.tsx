import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft, Circle, Info, LayoutGrid, Plus, RectangleHorizontal, Square, Armchair } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  IconoBloquear,
  IconoLapiz,
  IconoReactivar,
  Interruptor,
} from '../../comun/componentes'
import {
  ErrorApi,
  activarMesa,
  actualizarMesa,
  desactivarMesa,
  listarMesas,
  obtenerSalon,
  type Mesa,
  type Salon,
} from '../../api/cliente'
import { datosDeMesa } from './datosDeMesa'
import { etiquetaDeForma, mensajeDeError } from './salonesVisual'

const ICONO_DE_FORMA = { cuadrada: Square, redonda: Circle, rectangular: RectangleHorizontal }

/** Las mesas de un salón: lista para revisar y editar sus datos; el plano visual las ubica. */
export function PantallaSalonDetalle() {
  const { salonId } = useParams<{ salonId: string }>()
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()
  const nombreDelLocal = sesion!.locales.find((l) => l.id === sesion!.localActivoId)?.nombre ?? ''

  const [salon, setSalon] = useState<Salon | null>(null)
  const [mesas, setMesas] = useState<Mesa[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')

  function cargar() {
    setError('')
    Promise.all([obtenerSalon(token, salonId!), listarMesas(token, true)])
      .then(([s, todas]) => {
        setSalon(s)
        setMesas(todas.filter((m) => m.salonId === salonId))
      })
      .catch((e) => setError(e instanceof ErrorApi && e.estado === 404 ? 'Este salón ya no existe.' : 'No se pudo cargar el salón.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token, salonId])

  useEffect(() => {
    if (!mensaje) return
    const t = setTimeout(() => setMensaje(''), 2800)
    return () => clearTimeout(t)
  }, [mensaje])

  async function alternarVip(m: Mesa) {
    try {
      await actualizarMesa(token, m.id, datosDeMesa(m, { vip: !m.vip }))
      cargar()
    } catch (e) {
      setMensaje(mensajeDeError(e instanceof ErrorApi ? e.cuerpo?.error : undefined, 'No se pudo cambiar la mesa.'))
    }
  }

  async function alternarActiva(m: Mesa) {
    try {
      if (m.activo) await desactivarMesa(token, m.id)
      else await activarMesa(token, m.id)
      setMensaje(`Mesa ${m.numero} ${m.activo ? 'desactivada' : 'reactivada'}`)
      cargar()
    } catch (e) {
      setMensaje(mensajeDeError(e instanceof ErrorApi ? e.cuerpo?.error : undefined, 'No se pudo cambiar el estado.'))
    }
  }

  if (cargando) return <EstadoCargando />
  if (error || !salon) return <EstadoError mensaje={error || 'No se pudo cargar el salón.'} accion={<Boton onClick={cargar}>Reintentar</Boton>} />

  const activas = mesas.filter((m) => m.activo)
  const capacidad = activas.reduce((n, m) => n + (m.capacidad ?? 0), 0)
  const base = `/admin/configuracion/salones/${salon.id}`

  return (
    <section className="panel-admin sal-pagina">
      <div>
        <nav className="loc-migas" aria-label="Ruta">
          <Link to="/admin/configuracion" className="cfg-miga-enlace">
            Configuración
          </Link>
          <span aria-hidden="true">/</span>
          <Link to="/admin/configuracion/salones" className="cfg-miga-enlace">
            Salones y mesas
          </Link>
          <span aria-hidden="true">/</span>
          <span>{salon.nombre}</span>
        </nav>
      </div>

      <div className="sal-encabezado-salon">
        <button type="button" className="as-volver" onClick={() => navigate('/admin/configuracion/salones')} aria-label="Volver a Salones">
          <ChevronLeft size={16} aria-hidden="true" />
        </button>
        <div className="sal-encabezado-texto">
          <div>
            <h1>{salon.nombre}</h1>
            {nombreDelLocal && <span className="sal-insignia">LOCAL {nombreDelLocal.toUpperCase()}</span>}
            {!salon.activo && <span className="sal-insignia inactivo">INACTIVO</span>}
          </div>
          <p>
            {mesas.length} {mesas.length === 1 ? 'mesa ubicada' : 'mesas ubicadas'} · capacidad total de {capacidad} personas
          </p>
        </div>
        <Boton onClick={() => navigate(`${base}/plano`)}>
          <LayoutGrid size={15} strokeWidth={2.4} aria-hidden="true" /> Editor de plano visual
        </Boton>
        <Boton variante="secundario" onClick={() => navigate(`${base}/mesas/nueva`)}>
          <Plus size={16} strokeWidth={2} aria-hidden="true" /> Generar mesa
        </Boton>
      </div>

      <div className="sal-banner" role="note">
        <Info size={16} aria-hidden="true" />
        <div>
          <strong>Organiza también desde el plano visual</strong>
          <span>Arrastra las mesas para definir su ubicación y medidas. La lista de abajo sirve para revisar y editar sus datos.</span>
        </div>
      </div>

      {mesas.length === 0 ? (
        <div className="loc-vacio">
          <strong>Este salón todavía no tiene mesas</strong>
          <span>Genera la primera y luego la acomodas en el plano visual.</span>
          <Boton onClick={() => navigate(`${base}/mesas/nueva`)}>Generar mesa</Boton>
        </div>
      ) : (
        <section className="sal-tabla" aria-label={`Mesas de ${salon.nombre}`}>
          <div className="sal-fila sal-fila-mesa sal-fila-cabecera">
            <span>CÓD. INTERNO</span>
            <span>IDENTIFICADOR DE MESA</span>
            <span>SILLAS</span>
            <span>FORMATO</span>
            <span>VIP</span>
            <span>ESTADO</span>
            <span className="sal-acciones">ACCIONES</span>
          </div>
          {mesas.map((m) => {
            const IconoForma = ICONO_DE_FORMA[m.forma] ?? Square
            return (
              <div key={m.id} className="sal-fila sal-fila-mesa">
                <span className="sal-codigo">{m.numero}</span>
                <div className="sal-identificador">
                  <strong className={m.activo ? '' : 'inactivo'}>{m.nombre ?? `Mesa ${m.numero}`}</strong>
                  <div>
                    {m.vip && <span className="sal-chip vip">VIP</span>}
                    {m.zonaExterior && <span className="sal-chip exterior">Z. Exterior</span>}
                    {m.zonaFumadores && <span className="sal-chip fumador">Z. Fumador</span>}
                  </div>
                </div>
                <span className="sal-sillas">
                  <Armchair size={16} aria-hidden="true" /> {m.capacidad ?? '—'}
                </span>
                <span className="sal-forma">
                  <IconoForma size={17} aria-hidden="true" /> {etiquetaDeForma(m.forma)}
                </span>
                <Interruptor etiqueta={`Mesa VIP: ${m.nombre ?? m.numero}`} checked={m.vip} onChange={() => void alternarVip(m)} />
                <span className={`mot-estado ${m.activo ? 'activo' : ''}`}>
                  <i aria-hidden="true" /> {m.activo ? 'Activa' : 'Inactiva'}
                </span>
                <div className="sal-acciones">
                  <button type="button" className="as-icono-boton" aria-label={`Editar ${m.nombre ?? m.numero}`} onClick={() => navigate(`${base}/mesas/${m.id}/editar`)}>
                    <IconoLapiz width={18} height={18} aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    className={`as-icono-boton ${m.activo ? 'peligro' : ''}`}
                    aria-label={`${m.activo ? 'Desactivar' : 'Reactivar'} ${m.nombre ?? m.numero}`}
                    title={m.activo ? 'Desactivar (sale del mapa, conserva su historial)' : 'Reactivar'}
                    onClick={() => void alternarActiva(m)}
                  >
                    {m.activo ? <IconoBloquear width={18} height={18} aria-hidden="true" /> : <IconoReactivar width={18} height={18} aria-hidden="true" />}
                  </button>
                </div>
              </div>
            )
          })}
          <div className="sal-pie-tabla">
            {activas.length} {activas.length === 1 ? 'activa' : 'activas'} de {mesas.length} {mesas.length === 1 ? 'mesa' : 'mesas'}
          </div>
        </section>
      )}

      {mensaje && (
        <div className="loc-toast" role="status">
          <IconoReactivar width={16} height={16} aria-hidden="true" /> {mensaje}
        </div>
      )}
    </section>
  )
}
