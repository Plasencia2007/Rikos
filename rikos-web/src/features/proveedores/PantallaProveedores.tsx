import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, ModalDeConfirmacion, Tabla, type ColumnaTabla } from '../../comun/componentes'
import { listarProveedores, desactivarProveedor, activarProveedor, type Proveedor } from '../../api/cliente'

export function PantallaProveedores() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [paraCambiarEstado, setParaCambiarEstado] = useState<Proveedor | null>(null)

  function cargar() {
    setCargando(true)
    setError('')
    listarProveedores(token)
      .then(setProveedores)
      .catch(() => setError('No se pudieron cargar los proveedores.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  async function confirmarCambioEstado() {
    if (!paraCambiarEstado) return
    if (paraCambiarEstado.activo) {
      await desactivarProveedor(token, paraCambiarEstado.id)
    } else {
      await activarProveedor(token, paraCambiarEstado.id)
    }
    setParaCambiarEstado(null)
    cargar()
  }

  const columnas: ColumnaTabla<Proveedor>[] = [
    { encabezado: 'RUC', render: (p) => p.ruc ?? '— (informal)' },
    { encabezado: 'Razón social', render: (p) => p.razonSocial },
    { encabezado: 'Nombre comercial', render: (p) => p.nombreComercial ?? '—' },
    { encabezado: 'Teléfono', render: (p) => p.telefono ?? '—' },
    { encabezado: 'Estado', render: (p) => (p.activo ? 'Activo' : 'Inactivo') },
    {
      encabezado: 'Acciones',
      acciones: true,
      render: (p) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <Boton variante="secundario" onClick={() => navigate(`/admin/compras/proveedores/${p.id}/editar`)}>
            Editar
          </Boton>
          <Boton variante={p.activo ? 'peligro' : 'secundario'} onClick={() => setParaCambiarEstado(p)}>
            {p.activo ? 'Desactivar' : 'Activar'}
          </Boton>
        </div>
      ),
    },
  ]

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>Proveedores</h1>
        <Boton onClick={() => navigate('/admin/compras/proveedores/nuevo')}>Nuevo proveedor</Boton>
      </div>

      <Tabla
        columnas={columnas}
        filas={proveedores}
        obtenerClave={(p) => p.id}
        cargando={cargando}
        error={error}
        vacioTitulo="Todavía no hay proveedores"
        vacioDescripcion="Registra tu primer proveedor. El RUC es opcional: también puedes registrar compras al mercado de abastos."
        vacioAccion={<Boton onClick={() => navigate('/admin/compras/proveedores/nuevo')}>Nuevo proveedor</Boton>}
      />

      {paraCambiarEstado && (
        <ModalDeConfirmacion
          abierto
          titulo={paraCambiarEstado.activo ? 'Desactivar proveedor' : 'Activar proveedor'}
          mensaje={
            paraCambiarEstado.activo
              ? `"${paraCambiarEstado.razonSocial}" dejará de poder usarse en compras nuevas. Las compras anteriores se conservan.`
              : `"${paraCambiarEstado.razonSocial}" volverá a estar disponible para compras nuevas.`
          }
          onConfirmar={confirmarCambioEstado}
          onCancelar={() => setParaCambiarEstado(null)}
        />
      )}
    </section>
  )
}
