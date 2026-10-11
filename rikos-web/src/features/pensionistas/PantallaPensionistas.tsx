import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoBusqueda,
  IconoMas,
  Insignia,
} from '../../comun/componentes'
import { listarPensionistas, listarPlanes, type ComidaHabitual, type ResumenPensionista } from '../../api/cliente'

/**
 * US-016: lista de pensionistas. Activos e inactivos, con buscador por nombre
 * o DNI. La cuenta (deuda, saldo, tope) llega en la entrega siguiente.
 */

type Estado = 'activos' | 'inactivos' | 'todos'

const LETRA: Record<ComidaHabitual, string> = { desayuno: 'D', almuerzo: 'A', cena: 'C' }
const TITULO: Record<ComidaHabitual, string> = { desayuno: 'Desayuno', almuerzo: 'Almuerzo', cena: 'Cena' }
const ORDEN: ComidaHabitual[] = ['desayuno', 'almuerzo', 'cena']

export function PantallaPensionistas() {
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()
  const puedeCrear = tienePermiso('pensionistas.crear')

  const [pensionistas, setPensionistas] = useState<ResumenPensionista[]>([])
  const [hayPlanes, setHayPlanes] = useState(true)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState<Estado>('activos')

  function cargar() {
    setCargando(true)
    setError('')
    Promise.all([listarPensionistas(token), listarPlanes(token)])
      .then(([p, planes]) => {
        setPensionistas(p)
        setHayPlanes(planes.length > 0)
      })
      .catch(() => setError('No se pudieron cargar los pensionistas.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const activos = pensionistas.filter((p) => p.activo).length

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return pensionistas.filter((p) => {
      if (estado === 'activos' && !p.activo) return false
      if (estado === 'inactivos' && p.activo) return false
      if (!q) return true
      return `${p.nombres} ${p.apellidos}`.toLowerCase().includes(q) || p.dni.includes(q)
    })
  }, [pensionistas, busqueda, estado])

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Pensionistas</p>
          <h1>Pensionistas</h1>
          <p className="panel-admin-subtitulo">
            La ficha de cada pensionista con su responsable. La cuenta es una sola y vale en todos los locales.
          </p>
        </div>
        {puedeCrear && !cargando && !error && (
          <Boton onClick={() => navigate('/admin/pensionistas/nuevo')} disabled={!hayPlanes}>
            <IconoMas width={16} height={16} aria-hidden="true" /> Nuevo pensionista
          </Boton>
        )}
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : pensionistas.length === 0 ? (
        <div className="panel-admin-tarjeta">
          {!hayPlanes ? (
            <EstadoVacio
              titulo="Primero crea el plan tarifario"
              descripcion="Cada pensionista tiene un plan que fija lo que paga por desayuno, almuerzo y cena."
              accion={<Boton onClick={() => navigate('/admin/pensionistas/planes')}>Ir a Planes tarifarios</Boton>}
            />
          ) : (
            <EstadoVacio
              titulo="Todavía no hay pensionistas"
              descripcion="Da de alta al primero: sus datos, su responsable y su plan. El sistema le genera un PIN para el kiosco."
              accion={
                puedeCrear ? (
                  <Boton onClick={() => navigate('/admin/pensionistas/nuevo')}>Nuevo pensionista</Boton>
                ) : undefined
              }
            />
          )}
        </div>
      ) : (
        <div className="prod-tabla-tarjeta">
          <div className="prod-barra-filtros">
            <label className="prod-buscador">
              <IconoBusqueda width={15} height={15} aria-hidden="true" />
              <input
                type="text"
                aria-label="Buscar pensionista"
                placeholder="Buscar por nombre o DNI"
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
              />
            </label>
            <div className="prod-espaciador" />
            <div className="prod-pestanas" role="tablist" aria-label="Estado">
              {(['activos', 'inactivos', 'todos'] as Estado[]).map((e) => (
                <button
                  key={e}
                  type="button"
                  role="tab"
                  aria-selected={estado === e}
                  className={`prod-pestana ${estado === e ? 'prod-pestana-activa' : ''}`}
                  onClick={() => setEstado(e)}
                >
                  {e === 'activos' ? 'Activos' : e === 'inactivos' ? 'Inactivos' : 'Todos'}
                </button>
              ))}
            </div>
          </div>

          <div className="prod-tabla-scroll">
            {filtrados.length === 0 ? (
              <EstadoVacio titulo="Nada coincide con el filtro" descripcion="Prueba con otro nombre, otro DNI u otro estado." />
            ) : (
              <table className="prod-tabla">
                <thead>
                  <tr>
                    <th>Pensionista</th>
                    <th>DNI</th>
                    <th>Institución</th>
                    <th>Plan</th>
                    <th>Come</th>
                    <th>Estado</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {filtrados.map((p) => (
                    <tr
                      key={p.id}
                      className={`prod-fila ${p.activo ? '' : 'prod-fila-inactiva'}`}
                      onClick={() => navigate(`/admin/pensionistas/${p.id}`)}
                    >
                      <td>
                        <button
                          type="button"
                          className="prod-nombre"
                          onClick={(e) => {
                            e.stopPropagation()
                            navigate(`/admin/pensionistas/${p.id}`)
                          }}
                        >
                          {p.apellidos}, {p.nombres}
                        </button>
                      </td>
                      <td className="prod-codigo">{p.dni}</td>
                      <td>{p.institucion ?? <span className="prod-vacio">—</span>}</td>
                      <td>{p.planNombre ?? <span className="prod-vacio">—</span>}</td>
                      <td>
                        <div className="prod-slots" aria-label={`Come: ${p.comidas.map((c) => TITULO[c]).join(', ')}`}>
                          {ORDEN.map((c) => (
                            <span
                              key={c}
                              className={`prod-slot ${p.comidas.includes(c) ? 'prod-slot-on' : ''}`}
                              title={`${TITULO[c]}${p.comidas.includes(c) ? '' : ' (no)'}`}
                            >
                              {LETRA[c]}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>
                        <Insignia tono={p.activo ? 'activo' : 'inactivo'} />
                      </td>
                      <td>
                        <button
                          type="button"
                          className="plan-historial-boton"
                          onClick={(e) => {
                            e.stopPropagation()
                            navigate(`/admin/pensionistas/${p.id}/cuenta`)
                          }}
                        >
                          Ver cuenta
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          <div className="prod-pie">
            <span>
              {filtrados.length} de {pensionistas.length} pensionistas · {activos} activos
            </span>
            <span>Haz clic en un pensionista para ver o editar su ficha</span>
          </div>
        </div>
      )}
    </section>
  )
}
