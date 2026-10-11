import { useEffect, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Aviso, Boton, EstadoCargando, EstadoError, EstadoVacio, IconoLapiz } from '../../comun/componentes'
import { formatearMoneda } from '../../comun/formato/dinero'
import { ErrorApi, cambiarPrecioMenu, listarTiposMenu, type TipoMenu } from '../../api/cliente'

/**
 * US-014. Los dos menus los crea el sistema (V21); aca solo se ve su
 * composicion y se pone o cambia su precio. La composicion y la regla del
 * pensionista (RN-023) vienen del backend (TipoMenu.java): esta pantalla no
 * las decide, solo las muestra.
 */

const PARTES: Record<string, string> = {
  entrada: 'Entrada',
  segundo: 'Segundo',
  guarnicion: 'Guarnición',
  refresco: 'Refresco',
  postre: 'Postre',
}

/** CA1 habla de "segundo con guarnición": la guarnición va con el segundo, no como un tiempo aparte. */
function tiempos(componentes: string[]): string[] {
  const conGuarnicion = componentes.includes('guarnicion')
  return componentes
    .filter((c) => c !== 'guarnicion')
    .map((c) => (c === 'segundo' && conGuarnicion ? 'Segundo con guarnición' : (PARTES[c] ?? c)))
}

export function PantallaMenuNormalYExtra() {
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token

  const [tipos, setTipos] = useState<TipoMenu[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  function cargar() {
    setCargando(true)
    setError('')
    listarTiposMenu(token)
      .then(setTipos)
      .catch(() => setError('No se pudo cargar el menú.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const puedeCambiar = tienePermiso('menu.modificar')
  const sinPrecio = tipos.filter((t) => t.precioVenta == null)

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Menú semanal</p>
          <h1>Menú normal y extra</h1>
          <p className="panel-admin-subtitulo">
            Lo que trae cada menú del almuerzo y cuánto se cobra. El precio es el mismo sin importar qué opciones
            elija el cliente.
          </p>
        </div>
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : tipos.length === 0 ? (
        <div className="panel-admin-tarjeta">
          <EstadoVacio
            titulo="No se encontraron los menús"
            descripcion="El menú normal y el extra los crea el sistema al instalarse. Si no aparecen, avisa a soporte."
          />
        </div>
      ) : (
        <>
          {sinPrecio.length > 0 && (
            <Aviso>
              {sinPrecio.length === 2
                ? 'Los dos menús todavía no tienen precio. Hasta que lo tengan, la caja no podrá cobrarlos.'
                : `El ${sinPrecio[0].nombre.toLowerCase()} todavía no tiene precio. Hasta que lo tenga, la caja no podrá cobrarlo.`}
            </Aviso>
          )}
          <div className="menu-tipos">
            {tipos.map((t) => (
              <TarjetaMenu
                key={t.tipo}
                token={token}
                tipo={t}
                puedeCambiar={puedeCambiar}
                onCambio={(nuevo) => setTipos((lista) => lista.map((x) => (x.tipo === nuevo.tipo ? nuevo : x)))}
              />
            ))}
          </div>
        </>
      )}
    </section>
  )
}

function TarjetaMenu({
  token,
  tipo,
  puedeCambiar,
  onCambio,
}: {
  token: string
  tipo: TipoMenu
  puedeCambiar: boolean
  onCambio: (t: TipoMenu) => void
}) {
  const [editando, setEditando] = useState(false)
  const [valor, setValor] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [guardado, setGuardado] = useState(false)

  const esExtra = tipo.tipo === 'extra'

  function empezar() {
    setValor(tipo.precioVenta != null ? (tipo.precioVenta / 100).toFixed(2) : '')
    setError('')
    setGuardado(false)
    setEditando(true)
  }

  async function guardar() {
    const soles = parseFloat(valor.replace(',', '.'))
    if (Number.isNaN(soles) || soles <= 0) {
      setError('Pon un precio mayor que cero.')
      return
    }
    setGuardando(true)
    setError('')
    try {
      onCambio(await cambiarPrecioMenu(token, tipo.tipo, Math.round(soles * 100)))
      setEditando(false)
      setGuardado(true)
    } catch (e) {
      setError(
        e instanceof ErrorApi && e.estado === 403
          ? 'Tus permisos no incluyen cambiar el precio del menú.'
          : 'No se pudo guardar el precio.',
      )
    } finally {
      setGuardando(false)
    }
  }

  return (
    <article className="panel-admin-tarjeta menu-tarjeta" aria-labelledby={`menu-${tipo.tipo}`}>
      <header className="menu-tarjeta-cabecera">
        <div>
          <h2 id={`menu-${tipo.tipo}`}>{tipo.nombre}</h2>
          <span className="prod-codigo">{tipo.codigo}</span>
        </div>
        <span className={`prod-pastilla ${esExtra ? 'prod-pastilla-subreceta' : 'prod-pastilla-vendible'}`}>
          {esExtra ? 'Precio propio' : 'Precio único'}
        </span>
      </header>

      <div className="menu-precio">
        {editando ? (
          <div className="menu-precio-edicion">
            <label htmlFor={`precio-${tipo.tipo}`} className="campo-etiqueta">
              Precio
            </label>
            <div className="prod-moneda">
              <span>S/</span>
              <input
                id={`precio-${tipo.tipo}`}
                className="prod-input prod-input-monto"
                inputMode="decimal"
                placeholder="0.00"
                autoFocus
                value={valor}
                aria-invalid={Boolean(error) || undefined}
                onChange={(e) => setValor(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') guardar()
                  if (e.key === 'Escape') setEditando(false)
                }}
              />
            </div>
            <div className="menu-precio-acciones">
              <Boton variante="secundario" onClick={() => setEditando(false)} disabled={guardando}>
                Cancelar
              </Boton>
              <Boton cargando={guardando} onClick={guardar}>
                Guardar
              </Boton>
            </div>
          </div>
        ) : (
          <>
            <strong className={tipo.precioVenta == null ? 'menu-precio-vacio' : ''}>
              {tipo.precioVenta == null ? 'Sin precio' : formatearMoneda(tipo.precioVenta)}
            </strong>
            {puedeCambiar && (
              <Boton variante="secundario" onClick={empezar}>
                <IconoLapiz width={15} height={15} aria-hidden="true" />{' '}
                {tipo.precioVenta == null ? 'Poner precio' : 'Cambiar precio'}
              </Boton>
            )}
          </>
        )}
        {error && <p className="campo-error">{error}</p>}
        {guardado && !editando && <p className="prod-texto-bueno menu-guardado">Precio guardado. Quedó en la bitácora.</p>}
      </div>

      <section className="menu-seccion">
        <h3>Qué trae</h3>
        <ol className="menu-tiempos">
          {tiempos(tipo.componentes).map((t) => (
            <li key={t}>{t}</li>
          ))}
        </ol>
        {esExtra && (
          <p className="prod-nota">El segundo sale de las opciones marcadas como extra en la programación de la semana.</p>
        )}
      </section>

      <section className="menu-seccion">
        <h3>Pensionistas</h3>
        <p className="prod-nota">
          {tipo.admiteTarifaReducida
            ? 'Pagan la tarifa de su plan, no este precio.'
            : 'Pagan este precio completo: el extra no tiene tarifa reducida de pensionista.'}
        </p>
      </section>
    </article>
  )
}
