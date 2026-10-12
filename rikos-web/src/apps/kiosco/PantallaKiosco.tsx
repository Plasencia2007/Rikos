import { useEffect, useState, useRef } from 'react'
import {
  Printer,
  RotateCcw,
  ShieldAlert,
  Clock,
  ArrowRight,
  ArrowLeft,
  UtensilsCrossed,
  CheckCircle2,
  Lock,
  Sparkles,
} from 'lucide-react'
import { emitirPreTicketKiosco, ErrorApi, type PreTicketEmitido } from '../../api/cliente'
import { useCuentaRegresiva } from '../../comun/hooks/useCuentaRegresiva'
import { minutosSegundos } from '../../comun/formato/tiempo'
import '../../estilos/kiosco.css'

const LARGO_DNI = 8
const LARGO_PIN = 4
const SEGUNDOS_REINICIO_TICKET = 15

export default function PantallaKiosco() {
  const [paso, setPaso] = useState<'dni' | 'pin' | 'ticket'>('dni')
  const [dni, setDni] = useState('')
  const [pin, setPin] = useState('')
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [intentosRestantes, setIntentosRestantes] = useState<number | null>(null)
  const [ticketEmitido, setTicketEmitido] = useState<PreTicketEmitido | null>(null)

  // Opciones de entorno / desarrollo
  const [localCodigo, setLocalCodigo] = useState(import.meta.env.VITE_LOCAL_CODIGO ?? 'sanisidro')
  const [servicioManual, setServicioManual] = useState<string>('')

  // Temporizadores
  const bloqueo = useCuentaRegresiva()
  const reinicioTicket = useCuentaRegresiva()
  const refReinicio = useRef(reinicioTicket)
  refReinicio.current = reinicioTicket

  // Reset al inicio
  function reiniciar() {
    setPaso('dni')
    setDni('')
    setPin('')
    setError(null)
    setIntentosRestantes(null)
    setTicketEmitido(null)
    reinicioTicket.limpiar()
  }

  // Teclado tactil
  function presionarDigito(digito: string) {
    if (cargando || bloqueo.activa) return
    setError(null)

    if (paso === 'dni') {
      if (dni.length < LARGO_DNI) {
        setDni((prev) => prev + digito)
      }
    } else if (paso === 'pin') {
      if (pin.length < LARGO_PIN) {
        const nuevoPin = pin + digito
        setPin(nuevoPin)
        if (nuevoPin.length === LARGO_PIN) {
          solicitarEmision(dni, nuevoPin)
        }
      }
    }
  }

  function borrarUltimo() {
    if (cargando) return
    setError(null)
    if (paso === 'dni') {
      setDni((prev) => prev.slice(0, -1))
    } else if (paso === 'pin') {
      setPin((prev) => prev.slice(0, -1))
    }
  }

  function limpiarEntrada() {
    if (cargando) return
    setError(null)
    if (paso === 'dni') {
      setDni('')
    } else if (paso === 'pin') {
      setPin('')
    }
  }

  function avanzarAlPin() {
    if (dni.length === LARGO_DNI) {
      setPaso('pin')
      setPin('')
      setError(null)
    }
  }

  function volverAlDni() {
    setPaso('dni')
    setPin('')
    setError(null)
  }

  async function solicitarEmision(dniAValidar: string, pinAValidar: string) {
    setCargando(true)
    setError(null)
    setIntentosRestantes(null)

    try {
      const respuesta = await emitirPreTicketKiosco({
        localCodigo,
        dni: dniAValidar,
        pin: pinAValidar,
        servicioManual: servicioManual || null,
      })
      setTicketEmitido(respuesta)
      setPaso('ticket')
      // Iniciar cuenta regresiva de 15 segundos para reiniciar automaticamente
      reinicioTicket.iniciar(SEGUNDOS_REINICIO_TICKET)
    } catch (err) {
      setPin('')
      if (err instanceof ErrorApi) {
        const errCodigo = err.cuerpo?.error

        if (err.estado === 423 || errCodigo === 'PENSIONISTA_BLOQUEADO') {
          const segs = err.cuerpo?.segundosRestantes ?? 900
          bloqueo.iniciar(segs)
          setError('Cuenta bloqueada por 5 intentos fallidos consecutivos.')
        } else if (errCodigo === 'CREDENCIALES_INVALIDAS') {
          const restantes = err.cuerpo?.intentosRestantes
          if (restantes != null) {
            setIntentosRestantes(restantes)
            setError(`PIN o DNI incorrecto. Te quedan ${restantes} intento(s) antes del bloqueo.`)
          } else {
            setError('PIN o DNI incorrecto.')
          }
        } else if (errCodigo === 'FUERA_DE_HORARIO') {
          setError('El restaurante no se encuentra dentro del horario de atención de ningún servicio (desayuno/almuerzo/cena).')
        } else if (errCodigo === 'PENSIONISTA_INACTIVO') {
          setError('Tu cuenta de pensionista se encuentra inactiva. Por favor acércate a administración.')
        } else {
          setError(err.cuerpo?.error ?? 'No se pudo generar el pre-ticket. Verifica tus datos.')
        }
      } else {
        setError('Error de comunicación con el kiosco. Inténtalo de nuevo.')
      }
    } finally {
      setCargando(false)
    }
  }

  // Escuchar teclado fisico para comodidad o pruebas
  useEffect(() => {
    function manejarTeclado(e: KeyboardEvent) {
      if (cargando || bloqueo.activa || paso === 'ticket') return

      if (/^[0-9]$/.test(e.key)) {
        presionarDigito(e.key)
      } else if (e.key === 'Backspace') {
        borrarUltimo()
      } else if (e.key === 'Enter') {
        if (paso === 'dni' && dni.length === LARGO_DNI) {
          avanzarAlPin()
        }
      } else if (e.key === 'Escape') {
        if (paso === 'pin') volverAlDni()
        else limpiarEntrada()
      }
    }

    window.addEventListener('keydown', manejarTeclado)
    return () => window.removeEventListener('keydown', manejarTeclado)
  }, [paso, dni, pin, cargando, bloqueo.activa])

  // Si la cuenta regresiva de ticket finaliza, reinicia la pantalla
  useEffect(() => {
    if (paso === 'ticket' && reinicioTicket.activa && reinicioTicket.segundosRestantes === 0) {
      reiniciar()
    }
  }, [paso, reinicioTicket.activa, reinicioTicket.segundosRestantes])

  function imprimirTicket() {
    window.print()
  }

  return (
    <div className="kiosco-contenedor" id="kiosco-app">
      {/* Cabecera superior */}
      <header className="kiosco-cabecera">
        <div className="kiosco-marca">
          <div className="kiosco-logo-icono" aria-hidden="true">
            <UtensilsCrossed size={26} />
          </div>
          <div>
            <h1 className="kiosco-titulo-principal">RIKO'S</h1>
            <p className="kiosco-subtitulo-principal">Kiosco de Autoservicio · Pensionistas</p>
          </div>
        </div>

        <div className="kiosco-meta-info">
          <div className="kiosco-badge-local">
            <span>Local:</span>
            <strong>{localCodigo.toUpperCase()}</strong>
          </div>
          {ticketEmitido && (
            <div className="kiosco-badge-servicio">
              <Sparkles size={14} />
              <span>{ticketEmitido.servicio}</span>
            </div>
          )}
        </div>
      </header>

      {/* Indicador de pasos */}
      <nav className="kiosco-pasos" aria-label="Progreso de autoservicio">
        <div className={`kiosco-paso-item ${paso === 'dni' ? 'activo' : ''} ${paso !== 'dni' ? 'completado' : ''}`}>
          <div className="kiosco-paso-numero">1</div>
          <span>DNI</span>
        </div>
        <div className="kiosco-paso-separador" />
        <div className={`kiosco-paso-item ${paso === 'pin' ? 'activo' : ''} ${paso === 'ticket' ? 'completado' : ''}`}>
          <div className="kiosco-paso-numero">2</div>
          <span>PIN Secreto</span>
        </div>
        <div className="kiosco-paso-separador" />
        <div className={`kiosco-paso-item ${paso === 'ticket' ? 'activo' : ''}`}>
          <div className="kiosco-paso-numero">3</div>
          <span>Pre-Ticket</span>
        </div>
      </nav>

      {/* Cuerpo principal */}
      <main className="kiosco-cuerpo">
        {/* PASO 1: Ingreso de DNI */}
        {paso === 'dni' && (
          <section className="kiosco-tarjeta" aria-labelledby="titulo-dni">
            <h2 id="titulo-dni" className="kiosco-instruccion-titulo">
              Ingresa tu DNI
            </h2>
            <p className="kiosco-instruccion-sub">
              Digita tu número de DNI de pensionista de 8 dígitos
            </p>

            {error && (
              <div className="kiosco-mensaje-error" role="alert">
                <ShieldAlert size={20} />
                <span>{error}</span>
              </div>
            )}

            {/* 8 Casillas de DNI */}
            <div className="kiosco-dni-casillas" id="casillas-dni">
              {Array.from({ length: LARGO_DNI }).map((_, i) => {
                const digito = dni[i] ?? ''
                const esActual = i === dni.length
                return (
                  <div
                    key={i}
                    className={`kiosco-dni-casilla ${digito ? 'llena' : ''} ${esActual ? 'actual' : ''}`}
                  >
                    {digito}
                  </div>
                )
              })}
            </div>

            {/* Teclado tactil */}
            <div className="kiosco-teclado-grid" id="teclado-kiosco-dni">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((t) => (
                <button
                  key={t}
                  type="button"
                  className="kiosco-tecla"
                  id={`tecla-dni-${t}`}
                  onClick={() => presionarDigito(t)}
                >
                  {t}
                </button>
              ))}
              <button
                type="button"
                className="kiosco-tecla kiosco-tecla-secundaria"
                id="tecla-dni-limpiar"
                onClick={limpiarEntrada}
              >
                C
              </button>
              <button
                type="button"
                className="kiosco-tecla"
                id="tecla-dni-0"
                onClick={() => presionarDigito('0')}
              >
                0
              </button>
              <button
                type="button"
                className="kiosco-tecla kiosco-tecla-secundaria"
                id="tecla-dni-borrar"
                onClick={borrarUltimo}
                aria-label="Borrar último"
              >
                ⌫
              </button>
            </div>

            <div className="kiosco-acciones-fila">
              <button
                type="button"
                className="kiosco-btn-accion kiosco-btn-primario"
                id="btn-continuar-pin"
                disabled={dni.length !== LARGO_DNI}
                onClick={avanzarAlPin}
              >
                <span>Continuar</span>
                <ArrowRight size={18} />
              </button>
            </div>
          </section>
        )}

        {/* PASO 2: Ingreso de PIN */}
        {paso === 'pin' && (
          <section className="kiosco-tarjeta" aria-labelledby="titulo-pin">
            <h2 id="titulo-pin" className="kiosco-instruccion-titulo">
              Ingresa tu PIN
            </h2>
            <p className="kiosco-instruccion-sub">
              DNI: <strong>{dni}</strong> · Clave de 4 dígitos
            </p>

            {bloqueo.activa ? (
              <div className="kiosco-bloqueo-box" role="alert">
                <ShieldAlert size={36} color="#fca5a5" />
                <strong style={{ fontSize: '1.1rem', color: '#fff' }}>
                  Cuenta temporalmente bloqueada
                </strong>
                <p style={{ margin: 0, color: '#fca5a5', fontSize: '0.9rem' }}>
                  Demasiados intentos fallidos. Podrás intentar nuevamente en:
                </p>
                <div className="kiosco-bloqueo-contador" id="contador-bloqueo">
                  {minutosSegundos(bloqueo.segundosRestantes)}
                </div>
              </div>
            ) : (
              <>
                {error && (
                  <div className="kiosco-mensaje-error" role="alert">
                    <ShieldAlert size={20} />
                    <span>{error}</span>
                  </div>
                )}

                {intentosRestantes != null && intentosRestantes > 0 && (
                  <div className="kiosco-mensaje-aviso" role="status">
                    <Lock size={18} />
                    <span>Intentos disponibles: {intentosRestantes}</span>
                  </div>
                )}

                {/* 4 Puntos de PIN */}
                <div className="kiosco-pin-puntos" id="puntos-pin">
                  {Array.from({ length: LARGO_PIN }).map((_, i) => (
                    <div
                      key={i}
                      className={`kiosco-pin-punto ${i < pin.length ? 'lleno' : ''}`}
                    />
                  ))}
                </div>

                {/* Teclado tactil */}
                <div className="kiosco-teclado-grid" id="teclado-kiosco-pin">
                  {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((t) => (
                    <button
                      key={t}
                      type="button"
                      className="kiosco-tecla"
                      id={`tecla-pin-${t}`}
                      disabled={cargando}
                      onClick={() => presionarDigito(t)}
                    >
                      {t}
                    </button>
                  ))}
                  <button
                    type="button"
                    className="kiosco-tecla kiosco-tecla-secundaria"
                    id="tecla-pin-limpiar"
                    disabled={cargando}
                    onClick={limpiarEntrada}
                  >
                    C
                  </button>
                  <button
                    type="button"
                    className="kiosco-tecla"
                    id="tecla-pin-0"
                    disabled={cargando}
                    onClick={() => presionarDigito('0')}
                  >
                    0
                  </button>
                  <button
                    type="button"
                    className="kiosco-tecla kiosco-tecla-secundaria"
                    id="tecla-pin-borrar"
                    disabled={cargando}
                    onClick={borrarUltimo}
                    aria-label="Borrar último"
                  >
                    ⌫
                  </button>
                </div>
              </>
            )}

            <div className="kiosco-acciones-fila">
              <button
                type="button"
                className="kiosco-btn-accion kiosco-btn-secundario"
                id="btn-volver-dni"
                onClick={volverAlDni}
                disabled={cargando}
              >
                <ArrowLeft size={18} />
                <span>Volver</span>
              </button>
            </div>
          </section>
        )}

        {/* PASO 3: Pre-Ticket Emitido */}
        {paso === 'ticket' && ticketEmitido && (
          <div className="kiosco-ticket-contenedor">
            <article className="kiosco-ticket-papel" id="hoja-ticket-impresion">
              <header className="kiosco-ticket-header">
                <h3 className="kiosco-ticket-logo-texto">RIKO'S RESTAURANTE</h3>
                <p className="kiosco-ticket-subtitulo">Pre-Ticket de Autoservicio</p>
              </header>

              {/* Codigo destacado de 6 digitos */}
              <div className="kiosco-ticket-codigo-box">
                <div className="kiosco-ticket-codigo-label">Tu Código de Atención</div>
                <div className="kiosco-ticket-codigo-numero" id="codigo-pre-ticket">
                  {ticketEmitido.codigo.slice(0, 3)} {ticketEmitido.codigo.slice(3)}
                </div>
                {ticketEmitido.reutilizado && (
                  <div className="kiosco-badge-reutilizado">
                    Ticket recuperado de este turno
                  </div>
                )}
              </div>

              {/* Datos de la atencion */}
              <div className="kiosco-ticket-detalles">
                <div className="kiosco-ticket-fila">
                  <span className="kiosco-ticket-fila-etiqueta">Pensionista:</span>
                  <span className="kiosco-ticket-fila-valor" id="ticket-nombre">
                    {ticketEmitido.pensionistaNombre}
                  </span>
                </div>
                <div className="kiosco-ticket-fila">
                  <span className="kiosco-ticket-fila-etiqueta">DNI:</span>
                  <span className="kiosco-ticket-fila-valor">{ticketEmitido.dni}</span>
                </div>
                <div className="kiosco-ticket-fila">
                  <span className="kiosco-ticket-fila-etiqueta">Servicio:</span>
                  <span className="kiosco-ticket-fila-valor" style={{ textTransform: 'capitalize' }}>
                    {ticketEmitido.servicio}
                  </span>
                </div>
                <div className="kiosco-ticket-fila">
                  <span className="kiosco-ticket-fila-etiqueta">Local:</span>
                  <span className="kiosco-ticket-fila-valor">{ticketEmitido.localNombre}</span>
                </div>
                <div className="kiosco-ticket-fila">
                  <span className="kiosco-ticket-fila-etiqueta">Fecha y hora:</span>
                  <span className="kiosco-ticket-fila-valor">
                    {ticketEmitido.fecha} ·{' '}
                    {new Date(ticketEmitido.emitidoEn).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
              </div>

              {/* Disclaimer legal obligatorio (US-019) */}
              <div className="kiosco-ticket-aviso-legal" id="ticket-aviso-legal">
                {ticketEmitido.mensajeLegal}
              </div>

              <p className="kiosco-ticket-instruccion-final">
                Muestra este código al mozo o indícalo al momento de tomar tu pedido en mesa.
              </p>
            </article>

            {/* Acciones del Kiosco */}
            <div className="kiosco-ticket-acciones">
              <button
                type="button"
                className="kiosco-btn-accion kiosco-btn-primario"
                id="btn-imprimir-ticket"
                onClick={imprimirTicket}
              >
                <Printer size={20} />
                <span>Imprimir Ticket</span>
              </button>

              <button
                type="button"
                className="kiosco-btn-accion kiosco-btn-secundario"
                id="btn-finalizar-ticket"
                onClick={reiniciar}
              >
                <CheckCircle2 size={18} />
                <span>Finalizar y Salir</span>
              </button>

              <div className="kiosco-contador-reinicio" id="temporizador-reinicio">
                <Clock size={14} />
                <span>
                  Reiniciando pantalla en {reinicioTicket.segundosRestantes} segundos...
                </span>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Barra de Desarrollo / Pruebas (permite forzar turno o cambiar local) */}
      <aside className="kiosco-panel-dev" aria-label="Herramientas de prueba">
        <span>Local:</span>
        <input
          id="input-local-dev"
          type="text"
          value={localCodigo}
          onChange={(e) => setLocalCodigo(e.target.value.toLowerCase().trim())}
          className="kiosco-select-dev"
          style={{ width: '85px' }}
          title="Código del local para pruebas"
        />
        <span>Turno:</span>
        <select
          id="select-servicio-dev"
          className="kiosco-select-dev"
          value={servicioManual}
          onChange={(e) => setServicioManual(e.target.value)}
        >
          <option value="">Automático (Horario)</option>
          <option value="desayuno">Desayuno</option>
          <option value="almuerzo">Almuerzo</option>
          <option value="cena">Cena</option>
        </select>
        <button
          type="button"
          onClick={reiniciar}
          title="Reiniciar Kiosco"
          style={{ background: 'none', border: 'none', color: '#a4c4bb', cursor: 'pointer' }}
        >
          <RotateCcw size={14} />
        </button>
      </aside>
    </div>
  )
}
