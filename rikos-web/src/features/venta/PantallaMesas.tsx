import { useCallback, useEffect, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoMas,
  Insignia,
  Modal,
} from '../../comun/componentes'
import {
  ErrorApi,
  actualizarMesa,
  activarMesa,
  crearMesa,
  desactivarMesa,
  listarMesas,
  type Mesa,
} from '../../api/cliente'

/**
 * US-027: administrar las mesas del local activo (crear, editar, activar y
 * desactivar). Solo quien tiene venta.administrar; el resto ve la lista.
 * Cambiar de local en el encabezado cambia de que local son las mesas.
 * Una mesa no se borra: tiene historial de pedidos; se desactiva.
 */

function mensajeDe(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    switch (error.cuerpo?.error) {
      case 'MESA_MODIFICADA':
        return 'Otra persona cambió esta mesa al mismo tiempo. Se actualizó la lista: revisa y vuelve a intentarlo.'
      case 'NUMERO_INVALIDO':
        return 'El número es obligatorio y tiene hasta 10 caracteres.'
      case 'NUMERO_DUPLICADO':
        return 'Ya hay una mesa con ese número en este local.'
      case 'CAPACIDAD_INVALIDA':
        return 'La capacidad tiene que ser un número mayor que cero.'
      case 'SIN_LOCAL_ACTIVO':
        return 'Elige primero el local en el que estás trabajando.'
      case 'NO_ENCONTRADA':
        return 'La mesa ya no existe en este local.'
    }
    if (error.estado === 403) return 'No tienes permiso para hacer esto.'
  }
  return porDefecto
}

export function PantallaMesas() {
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const puedeAdministrar = tienePermiso('venta.administrar')

  const [mesas, setMesas] = useState<Mesa[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [dialogo, setDialogo] = useState<{ mesa: Mesa | null } | null>(null)

  const cargar = useCallback(() => {
    setCargando(true)
    setError('')
    listarMesas(token, true)
      .then(setMesas)
      .catch((e) => setError(mensajeDe(e, 'No se pudieron cargar las mesas.')))
      .finally(() => setCargando(false))
  }, [token])

  useEffect(cargar, [cargar])

  async function cambiarActiva(mesa: Mesa) {
    try {
      await (mesa.activo ? desactivarMesa(token, mesa.id) : activarMesa(token, mesa.id))
      cargar()
    } catch (e) {
      setError(mensajeDe(e, 'No se pudo cambiar la mesa.'))
    }
  }

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Configuración</p>
          <h1>Mesas</h1>
          <p className="panel-admin-subtitulo">
            Las mesas del local en el que estás trabajando. El mozo las ve en el mapa de mesas del POS.
          </p>
        </div>
        {puedeAdministrar && !cargando && !error && (
          <Boton onClick={() => setDialogo({ mesa: null })}>
            <IconoMas width={16} height={16} aria-hidden="true" /> Nueva mesa
          </Boton>
        )}
      </div>

      {!puedeAdministrar && (
        <Aviso tono="informacion">Puedes ver las mesas, pero no modificarlas con tus permisos.</Aviso>
      )}

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : mesas.length === 0 ? (
        <div className="panel-admin-tarjeta">
          <EstadoVacio
            titulo="Este local todavía no tiene mesas"
            descripcion="Crea las mesas del salón para que el mozo pueda verlas y tomar pedidos."
            accion={puedeAdministrar ? <Boton onClick={() => setDialogo({ mesa: null })}>Nueva mesa</Boton> : undefined}
          />
        </div>
      ) : (
        <div className="prod-tabla-tarjeta">
          <div className="prod-tabla-scroll">
            <table className="prod-tabla">
              <thead>
                <tr>
                  <th>Número</th>
                  <th>Nombre</th>
                  <th className="cta-num">Capacidad</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {mesas.map((m) => (
                  <tr key={m.id} className={`prod-fila ${m.activo ? '' : 'prod-fila-inactiva'}`}>
                    <td className="prod-codigo">{m.numero}</td>
                    <td>{m.nombre ?? <span className="prod-vacio">—</span>}</td>
                    <td className="cta-num">{m.capacidad ?? <span className="prod-vacio">—</span>}</td>
                    <td>
                      <Insignia tono={m.activo ? 'activo' : 'inactivo'} />
                      {m.estado === 'bloqueada' && <span className="cta-nota"> Bloqueada: {m.motivoBloqueo}</span>}
                    </td>
                    <td className="cta-acciones">
                      {puedeAdministrar && (
                        <>
                          <button type="button" className="plan-historial-boton" onClick={() => setDialogo({ mesa: m })}>
                            Editar
                          </button>
                          <button type="button" className="plan-historial-boton" onClick={() => cambiarActiva(m)}>
                            {m.activo ? 'Desactivar' : 'Activar'}
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="prod-pie">
            <span>
              {mesas.filter((m) => m.activo).length} activas de {mesas.length} mesas
            </span>
          </div>
        </div>
      )}

      {dialogo && (
        <DialogoMesa
          token={token}
          mesa={dialogo.mesa}
          onCerrar={() => setDialogo(null)}
          onGuardado={() => {
            setDialogo(null)
            cargar()
          }}
        />
      )}
    </section>
  )
}

function DialogoMesa({
  token,
  mesa,
  onCerrar,
  onGuardado,
}: {
  token: string
  mesa: Mesa | null
  onCerrar: () => void
  onGuardado: () => void
}) {
  const [numero, setNumero] = useState(mesa?.numero ?? '')
  const [nombre, setNombre] = useState(mesa?.nombre ?? '')
  const [capacidad, setCapacidad] = useState(mesa?.capacidad != null ? String(mesa.capacidad) : '')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  function guardar() {
    const cap = capacidad.trim() === '' ? null : Number(capacidad)
    if (cap !== null && (!Number.isInteger(cap) || cap <= 0)) {
      setError('La capacidad tiene que ser un número entero mayor que cero.')
      return
    }
    if (numero.trim() === '') {
      setError('El número de la mesa es obligatorio.')
      return
    }
    setGuardando(true)
    setError('')
    const datos = { numero: numero.trim(), nombre: nombre.trim() || null, capacidad: cap }
    ;(mesa ? actualizarMesa(token, mesa.id, datos) : crearMesa(token, datos))
      .then(onGuardado)
      .catch((e) => setError(mensajeDe(e, 'No se pudo guardar la mesa.')))
      .finally(() => setGuardando(false))
  }

  return (
    <Modal
      abierto
      titulo={mesa ? `Editar mesa ${mesa.numero}` : 'Nueva mesa'}
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={guardar}>
            {mesa ? 'Guardar cambios' : 'Crear mesa'}
          </Boton>
        </>
      }
    >
      <div className="cta-dialogo">
        {error && <Aviso tono="error">{error}</Aviso>}
        <Campo etiqueta="Número" maxLength={10} value={numero} onChange={(e) => setNumero(e.target.value)} />
        <Campo
          etiqueta="Nombre"
          ayuda="Opcional. Por ejemplo: Terraza 3."
          maxLength={60}
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
        />
        <Campo
          etiqueta="Capacidad"
          ayuda="Opcional. Cuántas personas se sientan."
          inputMode="numeric"
          value={capacidad}
          onChange={(e) => setCapacidad(e.target.value)}
        />
      </div>
    </Modal>
  )
}
