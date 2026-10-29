/** El logo de RIKO'S que viene con el sistema; se usa mientras el restaurante no suba el suyo. */
export const LOGO_PREDETERMINADO = '/marca/logo-predeterminado.svg'

/**
 * El espacio del logo en las barras laterales (POS y administración). Un logo subido desde Configuración → Mi
 * restaurante se ajusta a esta caja sin deformarse; por eso se recomienda horizontal, de proporción cercana a 4:1,
 * con fondo transparente, en SVG o PNG de al menos 680 × 160 px.
 */
export const MEDIDAS_DEL_LOGO = { ancho: 170, alto: 40 }

/** El logo del restaurante; por defecto, en la caja de las barras laterales (otras pantallas pasan sus medidas). */
export function LogoDelRestaurante({
  src = LOGO_PREDETERMINADO,
  className = '',
  medidas = MEDIDAS_DEL_LOGO,
}: {
  src?: string
  className?: string
  medidas?: { ancho: number; alto: number }
}) {
  return (
    <img
      className={`logo-restaurante ${className}`}
      src={src}
      alt="Riko's"
      style={{ maxWidth: medidas.ancho, maxHeight: medidas.alto }}
    />
  )
}
