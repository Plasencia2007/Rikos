import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton } from '../../comun/componentes/Boton'
import { ModalDeConfirmacion } from '../../comun/componentes/ModalDeConfirmacion'
import { Tabla, type ColumnaTabla } from '../../comun/componentes/Tabla'
import {
  IconoBusqueda,
  IconoEscudo,
  IconoEngranaje,
  IconoFlechaIzquierda,
  IconoFlechaDerecha,
  IconoLapiz,
  IconoMas,
  IconoPersonas,
  IconoProhibido,
  IconoTienda,
  IconoUbicacion,
} from '../../comun/componentes'
import { ErrorApi, eliminarLocal, listarLocales, type Local } from '../../api/cliente'

/** US-001: alta, edición y eliminación de locales. */
export function PantallaLocales() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [locales, setLocales] = useState<Local[]>([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [busqueda, setBusqueda] = useState('')

  const [localAEliminar, setLocalAEliminar] = useState<Local | null>(null)
  const [eliminando, setEliminando] = useState(false)
  const [errorEliminar, setErrorEliminar] = useState('')

  async function recargar() {
    setCargando(true)
    setErrorCarga('')
    try {
      setLocales(await listarLocales(token))
    } catch {
      setErrorCarga('No se pudo cargar la lista de locales.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    void recargar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function abrirEliminar(local: Local) {
    setLocalAEliminar(local)
    setErrorEliminar('')
  }

  async function confirmarEliminar() {
    if (!localAEliminar) return
    setEliminando(true)
    setErrorEliminar('')
    try {
      await eliminarLocal(token, localAEliminar.id)
      setLocalAEliminar(null)
      await recargar()
    } catch (excepcion) {
      setErrorEliminar(
        excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'TIENE_HISTORIAL'
          ? 'No se puede eliminar: este local ya tiene ventas registradas. Puedes desactivarlo en su lugar.'
          : 'No se pudo eliminar el local.',
      )
    } finally {
      setEliminando(false)
    }
  }

  const localesFiltrados = useMemo(() => {
    const termino = busqueda.trim().toLowerCase()
    if (!termino) return locales
    return locales.filter(
      (l) => l.nombre.toLowerCase().includes(termino) || l.direccion.toLowerCase().includes(termino),
    )
  }, [locales, busqueda])

  const totales = useMemo(
    () => ({
      total: locales.length,
      activos: locales.filter((l) => l.activo).length,
      inactivos: locales.filter((l) => !l.activo).length,
    }),
    [locales],
  )

  const columnas: ColumnaTabla<Local>[] = [
    {
      encabezado: 'Nombre',
      iconoEncabezado: <IconoTienda width={15} height={15} aria-hidden="true" />,
      render: (l) => (
        <div className="locales-fila-nombre">
          <span className="locales-avatar" aria-hidden="true">
            <IconoTienda width={18} height={18} />
          </span>
          <div>
            <div className="locales-nombre">{l.nombre}</div>
            <div className="locales-codigo">{l.codigo}</div>
          </div>
        </div>
      ),
    },
    {
      encabezado: 'Dirección',
      iconoEncabezado: <IconoUbicacion width={15} height={15} aria-hidden="true" />,
      render: (l) => (
        <span className="locales-direccion">
          <IconoUbicacion width={15} height={15} aria-hidden="true" />
          {l.direccion}
        </span>
      ),
    },
    {
      encabezado: 'Gerente',
      iconoEncabezado: <IconoPersonas width={15} height={15} aria-hidden="true" />,
      render: (l) => <span className="locales-gerente">{l.gerente ?? '—'}</span>,
    },
    {
      encabezado: 'Estado',
      iconoEncabezado: <IconoEscudo width={15} height={15} aria-hidden="true" />,
      render: (l) => <span className={`locales-estado ${l.activo ? 'activo' : 'inactivo'}`}>● {l.activo ? 'Activo' : 'Inactivo'}</span>,
    },
    {
      encabezado: 'Acciones',
      iconoEncabezado: <IconoEngranaje width={15} height={15} aria-hidden="true" />,
      acciones: true,
      render: (l) => (
        <div className="locales-acciones">
          <Boton variante="secundario" onClick={() => navigate(`/admin/locales/${l.id}/editar`)}>
            <IconoLapiz width={14} height={14} aria-hidden="true" /> Editar
          </Boton>
          <Boton variante="peligro" onClick={() => abrirEliminar(l)}>
            <IconoProhibido width={14} height={14} aria-hidden="true" /> Eliminar local
          </Boton>
        </div>
      ),
    },
  ]

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div className="panel-admin-titulo-grupo">
          <span className="panel-admin-titulo-icono" aria-hidden="true">
            <IconoUbicacion width={22} height={22} />
          </span>
          <div>
            <h1>Locales</h1>
            <p className="panel-admin-subtitulo">Gestiona los locales de tu restaurante.</p>
          </div>
        </div>
        <Boton onClick={() => navigate('/admin/locales/nuevo')}>
          <IconoMas width={16} height={16} aria-hidden="true" /> Nuevo local
        </Boton>
      </div>

      <div className="locales-resumen">
        <div className="locales-resumen-tarjeta">
          <span className="locales-resumen-numero">{totales.total}</span>
          <span className="locales-resumen-etiqueta">Total</span>
        </div>
        <div className="locales-resumen-tarjeta">
          <span className="locales-resumen-numero activo">{totales.activos}</span>
          <span className="locales-resumen-etiqueta">Activos</span>
        </div>
        <div className="locales-resumen-tarjeta">
          <span className="locales-resumen-numero inactivo">{totales.inactivos}</span>
          <span className="locales-resumen-etiqueta">Inactivos</span>
        </div>
      </div>

      <div className="panel-admin-tarjeta">
        <div className="locales-filtros">
          <div className="encabezado-buscador locales-buscador">
            <IconoBusqueda width={18} height={18} aria-hidden="true" />
            <input
              type="search"
              placeholder="Buscar por nombre o dirección…"
              aria-label="Buscar por nombre o dirección"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
            />
          </div>
          <div className="encabezado-local-selector locales-ubicacion">
            <IconoUbicacion width={16} height={16} aria-hidden="true" />
            <select aria-label="Filtrar por ubicación" defaultValue="todos">
              <option value="todos">Todos los locales</option>
            </select>
          </div>
        </div>

        <Tabla
          columnas={columnas}
          filas={localesFiltrados}
          obtenerClave={(l) => l.id}
          cargando={cargando}
          error={errorCarga}
          vacioTitulo={busqueda ? 'Sin resultados' : 'Todavía no hay locales'}
          vacioDescripcion={
            busqueda ? 'Prueba con otro nombre o dirección.' : 'Crea el primero para poder asignarle usuarios y empezar a operar.'
          }
          vacioAccion={!busqueda && <Boton onClick={() => navigate('/admin/locales/nuevo')}>Nuevo local</Boton>}
        />

        {!cargando && !errorCarga && locales.length > 0 && (
          <div className="locales-pie">
            <span>
              Mostrando {localesFiltrados.length} de {locales.length} registros
            </span>
            <div className="paginacion">
              <button type="button" disabled aria-label="Página anterior">
                <IconoFlechaIzquierda width={16} height={16} />
              </button>
              <span className="paginacion-actual">1</span>
              <button type="button" disabled aria-label="Página siguiente">
                <IconoFlechaDerecha width={16} height={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      <ModalDeConfirmacion
        abierto={localAEliminar != null}
        titulo="Eliminar local"
        mensaje={`Vas a eliminar "${localAEliminar?.nombre}" de forma permanente. Esta acción no se puede deshacer. Solo es posible si el local nunca tuvo usuarios asignados ni sesiones registradas.`}
        textoConfirmar="Eliminar"
        peligro
        confirmando={eliminando}
        error={errorEliminar}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setLocalAEliminar(null)}
      />
    </section>
  )
}
