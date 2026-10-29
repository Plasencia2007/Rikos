/** El logo de RIKO'S que viene con el sistema; se usa mientras el restaurante no suba el suyo. */
export const LOGO_PREDETERMINADO = '/marca/logo-predeterminado.svg'

/**
 * El espacio del logo en las barras laterales (POS y administración). Un logo subido desde Configuración → Mi
 * restaurante se ajusta a esta caja sin deformarse; por eso se recomienda horizontal, de proporción cercana a 4:1,
 * con fondo transparente, en SVG o PNG de al menos 680 × 160 px.
 */
export const MEDIDAS_DEL_LOGO = { ancho: 170, alto: 40 }

/** El logo del restaurante en la cabecera de una barra lateral. */
export function LogoDelRestaurante({ src = LOGO_PREDETERMINADO, className = '' }: { src?: string; className?: string }) {
  return (
    <img
      className={`logo-restaurante ${className}`}
      src={src}
      alt="Riko's"
      style={{ maxWidth: MEDIDAS_DEL_LOGO.ancho, maxHeight: MEDIDAS_DEL_LOGO.alto }}
    />
  )
}
