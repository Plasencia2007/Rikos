import { useEffect, useState } from 'react'
import { EntradaNumerica } from '../../comun/componentes/EntradaNumerica'
import { IconoUbicacion } from '../../comun/componentes/iconos'
import { useCuentaRegresiva } from '../../comun/hooks/useCuentaRegresiva'
import { minutosSegundos } from '../../comun/formato/tiempo'
import { iniciarSesionConPin, obtenerLocalPublico, ErrorApi, type SesionIniciada } from '../../api/cliente'

const LARGO_PIN = 4
const LOCAL_CODIGO = import.meta.env.VITE_LOCAL_CODIGO ?? 'sanisidro'

const MENSAJES: Record<string, string> = {
  CREDENCIALES_INVALIDAS: 'PIN incorrecto.',
}

export function PantallaPin({ onExito }: { onExito: (sesion: SesionIniciada) => void }) {
  const [pin, setPin] = useState('')
  const [verificando, setVerificando] = useState(false)
  const [error, setError] = useState('')
  const [nombreLocal, setNombreLocal] = useState('')
  const bloqueo = useCuentaRegresiva()

  useEffect(() => {
    obtenerLocalPublico(LOCAL_CODIGO)
      .then((local) => setNombreLocal(local.nombre))
      .catch(() => setNombreLocal(''))
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


  return (
    <div className="pantalla-pin">
      {/* Panel Izquierdo: Marca RIKO'S y Estado de Bloqueo (Fondo Marrón Oscuro) */}
      <aside className="pin-marca">
        {/* Capas orgánicas de fondo marrón */}
        <div className="pin-marca-ondas" aria-hidden="true">
          <svg viewBox="0 0 500 600" fill="none" preserveAspectRatio="none" className="pin-onda-oscura">
            <path
              d="M0 250 C120 220, 160 380, 240 450 C320 520, 360 560, 420 600 L0 600 Z"
              fill="#4a1c05"
            />
          </svg>
        </div>

        {/* Doodles tenues en el panel izquierdo */}
        <div className="pin-doodles-izq" aria-hidden="true">
          {/* Gorro de chef arriba a la izquierda */}
          <svg className="pin-doodle-chef-izq" viewBox="0 0 120 110" fill="none" stroke="rgba(255, 255, 255, 0.16)" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M 28 78 C 14 70, 10 46, 26 34 C 20 18, 40 8, 56 14 C 66 2, 86 2, 94 16 C 108 16, 116 32, 108 48 C 116 64, 104 80, 92 80" />
            <path d="M 26 78 L 94 78" />
            <path d="M 30 88 L 90 88" />
          </svg>

          {/* Cubiertos abajo a la derecha del panel izquierdo */}
          <svg className="pin-doodle-cubiertos-izq" viewBox="0 0 100 130" fill="none" stroke="rgba(255, 255, 255, 0.16)" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M 26 15 L 26 44 C 26 50, 36 50, 36 44 L 36 15" />
            <path d="M 31 15 L 31 45" />
            <path d="M 31 50 L 31 108" />
            <path d="M 72 15 C 72 15, 86 28, 86 52 C 86 60, 78 62, 72 62 L 72 108" />
          </svg>

          {/* Rayos / chispas decorativas junto al plato */}
          <svg className="pin-chispas-plato" viewBox="0 0 60 60" fill="none" stroke="#ffffff" strokeWidth="4" strokeLinecap="round">
            <line x1="16" y1="12" x2="22" y2="30" />
            <line x1="38" y1="14" x2="48" y2="32" />
            <line x1="8" y1="40" x2="26" y2="44" />
          </svg>
        </div>

        {/* Contenido Central de Marca */}
        <div className="pin-marca-contenido">
          <div className="pin-logo-contenedor">
            <svg className="pin-logo-chef" viewBox="0 0 100 85" fill="none">
              {/* Gorro de chef con corona/pliegue central en blanco */}
              <path
                d="M 22 56 C 14 50, 11 36, 21 26 C 16 14, 30 5, 42 9 C 50 1, 64 2, 69 11 C 80 8, 88 18, 82 28 C 89 38, 84 52, 73 56 Z"
                fill="none"
                stroke="#ffffff"
                strokeWidth="3.6"
                strokeLinejoin="round"
              />
              {/* Detalle corona / pliegue central */}
              <path d="M 42 42 L 50 32 L 58 42 L 54 44 L 46 44 Z" fill="none" stroke="#ffffff" strokeWidth="2.4" strokeLinejoin="round" />
              {/* Banda base del gorro */}
              <rect x="22" y="56" width="52" height="11" rx="3" fill="#ffffff" />
            </svg>

            <h2 className="pin-marca-titulo">RIKO'S</h2>
            <div className="pin-marca-subtitulo">
              <span className="pin-linea-adorno" />
              <span className="pin-texto-adorno">RESTAURANTE</span>
              <span className="pin-linea-adorno" />
            </div>
          </div>

          <h1 className="pin-bloqueo-titulo">Tableta bloqueada</h1>
          <p className="pin-bloqueo-subtitulo">Ingresa tu PIN para retomar el sistema de ventas.</p>

          <div className="pin-marca-local">
            <IconoUbicacion width={16} height={16} aria-hidden="true" />
            <span>{nombreLocal || 'San Isidro'}</span>
          </div>
        </div>

        {/* Plato de comida apetitosa en esquina inferior izquierda */}
        <div className="pin-plato-envoltorio" aria-hidden="true">
          <img
            src="/assets/plato-carne.png"
            alt="Plato gourmet RIKO'S"
            className="pin-plato-imagen"
          />
        </div>
      </aside>

      {/* Panel Derecho: Teclado Numérico y Acceso (Fondo Claro) */}
      <main className="pin-acceso">
        {/* Onda decorativa superior derecha con espiga de trigo botánica */}
        <div className="pin-fondo-onda-der" aria-hidden="true">
          <svg viewBox="0 0 340 260" fill="none" preserveAspectRatio="none">
            <path
              d="M 60 0 C 140 30, 180 80, 220 125 C 260 170, 290 205, 340 260 L 340 0 Z"
              fill="#7b3512"
            />
          </svg>
          {/* Espiga de trigo botánica en trazo claro */}
          <svg className="pin-espiga-trigo" viewBox="0 0 140 180" fill="none" stroke="#f6eee5" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
            <path d="M 120 20 C 85 60, 45 110, 25 160" />
            {/* Hojas / granos de trigo emparejados */}
            <path d="M 105 32 C 102 24, 94 25, 96 36 C 98 42, 106 40, 105 32 Z" />
            <path d="M 115 45 C 122 42, 122 34, 114 34 C 108 34, 108 42, 115 45 Z" />
            <path d="M 88 55 C 84 46, 76 48, 78 58 C 80 64, 88 62, 88 55 Z" />
            <path d="M 98 68 C 106 65, 106 56, 98 56 C 91 56, 91 65, 98 68 Z" />
            <path d="M 72 80 C 68 70, 59 73, 62 83 C 64 90, 72 88, 72 80 Z" />
            <path d="M 82 92 C 90 90, 90 80, 82 80 C 75 80, 75 90, 82 92 Z" />
            <path d="M 54 106 C 50 96, 41 99, 44 109 C 46 116, 54 114, 54 106 Z" />
            <path d="M 64 118 C 72 116, 72 106, 64 106 C 57 106, 57 116, 64 118 Z" />
          </svg>
        </div>

        {/* Doodles inferiores derechos del panel de acceso */}
        <div className="pin-doodles-der" aria-hidden="true">
          {/* Gorro de chef */}
          <svg className="pin-doodle-chef-der" viewBox="0 0 110 100" fill="none" stroke="#ebdcd0" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M 24 70 C 12 62, 10 42, 24 30 C 20 16, 38 7, 50 12 C 60 2, 78 2, 85 14 C 98 14, 104 28, 97 42 C 104 56, 94 70, 84 70" />
            <path d="M 23 70 L 85 70" />
            <path d="M 26 78 L 82 78" />
          </svg>

          {/* Rayos / chispas decorativas */}
          <svg className="pin-chispas-der" viewBox="0 0 60 60" fill="none" stroke="#7b3512" strokeWidth="4.5" strokeLinecap="round">
            <line x1="8" y1="22" x2="26" y2="30" />
            <line x1="38" y1="14" x2="48" y2="30" />
            <line x1="16" y1="46" x2="36" y2="44" />
          </svg>
        </div>

        {/* Tarjeta Flotante con Teclado Numérico */}
        <div className="pin-tarjeta">
          <header className="pin-tarjeta-cabecera">
            <h2 className="pin-tarjeta-titulo">
              <svg className="pin-icono-candado" viewBox="0 0 24 24" fill="#61260b" aria-hidden="true">
                <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z" />
              </svg>
              <span>Ingresa tu PIN</span>
            </h2>
            <p className="pin-ayuda">Usa tu código de acceso rápido</p>
          </header>

          <EntradaNumerica
            valor={pin}
            onCambiar={setPin}
            largoFijo={LARGO_PIN}
            oculto
            alCompletar={intentar}
            deshabilitado={verificando || bloqueo.activa}
          />

          {verificando && <p className="pin-mensaje">Comprobando…</p>}
          {error && <p className="pin-mensaje error">{error}</p>}
          {bloqueo.activa && (
            <p className="pin-mensaje error">
              Demasiados intentos fallidos. Intenta de nuevo en {minutosSegundos(bloqueo.segundosRestantes)}.
            </p>
          )}
        </div>
      </main>
    </div>
  )
}

