import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, IconoBorrar, ModalDeConfirmacion, Tabla, type ColumnaTabla } from '../../comun/componentes'
import { listarProveedores, eliminarProveedor, ErrorApi, type Proveedor } from '../../api/cliente'

export function PantallaProveedores() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [paraEliminar, setParaEliminar] = useState<Proveedor | null>(null)
  const [errorEliminar, setErrorEliminar] = useState('')

  function cargar() {
    setCargando(true)
    setError('')
    listarProveedores(token)
      .then(setProveedores)
      .catch(() => setError('No se pudieron cargar los proveedores.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  async function confirmarEliminar() {
    if (!paraEliminar) return
    setErrorEliminar('')
    try {
      await eliminarProveedor(token, paraEliminar.id)
      setParaEliminar(null)
      cargar()
    } catch (e) {
      if (e instanceof ErrorApi && e.cuerpo?.error === 'TIENE_HISTORIAL') {
        setErrorEliminar('No se puede eliminar: este proveedor ya tiene compras registradas y su historial debe conservarse.')
      } else {
        setErrorEliminar('No se pudo eliminar el proveedor.')
      }
    }
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
          <Boton variante="peligro" onClick={() => setParaEliminar(p)}>
            <IconoBorrar width={14} height={14} aria-hidden="true" /> Eliminar
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

      {paraEliminar && (
        <ModalDeConfirmacion
          abierto
          titulo="Eliminar proveedor"
          mensaje={`"${paraEliminar.razonSocial}" se eliminará permanentemente. Esta acción no se puede deshacer.`}
          peligro
          error={errorEliminar}
          onConfirmar={confirmarEliminar}
          onCancelar={() => {
            setParaEliminar(null)
            setErrorEliminar('')
          }}
        />
      )}
    </section>
  )
}
