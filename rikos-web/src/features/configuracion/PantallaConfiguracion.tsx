import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowRight,
  Bike,
  ChefHat,
  CircleDollarSign,
  CreditCard,
  List,
  MapPin,
  Printer,
  Store,
  Table2,
  Vault,
  type LucideIcon,
} from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { listarLocales, listarMotivos } from '../../api/cliente'

interface Ajuste {
  clave: string
  titulo: string
  descripcion: string
  Icono: LucideIcon
  /** Sin ruta = todavía no existe la pantalla: se muestra "Próximamente" y no se puede abrir. */
  ruta?: string
}

const AJUSTES: Ajuste[] = [
  { clave: 'restaurante', titulo: 'Mi restaurante', Icono: Store, descripcion: 'Gestiona los datos generales de la empresa: razón social, marca, logotipo y datos de contacto.' },
  { clave: 'locales', titulo: 'Locales', Icono: MapPin, ruta: '/admin/locales', descripcion: 'Administra tus sucursales, direcciones físicas, teléfonos, horarios y canales de venta.' },
  { clave: 'cobro', titulo: 'Parámetros de cobro', Icono: CircleDollarSign, ruta: '/admin/configuracion/parametros-de-cobro', descripcion: 'Reglas y condiciones para el cobro: planes tarifarios, táper y segundo consumo.' },
  { clave: 'pagos', titulo: 'Medios de pago', Icono: CreditCard, descripcion: 'Formas de pago disponibles en el sistema.' },
  { clave: 'salones', titulo: 'Salones y mesas', Icono: Table2, ruta: '/admin/configuracion/salones', descripcion: 'Organiza las zonas de tu restaurante (terraza, salón principal, barra) y las mesas de cada una, con su plano visual. El mozo las ve en el mapa del POS.' },
  { clave: 'produccion', titulo: 'Producción', Icono: ChefHat, descripcion: 'Define las áreas de producción (cocina caliente, cocina fría, bar) y el enrutamiento de las comandas.' },
  { clave: 'impresoras', titulo: 'Impresoras', Icono: Printer, descripcion: 'Configura las impresoras de tickets para pre cuentas, facturación y comandas de cocina/bar.' },
  { clave: 'delivery', titulo: 'Delivery', Icono: Bike, descripcion: 'Establece las zonas de entrega, tarifas, tiempos de reparto y pedidos mínimos.' },
  { clave: 'cajas', titulo: 'Cajas registradoras', Icono: Vault, descripcion: 'Administra las cajas físicas del restaurante, asigna impresoras y valida IPs de acceso.' },
  { clave: 'operacion', titulo: 'Operación', Icono: List, ruta: '/admin/configuracion/motivos', descripcion: 'Administra los motivos utilizados en los procesos del sistema (p. ej. anulaciones, descuentos).' },
]

const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`

/** Portada de Configuración: un ajuste por tarjeta. Locales y Operación muestran un dato en vivo. */
export function PantallaConfiguracion() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const [datos, setDatos] = useState<Record<string, string>>({})

  useEffect(() => {
    let vigente = true
    // El dato en vivo es un extra: si falla, la tarjeta queda sin él.
    listarLocales(token)
      .then((ls) => {
        const activos = ls.filter((l) => l.activo).length
        if (vigente) setDatos((d) => ({ ...d, locales: `${plural(ls.length, 'local', 'locales')} · ${plural(activos, 'activo', 'activos')}` }))
      })
      .catch(() => undefined)
    listarMotivos(token)
      .then((ms) => {
        const activos = ms.filter((m) => m.activo).length
        if (vigente) setDatos((d) => ({ ...d, operacion: plural(activos, 'motivo activo', 'motivos activos') }))
      })
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [token])

  return (
    <section className="panel-admin cfg-pagina">
      <div>
        <nav className="loc-migas" aria-label="Ruta">
          <span className="cfg-miga-enlace">Administración</span>
          <span aria-hidden="true">/</span>
          <span>Configuración</span>
        </nav>
        <h1 className="cfg-titulo">Configuración del sistema</h1>
        <p className="cfg-subtitulo">Administra los parámetros generales y operativos de Riko&apos;s.</p>
      </div>

      <div className="cfg-grilla">
        {AJUSTES.map(({ clave, titulo, descripcion, Icono, ruta }) => {
          const contenido = (
            <>
              <div className="cfg-tarjeta-cabecera">
                <span className="cfg-icono" aria-hidden="true">
                  <Icono size={24} strokeWidth={1.6} />
                </span>
                <h2>{titulo}</h2>
                {ruta ? (
                  <ArrowRight size={20} strokeWidth={2} className="cfg-flecha" aria-hidden="true" />
                ) : (
                  <span className="cfg-pronto">Próximamente</span>
                )}
              </div>
              <p>{descripcion}</p>
              {datos[clave] && (
                <div className="cfg-dato">
                  <span aria-hidden="true" /> {datos[clave]}
                </div>
              )}
            </>
          )
          return ruta ? (
            <Link key={clave} to={ruta} className="cfg-tarjeta">
              {contenido}
            </Link>
          ) : (
            <div key={clave} className="cfg-tarjeta cfg-tarjeta-pronto" role="group" aria-disabled="true">
              {contenido}
            </div>
          )
        })}
      </div>
    </section>
  )
}
