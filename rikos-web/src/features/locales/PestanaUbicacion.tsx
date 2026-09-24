import { useEffect, useRef } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import iconoMarcador from 'leaflet/dist/images/marker-icon.png?url'
import iconoMarcador2x from 'leaflet/dist/images/marker-icon-2x.png?url'
import sombraMarcador from 'leaflet/dist/images/marker-shadow.png?url'
import { Aviso } from '../../comun/componentes'

// Los bundlers rompen las rutas relativas que trae Leaflet por defecto para
// su icono -- hay que decirle explicitamente donde estan los PNG resueltos
// por Vite. Se hace una sola vez, al cargar el modulo.
L.Icon.Default.mergeOptions({
  iconUrl: iconoMarcador,
  iconRetinaUrl: iconoMarcador2x,
  shadowUrl: sombraMarcador,
})

// Centro por defecto: Lima, cuando el local todavia no tiene coordenada.
const CENTRO_POR_DEFECTO: [number, number] = [-12.0464, -77.0428]

interface Props {
  latitud: number | null
  longitud: number | null
  onCambiar: (latitud: number, longitud: number) => void
}

export function PestanaUbicacion({ latitud, longitud, onCambiar }: Props) {
  const contenedorRef = useRef<HTMLDivElement>(null)
  const mapaRef = useRef<L.Map | null>(null)
  const marcadorRef = useRef<L.Marker | null>(null)
  // Evita que el efecto de "latitud/longitud cambiaron" reposicione el mapa
  // mientras el propio mapa es el que esta generando ese cambio (arrastrar
  // el marcador dispararia un bucle: mueve -> onCambiar -> prop nueva -> mueve).
  const actualizandoDesdeMapa = useRef(false);

  useEffect(() => {
    if (!contenedorRef.current || mapaRef.current) return

    const centroInicial: [number, number] =
      latitud != null && longitud != null ? [latitud, longitud] : CENTRO_POR_DEFECTO
    const mapa = L.map(contenedorRef.current).setView(centroInicial, latitud != null ? 15 : 11)
    mapaRef.current = mapa

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(mapa)

    function colocarMarcador(lat: number, lng: number) {
      if (marcadorRef.current) {
        marcadorRef.current.setLatLng([lat, lng])
      } else {
        marcadorRef.current = L.marker([lat, lng], { draggable: true }).addTo(mapa)
        marcadorRef.current.on('dragend', () => {
          const pos = marcadorRef.current!.getLatLng()
          actualizandoDesdeMapa.current = true
          onCambiar(Number(pos.lat.toFixed(7)), Number(pos.lng.toFixed(7)))
        })
      }
    }

    if (latitud != null && longitud != null) {
      colocarMarcador(latitud, longitud)
    }

    mapa.on('click', (evento: L.LeafletMouseEvent) => {
      colocarMarcador(evento.latlng.lat, evento.latlng.lng)
      actualizandoDesdeMapa.current = true
      onCambiar(Number(evento.latlng.lat.toFixed(7)), Number(evento.latlng.lng.toFixed(7)))
    })

    return () => {
      mapa.remove()
      mapaRef.current = null
      marcadorRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (actualizandoDesdeMapa.current) {
      actualizandoDesdeMapa.current = false
      return
    }
    if (!mapaRef.current || latitud == null || longitud == null) return
    if (marcadorRef.current) {
      marcadorRef.current.setLatLng([latitud, longitud])
    } else {
      marcadorRef.current = L.marker([latitud, longitud], { draggable: true }).addTo(mapaRef.current)
      marcadorRef.current.on('dragend', () => {
        const pos = marcadorRef.current!.getLatLng()
        actualizandoDesdeMapa.current = true
        onCambiar(Number(pos.lat.toFixed(7)), Number(pos.lng.toFixed(7)))
      })
    }
    mapaRef.current.setView([latitud, longitud], mapaRef.current.getZoom() < 13 ? 15 : mapaRef.current.getZoom())
  }, [latitud, longitud])

  function usarUbicacionActual() {
    if (!navigator.geolocation) return
    navigator.geolocation.getCurrentPosition((posicion) => {
      onCambiar(Number(posicion.coords.latitude.toFixed(7)), Number(posicion.coords.longitude.toFixed(7)))
    })
  }

  return (
    <div className="pestana-ubicacion">
      <Aviso tono="informacion">
        Hoy es solo informativo: el delivery es gratuito y sin zonas, así que esta coordenada todavía no calcula
        nada. Sirve para tener el punto guardado si en el futuro hace falta.
      </Aviso>

      <div className="ubicacion-columnas">
        <div className="ubicacion-campos">
          <div className="campo">
            <span className="campo-etiqueta">Latitud</span>
            <div className="campo-valor-solo-lectura">{latitud != null ? latitud.toFixed(7) : '—'}</div>
          </div>
          <div className="campo">
            <span className="campo-etiqueta">Longitud</span>
            <div className="campo-valor-solo-lectura">{longitud != null ? longitud.toFixed(7) : '—'}</div>
          </div>
          <button type="button" className="boton boton-secundario boton-normal" onClick={usarUbicacionActual}>
            Usar mi ubicación actual
          </button>
          <p className="ubicacion-ayuda">Haz clic en el mapa o arrastra el marcador para ajustar el punto.</p>
        </div>

        <div ref={contenedorRef} className="ubicacion-mapa" role="application" aria-label="Mapa para ubicar el local" />
      </div>
    </div>
  )
}
