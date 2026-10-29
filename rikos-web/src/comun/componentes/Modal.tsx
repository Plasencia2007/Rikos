import { useEffect, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

interface Props {
  abierto: boolean
  titulo: string
  onCerrar: () => void
  children: ReactNode
  pie?: ReactNode
  /** "amplio" para diálogos con mucho contenido (hasta 880 px de ancho). */
  ancho?: 'normal' | 'amplio'
  /** Línea chica bajo el título y un ícono en un cuadro a su izquierda (opcionales). */
  subtitulo?: string
  icono?: ReactNode
}

/**
 * Base de Modal, ModalDeConfirmacion y ModalDeMotivo. Patron unico para
 * "Nuevo": este mismo componente es el que va a abrir la creacion en las
 * treinta pantallas de administracion que siguen.
 */
export function Modal({ abierto, titulo, onCerrar, children, pie, ancho = 'normal', subtitulo, icono }: Props) {
  useEffect(() => {
    if (!abierto) return
    function alEscape(evento: KeyboardEvent) {
      if (evento.key === 'Escape') onCerrar()
    }
    window.addEventListener('keydown', alEscape)
    return () => window.removeEventListener('keydown', alEscape)
  }, [abierto, onCerrar])

  if (!abierto) return null

  return createPortal(
    <div className="modal-fondo" onMouseDown={onCerrar}>
      <div
        className={`modal-caja ${ancho === 'amplio' ? 'modal-amplio' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        onMouseDown={(evento) => evento.stopPropagation()}
      >
        <header className="modal-encabezado">
          {icono || subtitulo ? (
            <div className="modal-titulo-con-icono">
              {icono && <span className="modal-icono">{icono}</span>}
              <div>
                <h2>{titulo}</h2>
                {subtitulo && <p className="modal-subtitulo">{subtitulo}</p>}
              </div>
            </div>
          ) : (
            <h2>{titulo}</h2>
          )}
          <button type="button" className="modal-cerrar" onClick={onCerrar} aria-label="Cerrar">
            ×
          </button>
        </header>
        <div className="modal-cuerpo">{children}</div>
        {pie && <footer className="modal-pie">{pie}</footer>}
      </div>
    </div>,
    document.body,
  )
}
