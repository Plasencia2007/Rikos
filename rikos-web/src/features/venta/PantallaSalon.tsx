import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton } from '../../comun/componentes'
import { MapaDeMesas } from './MapaDeMesas'

/**
 * El mapa de mesas dentro del panel de administracion: el administrador ve el salon del local activo
 * (el que eligio en el encabezado), y bloquea o libera mesas igual que el mozo en el POS. Reusa el
 * mismo componente; el servidor decide cada accion segun el permiso. Crear y editar mesas vive en
 * Configuracion -> Salones y mesas.
 */
export function PantallaSalon() {
  const { sesion, tienePermiso } = useSesion()
  const navigate = useNavigate()

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Operación</p>
          <h1>Mapa de mesas</h1>
          <p className="panel-admin-subtitulo">
            El salón del local en el que estás trabajando, con el estado de cada mesa. Se actualiza solo.
          </p>
        </div>
        {tienePermiso('venta.administrar') && (
          <Boton variante="secundario" onClick={() => navigate('/admin/configuracion/mesas')}>
            Administrar mesas
          </Boton>
        )}
      </div>
      <div className="prod-tabla-tarjeta">
        <MapaDeMesas token={sesion!.token} />
      </div>
    </section>
  )
}
