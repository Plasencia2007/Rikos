import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Aviso, Boton, Campo, EstadoCargando, EstadoError } from '../../comun/componentes'
import {
  listarProveedores,
  crearProveedor,
  actualizarProveedor,
  activarProveedor,
  type DatosProveedor,
} from '../../api/cliente'

export function PantallaProveedorFormulario() {
  const { id } = useParams<{ id: string }>()
  const esNuevo = id === undefined
  const navigate = useNavigate()
  const { sesion } = useSesion()
  const token = sesion!.token

  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')

  const [ruc, setRuc] = useState('')
  const [razonSocial, setRazonSocial] = useState('')
  const [nombreComercial, setNombreComercial] = useState('')
  const [telefono, setTelefono] = useState('')
  const [correo, setCorreo] = useState('')
  const [direccion, setDireccion] = useState('')
  const [contacto, setContacto] = useState('')

  const [activo, setActivo] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [reactivando, setReactivando] = useState(false)

  useEffect(() => {
    setCargando(true)
    setErrorCarga('')
    if (esNuevo) {
      setCargando(false)
      return
    }
    listarProveedores(token)
      .then((todos) => {
        const p = todos.find((x) => x.id === id)
        if (!p) throw new Error('proveedor no encontrado')
        setRuc(p.ruc ?? '')
        setRazonSocial(p.razonSocial)
        setNombreComercial(p.nombreComercial ?? '')
        setTelefono(p.telefono ?? '')
        setCorreo(p.correo ?? '')
        setDireccion(p.direccion ?? '')
        setContacto(p.contacto ?? '')
        setActivo(p.activo)
      })
      .catch(() => setErrorCarga('No se pudo cargar el proveedor.'))
      .finally(() => setCargando(false))
  }, [id, esNuevo, token])

  async function guardar() {
    setError('')
    setGuardando(true)
    try {
      const datos: DatosProveedor = {
        ruc: ruc || null,
        razonSocial,
        nombreComercial: nombreComercial || null,
        telefono: telefono || null,
        correo: correo || null,
        direccion: direccion || null,
        contacto: contacto || null,
      }
      if (esNuevo) {
        await crearProveedor(token, datos)
      } else {
        await actualizarProveedor(token, id!, datos)
      }
      navigate('/admin/compras/proveedores')
    } catch {
      setError('No se pudo guardar el proveedor. Revisa que el RUC tenga 11 dígitos y no esté repetido.')
    } finally {
      setGuardando(false)
    }
  }

  async function reactivar() {
    if (!id) return
    setReactivando(true)
    try {
      await activarProveedor(token, id)
      setActivo(true)
    } catch {
      setError('No se pudo reactivar el proveedor.')
    } finally {
      setReactivando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>{esNuevo ? 'Nuevo proveedor' : 'Editar proveedor'}</h1>
        <Boton onClick={guardar} cargando={guardando}>
          {esNuevo ? 'Crear proveedor' : 'Guardar cambios'}
        </Boton>
      </div>

      {!esNuevo && !activo && (
        <Aviso>
          Este proveedor está eliminado: no aparece en compras nuevas.{' '}
          <Boton variante="secundario" onClick={reactivar} cargando={reactivando}>
            Reactivar
          </Boton>
        </Aviso>
      )}

      <div className="panel-admin-tarjeta">
        <Campo
          etiqueta="RUC (opcional)"
          value={ruc}
          onChange={(e) => setRuc(e.target.value)}
          ayuda="Déjalo vacío para un proveedor informal, como el mercado de abastos."
        />
        <Campo
          etiqueta="Razón social"
          value={razonSocial}
          onChange={(e) => setRazonSocial(e.target.value)}
          required
        />
        <Campo
          etiqueta="Nombre comercial"
          value={nombreComercial}
          onChange={(e) => setNombreComercial(e.target.value)}
        />
        <Campo etiqueta="Teléfono" value={telefono} onChange={(e) => setTelefono(e.target.value)} />
        <Campo etiqueta="Correo" type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} />
        <Campo etiqueta="Dirección" value={direccion} onChange={(e) => setDireccion(e.target.value)} />
        <Campo etiqueta="Persona de contacto" value={contacto} onChange={(e) => setContacto(e.target.value)} />

        {error && <p className="campo-error">{error}</p>}
      </div>
    </section>
  )
}
