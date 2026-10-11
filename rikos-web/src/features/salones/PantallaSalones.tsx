import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, Info, Plus, Search } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  IconoBorrar,
  IconoLapiz,
  IconoReactivar,
  Interruptor,
  ModalDeConfirmacion,
} from '../../comun/componentes'
import { ErrorApi, cambiarEstadoSalon, eliminarSalon, listarSalones, type Salon } from '../../api/cliente'
import { EtiquetaDeSalon, mensajeDeError } from './salonesVisual'

/** Salones del local activo (terraza, salón principal, barra...): cada uno agrupa sus mesas y tiene su plano. */
export function PantallaSalones() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()
  const nombreDelLocal = sesion!.locales.find((l) => l.id === sesion!.localActivoId)?.nombre ?? 'el local activo'

  const [salones, setSalones] = useState<Salon[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [aEliminar, setAEliminar] = useState<Salon | null>(null)
  const [confirmando, setConfirmando] = useState(false)
  const [errorEliminar, setErrorEliminar] = useState('')

  function cargar() {
    setError('')
    listarSalones(token)
      .then(setSalones)
      .catch(() => setError('No se pudieron cargar los salones.'))
      .finally(() => setCargando(false))
  }

  // Cambiar de local en el encabezado vuelve a pedir la lista: los salones son del local activo.
  useEffect(cargar, [token, sesion!.localActivoId])

  useEffect(() => {
    if (!mensaje) return
    const t = setTimeout(() => setMensaje(''), 2800)
    return () => clearTimeout(t)
  }, [mensaje])

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return salones.filter((s) => !q || `${s.nombre} ${s.descripcion ?? ''}`.toLowerCase().includes(q))
  }, [salones, busqueda])

  const totalMesas = salones.reduce((n, s) => n + s.mesas, 0)
  const totalActivos = salones.filter((s) => s.activo).length

  async function alternar(s: Salon) {
    try {
      await cambiarEstadoSalon(token, s.id, !s.activo)
      setMensaje(`${s.nombre} ${s.activo ? 'desactivado' : 'reactivado'}`)
      cargar()
    } catch (e) {
      setMensaje(mensajeDeError(e instanceof ErrorApi ? e.cuerpo?.error : undefined, 'No se pudo cambiar el estado.'))
    }
  }

  async function confirmarEliminar() {
    if (!aEliminar) return
    setConfirmando(true)
    setErrorEliminar('')
    try {
      await eliminarSalon(token, aEliminar.id)
      setAEliminar(null)
      setMensaje('Salón eliminado')
      cargar()
    } catch (e) {
      setErrorEliminar(mensajeDeError(e instanceof ErrorApi ? e.cuerpo?.error : undefined, 'No se pudo eliminar el salón.'))
    } finally {
      setConfirmando(false)
    }
  }

  return (
    <section className="panel-admin sal-pagina">
      <div className="sal-cabecera">
        <div>
          <nav className="loc-migas" aria-label="Ruta">
            <Link to="/admin/configuracion" className="cfg-miga-enlace">
              Configuración
            </Link>
            <span aria-hidden="true">/</span>
            <span>Salones y mesas</span>
          </nav>
          <h1 className="cfg-titulo pc-titulo">Salones y mesas</h1>
          <p className="cfg-subtitulo">
            Organiza las zonas de tu restaurante y las mesas de cada una. Aplica al local <strong>{nombreDelLocal}</strong>.
          </p>
        </div>
        <Boton onClick={() => navigate('/admin/configuracion/salones/nuevo')}>
          <Plus size={17} strokeWidth={2.2} aria-hidden="true" /> Añadir salón
        </Boton>
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : (
        <>
          <div className="sal-filtros">
            <label className="loc-buscador sal-buscador">
              <Search size={17} aria-hidden="true" />
              <input
                type="search"
                aria-label="Buscar salón"
                placeholder="Buscar salón..."
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
              />
            </label>
            <span className="sal-resumen">
              {salones.length} {salones.length === 1 ? 'salón' : 'salones'} · {totalMesas} {totalMesas === 1 ? 'mesa' : 'mesas'} ·{' '}
              {totalActivos} {totalActivos === 1 ? 'activo' : 'activos'}
            </span>
          </div>

          {salones.length === 0 ? (
            <div className="loc-vacio">
              <strong>Todavía no hay salones</strong>
              <span>Crea el primero (por ejemplo, Salón principal o Terraza) y agrégale sus mesas.</span>
              <Boton onClick={() => navigate('/admin/configuracion/salones/nuevo')}>Añadir salón</Boton>
            </div>
          ) : (
            <section className="sal-tabla" aria-label="Salones">
              <div className="sal-fila sal-fila-cabecera">
                <span>NOMBRE DEL SALÓN</span>
                <span>DESCRIPCIÓN</span>
                <span>ESTADO</span>
                <span>MESAS</span>
                <span className="sal-acciones">ACCIONES</span>
              </div>
              {filtrados.length === 0 && <p className="sal-sin-resultados">Ningún salón coincide con la búsqueda.</p>}
              {filtrados.map((s) => (
                <div key={s.id} className="sal-fila">
                  <div className="sal-nombre">
                    <EtiquetaDeSalon icono={s.icono} color={s.color} />
                    <div>
                      <strong className={s.activo ? '' : 'inactivo'}>{s.nombre}</strong>
                      <span>Área de consumo</span>
                    </div>
                  </div>
                  <p className="sal-descripcion">{s.descripcion ?? '—'}</p>
                  <label className="sal-estado">
                    <Interruptor etiqueta={`${s.activo ? 'Desactivar' : 'Activar'} ${s.nombre}`} checked={s.activo} onChange={() => void alternar(s)} />
                    <span className={s.activo ? 'activo' : ''}>{s.activo ? 'Activo' : 'Inactivo'}</span>
                  </label>
                  <div className="sal-mesas">
                    <strong>
                      {s.mesas} {s.mesas === 1 ? 'mesa' : 'mesas'}
                    </strong>
                    <span>Capacidad: {s.capacidad}</span>
                  </div>
                  <div className="sal-acciones">
                    <button type="button" className="loc-boton-tenue sal-ver" onClick={() => navigate(`/admin/configuracion/salones/${s.id}`)}>
                      Ver mesas <ArrowRight size={15} strokeWidth={2.2} aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      className="as-icono-boton"
                      aria-label={`Editar ${s.nombre}`}
                      onClick={() => navigate(`/admin/configuracion/salones/${s.id}/editar`)}
                    >
                      <IconoLapiz width={18} height={18} aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      className={`as-icono-boton ${s.mesas === 0 ? 'peligro' : ''}`}
                      aria-label={s.mesas > 0 ? `No se puede eliminar ${s.nombre}: tiene mesas` : `Eliminar ${s.nombre}`}
                      aria-disabled={s.mesas > 0}
                      disabled={s.mesas > 0}
                      title={s.mesas > 0 ? 'Tiene mesas: desactívalo en lugar de eliminarlo' : 'Eliminar salón'}
                      onClick={() => {
                        setErrorEliminar('')
                        setAEliminar(s)
                      }}
                    >
                      <IconoBorrar width={18} height={18} aria-hidden="true" />
                    </button>
                  </div>
                </div>
              ))}
              <div className="sal-pie-tabla">
                Selecciona <strong>Ver mesas</strong> para administrar las mesas de un salón.
              </div>
            </section>
          )}

          <div className="as-nota" role="note">
            <Info size={18} aria-hidden="true" />
            <span>
              Un salón con mesas conserva su historial: desactívalo para que deje de aparecer en el POS. Solo se puede eliminar un
              salón sin mesas.
            </span>
          </div>
        </>
      )}

      {mensaje && (
        <div className="loc-toast" role="status">
          <IconoReactivar width={16} height={16} aria-hidden="true" /> {mensaje}
        </div>
      )}

      <ModalDeConfirmacion
        abierto={aEliminar != null}
        titulo="Eliminar salón"
        mensaje={`"${aEliminar?.nombre}" se eliminará permanentemente. Esta acción no se puede deshacer.`}
        textoConfirmar="Eliminar"
        peligro
        error={errorEliminar}
        confirmando={confirmando}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setAEliminar(null)}
      />
    </section>
  )
}
