import { useSesion } from './SesionContexto'
import { IconoFlechaAbajo, IconoMenuHamburguesa, IconoUbicacion, IconoUsuario } from '../componentes'

/**
 * US-002 / DT-026: el selector de local siempre esta presente, con los
 * locales asignados al usuario -- incluido el admin, que hoy tiene San
 * Isidro asignado como cualquier otro usuario. No depende de cuantos
 * locales tenga: con uno solo, sigue siendo el selector (deshabilitado si
 * no hay entre que elegir), nunca texto plano.
 *
 * El buscador global y la campana de notificaciones se quitaron: no tienen
 * ninguna funcion detras todavia (no hay indice de busqueda ni sistema de
 * notificaciones), y un badge con un numero inventado es peor que no
 * mostrar nada. Las notificaciones reales las define RN-029 (abono
 * aprobado/rechazado, estado de cuenta mensual) y ninguna de esas dos
 * cosas existe todavia -- se agregan cuando existan, no antes.
 */
export function EncabezadoLocal({ onAbrirMenu }: { onAbrirMenu?: () => void }) {
  const { sesion, cambiarLocalActivo, salir } = useSesion()

  if (!sesion) return null

  return (
    <header className="encabezado-local">
      <button type="button" className="panel-menu-boton" onClick={onAbrirMenu} aria-label="Abrir menú">
        <IconoMenuHamburguesa width={20} height={20} />
      </button>

      <div className="encabezado-local-selector">
        <IconoUbicacion width={16} height={16} aria-hidden="true" />
        <select
          aria-label="Local activo"
          value={sesion.localActivoId ?? ''}
          onChange={(e) => cambiarLocalActivo(e.target.value)}
          disabled={sesion.locales.length <= 1}
        >
          {sesion.locales.map((local) => (
            <option key={local.id} value={local.id}>
              {local.nombre}
            </option>
          ))}
        </select>
      </div>

      <div className="encabezado-usuario">
        <span className="encabezado-usuario-avatar" aria-hidden="true">
          <IconoUsuario width={18} height={18} />
        </span>
        <div className="encabezado-usuario-texto">
          <strong>{sesion.nombreUsuario}</strong>
          <span className="encabezado-rol">{sesion.roles.join(', ')}</span>
        </div>
        <IconoFlechaAbajo width={16} height={16} aria-hidden="true" />
      </div>

      <button type="button" className="boton boton-texto boton-normal encabezado-salir" onClick={salir}>
        Salir
      </button>
    </header>
  )
}
