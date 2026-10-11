import { useCallback, useEffect, useState } from 'react'
import { Clock, Lock, RefreshCw, Undo2 } from 'lucide-react'
import { Aviso, Boton, EstadoCargando, EstadoError, EstadoVacio, Insignia } from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import {
  cobrosDelTurno,
  listarCajas,
  listarPedidos,
  permisosPropios,
  resumenDelTurno,
  turnoActualDeCaja,
  type CajaDelLocal,
  type CobroDelTurno,
  type ResumenDePedido,
  type ResumenDeTurno,
  type TurnoDeCaja,
} from '../../api/cliente'
import { AbrirTurnoDeCaja } from './AbrirTurnoDeCaja'
import { CobrarPedidoDeCaja } from './CobrarPedidoDeCaja'
import { DialogoCierre, DialogoReabrir } from './DialogosDeCaja'

function tituloDe(p: ResumenDePedido): string {
  if (p.mesaNumero) return p.mesaNombre ? `${p.mesaNombre}` : `Mesa ${p.mesaNumero}`
  return p.nombreCliente ? `Para llevar · ${p.nombreCliente}` : 'Para llevar'
}

/** "2 h 15 min" desde que se abrio la sesion. */
function transcurrido(iso: string): string {
  const minutos = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 60000))
  const h = Math.floor(minutos / 60)
  return h > 0 ? `${h} h ${minutos % 60} min` : `${minutos} min`
}

function hora(iso: string): string {
  const f = new Date(iso)
  return Number.isNaN(f.getTime()) ? '' : f.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })
}

/**
 * Sprint 7: la caja del POS. Sin turno abierto se abre uno (con su fondo); con turno se ve el resumen del turno, lo que hay por
 * cobrar y lo ya cobrado (donde se reabre un cobro), y se puede cerrar el turno con el conteo del efectivo. Cobrar es solo de
 * quien tiene permiso (cajero y administradores): el mozo pide pero no cobra.
 */
/** pedidoInicial: si viene (el boton "Cobrar" de la pantalla del pedido), se abre directo a cobrarlo en cuanto hay turno. */
export function PantallaCaja({ token, pedidoInicial }: { token: string; pedidoInicial?: string | null }) {
  const [permisos, setPermisos] = useState<string[] | null>(null)
  const [turno, setTurno] = useState<TurnoDeCaja | null | undefined>(undefined)
  const [error, setError] = useState(false)

  const cargarTurno = useCallback(() => {
    setError(false)
    turnoActualDeCaja(token)
      .then((t) => setTurno(t ?? null))
      .catch(() => setError(true))
  }, [token])

  useEffect(() => {
    permisosPropios(token)
      .then(setPermisos)
      .catch(() => setPermisos([]))
    cargarTurno()
  }, [token, cargarTurno])

  if (error) {
    return <EstadoError mensaje="No se pudo cargar la caja." accion={<Boton onClick={cargarTurno}>Reintentar</Boton>} />
  }
  if (permisos === null || turno === undefined) return <EstadoCargando />

  if (!permisos.includes('caja.ver')) {
    return (
      <EstadoVacio
        titulo="La caja es del cajero"
        descripcion="Tu usuario no tiene acceso a la caja. Los productos que pides se cobran allí."
      />
    )
  }
  if (turno === null) {
    if (!permisos.includes('caja.crear')) {
      return <EstadoVacio titulo="No hay un turno abierto" descripcion="Un cajero tiene que abrir su turno para cobrar." />
    }
    return <AbrirTurnoDeCaja token={token} onAbierto={setTurno} />
  }
  return (
    <PanelDelTurno
      token={token}
      turno={turno}
      permisos={permisos}
      pedidoInicial={pedidoInicial ?? null}
      onTurnoCerrado={() => setTurno(null)}
    />
  )
}

