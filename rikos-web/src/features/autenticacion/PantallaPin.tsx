import { useEffect, useState } from 'react'
import { ArrowRight, Clock, Delete, LifeBuoy, Lock, MapPin, Monitor } from 'lucide-react'
import { LogoDelRestaurante } from '../../comun/componentes'
import { useCuentaRegresiva } from '../../comun/hooks/useCuentaRegresiva'
import { minutosSegundos } from '../../comun/formato/tiempo'
import { iniciarSesionConPin, obtenerLocalPublico, ErrorApi, type SesionIniciada } from '../../api/cliente'

const LARGO_PIN = 4
const LOCAL_CODIGO = import.meta.env.VITE_LOCAL_CODIGO ?? 'sanisidro'
const TECLAS = ['1', '2', '3', '4', '5', '6', '7', '8', '9']

/** "Jueves 29 de octubre": la fecha del día en la foto de la tableta bloqueada. */
const fechaLarga = (fecha: Date) => {
  const texto = fecha.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' })
  return (texto.charAt(0).toUpperCase() + texto.slice(1)).replace(',', '')
}

const MENSAJES: Record<string, string> = {
  CREDENCIALES_INVALIDAS: 'PIN incorrecto.',
  LOCAL_INACTIVO: 'Este local está desactivado. Contacta a un administrador.',
}

/**
 * HU-003: la tableta bloqueada del POS. El mozo desbloquea con su PIN de 4 dígitos (al completar se verifica solo;
 * "Desbloquear" hace lo mismo). Diseño 2026-10-29: a la izquierda la foto del restaurante con el mensaje; a la
 * derecha la tarjeta con las casillas del PIN y el teclado. En web no se desplaza: los tamaños siguen el alto de la
 * pantalla. En celular solo se ve la tarjeta, con el logo arriba, y ahi si se puede desplazar.
 */
