import { useRef, useState } from 'react'
import { ImagePlus, Trash2 } from 'lucide-react'
import { Aviso, Boton } from '../../comun/componentes'
import { ponerImagenDeProducto } from '../../api/cliente'

const LADO_MAXIMO = 480

/** Reduce la foto en el navegador (lado mayor de 480 px, JPEG): el catalogo es corto y la foto viaja dentro del producto. */
async function reducir(archivo: File): Promise<string> {
  const mapa = await createImageBitmap(archivo)
  const escala = Math.min(1, LADO_MAXIMO / Math.max(mapa.width, mapa.height))
  const lienzo = document.createElement('canvas')
  lienzo.width = Math.round(mapa.width * escala)
  lienzo.height = Math.round(mapa.height * escala)
  const contexto = lienzo.getContext('2d')
  if (!contexto) throw new Error('sin lienzo')
  contexto.drawImage(mapa, 0, 0, lienzo.width, lienzo.height)
  mapa.close()
  return lienzo.toDataURL('image/jpeg', 0.82)
}

/**
 * La foto del producto (la ve el mozo en la pantalla del pedido). Se sube y se guarda al instante: hace falta que el
 * producto ya exista.
 */
export function FotoDelProducto({
  token,
  productoId,
  imagenInicial,
}: {
  token: string
  productoId: string
  imagenInicial: string | null
}) {
  const entrada = useRef<HTMLInputElement>(null)
  const [imagen, setImagen] = useState<string | null>(imagenInicial)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(nueva: string | null) {
    setTrabajando(true)
    setError('')
    try {
      const producto = await ponerImagenDeProducto(token, productoId, nueva)
      setImagen(producto.imagen)
    } catch {
      setError('No se pudo guardar la foto. Prueba con otra imagen más chica.')
    } finally {
      setTrabajando(false)
    }
  }

  async function elegir(archivo: File | undefined) {
    if (!archivo) return
    if (!archivo.type.startsWith('image/')) {
      setError('Elige un archivo de imagen (JPG, PNG o WEBP).')
      return
    }
    try {
      await guardar(await reducir(archivo))
    } catch {
      setError('No se pudo leer esa imagen.')
    }
  }

  return (
    <div className="prod-tarjeta-lateral prod-foto">
      <h3>Foto</h3>
      <div className="prod-foto-marco">
        {imagen ? <img src={imagen} alt="Foto del producto" /> : <span>Sin foto</span>}
      </div>
      {error && <Aviso tono="error">{error}</Aviso>}
      <div className="prod-foto-acciones">
        <input
          ref={entrada}
          type="file"
          accept="image/*"
          hidden
          onChange={(e) => {
            void elegir(e.target.files?.[0])
            e.target.value = ''
          }}
        />
        <Boton variante="secundario" cargando={trabajando} onClick={() => entrada.current?.click()}>
          <ImagePlus size={16} aria-hidden="true" /> {imagen ? 'Cambiar foto' : 'Subir foto'}
        </Boton>
        {imagen && (
          <Boton variante="secundario" disabled={trabajando} onClick={() => void guardar(null)}>
            <Trash2 size={16} aria-hidden="true" /> Quitar
          </Boton>
        )}
      </div>
    </div>
  )
}
