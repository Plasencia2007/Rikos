import { useState, type FormEvent } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { useCuentaRegresiva } from '../../comun/hooks/useCuentaRegresiva'
import { minutosSegundos } from '../../comun/formato/tiempo'
import { ErrorApi } from '../../api/cliente'
import { Boton } from '../../comun/componentes/Boton'
import { Campo } from '../../comun/componentes/Campo'
import { Aviso } from '../../comun/componentes/Aviso'

const MENSAJES: Record<string, string> = {
  CREDENCIALES_INVALIDAS: 'Usuario o contraseña incorrectos.',
  USUARIO_INACTIVO: 'Este usuario está desactivado.',
}

export function PantallaLogin() {
  const { entrarConContrasena, cargando } = useSesion()
  const [nombreUsuario, setNombreUsuario] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [mostrarContrasena, setMostrarContrasena] = useState(false)
  const [error, setError] = useState('')
  const bloqueo = useCuentaRegresiva()

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError('')
    try {
      await entrarConContrasena(nombreUsuario, contrasena)
    } catch (excepcion) {
      if (excepcion instanceof ErrorApi && excepcion.cuerpo?.error === 'BLOQUEADO') {
        bloqueo.iniciar(excepcion.cuerpo.segundosRestantes ?? 0)
      } else {
        const codigo = excepcion instanceof ErrorApi ? excepcion.cuerpo?.error : undefined
        setError((codigo && MENSAJES[codigo]) ?? 'No se pudo iniciar sesión. Intenta de nuevo.')
      }
    }
  }

  return (
    <div className="pantalla-login-contenedor">
      {/* Onda orgánica marrón superior derecha */}
      <div className="login-fondo-onda onda-superior-derecha" aria-hidden="true">
        <svg viewBox="0 0 300 240" fill="none" preserveAspectRatio="none">
          <path
            d="M 60 0 C 140 35, 175 75, 210 110 C 255 155, 275 190, 300 240 L 300 0 Z"
            fill="#1d4ed8"
          />
        </svg>
      </div>

      {/* Onda orgánica marrón inferior izquierda (abraza el plato de comida) */}
      <div className="login-fondo-onda onda-inferior-izquierda" aria-hidden="true">
        <svg viewBox="0 0 360 380" fill="none" preserveAspectRatio="none">
          <path
            d="M 0 90 C 85 105, 120 185, 150 250 C 185 320, 220 355, 270 380 L 0 380 Z"
            fill="#1d4ed8"
          />
        </svg>
      </div>

      {/* Ilustraciones lineales tenues de fondo (Doodles tono beige cálido) */}
      <div className="login-doodles" aria-hidden="true">
        {/* Gorro de chef arriba a la izquierda */}
        <svg className="doodle doodle-chef-izq" viewBox="0 0 120 110" fill="none" stroke="#dbe3f0" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round">
          <path d="M 28 78 C 14 70, 10 46, 26 34 C 20 18, 40 8, 56 14 C 66 2, 86 2, 94 16 C 108 16, 116 32, 108 48 C 116 64, 104 80, 92 80" />
          <path d="M 26 78 L 94 78" />
          <path d="M 30 88 L 90 88" />
        </svg>

        {/* Gorro de chef arriba a la derecha */}
        <svg className="doodle doodle-chef-der" viewBox="0 0 110 100" fill="none" stroke="#dbe3f0" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M 24 70 C 12 62, 10 42, 24 30 C 20 16, 38 7, 50 12 C 60 2, 78 2, 85 14 C 98 14, 104 28, 97 42 C 104 56, 94 70, 84 70" />
          <path d="M 23 70 L 85 70" />
          <path d="M 26 78 L 82 78" />
        </svg>

        {/* Cubiertos (Tenedor y Cuchillo) en el lateral derecho */}
        <svg className="doodle doodle-cubiertos-der" viewBox="0 0 100 130" fill="none" stroke="#dbe3f0" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round">
          {/* Tenedor */}
          <path d="M 26 15 L 26 44 C 26 50, 36 50, 36 44 L 36 15" />
          <path d="M 31 15 L 31 45" />
          <path d="M 31 50 L 31 108" />
          {/* Cuchillo */}
          <path d="M 72 15 C 72 15, 86 28, 86 52 C 86 60, 78 62, 72 62 L 72 108" />
        </svg>

        {/* Campana de plato / Cloche abajo a la derecha */}
        <svg className="doodle doodle-campana-der" viewBox="0 0 150 110" fill="none" stroke="#dbe3f0" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="75" cy="22" r="7" />
          <path d="M 75 29 L 75 35" />
          <path d="M 20 78 C 24 45, 46 35, 75 35 C 104 35, 126 45, 130 78 Z" />
          <path d="M 10 88 C 15 84, 45 84, 75 84 C 105 84, 135 84, 140 88" strokeWidth="4.5" />
        </svg>

        {/* Chispas / rayos decorativos inferiores derechos */}
        <svg className="doodle-chispas chispas-derecha" viewBox="0 0 60 60" fill="none" stroke="#1d4ed8" strokeWidth="4.5" strokeLinecap="round">
          <line x1="8" y1="22" x2="26" y2="30" />
          <line x1="38" y1="14" x2="48" y2="30" />
          <line x1="16" y1="46" x2="36" y2="44" />
        </svg>

        {/* Chispas / rayos decorativos junto al plato de comida */}
        <svg className="doodle-chispas chispas-plato" viewBox="0 0 60 60" fill="none" stroke="#1d4ed8" strokeWidth="4.5" strokeLinecap="round">
          <line x1="16" y1="12" x2="22" y2="30" />
          <line x1="38" y1="14" x2="48" y2="32" />
          <line x1="8" y1="40" x2="26" y2="44" />
        </svg>
      </div>

      {/* Imagen del plato gastronómico apetitoso */}
      <div className="login-plato-envoltorio" aria-hidden="true">
        <img
          src="/assets/plato-carne.png"
          alt="Plato gourmet RIKO'S"
          className="login-plato-imagen"
        />
      </div>

      {/* Tarjeta Central del Login */}
      <main className="pantalla-login-tarjeta">
        <header className="login-cabecera">
          {/* Logo RIKO'S con gorro de chef */}
          <div className="login-logo">
            <svg className="login-logo-icono" viewBox="0 0 100 85" fill="none">
              {/* Gorro de chef con carita determinada/pícara */}
              <path
                d="M 22 56 C 14 50, 11 36, 21 26 C 16 14, 30 5, 42 9 C 50 1, 64 2, 69 11 C 80 8, 88 18, 82 28 C 89 38, 84 52, 73 56 Z"
                fill="#ffffff"
                stroke="#1d4ed8"
                strokeWidth="3.6"
                strokeLinejoin="round"
              />
              {/* Pliegues verticales suaves */}
              <path d="M 36 20 C 37 30, 36 40, 35 46" stroke="#1d4ed8" strokeWidth="2.4" strokeLinecap="round" />
              <path d="M 60 20 C 59 30, 60 40, 61 46" stroke="#1d4ed8" strokeWidth="2.4" strokeLinecap="round" />
              {/* Cejas pícaras inclinadas hacia abajo al centro */}
              <path d="M 37 38 L 44 42" stroke="#1d4ed8" strokeWidth="3" strokeLinecap="round" />
              <path d="M 59 38 L 52 42" stroke="#1d4ed8" strokeWidth="3" strokeLinecap="round" />
              {/* Ojos determinados */}
              <circle cx="43" cy="46" r="2.5" fill="#1d4ed8" />
              <circle cx="53" cy="46" r="2.5" fill="#1d4ed8" />
              {/* Banda base del gorro */}
              <rect x="22" y="56" width="52" height="12" rx="3.5" fill="#1d4ed8" />
              <line x1="25" y1="62" x2="71" y2="62" stroke="#ffffff" strokeWidth="1.8" strokeLinecap="round" />
            </svg>

            <h1 className="login-marca-nombre">RIKO'S</h1>
            <div className="login-marca-subtitulo">
              <span className="linea-adorno" />
              <span className="texto-adorno">RESTAURANTE</span>
              <span className="linea-adorno" />
            </div>
          </div>

          <h2 className="login-titulo">Panel de Administración</h2>
          <p className="login-subtitulo">Ingresa con tu usuario y contraseña para continuar.</p>
        </header>

        <form onSubmit={enviar} className="login-formulario" noValidate>
          <Campo
            etiqueta="Usuario"
            icono={
              <svg width="17" height="17" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
              </svg>
            }
            placeholder="Ingresa tu usuario"
            value={nombreUsuario}
            onChange={(e) => setNombreUsuario(e.target.value)}
            autoComplete="username"
            required
          />

          <Campo
            etiqueta="Contraseña"
            type={mostrarContrasena ? 'text' : 'password'}
            icono={
              <svg width="17" height="17" viewBox="0 0 24 24" fill="currentColor">
                <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z" />
              </svg>
            }
            sufijo={
              <button
                type="button"
                className="boton-alternar-contrasena"
                onClick={() => setMostrarContrasena((prev) => !prev)}
                aria-label={mostrarContrasena ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                title={mostrarContrasena ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {mostrarContrasena ? (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                ) : (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                    <line x1="1" y1="1" x2="23" y2="23" />
                  </svg>
                )}
              </button>
            }
            placeholder="Ingresa tu contraseña"
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
            autoComplete="current-password"
            required
          />

          {error && <Aviso tono="error">{error}</Aviso>}
          {bloqueo.activa && (
            <Aviso tono="error">
              Demasiados intentos fallidos. Intenta de nuevo en {minutosSegundos(bloqueo.segundosRestantes)}.
            </Aviso>
          )}

          <Boton
            type="submit"
            className="login-boton-entrar"
            disabled={cargando || bloqueo.activa}
            cargando={cargando}
          >
            <span>Ingresar</span>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </Boton>
        </form>

        <footer className="login-pie">
          <div className="login-pie-divisor">
            <span className="divisor-linea" />
            <span className="divisor-icono" aria-hidden="true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="#2563eb">
                <path d="M11 9H9V2H7v7H5V2H3v7c0 2.12 1.66 3.84 3.75 3.97V22h2.5v-9.03C11.34 12.84 13 11.12 13 9V2h-2v7z" />
                <path d="M19 2c-2.76 0-5 2.24-5 5v5h2v10h3V2h0z" />
              </svg>
            </span>
            <span className="divisor-linea" />
          </div>
          <p className="login-pie-texto">
            RIKO'S • Sistema de Gestión de Restaurantes
          </p>
        </footer>
      </main>
    </div>
  )
}

