import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Building2, Calculator, ExternalLink, FileText, Info, Lock, MapPin, Store, TriangleAlert, UserRound } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { formatearMomento } from '../../comun/formato/momento'
import { Aviso, Boton, EstadoCargando, EstadoError, IconoReactivar } from '../../comun/componentes'
import {
  ErrorApi,
  cambiarParametro,
  listarAuditoria,
  obtenerParametros,
  type ParametrosDeCobro,
  type RegistroAuditoria,
} from '../../api/cliente'
import { aCentimos, desdeCentimos, enSoles } from './dinero'

/**
 * Parámetros de cobro (sprint 5, 3.6): el precio del táper y a qué precio va el segundo consumo del
 * día de un pensionista. Valen para toda la empresa (no hay parámetros por local). Solo el
 * administrador central (pensionistas.administrar) los cambia; el resto los ve. Cada cambio queda
 * en la bitácora. Se editan las dos tarjetas y se guardan juntas con "Guardar cambios".
 */

type Regla = ParametrosDeCobro['reglaSegundoConsumo']

const REGLAS: { valor: Regla; titulo: string; detalle: string; recomendada: boolean }[] = [
  { valor: 'precio_del_plan', titulo: 'Precio de su plan', detalle: 'Se cobra el mismo precio de su plan de pensionista.', recomendada: true },
  { valor: 'precio_normal', titulo: 'Precio normal del menú', detalle: 'Se cobra el precio normal del menú.', recomendada: false },
]

function mensajeError(e: unknown): string {
  if (e instanceof ErrorApi) {
    if (e.cuerpo?.error === 'VALOR_INVALIDO') return 'Ese valor no corresponde a este parámetro.'
    if (e.estado === 403) return 'Tus permisos no alcanzan para esto.'
  }
  return 'No se pudo guardar el cambio.'
}

