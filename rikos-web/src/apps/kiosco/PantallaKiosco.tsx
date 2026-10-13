import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowRight, Check, Delete, ChevronDown, CircleHelp, Clock3, ConciergeBell, LockKeyhole, MapPin, Printer, ShieldCheck, UtensilsCrossed } from 'lucide-react'
import { consultarContextoKiosco, emitirPreTicketKiosco, listarLocalesPublicosKiosco, ErrorApi, type ContextoKiosco, type PreTicketEmitido } from '../../api/cliente'
import '../../estilos/kiosco.css'

const DURACION_TICKET_MS = 20_000
const DURACION_INACTIVIDAD_MS = 45_000

function codigoLocalInicial() {
  try { return localStorage.getItem('rikos-kiosco-local') || import.meta.env.VITE_LOCAL_CODIGO || '' }
  catch { return import.meta.env.VITE_LOCAL_CODIGO || '' }
}
function hora(valor: string) { return valor.slice(11, 16) }
function tituloServicio(servicio: string) { return servicio ? servicio.charAt(0).toUpperCase() + servicio.slice(1) : '' }

export default function PantallaKiosco() {
  const [localCodigo, setLocalCodigo] = useState(codigoLocalInicial)
  const [locales, setLocales] = useState<{ codigo: string; nombre: string }[]>([])
  const [errorLocales, setErrorLocales] = useState(false)
  const [contexto, setContexto] = useState<ContextoKiosco | null>(null)
  const [errorContexto, setErrorContexto] = useState(false)
  const [paso, setPaso] = useState<'pin' | 'ticket'>('pin')
  const [pin, setPin] = useState('')
  const [ticket, setTicket] = useState<PreTicketEmitido | null>(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const [imprimiendo, setImprimiendo] = useState(false)
  const [segundos, setSegundos] = useState(0)
  const [venceSesion, setVenceSesion] = useState<number | null>(null)
  const version = useRef(0)
  const [menuLocal, setMenuLocal] = useState(false)
  const selectorLocal = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!menuLocal) return
    const fuera = (e: PointerEvent) => { if (!selectorLocal.current?.contains(e.target as Node)) setMenuLocal(false) }
    const tecla = (e: KeyboardEvent) => { if (e.key === 'Escape') setMenuLocal(false) }
    document.addEventListener('pointerdown', fuera)
    document.addEventListener('keydown', tecla)
    return () => { document.removeEventListener('pointerdown', fuera); document.removeEventListener('keydown', tecla) }
  }, [menuLocal])
  const enviando = useRef(false)
  const entrada = useRef<HTMLInputElement>(null)

  const reiniciar = useCallback(() => {
    version.current++
    enviando.current = false
    setPaso('pin'); setPin(''); setTicket(null); setError(''); setCargando(false)
    setImprimiendo(false); setSegundos(0); setVenceSesion(null)
  }, [])

  useEffect(() => {
    let vigente = true
    const cargar = () => {
      listarLocalesPublicosKiosco().then((disponibles) => {
        if (!vigente) return
        setLocales(disponibles)
        setErrorLocales(false)
        setLocalCodigo((actual: string) => {
          if (disponibles.some((local) => local.codigo === actual)) return actual
          const primero = disponibles[0]?.codigo || ''
          try { localStorage.setItem('rikos-kiosco-local', primero) } catch { /* almacenamiento no disponible */ }
          return primero
        })
      }).catch(() => { if (vigente) setErrorLocales(true) })
    }
    cargar()
    const intervalo = window.setInterval(cargar, 15_000)
    return () => { vigente = false; window.clearInterval(intervalo) }
  }, [])

  useEffect(() => {
    if (!localCodigo || !locales.some((local) => local.codigo === localCodigo)) return
    let vigente = true
    const cargar = () => {
      consultarContextoKiosco(localCodigo)
        .then((respuesta) => { if (vigente) { setContexto(respuesta); setErrorContexto(false) } })
        .catch(() => { if (vigente) { setContexto(null); setErrorContexto(true) } })
    }
    cargar()
    const intervalo = window.setInterval(cargar, 15_000)
    return () => { vigente = false; window.clearInterval(intervalo) }
  }, [localCodigo, locales])

  useEffect(() => {
    if (paso !== 'pin' || !pin) return
    const id = window.setTimeout(() => { setPin(''); setError('') }, DURACION_INACTIVIDAD_MS)
    return () => window.clearTimeout(id)
  }, [pin, paso])

  useEffect(() => {
    if (paso !== 'ticket' || imprimiendo || !venceSesion) return
    const actualizar = () => {
      const restante = Math.max(0, Math.ceil((venceSesion - Date.now()) / 1000))
      setSegundos(restante)
      if (restante === 0) reiniciar()
    }
    actualizar()
    const id = window.setInterval(actualizar, 500)
    return () => window.clearInterval(id)
  }, [paso, imprimiendo, venceSesion, reiniciar])

  useEffect(() => {
    if (!imprimiendo) return
    const volver = () => { setImprimiendo(false); setVenceSesion(Date.now() + DURACION_TICKET_MS) }
    window.addEventListener('afterprint', volver)
    return () => window.removeEventListener('afterprint', volver)
  }, [imprimiendo])

  const longitud = contexto?.longitudPin || 6
  const franja = contexto?.servicioActual
  const listo = Boolean(localCodigo && locales.some((local) => local.codigo === localCodigo) && contexto?.estado === 'en_atencion' && !errorContexto)

  function escribirPin(valor: string) {
    if (cargando || paso !== 'pin') return
    setPin(valor.replace(/\D/g, '').slice(0, longitud))
    setError('')
  }

  function elegirLocal(codigo: string) {
    reiniciar(); setContexto(null); setErrorContexto(false); setLocalCodigo(codigo)
    try { localStorage.setItem('rikos-kiosco-local', codigo) } catch { /* almacenamiento no disponible */ }
  }

  async function obtenerTicket() {
    if (!listo || pin.length !== longitud || enviando.current) return
    enviando.current = true
    const versionSolicitud = version.current
    setCargando(true); setError('')
    try {
      const recibido = await emitirPreTicketKiosco({ localCodigo, pin })
      if (version.current !== versionSolicitud) return
      setPin(''); setTicket(recibido); setPaso('ticket')
      setVenceSesion(Date.now() + DURACION_TICKET_MS)
    } catch (fallo) {
      if (version.current !== versionSolicitud) return
      setPin('')
      if (fallo instanceof ErrorApi) {
        const codigo = fallo.cuerpo?.error
        if (codigo === 'FUERA_DE_HORARIO') setError('La atención de esta comida ya terminó. Revisa el horario del local.')
        else if (codigo === 'PENSIONISTA_BLOQUEADO') setError('Tu PIN está temporalmente bloqueado. Acércate al personal. El siguiente pensionista puede continuar.')
        else if (codigo === 'PENSIONISTA_INACTIVO') setError('Esta cuenta no está activa. Acércate al personal del local.')
        else if (fallo.estado === 429) setError('El kiosco recibió demasiados intentos. Espera un momento o acércate al personal.')
        else if (codigo === 'CREDENCIALES_INVALIDAS') setError('No reconocemos este PIN. Compruébalo o acércate al personal para activarlo.')
        else setError('No pudimos obtener tu ticket. Inténtalo nuevamente o pide ayuda al personal.')
      } else setError('No hay comunicación con el sistema. Acércate al personal del local.')
    } finally {
      if (version.current === versionSolicitud) { enviando.current = false; setCargando(false) }
    }
  }

  function imprimir() { if (ticket) { setImprimiendo(true); window.print() } }

  return <div className="kiosco" id="kiosco-app">
    <header className="kiosco__cabecera">
      <div className="kiosco__marca"><UtensilsCrossed aria-hidden="true" /><strong>RIKO’S</strong><span className="kiosco__marca-separador" aria-hidden="true" /><span className="kiosco__marca-descripcion">Autoservicio para pensionistas</span></div>
      <div className="kiosco__selector" ref={selectorLocal}>
        <button type="button" className="kiosco__local" aria-haspopup="listbox" aria-expanded={menuLocal} aria-label="Seleccionar local" disabled={!locales.length} onClick={() => setMenuLocal((a) => !a)}>
          <MapPin aria-hidden="true" /><span><small>Local</small><strong>{locales.find((l) => l.codigo === localCodigo)?.nombre ?? (!locales.length ? (errorLocales ? 'Locales no disponibles' : 'Cargando locales…') : 'Elige un local')}</strong></span><ChevronDown aria-hidden="true" className={menuLocal ? 'kiosco__chevron--abierto' : ''} />
        </button>
        {menuLocal && <ul className="kiosco__locales" role="listbox" aria-label="Locales">
          {locales.map((l) => <li key={l.codigo} role="option" aria-selected={l.codigo === localCodigo}>
            <button type="button" className={l.codigo === localCodigo ? 'kiosco__local-opcion kiosco__local-opcion--activa' : 'kiosco__local-opcion'} onClick={() => { elegirLocal(l.codigo); setMenuLocal(false) }}>
              <span>{l.nombre}</span>{l.codigo === localCodigo && <Check aria-hidden="true" />}
            </button>
          </li>)}
        </ul>}
      </div>
    </header>
    <main className="kiosco__escena">
      <div className="kiosco__onda kiosco__onda--uno" aria-hidden="true" /><div className="kiosco__onda kiosco__onda--dos" aria-hidden="true" />
      <section className="kiosco__panel-servicio" aria-label="Atención actual"><div className="kiosco__servicio-contenido">
        <div className="kiosco__eyebrow"><span className="kiosco__icono"><ConciergeBell aria-hidden="true" /></span> SERVICIO ACTUAL</div>
        <h1>{franja ? tituloServicio(franja.servicio) : 'Atención'}</h1>
        {franja && <p className="kiosco__horario"><Clock3 aria-hidden="true" /> {hora(franja.inicio)} a {hora(franja.fin)}</p>}
        <div className={`kiosco__estado ${listo ? 'kiosco__estado--abierto' : ''}`} role="status"><span aria-hidden="true" />{listo ? 'En atención' : errorContexto ? 'Sin conexión' : contexto?.estado === 'fuera_de_horario' ? 'Fuera de horario' : 'Consultando horario'}</div>
        {contexto?.estado === 'fuera_de_horario' && contexto.siguienteServicio && <p className="kiosco__proximo">Próxima atención: {tituloServicio(contexto.siguienteServicio.servicio)} · {hora(contexto.siguienteServicio.inicio)}</p>}
        <div className="kiosco__linea" /><h2>Tu ticket en dos pasos</h2><ol className="kiosco__pasos"><li><span>1</span><div><strong>Ingresa tu PIN</strong><small>Tu código personal de {longitud} dígitos.</small></div></li><li><span>2</span><div><strong>Recoge tu ticket</strong><small>Entrégalo al mozo para tu atención.</small></div></li></ol>
        <div className="kiosco__ayuda"><CircleHelp aria-hidden="true" /><div><strong>¿Olvidaste tu PIN?</strong><p>Acércate al personal del local.</p></div></div>
      </div><img className="kiosco__plato" src="/imagenes/kiosco/plato-pensionista.png" alt="" aria-hidden="true" /></section>
      <section className="kiosco__panel-accion" aria-labelledby="kiosco-titulo">
      {paso === 'pin' ? <>
        <div className="kiosco__accion-encabezado"><div><span className="kiosco__paso-actual">PASO 1 DE 2</span><h2 id="kiosco-titulo">Ingresa tu PIN</h2><p>Usa tu PIN personal para obtener tu ticket.</p></div><div className="kiosco__seguro"><ShieldCheck aria-hidden="true" /><span>Tu información<br />es segura</span></div></div>
        <div className="kiosco__campo-pin" onClick={() => entrada.current?.focus()}><LockKeyhole aria-hidden="true" /><div className="kiosco__puntos" aria-hidden="true">{Array.from({ length: longitud }, (_, i) => <span className={i < pin.length ? 'lleno' : ''} key={i} />)}</div><input ref={entrada} aria-label="PIN personal" type="password" inputMode="numeric" autoComplete="off" value={pin} onChange={(e) => escribirPin(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); void obtenerTicket() } }} maxLength={longitud} disabled={cargando || !listo} /></div>
        {error && <p className="kiosco__error" role="alert">{error}</p>}
        {errorContexto && <p className="kiosco__error" role="alert">No podemos consultar el horario. Acércate al personal del local.</p>}
        <div className="kiosco__teclado" aria-label="Teclado numérico">{['1','2','3','4','5','6','7','8','9'].map((n) => <button type="button" key={n} disabled={cargando || !listo} onClick={() => escribirPin(pin + n)}>{n}</button>)}<button type="button" className="kiosco__tecla-secundaria" disabled={cargando || !listo} onClick={() => escribirPin('')}>Limpiar</button><button type="button" disabled={cargando || !listo} onClick={() => escribirPin(pin + '0')}>0</button><button type="button" className="kiosco__tecla-secundaria" disabled={cargando || !listo} onClick={() => escribirPin(pin.slice(0, -1))}><Delete aria-hidden="true" /> Borrar</button></div>
        <button className="kiosco__boton-principal" type="button" disabled={!listo || cargando || pin.length !== longitud} onClick={() => void obtenerTicket()}>{cargando ? 'Validando…' : 'Obtener ticket'} {!cargando && <ArrowRight aria-hidden="true" />}</button>
      </> : ticket && <><span className="kiosco__paso-actual">PASO 2 DE 2</span><h2 className="kiosco__titulo-ticket" id="kiosco-titulo">{ticket.reutilizado ? 'Ya tienes tu ticket' : 'Tu ticket está listo'}</h2><p className="kiosco__ticket-instruccion">Entrega este código al mozo cuando te atienda.</p><article className="kiosco__ticket" id="hoja-ticket-impresion"><strong className="kiosco__ticket-marca">RIKO’S · Ticket de atención</strong><span className="kiosco__ticket-codigo-label">TU CÓDIGO</span><strong className="kiosco__ticket-codigo">{ticket.codigo.slice(0, 3)} {ticket.codigo.slice(3)}</strong>{ticket.reutilizado && <p className="kiosco__ticket-repetido">Es el mismo ticket pendiente de esta atención.</p>}<dl><div><dt>Pensionista</dt><dd>{ticket.pensionistaNombre}</dd></div><div><dt>Servicio</dt><dd>{tituloServicio(ticket.servicio)}</dd></div><div><dt>Local</dt><dd>{ticket.localNombre}</dd></div><div><dt>Válido hasta</dt><dd>{hora(ticket.venceEn)}</dd></div></dl><p className="kiosco__ticket-legal">{ticket.mensajeLegal}</p></article><div className="kiosco__ticket-acciones"><button className="kiosco__boton-principal" type="button" onClick={imprimir}><Printer aria-hidden="true" /> {ticket.reutilizado ? 'Reimprimir ticket' : 'Imprimir ticket'}</button><button className="kiosco__boton-salida" type="button" onClick={reiniciar}>Finalizar y salir</button></div>{!imprimiendo && <p className="kiosco__contador" role="status">La pantalla se limpiará en {segundos} segundos.</p>}</>}
      </section>
    </main>
  </div>
}
