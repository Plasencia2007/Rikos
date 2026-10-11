import { Info, Mail, Phone } from 'lucide-react'
import { Campo, Select } from '../../comun/componentes'
import type { FormularioLocal } from './formularioLocal'
import { PREFIJOS_PAIS } from './prefijosPais'
import { IconoFacebook, IconoInstagram, IconoTiktok } from './iconosRedes'

interface Props {
  form: FormularioLocal
  errores: { numero?: string; correo?: string }
  onCambiar: (cambios: Partial<FormularioLocal>) => void
}

export function PasoContacto({ form, errores, onCambiar }: Props) {
  const redes = [
    { clave: 'facebookUrl' as const, nombre: 'Facebook', ph: 'Ej. tu-restaurante', Icono: IconoFacebook },
    { clave: 'instagramUrl' as const, nombre: 'Instagram', ph: 'Ej. @tu-restaurante', Icono: IconoInstagram },
    { clave: 'tiktokUrl' as const, nombre: 'TikTok', ph: 'Ej. @tu-restaurante', Icono: IconoTiktok },
  ]

  return (
    <>
      <div className="as-titulo-bloque">
        <h2 className="as-h2">Contacto directo</h2>
        <span className="as-ayuda">Estos datos aparecerán en tu menú digital y en la información del local.</span>
      </div>
      <div className="as-grupo">
        <div className="as-fila-telefono">
          <Select etiqueta="Prefijo" value={form.prefijo} onChange={(e) => onCambiar({ prefijo: e.target.value })}>
            {PREFIJOS_PAIS.map((p) => (
              <option key={p.codigo} value={p.prefijo}>
                {p.nombre} ({p.prefijo})
              </option>
            ))}
          </Select>
          <Campo
            etiqueta={<>Teléfono <span className="as-req">*</span></>}
            type="tel"
            placeholder="Ej. 987 654 321"
            value={form.numero}
            onChange={(e) => onCambiar({ numero: e.target.value })}
            error={errores.numero}
          />
        </div>
        <Campo
          etiqueta={<>Correo del local <span className="as-req">*</span></>}
          type="email"
          placeholder="Ej. contacto@tu-restaurante.pe"
          ayuda="Se mostrará en el menú digital y comunicaciones del local."
          value={form.correo}
          onChange={(e) => onCambiar({ correo: e.target.value })}
          error={errores.correo}
        />
      </div>

      <hr className="as-separador" />

      <div className="as-titulo-bloque">
        <h2 className="as-h2">Redes sociales</h2>
        <span className="as-ayuda">Enlaces directos para los botones del menú digital. Son opcionales.</span>
      </div>
      <div className="as-grupo">
        {redes.map(({ clave, nombre, ph, Icono }) => (
          <div key={clave} className="as-red">
            <span className="as-red-icono">
              <Icono width={20} height={20} aria-hidden="true" />
            </span>
            <Campo
              className="as-red-campo"
              etiqueta={
                <>
                  {nombre} <span className="as-opcional">Opcional</span>
                </>
              }
              placeholder={ph}
              value={form[clave]}
              onChange={(e) => onCambiar({ [clave]: e.target.value })}
            />
          </div>
        ))}
      </div>
    </>
  )
}

export function LateralContacto({ form }: { form: FormularioLocal }) {
  const telefono = form.numero.trim() ? `${form.prefijo} ${form.numero.trim()}` : ''
  const redes = [
    { nombre: 'Facebook', valor: form.facebookUrl, Icono: IconoFacebook },
    { nombre: 'Instagram', valor: form.instagramUrl, Icono: IconoInstagram },
    { nombre: 'TikTok', valor: form.tiktokUrl, Icono: IconoTiktok },
  ].filter((r) => r.valor.trim())
  const hayDatos = telefono || form.correo.trim() || redes.length > 0

  return (
    <>
      <h2 className="as-h2 as-h2-neutro">Vista previa de contacto</h2>
      <p className="as-texto-suave">Así se verá la información en tu menú digital para los clientes.</p>
      <div className="as-vista-contacto">
        {hayDatos ? (
          <ul>
            {telefono && (
              <li>
                <Phone size={16} aria-hidden="true" /> {telefono}
              </li>
            )}
            {form.correo.trim() && (
              <li>
                <Mail size={16} aria-hidden="true" /> {form.correo.trim()}
              </li>
            )}
            {redes.map(({ nombre, valor, Icono }) => (
              <li key={nombre}>
                <Icono width={16} height={16} aria-hidden="true" /> {valor}
              </li>
            ))}
          </ul>
        ) : (
          <>
            <span className="as-vacio-icono">
              <Phone size={30} strokeWidth={1.6} aria-hidden="true" />
            </span>
            <strong>Aún no hay datos de contacto</strong>
            <span className="as-texto-suave">
              Completa el teléfono y el correo (obligatorios) y, si quieres, tus redes sociales para que tus clientes
              puedan encontrarte desde el menú digital.
            </span>
          </>
        )}
      </div>
      <div className="as-nota" role="note">
        <Info size={18} aria-hidden="true" />
        <span>
          El teléfono y el correo son obligatorios. Las redes sociales son opcionales y puedes agregarlas después desde
          la edición del local.
        </span>
      </div>
    </>
  )
}
