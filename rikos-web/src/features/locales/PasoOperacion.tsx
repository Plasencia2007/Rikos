import type { Ref } from 'react'
import { Info, ShoppingBag, Truck } from 'lucide-react'
import { Campo, Interruptor } from '../../comun/componentes'
import type { DiaSemana } from '../../api/cliente'
import type { FormularioLocal } from './formularioLocal'
import { ETIQUETA_DIA, ETIQUETA_SERVICIO, HorarioSemanal, type ManejadorHorario, type TurnoEdicion } from './HorarioSemanal'

interface Props {
  token: string
  localId: string
  form: FormularioLocal
  errores: { tiempos?: string }
  horarioRef: Ref<ManejadorHorario>
  onCambiar: (cambios: Partial<FormularioLocal>) => void
  onCambiarTurnos: (turnos: TurnoEdicion[]) => void
}

export function PasoOperacion({ token, localId, form, errores, horarioRef, onCambiar, onCambiarTurnos }: Props) {
  return (
    <>
      <div className="as-titulo-bloque">
        <h2 className="as-h2">Canales de venta</h2>
        <span className="as-ayuda">Selecciona cómo quieres recibir pedidos en este local.</span>
      </div>
      <div className="as-canales">
        <div className="as-canal">
          <div className="as-canal-cabecera">
            <Interruptor
              etiqueta="Habilitar delivery"
              checked={form.deliveryHabilitado}
              onChange={(e) => onCambiar({ deliveryHabilitado: e.target.checked })}
            />
            <div>
              <strong>Delivery</strong>
              <span>Recibe pedidos para entrega a domicilio.</span>
            </div>
          </div>
          <div className="as-fila-doble">
            <Campo
              etiqueta="Tiempo mín. de entrega (min)"
              type="number"
              min={0}
              value={form.tiempoMinEntregaMin}
              onChange={(e) => onCambiar({ tiempoMinEntregaMin: e.target.value })}
              disabled={!form.deliveryHabilitado}
            />
            <Campo
              etiqueta="Tiempo máx. de entrega (min)"
              type="number"
              min={0}
              value={form.tiempoMaxEntregaMin}
              onChange={(e) => onCambiar({ tiempoMaxEntregaMin: e.target.value })}
              disabled={!form.deliveryHabilitado}
              error={errores.tiempos}
            />
          </div>
        </div>
        <span className="as-canal-linea" aria-hidden="true" />
        <div className="as-canal">
          <div className="as-canal-cabecera">
            <Interruptor
              etiqueta="Habilitar recojo en tienda"
              checked={form.recojoHabilitado}
              onChange={(e) => onCambiar({ recojoHabilitado: e.target.checked })}
            />
            <div>
              <strong>Recojo en tienda</strong>
              <span>Permite que los clientes recojan sus pedidos en el local.</span>
            </div>
          </div>
        </div>
      </div>

      <hr className="as-separador" />

      <HorarioSemanal token={token} localId={localId} ref={horarioRef} onCambiarTurnos={onCambiarTurnos} />
    </>
  )
}

const DIA_POR_JS: DiaSemana[] = ['domingo', 'lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado']

export function LateralOperacion({ form, turnos }: { form: FormularioLocal; turnos: TurnoEdicion[] }) {
  const hoy = DIA_POR_JS[new Date().getDay()]
  const deHoy = turnos.filter((t) => t.diaSemana === hoy).sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))
  const hayTiempos = form.deliveryHabilitado && form.tiempoMinEntregaMin && form.tiempoMaxEntregaMin

  return (
    <>
      <h2 className="as-h2 as-h2-neutro">Así verá el cliente tu local</h2>
      <p className="as-texto-suave">Esta es la información que se mostrará en Riko&apos;s para tus clientes.</p>
      <div className="as-vista-local">
        <strong className="as-vista-nombre">{form.nombre || 'Nombre del local'}</strong>
        <div className="as-chips">
          {form.deliveryHabilitado && (
            <span>
              <Truck size={17} aria-hidden="true" /> Delivery
            </span>
          )}
          {form.recojoHabilitado && (
            <span>
              <ShoppingBag size={17} aria-hidden="true" /> Recojo en tienda
            </span>
          )}
          {!form.deliveryHabilitado && !form.recojoHabilitado && (
            <span className="as-chip-vacio">Sin canales de venta</span>
          )}
        </div>
        <hr className="as-separador" />
        <div className="as-vista-hoy">
          <strong>Horarios de hoy</strong>
          <span className="as-suave">{ETIQUETA_DIA[hoy]}</span>
        </div>
        {deHoy.length === 0 ? (
          <span className="as-pastilla cerrado">Cerrado</span>
        ) : (
          <>
            <ul className="as-turnos-hoy">
              {deHoy.map((t) => (
                <li key={t.clave}>
                  <span className="as-suave">{ETIQUETA_SERVICIO[t.servicio]}</span>
                  <span>
                    {t.horaInicio} – {t.horaFin}
                  </span>
                </li>
              ))}
            </ul>
            <span className="as-pastilla abierto">Abierto</span>
          </>
        )}
        {hayTiempos && (
          <>
            <hr className="as-separador" />
            <strong>Tiempo de entrega estimado</strong>
            <span className="as-vista-tiempo">
              {form.tiempoMinEntregaMin} – {form.tiempoMaxEntregaMin} min
            </span>
            <span className="as-ayuda as-con-icono">
              <Info size={15} aria-hidden="true" /> El tiempo puede variar según la demanda.
            </span>
          </>
        )}
      </div>
    </>
  )
}
