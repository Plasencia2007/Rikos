import { useState } from 'react'
import { BookOpen, Image as ImagenIcono, Info, Link2, ShoppingBag, Truck } from 'lucide-react'
import { Campo } from '../../comun/componentes'
import { COLOR_HEX, type FormularioLocal } from './formularioLocal'

const PALETA = ['#2B7A68', '#3D9AA1', '#C4633F', '#DFA83B']

interface Props {
  form: FormularioLocal
  onCambiar: (cambios: Partial<FormularioLocal>) => void
}

/**
 * Las imágenes se guardan como enlace: todavía no hay un servicio de
 * almacenamiento donde subirlas, así que no se ofrece "Subir imagen".
 */
export function PasoAspecto({ form, onCambiar }: Props) {
  const [hex, setHex] = useState(form.colorPrincipal.replace('#', ''))
  const [hexInvalido, setHexInvalido] = useState(false)

  function elegirColor(color: string) {
    setHex(color.replace('#', ''))
    setHexInvalido(false)
    onCambiar({ colorPrincipal: color })
  }

  function escribirHex(valor: string) {
    const limpio = valor.replace('#', '').slice(0, 6)
    setHex(limpio)
    const completo = `#${limpio}`
    if (COLOR_HEX.test(completo)) {
      setHexInvalido(false)
      onCambiar({ colorPrincipal: completo.toUpperCase() })
    } else {
      setHexInvalido(limpio.length > 0)
    }
  }

  return (
    <>
      <h2 className="as-h2">Logo del local</h2>
      <div className="as-imagen">
        <div className="as-imagen-zona">
          <span className="as-logo-caja">
            {form.logoUrl ? (
              <img src={form.logoUrl} alt="Logo del local" onError={(e) => (e.currentTarget.style.display = 'none')} />
            ) : (
              <span aria-hidden="true">{form.nombre.charAt(0).toUpperCase() || 'L'}</span>
            )}
          </span>
          <div className="as-imagen-campo">
            <Campo
              etiqueta={
                <>
                  <Link2 size={16} aria-hidden="true" /> URL del logo
                </>
              }
              placeholder="https://…"
              ayuda="PNG, JPG o SVG · recomendado 512 × 512 px."
              value={form.logoUrl}
              onChange={(e) => onCambiar({ logoUrl: e.target.value })}
            />
          </div>
        </div>
      </div>

      <hr className="as-separador" />

      <div className="as-titulo-bloque">
        <h2 className="as-h2">Color principal</h2>
        <span className="as-ayuda">Este color se aplicará en los botones y enlaces de tu menú digital.</span>
      </div>
      <div className="as-colores">
        {PALETA.map((c) => (
          <button
            key={c}
            type="button"
            className={`as-color ${form.colorPrincipal.toUpperCase() === c ? 'elegido' : ''}`}
            style={{ background: c, ['--as-color' as string]: c }}
            aria-label={`Color ${c}`}
            aria-pressed={form.colorPrincipal.toUpperCase() === c}
            onClick={() => elegirColor(c)}
          />
        ))}
        <label className={`as-hex ${hexInvalido ? 'invalido' : ''}`}>
          <span aria-hidden="true">#</span>
          <input
            type="text"
            aria-label="Color en hexadecimal"
            value={hex}
            maxLength={7}
            onChange={(e) => escribirHex(e.target.value)}
          />
        </label>
        {hexInvalido && <span className="campo-error">Usa 6 caracteres hexadecimales, por ejemplo 2B7A68.</span>}
      </div>

      <hr className="as-separador" />

      <div className="as-titulo-bloque">
        <h2 className="as-h2">Imagen de portada</h2>
        <span className="as-ayuda">Se verá en la cabecera del menú digital. Formato recomendado: 1440 × 480 px.</span>
      </div>
      <div className="as-portada-campo">
        <div className="as-portada-mini" style={form.portadaUrl ? { backgroundImage: `url("${form.portadaUrl}")` } : undefined}>
          {!form.portadaUrl && <ImagenIcono size={40} strokeWidth={1.4} aria-hidden="true" />}
        </div>
        <Campo
          etiqueta={
            <>
              <Link2 size={16} aria-hidden="true" /> URL de la portada
            </>
          }
          placeholder="https://…"
          value={form.portadaUrl}
          onChange={(e) => onCambiar({ portadaUrl: e.target.value })}
        />
      </div>

      <div className="as-nota" role="note">
        <Info size={18} aria-hidden="true" />
        <span>El logo, color y portada son opcionales. Podrás cambiarlos en cualquier momento desde la configuración del local.</span>
      </div>
    </>
  )
}

export function LateralAspecto({ form }: { form: FormularioLocal }) {
  const color = COLOR_HEX.test(form.colorPrincipal) ? form.colorPrincipal : '#2B7A68'
  return (
    <>
      <h2 className="as-h2 as-h2-neutro">Vista previa del menú digital</h2>
      <p className="as-texto-suave">Así se verá el local para tus clientes.</p>
      <div className="as-menu-vista">
        <div className="as-menu-portada" style={form.portadaUrl ? { backgroundImage: `url("${form.portadaUrl}")` } : undefined}>
          {!form.portadaUrl && <ImagenIcono size={44} strokeWidth={1.4} aria-hidden="true" />}
        </div>
        <div className="as-menu-cuerpo">
          <span className="as-menu-logo">
            {form.logoUrl ? (
              <img src={form.logoUrl} alt="" onError={(e) => (e.currentTarget.style.display = 'none')} />
            ) : (
              <span aria-hidden="true">{form.nombre.charAt(0).toUpperCase() || 'L'}</span>
            )}
          </span>
          <strong>{form.nombre || 'Nombre del local'}</strong>
          <div className="as-menu-canales">
            {form.deliveryHabilitado && (
              <span>
                <Truck size={18} aria-hidden="true" /> Delivery
              </span>
            )}
            {form.recojoHabilitado && (
              <span>
                <ShoppingBag size={18} aria-hidden="true" /> Recojo
              </span>
            )}
          </div>
          <button type="button" className="as-menu-carta" style={{ background: color }} tabIndex={-1}>
            <BookOpen size={18} aria-hidden="true" /> Ver carta
          </button>
        </div>
      </div>
    </>
  )
}