function PanelDelTurno({
  token,
  turno,
  permisos,
  pedidoInicial,
  onTurnoCerrado,
}: {
  token: string
  turno: TurnoDeCaja
  permisos: string[]
  pedidoInicial: string | null
  onTurnoCerrado: () => void
}) {
  const [resumen, setResumen] = useState<ResumenDeTurno | null>(null)
  const [pedidos, setPedidos] = useState<ResumenDePedido[] | null>(null)
  const [cobros, setCobros] = useState<CobroDelTurno[]>([])
  const [caja, setCaja] = useState<CajaDelLocal | null>(null)
  const [error, setError] = useState(false)
  const [cobrando, setCobrando] = useState<string | null>(pedidoInicial)
  const [cerrando, setCerrando] = useState(false)
  const [reabriendo, setReabriendo] = useState<CobroDelTurno | null>(null)
  const [aviso, setAviso] = useState('')

  const puedeCobrar = permisos.includes('caja.crear')
  const puedeReabrir = permisos.includes('caja.reabrir')

  const cargar = useCallback(() => {
    setError(false)
    Promise.all([
      resumenDelTurno(token),
      listarPedidos(token, { situacion: 'en_curso', tamano: 100 }),
      cobrosDelTurno(token),
      listarCajas(token),
    ])
      .then(([r, p, c, cs]) => {
        setCaja(cs.find((x) => x.id === turno.cajaId) ?? null)
        setResumen(r)
        setPedidos(p.contenido.filter((x) => x.estado === 'comandado'))
        setCobros(c)
      })
      .catch(() => setError(true))
  }, [token, turno.cajaId])

  useEffect(() => {
    cargar()
    const reloj = window.setInterval(cargar, 20_000)
    return () => window.clearInterval(reloj)
  }, [cargar])

  if (cobrando) {
    return (
      <CobrarPedidoDeCaja
        token={token}
        pedidoId={cobrando}
        puedeDescontar={permisos.includes('caja.descontar')}
        onVolver={() => {
          setCobrando(null)
          cargar()
        }}
        onCobrado={() => {
          setCobrando(null)
          cargar()
        }}
      />
    )
  }
  if (error && !resumen) {
    return <EstadoError mensaje="No se pudo cargar el turno." accion={<Boton onClick={cargar}>Reintentar</Boton>} />
  }
  if (!resumen || !pedidos) return <EstadoCargando />

  const vigentes = cobros.filter((c) => !c.revertido)

  return (
    <div className="caja-panel">
      <header className="caja-banner">
        <div className="caja-banner-texto">
          <span className="caja-banner-etiqueta">Sesión activa</span>
          <h2>{caja?.nombre ?? 'Caja'}</h2>
          <span>
            {turno.etiqueta ?? ''}
            {caja ? ` · ${caja.codigo}` : ''}
          </span>
          {turno.notaApertura && <span className="caja-banner-nota">«{turno.notaApertura}»</span>}
        </div>
        <div className="caja-banner-lado">
          <span className="caja-banner-tiempo">
            <Clock size={16} aria-hidden="true" /> {transcurrido(turno.abiertoEn)}
          </span>
          <span>desde las {hora(turno.abiertoEn)}</span>
          <div className="caja-cabecera-acciones">
            <Boton variante="secundario" onClick={cargar} aria-label="Actualizar">
              <RefreshCw size={16} aria-hidden="true" />
            </Boton>
            {puedeCobrar && (
              <Boton variante="peligro" onClick={() => setCerrando(true)}>
                <Lock size={16} aria-hidden="true" /> Cerrar caja
              </Boton>
            )}
          </div>
        </div>
      </header>

      {aviso && <Aviso>{aviso}</Aviso>}

      <ul className="caja-tarjetas" aria-label="Resumen de la sesión">
        <li>
          <span>Monto inicial</span>
          <strong>{enSoles(resumen.fondoInicial)}</strong>
        </li>
        <li>
          <span>Efectivo cobrado</span>
          <strong className="caja-verde">{enSoles(resumen.efectivoCobrado)}</strong>
        </li>
        <li>
          <span>Monto esperado</span>
          <strong className="caja-azul">{enSoles(resumen.efectivoEsperado)}</strong>
          <small>Inicial + efectivo</small>
        </li>
        <li>
          <span>Cobros</span>
          <strong>{resumen.cobros}</strong>
        </li>
      </ul>

      <section className="caja-metodos" aria-label="Resumen de métodos de pago">
        <h3 className="caja-subtitulo">Resumen de métodos de pago</h3>
        {resumen.cobros === 0 ? (
          <p className="cta-nota">Sin pagos registrados en esta sesión.</p>
        ) : (
          <ul>
            <li>
              <span>Efectivo</span>
              <strong>{enSoles(resumen.efectivoCobrado)}</strong>
            </li>
            <li>
              <span>Yape</span>
              <strong>{enSoles(resumen.totalYape)}</strong>
            </li>
            <li>
              <span>Plin</span>
              <strong>{enSoles(resumen.totalPlin)}</strong>
            </li>
            <li>
              <span>Tarjeta</span>
              <strong>{enSoles(resumen.totalTarjeta)}</strong>
            </li>
          </ul>
        )}
      </section>

      <section aria-label="Por cobrar">
        <h3 className="caja-subtitulo">Por cobrar</h3>
        {pedidos.length === 0 ? (
          <EstadoVacio titulo="Nada por cobrar" descripcion="Los pedidos que ya salieron a cocina aparecen aquí para cobrarlos." />
        ) : (
          <ul className="caja-lista">
            {pedidos.map((p) => (
              <li key={p.id} className="caja-fila">
                <div>
                  <strong>{tituloDe(p)}</strong>
                  <span className="cta-nota">
                    Pedido {p.numero} · {p.comensales} {p.comensales === 1 ? 'comensal' : 'comensales'} · desde {hora(p.abiertoEn)}
                  </span>
                </div>
                <strong className="caja-fila-monto">{enSoles(p.total)}</strong>
                {puedeCobrar && (
                  <Boton tamano="tactil" onClick={() => setCobrando(p.id)}>
                    Cobrar
                  </Boton>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-label="Cobrado en este turno">
        <h3 className="caja-subtitulo">Transacciones recientes</h3>
        {cobros.length === 0 ? (
          <EstadoVacio titulo="Todavía no cobraste nada" descripcion="Cada cobro de este turno aparece aquí." />
        ) : (
          <ul className="caja-lista">
            {cobros.map((c) => (
              <li key={c.id} className={`caja-fila ${c.revertido ? 'caja-fila-reabierta' : ''}`}>
                <div>
                  <strong>Pedido {c.pedidoNumero}</strong>
                  <span className="cta-nota">
                    {hora(c.cobradoEn)}
                    {c.totalCuentas > 0 ? ` · ${enSoles(c.totalCuentas)} a cuenta de pensionista` : ''}
                    {c.vuelto > 0 ? ` · vuelto ${enSoles(c.vuelto)}` : ''}
                  </span>
                </div>
                <strong className="caja-fila-monto">{enSoles(c.total)}</strong>
                {c.revertido ? (
                  <Insignia tono="inactivo">Reabierto</Insignia>
                ) : (
                  puedeReabrir && (
                    <Boton variante="secundario" onClick={() => setReabriendo(c)}>
                      <Undo2 size={16} aria-hidden="true" /> Reabrir
                    </Boton>
                  )
                )}
              </li>
            ))}
          </ul>
        )}
        {vigentes.length > 0 && <p className="cta-nota">Reabrir un cobro solo se puede mientras este turno siga abierto.</p>}
      </section>

      {cerrando && (
        <DialogoCierre
          token={token}
          onCerrar={() => setCerrando(false)}
          onCerrado={() => {
            setCerrando(false)
            onTurnoCerrado()
          }}
        />
      )}
      {reabriendo && (
        <DialogoReabrir
          token={token}
          cobro={reabriendo}
          onCerrar={() => setReabriendo(null)}
          onReabierto={() => {
            setReabriendo(null)
            setAviso('Cobro reabierto: los productos volvieron a estar por cobrar.')
            cargar()
          }}
        />
      )}
    </div>
  )
}
