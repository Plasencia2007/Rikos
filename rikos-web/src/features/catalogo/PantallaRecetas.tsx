import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, EstadoCargando, EstadoError, EstadoVacio, IconoBusqueda, IconoMas } from '../../comun/componentes'
import { listarRecetas, type RecetaResumen } from '../../api/cliente'
import { Mosaico, PastillaUso, soles } from './productoComun'

/**
 * US-010. Los costos llegan en null si la sesion no tiene
 * recetas.ver_costos (el backend los quita) -- aca solo se decide si
 * mostrar la columna, nunca se "esconde" un dato que si llego.
 */
export function PantallaRecetas() {
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [recetas, setRecetas] = useState<RecetaResumen[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')

  function cargar() {
    setCargando(true)
    setError('')
    listarRecetas(token)
      .then(setRecetas)
      .catch(() => setError('No se pudieron cargar las recetas.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const puedeCrear = tienePermiso('recetas.crear')
  const verCostos = recetas.some((r) => r.costosVisibles)
  const incompletas = recetas.filter((r) => r.costoIncompleto).length

  const filtradas = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    if (!q) return recetas
    return recetas.filter((r) => r.productoNombre.toLowerCase().includes(q) || r.productoCodigo.toLowerCase().includes(q))
  }, [recetas, busqueda])

  if (cargando) return <EstadoCargando />
  if (error) return <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Catálogo y recetas</p>
          <h1>Recetas</h1>
          <p className="panel-admin-subtitulo">
            Qué lleva cada plato y cada subreceta, y cuánto cuesta una porción con el último costo de sus insumos.
          </p>
        </div>
        {puedeCrear && (
          <Boton onClick={() => navigate('/admin/catalogo/recetas/nueva')}>
            <IconoMas width={16} height={16} aria-hidden="true" /> Nueva receta
          </Boton>
        )}
      </div>

      {recetas.length === 0 ? (
        <div className="panel-admin-tarjeta">
          <EstadoVacio
            titulo="Todavía no hay recetas"
            descripcion="Empieza por las subrecetas (un aderezo, un caldo): después se usan como ingrediente de los platos."
            accion={
              puedeCrear ? (
                <Boton onClick={() => navigate('/admin/catalogo/recetas/nueva')}>Nueva receta</Boton>
              ) : undefined
            }
          />
        </div>
      ) : (
        <div className="prod-tabla-tarjeta">
          <div className="prod-barra-filtros">
            <label className="prod-buscador">
              <IconoBusqueda width={15} height={15} aria-hidden="true" />
              <input
                type="text"
                aria-label="Buscar receta"
                placeholder="Buscar por producto o código"
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
              />
            </label>
            {verCostos && incompletas > 0 && (
              <span className="prod-nota">
                {incompletas === 1 ? '1 receta sin costo completo' : `${incompletas} recetas sin costo completo`}
              </span>
            )}
          </div>

          <div className="prod-tabla-scroll">
            {filtradas.length === 0 ? (
              <EstadoVacio titulo="Nada coincide con la búsqueda" descripcion="Prueba con otro nombre o código." />
            ) : (
              <table className="prod-tabla">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Tipo</th>
                    <th className="prod-num">Rinde</th>
                    <th className="prod-num">Porciones</th>
                    <th className="prod-num">Ingredientes</th>
                    {verCostos && <th className="prod-num">Costo por porción</th>}
                  </tr>
                </thead>
                <tbody>
                  {filtradas.map((r) => {
                    const uso = r.esSubreceta ? 'subreceta' : 'vendible'
                    return (
                      <tr key={r.id} className="prod-fila" onClick={() => navigate(`/admin/catalogo/recetas/${r.id}`)}>
                        <td>
                          <div className="prod-celda-nombre">
                            <Mosaico nombre={r.productoNombre} uso={uso} />
                            <div>
                              <button
                                type="button"
                                className="prod-nombre"
                                onClick={(e) => {
                                  e.stopPropagation()
                                  navigate(`/admin/catalogo/recetas/${r.id}`)
                                }}
                              >
                                {r.productoNombre}
                              </button>
                              <span className="prod-sub">
                                <span className="prod-codigo">{r.productoCodigo}</span>
                              </span>
                            </div>
                          </div>
                        </td>
                        <td>
                          <PastillaUso uso={uso} />
                        </td>
                        <td className="prod-num">
                          {/* Solo la subreceta tiene rendimiento propio (el plato sale en porciones). */}
                          {r.esSubreceta ? (
                            `${cantidad(r.rendimiento)} ${r.unidadBaseCodigo}`
                          ) : (
                            <span className="prod-vacio">—</span>
                          )}
                        </td>
                        <td className="prod-num">{r.porciones}</td>
                        <td className="prod-num">{r.cantidadIngredientes}</td>
                        {verCostos && (
                          <td className="prod-num">
                            {r.costoIncompleto ? (
                              <span className="prod-estimado">incompleto</span>
                            ) : (
                              soles(r.costoPorPorcion)
                            )}
                          </td>
                        )}
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </div>
          <div className="prod-pie">
            <span>
              {filtradas.length} de {recetas.length} recetas
            </span>
            <span>Haz clic en una receta para ver sus ingredientes</span>
          </div>
        </div>
      )}
    </section>
  )
}

/** Cantidades con hasta 3 decimales, sin ceros de relleno (1.5, no 1.500000). */
export function cantidad(valor: number): string {
  return Number(valor.toFixed(3)).toLocaleString('es-PE', { maximumFractionDigits: 3 })
}
