/**
 * El dibujo de la sala alrededor de las mesas del plano: paredes con ventanas, la puerta de arriba con sus jardineras,
 * plantas en las esquinas y el acceso abajo. Es solo decoración (no viene del salón ni se puede tocar).
 */

type Tramo = [inicio: number, fin: number, tipo: 'muro' | 'ventana' | 'puerta']

/** Cada pared, en % de su largo. Lo que no está en la lista es un hueco. */
const PAREDES: Record<'arriba' | 'abajo' | 'izquierda' | 'derecha', Tramo[]> = {
  arriba: [
    [0, 24, 'muro'],
    [24, 33, 'ventana'],
    [33, 44, 'muro'],
    [44, 56, 'puerta'],
    [56, 67, 'muro'],
    [67, 76, 'ventana'],
    [76, 100, 'muro'],
  ],
  abajo: [
    [0, 20, 'muro'],
    [20, 40, 'ventana'],
    [40, 46, 'muro'],
    [54, 60, 'muro'],
    [60, 80, 'ventana'],
    [80, 100, 'muro'],
  ],
  izquierda: [
    [0, 58, 'muro'],
    [58, 76, 'ventana'],
    [76, 100, 'muro'],
  ],
  derecha: [
    [0, 26, 'muro'],
    [26, 74, 'ventana'],
    [74, 100, 'muro'],
  ],
}

function Planta() {
  const hojas = [0, 45, 90, 135, 180, 225, 270, 315]
  return (
    <svg viewBox="0 0 48 48" width="100%" height="100%">
      <g stroke="#2f5d33" strokeWidth="0.7">
        {hojas.map((a) => (
          <path key={a} d="M24 24 C 29 16, 27 7, 24 3 C 21 7, 19 16, 24 24 Z" fill="#4e8b4f" transform={`rotate(${a} 24 24)`} />
        ))}
        {hojas.map((a) => (
          <path
            key={`c${a}`}
            d="M24 24 C 27 18, 26 11, 24 8 C 22 11, 21 18, 24 24 Z"
            fill="#78b06d"
            transform={`rotate(${a + 22} 24 24)`}
          />
        ))}
      </g>
      <circle cx="24" cy="24" r="3.2" fill="#3c6e3a" />
    </svg>
  )
}

function Jardinera() {
  return (
    <svg viewBox="0 0 60 20" width="100%" height="100%">
      <rect x="1" y="4" width="58" height="14" rx="2" fill="#b8a487" stroke="#8c7a60" strokeWidth="1" />
      {[9, 21, 33, 45].map((x, i) => (
        <g key={x}>
          <circle cx={x + 2} cy="10" r="6" fill={i % 2 ? '#5f9a58' : '#4c8549'} stroke="#2f5d33" strokeWidth="0.6" />
          <circle cx={x + 5} cy="8" r="3.4" fill="#86b979" />
        </g>
      ))}
    </svg>
  )
}

export function DecoradoDeSala() {
  return (
    <div className="mapa-sala" aria-hidden="true">
      {(Object.keys(PAREDES) as (keyof typeof PAREDES)[]).map((lado) => (
        <div key={lado} className={`mapa-pared ${lado}`}>
          {PAREDES[lado].map(([inicio, fin, tipo]) => (
            <span key={inicio} className={`mapa-tramo ${tipo}`} style={{ ['--inicio' as string]: `${inicio}%`, ['--largo' as string]: `${fin - inicio}%` }} />
          ))}
        </div>
      ))}
      <span className="mapa-jardinera izquierda">
        <Jardinera />
      </span>
      <span className="mapa-jardinera derecha">
        <Jardinera />
      </span>
      {['arriba-izquierda', 'arriba-derecha', 'abajo-izquierda', 'abajo-derecha'].map((esquina) => (
        <span key={esquina} className={`mapa-planta ${esquina}`}>
          <Planta />
        </span>
      ))}
      <span className="mapa-acceso" />
    </div>
  )
}
