import { Aviso, Boton, Campo, Checkbox, IconoMoto } from '../../comun/componentes'
import type { Local } from '../../api/cliente'
import { PestanaHorarios } from './PestanaHorarios'

interface CamposCanales {
  deliveryHabilitado: boolean
  recojoHabilitado: boolean
  tiempoMinEntregaMin: string
  tiempoMaxEntregaMin: string
}

interface Props {
  token: string
  local: Local
  formulario: CamposCanales
  onCambiarFormulario: (cambios: Partial<CamposCanales>) => void
  onGuardarCanales: () => void
  guardando: boolean
  errorGuardar: string
  guardadoOk: boolean
}

/**
 * Fusiona canales de venta y horarios en una sola pestaña ("Online y
 * Horarios" en la referencia de diseño). La ubicación en el mapa vive en
 * "Datos básicos" -- es parte de identificar el local, no de sus canales.
 */
export function PestanaOnlineYHorarios({
  token,
  local,
  formulario,
  onCambiarFormulario,
  onGuardarCanales,
  guardando,
  errorGuardar,
  guardadoOk,
}: Props) {
  return (
    <div className="pestana-online-horarios">
      <section className="canales-de-venta">
        <h3 className="formulario-local-subtitulo">
          <IconoMoto width={16} height={16} aria-hidden="true" /> Canales de venta
        </h3>

        <Checkbox
          etiqueta="Habilitar delivery"
          checked={formulario.deliveryHabilitado}
          onChange={(e) => onCambiarFormulario({ deliveryHabilitado: e.target.checked })}
        />
        <Checkbox
          etiqueta="Habilitar recojo en tienda"
          checked={formulario.recojoHabilitado}
          onChange={(e) => onCambiarFormulario({ recojoHabilitado: e.target.checked })}
        />

        <div className="canales-tiempos">
          <Campo
            etiqueta="Tiempo mín. entrega (min)"
            type="number"
            min={0}
            value={formulario.tiempoMinEntregaMin}
            onChange={(e) => onCambiarFormulario({ tiempoMinEntregaMin: e.target.value })}
            disabled={!formulario.deliveryHabilitado}
          />
          <Campo
            etiqueta="Tiempo máx. entrega (min)"
            type="number"
            min={0}
            value={formulario.tiempoMaxEntregaMin}
            onChange={(e) => onCambiarFormulario({ tiempoMaxEntregaMin: e.target.value })}
            disabled={!formulario.deliveryHabilitado}
          />
        </div>

        {errorGuardar && <Aviso tono="error">{errorGuardar}</Aviso>}
        {guardadoOk && <Aviso tono="exito">Guardado.</Aviso>}
        <div className="formulario-local-guardar">
          <Boton onClick={onGuardarCanales} cargando={guardando}>
            Guardar canales de venta
          </Boton>
        </div>
      </section>

      <hr className="separador" />

      <section>
        <h3 className="formulario-local-subtitulo">Horarios de atención (restaurante físico)</h3>
        <PestanaHorarios token={token} localId={local.id} />
      </section>
    </div>
  )
}