export function PantallaParametrosDeCobro() {
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const puedeAdministrar = tienePermiso('pensionistas.administrar')
  const puedeVerBitacora = tienePermiso('auditoria.ver')

  const [guardados, setGuardados] = useState<ParametrosDeCobro | null>(null)
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')

  const [monto, setMonto] = useState('')
  const [regla, setRegla] = useState<Regla>('precio_del_plan')
  const [guardando, setGuardando] = useState(false)
  const [errorGuardar, setErrorGuardar] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [ultimoCambio, setUltimoCambio] = useState<RegistroAuditoria | null>(null)

  function aplicar(p: ParametrosDeCobro) {
    setGuardados(p)
    setMonto(desdeCentimos(p.taperPrecio))
    setRegla(p.reglaSegundoConsumo)
  }

  function cargarUltimoCambio() {
    if (!puedeVerBitacora) return
    listarAuditoria(token, { evento: 'cambio_parametro', tamano: 1 })
      .then((pagina) => setUltimoCambio(pagina.content[0] ?? null))
      .catch(() => setUltimoCambio(null))
  }

  function cargar() {
    setCargando(true)
    setErrorCarga('')
    obtenerParametros(token)
      .then(aplicar)
      .catch(() => setErrorCarga('No se pudieron cargar los parámetros.'))
      .finally(() => setCargando(false))
    cargarUltimoCambio()
  }

  useEffect(cargar, [token])

  useEffect(() => {
    if (!mensaje) return
    const t = setTimeout(() => setMensaje(''), 2800)
    return () => clearTimeout(t)
  }, [mensaje])

  const centimos = aCentimos(monto)
  const montoInvalido = Number.isNaN(centimos)
  const sinMonto = !montoInvalido && centimos == null
  const cambioMonto = guardados != null && !montoInvalido && centimos !== guardados.taperPrecio
  const cambioRegla = guardados != null && regla !== guardados.reglaSegundoConsumo
  const cambios = (cambioMonto ? 1 : 0) + (cambioRegla ? 1 : 0)
  const puedeGuardar = puedeAdministrar && cambios > 0 && !montoInvalido

  async function guardar() {
    if (!guardados || !puedeGuardar) return
    setGuardando(true)
    setErrorGuardar('')
    try {
      let ultimo = guardados
      if (cambioMonto) ultimo = await cambiarParametro(token, 'taper_precio', centimos == null ? null : String(centimos))
      if (cambioRegla) ultimo = await cambiarParametro(token, 'segundo_consumo_regla', regla)
      aplicar(ultimo)
      setMensaje('Cambios guardados. Quedaron en la bitácora.')
      cargarUltimoCambio()
    } catch (e) {
      setErrorGuardar(mensajeError(e))
      // Si el primero se guardó y el segundo falló, se vuelve a leer lo real para no mostrar algo falso.
      obtenerParametros(token).then((p) => setGuardados(p)).catch(() => undefined)
    } finally {
      setGuardando(false)
    }
  }

  function descartar() {
    if (guardados) aplicar(guardados)
    setErrorGuardar('')
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga || !guardados) {
    return <EstadoError mensaje={errorCarga || 'No se pudieron cargar los parámetros.'} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
  }

  const estadoPie = montoInvalido
    ? 'Corrige el monto para poder guardar.'
    : cambios === 0
      ? 'Sin cambios por guardar.'
      : `${cambios} ${cambios === 1 ? 'cambio sin guardar' : 'cambios sin guardar'}`

  return (
    <div className="as-pagina pc-pagina">
      <div className="pc-cuerpo">
        <div>
          <nav className="loc-migas" aria-label="Ruta">
            <Link to="/admin/configuracion" className="cfg-miga-enlace">
              Configuración
            </Link>
            <span aria-hidden="true">/</span>
            <span>Parámetros de cobro</span>
          </nav>
          <h1 className="cfg-titulo pc-titulo">Parámetros de cobro</h1>
          <p className="cfg-subtitulo">Reglas que usa caja al calcular pedidos y almuerzos.</p>
        </div>

        {!puedeAdministrar && <Aviso tono="informacion">Puedes ver los parámetros, pero no cambiarlos con tus permisos.</Aviso>}

        <section className="pc-ambito" aria-label="Ámbito de aplicación">
          <span className="pc-icono" aria-hidden="true">
            <MapPin size={22} strokeWidth={1.7} />
          </span>
          <div className="pc-ambito-texto">
            <strong>Ámbito de aplicación</strong>
            <span>Estas reglas valen para toda la empresa; cada local no tiene las suyas.</span>
          </div>
          <div className="pc-ambito-valor">
            <Building2 size={18} aria-hidden="true" /> Todos los locales <Lock size={15} aria-hidden="true" />
          </div>
          <span className="pc-separador" aria-hidden="true" />
          <div className="pc-ambito-nota">
            <Info size={22} strokeWidth={1.6} aria-hidden="true" />
            Los cambios se aplican a nuevos cobros, no a los ya registrados.
          </div>
        </section>

        <div className="pc-columnas">
          <div className="pc-principal">
            <section className="pc-tarjeta" aria-labelledby="pc-taper">
              <div className="pc-tarjeta-cabecera">
                <span className="pc-icono" aria-hidden="true">
                  <Store size={22} strokeWidth={1.7} />
                </span>
                <div>
                  <h2 id="pc-taper">Cargo por táper</h2>
                  <p>Se cobra por cada menú en pedidos para llevar, recojo o delivery; aplica a pensionistas y clientes.</p>
                </div>
              </div>
              <div className="pc-monto-fila">
                <label className="pc-monto">
                  <span>Monto por menú</span>
                  <div className={`pc-monto-control ${montoInvalido ? 'invalido' : ''}`}>
                    <span aria-hidden="true">S/</span>
                    <input
                      type="text"
                      inputMode="decimal"
                      placeholder="Sin monto"
                      value={monto}
                      disabled={!puedeAdministrar}
                      aria-invalid={montoInvalido || undefined}
                      onChange={(e) => {
                        setMonto(e.target.value)
                        setMensaje('')
                      }}
                    />
                  </div>
                  <span className={montoInvalido ? 'campo-error' : 'as-ayuda'}>
                    {montoInvalido
                      ? 'Escribe un monto mayor que cero, por ejemplo 1.00.'
                      : sinMonto
                        ? 'Déjalo vacío para no cobrar táper.'
                        : `Ahora: ${enSoles(guardados.taperPrecio)}`}
                  </span>
                </label>
                <div className="pc-ejemplo" aria-live="polite">
                  <Calculator size={22} strokeWidth={1.7} aria-hidden="true" />
                  <span aria-hidden="true" className="pc-separador-chico" />
                  <span>
                    Ejemplo: 2 menús para llevar →{' '}
                    <strong>{montoInvalido || centimos == null ? 'sin cargo por táper' : `${enSoles(centimos * 2)} por táper.`}</strong>
                  </span>
                </div>
              </div>
              {sinMonto && (
                <div className="pc-aviso" role="note">
                  <TriangleAlert size={18} aria-hidden="true" />
                  <span>
                    <strong>No se cobra táper</strong> mientras el monto esté vacío.
                  </span>
                </div>
              )}
            </section>

            <section className="pc-tarjeta" aria-labelledby="pc-segundo">
              <div className="pc-tarjeta-cabecera">
                <span className="pc-icono" aria-hidden="true">
                  <UserRound size={22} strokeWidth={1.7} />
                </span>
                <div>
                  <h2 id="pc-segundo">Segundo almuerzo de pensionista</h2>
                  <p>Si la misma persona consume un segundo almuerzo el mismo día, ¿qué precio se cobra?</p>
                </div>
              </div>
              <div className="pc-reglas" role="radiogroup" aria-label="Precio del segundo almuerzo">
                {REGLAS.map((r) => {
                  const on = regla === r.valor
                  return (
                    <button
                      key={r.valor}
                      type="button"
                      role="radio"
                      aria-checked={on}
                      disabled={!puedeAdministrar}
                      className={`pc-regla ${on ? 'on' : ''}`}
                      onClick={() => {
                        setRegla(r.valor)
                        setMensaje('')
                      }}
                    >
                      <span className="pc-radio" aria-hidden="true">
                        <span />
                      </span>
                      <span className="pc-regla-texto">
                        <strong>
                          {r.titulo} {r.recomendada && <em>Recomendado</em>}
                        </strong>
                        <span>{r.detalle}</span>
                      </span>
                    </button>
                  )
                })}
              </div>
            </section>
          </div>

          <aside className="pc-auditoria" aria-label="Auditoría">
            <div className="pc-tarjeta-cabecera pc-auditoria-cabecera">
              <span className="pc-icono" aria-hidden="true">
                <FileText size={22} strokeWidth={1.7} />
              </span>
              <h2>Cambios y auditoría</h2>
            </div>
            <p>Cada cambio queda registrado en la bitácora del sistema.</p>
            {puedeVerBitacora && (
              <>
                <hr className="as-separador" />
                <div className="pc-ultimo">
                  <span>Último cambio</span>
                  <strong>
                    {ultimoCambio
                      ? `${formatearMomento(ultimoCambio.ocurridoEn)}${ultimoCambio.usuarioNombre ? ` · ${ultimoCambio.usuarioNombre}` : ''}`
                      : 'Todavía no hay cambios registrados'}
                  </strong>
                </div>
                <Link to="/admin/auditoria?evento=cambio_parametro" className="pc-historial">
                  <ExternalLink size={18} aria-hidden="true" /> Ver historial de cambios
                </Link>
              </>
            )}
          </aside>
        </div>
      </div>

      {puedeAdministrar && (
        <footer className="as-pie pc-pie">
          <span className={`pc-estado ${montoInvalido ? 'error' : cambios > 0 ? 'pendiente' : ''}`}>{errorGuardar || estadoPie}</span>
          <Boton variante="secundario" disabled={cambios === 0 || guardando} onClick={descartar}>
            Descartar cambios
          </Boton>
          <Boton cargando={guardando} disabled={!puedeGuardar} onClick={guardar}>
            Guardar cambios
          </Boton>
        </footer>
      )}

      {mensaje && (
        <div className="loc-toast" role="status">
          <IconoReactivar width={16} height={16} aria-hidden="true" /> {mensaje}
        </div>
      )}
    </div>
  )
}
