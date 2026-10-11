import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoBusqueda,
  Insignia,
} from '../../comun/componentes'
import { listarCuentasCorrientes, type FilaDeCuentaCorriente } from '../../api/cliente'
import { enSoles } from './dinero'

/**
 * Cuentas corrientes: todos los pensionistas con su saldo, la mayor deuda
 * primero, para saber a quién recordarle el pago. El tope es un recordatorio,
 * no un límite: "Pasó su tope" nunca significa que no pueda consumir.
 */

type Filtro = 'todos' | 'con-deuda' | 'pasaron-tope' | 'a-favor'

const FILTROS: { id: Filtro; titulo: string }[] = [
  { id: 'todos', titulo: 'Todos' },
  { id: 'con-deuda', titulo: 'Con deuda' },
  { id: 'pasaron-tope', titulo: 'Pasaron su tope' },
  { id: 'a-favor', titulo: 'Con saldo a favor' },
]

function coincide(f: FilaDeCuentaCorriente, filtro: Filtro): boolean {
  switch (filtro) {
    case 'con-deuda':
      return f.deuda > 0
    case 'pasaron-tope':
      return f.superaTope
    case 'a-favor':
      return f.saldoAFavor > 0
    default:
      return true
  }
}

export function PantallaCuentasCorrientes() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [filas, setFilas] = useState<FilaDeCuentaCorriente[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [filtro, setFiltro] = useState<Filtro>('todos')

  function cargar() {
    setCargando(true)
    setError('')
    listarCuentasCorrientes(token)
      .then(setFilas)
      .catch(() => setError('No se pudieron cargar las cuentas corrientes.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const totales = useMemo(
    () => ({
      deuda: filas.reduce((s, f) => s + f.deuda, 0),
      aFavor: filas.reduce((s, f) => s + f.saldoAFavor, 0),
      conDeuda: filas.filter((f) => f.deuda > 0).length,
      pasaron: filas.filter((f) => f.superaTope).length,
    }),
    [filas],
  )

  const visibles = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return filas.filter(
      (f) =>
        coincide(f, filtro) &&
        (!q || `${f.nombres} ${f.apellidos}`.toLowerCase().includes(q) || f.dni.includes(q)),
    )
  }, [filas, filtro, busqueda])

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Pensionistas</p>
          <h1>Cuentas corrientes</h1>
          <p className="panel-admin-subtitulo">
            Lo que debe o tiene a favor cada pensionista, con la mayor deuda primero. La cuenta es una sola y vale en
            todos los locales.
          </p>
        </div>
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : filas.length === 0 ? (
        <div className="panel-admin-tarjeta">
          <EstadoVacio
            titulo="Todavía no hay pensionistas"
            descripcion="Cuando des de alta al primero, su cuenta aparecerá aquí."
            accion={<Boton onClick={() => navigate('/admin/pensionistas')}>Ir a Pensionistas</Boton>}
          />
        </div>
      ) : (
        <>
          <div className="cta-resumen">
            <div className="panel-admin-tarjeta cta-cifra">
              <span className="cta-etiqueta">Total que deben</span>
              <strong className={totales.deuda > 0 ? 'cta-deuda' : ''}>{enSoles(totales.deuda)}</strong>
              <span className="cta-nota">{totales.conDeuda} pensionistas con deuda</span>
            </div>
            <div className="panel-admin-tarjeta cta-cifra">
              <span className="cta-etiqueta">Total a favor</span>
              <strong className={totales.aFavor > 0 ? 'cta-favor' : ''}>{enSoles(totales.aFavor)}</strong>
            </div>
            <div className="panel-admin-tarjeta cta-cifra">
              <span className="cta-etiqueta">Pasaron su tope</span>
              <strong>{totales.pasaron}</strong>
              <span className="cta-nota">Recuérdales el pago; no se les impide consumir</span>
            </div>
          </div>

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
              <div className="prod-pestanas" role="tablist" aria-label="Filtro">
                {FILTROS.map((f) => (
                  <button
                    key={f.id}
                    type="button"
                    role="tab"
                    aria-selected={filtro === f.id}
                    className={`prod-pestana ${filtro === f.id ? 'prod-pestana-activa' : ''}`}
                    onClick={() => setFiltro(f.id)}
                  >
                    {f.titulo}
                  </button>
                ))}
              </div>
            </div>

            <div className="prod-tabla-scroll">
              {visibles.length === 0 ? (
                <EstadoVacio titulo="Nada coincide con el filtro" descripcion="Prueba con otro filtro o con otro nombre." />
              ) : (
                <table className="prod-tabla">
                  <thead>
                    <tr>
                      <th>Pensionista</th>
                      <th className="cta-num">Debe</th>
                      <th className="cta-num">A favor</th>
                      <th className="cta-num">Tope</th>
                      <th>Situación</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibles.map((f) => (
                      <tr
                        key={f.pensionistaId}
                        className={`prod-fila ${f.activo ? '' : 'prod-fila-inactiva'}`}
                        onClick={() => navigate(`/admin/pensionistas/${f.pensionistaId}/cuenta`)}
                      >
                        <td>
                          <button
                            type="button"
                            className="prod-nombre"
                            onClick={(e) => {
                              e.stopPropagation()
                              navigate(`/admin/pensionistas/${f.pensionistaId}/cuenta`)
                            }}
                          >
                            {f.apellidos}, {f.nombres}
                          </button>
                          <span className="cta-nota">
                            {' '}
                            DNI {f.dni}
                            {f.activo ? '' : ' · inactivo'}
                          </span>
                        </td>
                        <td className={`cta-num ${f.deuda > 0 ? 'cta-deuda' : ''}`}>
                          {f.deuda > 0 ? enSoles(f.deuda) : <span className="prod-vacio">—</span>}
                        </td>
                        <td className={`cta-num ${f.saldoAFavor > 0 ? 'cta-favor' : ''}`}>
                          {f.saldoAFavor > 0 ? enSoles(f.saldoAFavor) : <span className="prod-vacio">—</span>}
                        </td>
                        <td className="cta-num">
                          {f.tope.monto == null ? (
                            <span className="prod-vacio" title="No se pudo calcular">
                              Sin calcular
                            </span>
                          ) : (
                            <>
                              {enSoles(f.tope.monto)}
                              {f.tope.origen === 'fijado' && <span className="cta-nota"> (fijado)</span>}
                            </>
                          )}
                        </td>
                        <td>
                          {f.superaTope ? (
                            <Insignia tono="pendiente">Pasó su tope</Insignia>
                          ) : f.deuda > 0 ? (
                            <Insignia tono="pendiente">Debe</Insignia>
                          ) : f.saldoAFavor > 0 ? (
                            <Insignia tono="activo">A favor</Insignia>
                          ) : (
                            <Insignia tono="activo">Al día</Insignia>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <div className="prod-pie">
              <span>
                {visibles.length} de {filas.length} pensionistas
              </span>
              <span>Haz clic en uno para ver su cuenta</span>
            </div>
          </div>
        </>
      )}
    </section>
  )
}