export function PantallaPin({ onExito }: { onExito: (sesion: SesionIniciada) => void }) {
  const [pin, setPin] = useState('')
  const [verificando, setVerificando] = useState(false)
  const [error, setError] = useState('')
  const [nombreLocal, setNombreLocal] = useState('')
  const [enLinea, setEnLinea] = useState(() => navigator.onLine)
  const [ahora, setAhora] = useState(() => new Date())
  const bloqueo = useCuentaRegresiva()
  const apagado = verificando || bloqueo.activa

  useEffect(() => {
    obtenerLocalPublico(LOCAL_CODIGO)
      .then((local) => setNombreLocal(local.nombre))
      .catch(() => setNombreLocal(''))
  }, [])

  // La hora de la foto avanza sola (cada 15 s basta para los minutos).
  useEffect(() => {
    const reloj = window.setInterval(() => setAhora(new Date()), 15_000)
    return () => window.clearInterval(reloj)
  }, [])

  useEffect(() => {
    const cambiar = () => setEnLinea(navigator.onLine)
    window.addEventListener('online', cambiar)
    window.addEventListener('offline', cambiar)
    return () => {
      window.removeEventListener('online', cambiar)
      window.removeEventListener('offline', cambiar)
    }
  }, [])

  async function intentar(pinCompleto: string) {
    setVerificando(true)
    setError('')
    try {
      const sesion = await iniciarSesionConPin(LOCAL_CODIGO, pinCompleto)
      onExito(sesion)
    } catch (excepcion) {
      if (excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'BLOQUEADO') {
        bloqueo.iniciar(excepcion.cuerpo.segundosRestantes ?? 0)
      } else {
        const codigo = excepcion instanceof ErrorApi ? excepcion.cuerpo?.error : undefined
        setError((codigo && MENSAJES[codigo]) ?? 'No se pudo verificar el PIN.')
      }
      setPin('')
    } finally {
      setVerificando(false)
    }
  }

  // Se calcula con el PIN actual (no dentro de setPin) para no verificar dos veces en modo estricto.
  function digito(d: string) {
    if (apagado || pin.length >= LARGO_PIN) return
    setError('')
    const nuevo = pin + d
    setPin(nuevo)
    if (nuevo.length === LARGO_PIN) void intentar(nuevo)
  }

  useEffect(() => {
    function manejarTeclado(e: KeyboardEvent) {
      if (verificando || bloqueo.activa) return
      if (/^[0-9]$/.test(e.key)) digito(e.key)
      else if (e.key === 'Backspace') setPin((actual) => actual.slice(0, -1))
      else if (e.key === 'Escape') setPin('')
    }
    window.addEventListener('keydown', manejarTeclado)
    return () => window.removeEventListener('keydown', manejarTeclado)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [verificando, bloqueo.activa, pin])

  const local = nombreLocal || 'San Isidro'

  return (
    <div className="pin2">
      <aside className="pin2-foto">
        <LogoDelRestaurante className="pin2-logo" medidas={{ ancho: 300, alto: 72 }} />
        <div className="pin2-mensaje">
          <h1>
            Una pausa.
            <em>Todo sigue en orden.</em>
          </h1>
          <p>Ingresa tu PIN para continuar atendiendo.</p>
          <dl className="pin2-datos">
            <div>
              <dt>Local</dt>
              <dd>
                <MapPin size={18} aria-hidden="true" />
                {local}
              </dd>
            </div>
            <div>
              <dt>{fechaLarga(ahora)}</dt>
              <dd>
                <Clock size={18} aria-hidden="true" />
                {ahora.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}
              </dd>
            </div>
          </dl>
        </div>
        <span className="pin2-protegida">
          <Lock size={17} aria-hidden="true" />
          Tu sesión está protegida
        </span>
      </aside>

      <main className="pin2-acceso">
        <div className="pin2-terminal">
          <Monitor size={26} strokeWidth={1.6} aria-hidden="true" />
          <span>
            <strong>Punto de venta</strong>
            <small>{local}</small>
          </span>
        </div>

        <div className="pin2-marca-movil">
          <LogoDelRestaurante className="pin2-logo-movil" medidas={{ ancho: 200, alto: 48 }} />
          <span>{local}</span>
        </div>

        <section className="pin2-tarjeta" aria-label="Desbloquear terminal">
          <span className="pin2-candado" aria-hidden="true">
            <Lock size={26} strokeWidth={2.2} />
          </span>
          <h2>Desbloquear terminal</h2>
          <p className="pin2-ayuda">Ingresa tu PIN de {LARGO_PIN} dígitos</p>

          <div className="pin2-casillas" aria-label={`${pin.length} de ${LARGO_PIN} dígitos`} role="status">
            {Array.from({ length: LARGO_PIN }).map((_, i) => (
              <span
                key={i}
                className={`pin2-casilla ${i < pin.length ? 'llena' : ''} ${i === pin.length && !apagado ? 'actual' : ''} ${error ? 'error' : ''}`}
              >
                {i < pin.length && <i />}
              </span>
            ))}
          </div>

          <div className="pin2-teclado">
            {TECLAS.map((t) => (
              <button key={t} type="button" className="pin2-tecla" disabled={apagado} onClick={() => digito(t)}>
                {t}
              </button>
            ))}
            <button type="button" className="pin2-tecla suave" disabled={apagado} onClick={() => setPin('')}>
              Limpiar
            </button>
            <button type="button" className="pin2-tecla" disabled={apagado} onClick={() => digito('0')}>
              0
            </button>
            <button
              type="button"
              className="pin2-tecla suave"
              disabled={apagado}
              aria-label="Borrar el último dígito"
              onClick={() => setPin((actual) => actual.slice(0, -1))}
            >
              <Delete size={26} strokeWidth={1.8} aria-hidden="true" />
            </button>
          </div>

          <button
            type="button"
            className="pin2-desbloquear"
            disabled={pin.length < LARGO_PIN || apagado}
            onClick={() => void intentar(pin)}
          >
            {verificando ? 'Comprobando…' : 'Desbloquear'}
            <ArrowRight size={20} aria-hidden="true" />
          </button>

          {error && <p className="pin2-error">{error}</p>}
          {bloqueo.activa && (
            <p className="pin2-error">
              Demasiados intentos fallidos. Intenta de nuevo en {minutosSegundos(bloqueo.segundosRestantes)}.
            </p>
          )}
        </section>

        <footer className="pin2-pie">
          <span>
            <LifeBuoy size={20} strokeWidth={1.7} aria-hidden="true" />
            ¿No recuerdas tu PIN? Contacta al administrador.
          </span>
          <span className={`pin2-sistema ${enLinea ? '' : 'caido'}`} role="status">
            <i aria-hidden="true" />
            {enLinea ? 'Sistema en línea' : 'Sin conexión'}
          </span>
        </footer>
      </main>
    </div>
  )
}
