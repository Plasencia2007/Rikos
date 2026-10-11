import { useState } from 'react'
import { Clock, Image as ImagenIcono, Info, MapPin, Phone } from 'lucide-react'
import { Campo } from '../../comun/componentes'
import type { FormularioLocal } from './formularioLocal'
import { PestanaUbicacion } from './PestanaUbicacion'

interface Props {
  form: FormularioLocal
  esNuevo: boolean
  errores: { codigo?: string; nombre?: string; direccion?: string }
  onCambiar: (cambios: Partial<FormularioLocal>) => void
  onCambiarNombre: (nombre: string) => void
  onCambiarCodigo: (codigo: string) => void
}

export function PasoDatos({ form, esNuevo, errores, onCambiar, onCambiarNombre, onCambiarCodigo }: Props) {
  const [verMapa, setVerMapa] = useState(form.latitud != null)

  return (
    <>
      <h2 className="as-h2">Identificación</h2>
      <div className="as-grupo">
        <Campo
          etiqueta={<>Nombre del local <span className="as-req">*</span></>}
          placeholder="Ej. San Isidro"
          value={form.nombre}
          onChange={(e) => onCambiarNombre(e.target.value)}
          error={errores.nombre}
        />
        <div className="as-fila-codigo">
          <Campo
            etiqueta={<>Código para el POS <span className="as-req">*</span></>}
            placeholder="Ej. san-isidro"
            ayuda={
              esNuevo
                ? 'Forma parte de la dirección del POS; podrás revisarlo antes de crear el local.'
                : 'El código no se puede cambiar una vez creado el local.'
            }
            value={form.codigo}
            onChange={(e) => onCambiarCodigo(e.target.value)}
            error={errores.codigo}
            disabled={!esNuevo}
          />
          <div className="as-vista-direccion" aria-live="polite">
            <span>Vista previa de la dirección</span>
            <strong>
              <b>{form.codigo || 'san-isidro'}</b>.rikos.pe
            </strong>
          </div>
        </div>
      </div>

      <hr className="as-separador" />

      <h2 className="as-h2">Ubicación</h2>
      <div className="as-grupo">
        <Campo
          etiqueta={<>Dirección <span className="as-req">*</span></>}
          placeholder="Ej. Av. Arequipa 123, San Isidro, Lima"
          value={form.direccion}
          onChange={(e) => onCambiar({ direccion: e.target.value })}
          error={errores.direccion}
        />
        <div className="as-fila-mapa">
          <button type="button" className="as-boton-tenue" onClick={() => setVerMapa((v) => !v)} aria-expanded={verMapa}>
            <MapPin size={17} aria-hidden="true" /> {verMapa ? 'Ocultar mapa' : 'Ubicar en el mapa (opcional)'}
          </button>
          <span className="as-ayuda">
            {form.latitud != null
              ? 'Punto marcado. Puedes arrastrarlo para ajustarlo.'
              : 'Luego podrás ajustar el punto exacto en el mapa.'}
          </span>
        </div>
        {verMapa && (
          <PestanaUbicacion
            latitud={form.latitud}
            longitud={form.longitud}
            onCambiar={(latitud, longitud) => onCambiar({ latitud, longitud })}
          />
        )}
      </div>
    </>
  )
}

export function LateralDatos() {
  const items = [
    { Icono: Phone, titulo: 'Contacto y redes', texto: 'Teléfonos, email y redes sociales.' },
    { Icono: Clock, titulo: 'Canales y horarios', texto: 'Delivery, recojo en tienda y horarios de atención.' },
    { Icono: ImagenIcono, titulo: 'Logo y aspecto', texto: 'Foto, colores y apariencia en la app y el sitio web.' },
  ]
  return (
    <>
      <h2 className="as-h2 as-h2-neutro">Qué sigue</h2>
      <p className="as-texto-suave">Después de crear el local podrás completar la siguiente información:</p>
      {items.map(({ Icono, titulo, texto }) => (
        <div key={titulo} className="as-item-lateral">
          <span className="as-item-icono">
            <Icono size={20} aria-hidden="true" />
          </span>
          <div>
            <strong>{titulo}</strong>
            <span>{texto}</span>
          </div>
        </div>
      ))}
      <div className="as-nota" role="note">
        <Info size={18} aria-hidden="true" />
        <span>Podrás editar todos estos datos en cualquier momento.</span>
      </div>
    </>
  )
}
