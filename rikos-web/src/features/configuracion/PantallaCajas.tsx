import { useEffect, useMemo, useState } from 'react'
import { Info, Monitor, Pencil, Plus, Search, Trash2 } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  Interruptor,
  Modal,
  ModalDeConfirmacion,
  Select,
} from '../../comun/componentes'
import {
  ErrorApi,
  activarCaja,
  crearCaja,
  definirCajaPrincipal,
  desactivarCaja,
  editarCaja,
  eliminarCaja,
  listarCajas,
  type CajaDelLocal,
} from '../../api/cliente'

const MENSAJES: Record<string, string> = {
  NOMBRE_INVALIDO: 'Escribe el nombre de la caja (hasta 60 caracteres).',
  NOMBRE_REPETIDO: 'Ya existe una caja con ese nombre en este local.',
  CODIGO_INVALIDO: 'El código es obligatorio al editar y tiene hasta 20 caracteres.',
  CODIGO_REPETIDO: 'Ya existe una caja con ese código en este local.',
  CAJA_PRINCIPAL: 'La caja principal no se desactiva ni se borra: primero elige otra como principal.',
  CAJA_CON_TURNO_ABIERTO: 'Esa caja tiene un turno abierto: el cajero tiene que cerrarlo primero.',
  CAJA_INACTIVA: 'Una caja desactivada no puede ser la principal: actívala primero.',
  TIENE_HISTORIAL: 'Esa caja ya tuvo turnos, así que no se puede borrar. Desactívala para que deje de ofrecerse.',
  CAJA_NO_ENCONTRADA: 'Esa caja ya no existe. Recarga la página.',
}

function mensajeDe(e: unknown, porDefecto: string): string {
  if (e instanceof ErrorApi) {
    const codigo = e.cuerpo?.error
    if (codigo && MENSAJES[codigo]) return MENSAJES[codigo]
    if (e.estado === 403) return 'No tienes permiso para administrar las cajas.'
  }
  return porDefecto
}

interface Formulario {
  /** null = caja nueva. */
  id: string | null
  nombre: string
  codigo: string
  error: string
}

/**
 * Cajas registradoras (Configuración): las cajas físicas del local. Cada una tiene nombre y código únicos, una sola es la principal (la
 * que viene elegida al abrir turno en el POS) y se activan o desactivan. El POS no crea cajas: el cajero elige entre las registradas.
 */
export function PantallaCajas() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const [cajas, setCajas] = useState<CajaDelLocal[] | null>(null)
  const [error, setError] = useState(false)
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState<'todos' | 'activas' | 'inactivas'>('todos')
  const [form, setForm] = useState<Formulario | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [aBorrar, setABorrar] = useState<CajaDelLocal | null>(null)
  const [aviso, setAviso] = useState('')
  const [avisoError, setAvisoError] = useState('')

  function cargar() {
    setError(false)
    listarCajas(token)
      .then(setCajas)
      .catch(() => setError(true))
  }
  useEffect(cargar, [token])

  useEffect(() => {
    if (!aviso) return
    const t = setTimeout(() => setAviso(''), 2800)
    return () => clearTimeout(t)
  }, [aviso])

  const visibles = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    return (cajas ?? []).filter(
      (c) =>
        (estado === 'todos' || (estado === 'activas' ? c.activo : !c.activo)) &&
        (q === '' || `${c.nombre} ${c.codigo}`.toLowerCase().includes(q)),
    )
  }, [cajas, busqueda, estado])

  if (error && !cajas) return <EstadoError mensaje="No se pudieron cargar las cajas." accion={<Boton onClick={cargar}>Reintentar</Boton>} />
  if (!cajas) return <EstadoCargando />

  const activas = cajas.filter((c) => c.activo).length
  const abiertas = cajas.filter((c) => c.turnoAbierto).length

  async function ejecutar(accion: () => Promise<unknown>, exito: string) {
    setAvisoError('')
    try {
      await accion()
      setAviso(exito)
      cargar()
    } catch (e) {
      setAvisoError(mensajeDe(e, 'No se pudo hacer el cambio. Inténtalo de nuevo.'))
      cargar()
    }
  }

  async function guardar() {
    if (!form) return
    if (form.nombre.trim() === '') {
      setForm({ ...form, error: 'Escribe el nombre de la caja.' })
      return
    }
    setGuardando(true)
    try {
      if (form.id) await editarCaja(token, form.id, { nombre: form.nombre, codigo: form.codigo })
      else await crearCaja(token, { nombre: form.nombre, codigo: form.codigo.trim() || null })
      setAviso(form.id ? 'Caja actualizada' : 'Caja creada')
      setForm(null)
      cargar()
    } catch (e) {
      setForm({ ...form, error: mensajeDe(e, 'No se pudo guardar la caja.') })
    } finally {
      setGuardando(false)
    }
  }

  return (
    <section className="panel-admin cfg-pagina">
      <div>
        <nav className="loc-migas" aria-label="Ruta">
          <span className="cfg-miga-enlace">Administración</span>
          <span aria-hidden="true">/</span>
          <span className="cfg-miga-enlace">Configuración</span>
          <span aria-hidden="true">/</span>
          <span>Cajas registradoras</span>
        </nav>
        <h1 className="cfg-titulo">Cajas registradoras</h1>
        <p className="cfg-subtitulo">Configura las cajas físicas del restaurante. El cajero elige entre ellas al abrir su turno.</p>
      </div>

      {aviso && <Aviso>{aviso}</Aviso>}
      {avisoError && <Aviso tono="error">{avisoError}</Aviso>}

      <ul className="cjr-resumen" aria-label="Resumen de cajas">
        <li>
          <span>Total cajas</span>
          <strong>{cajas.length}</strong>
        </li>
        <li className="cjr-verde">
          <span>Activas</span>
          <strong>{activas}</strong>
        </li>
        <li className="cjr-naranja">
          <span>Abiertas ahora</span>
          <strong>{abiertas}</strong>
        </li>
        <li className="cjr-rojo">
          <span>Inactivas</span>
          <strong>{cajas.length - activas}</strong>
        </li>
      </ul>

      <div className="cjr-filtros">
        <label className="cjr-buscador">
          <Search size={17} aria-hidden="true" />
          <input
            type="search"
            placeholder="Buscar por nombre o código…"
            aria-label="Buscar cajas"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </label>
        <Select etiqueta="Estado" value={estado} onChange={(e) => setEstado(e.target.value as typeof estado)}>
          <option value="todos">Todos los estados</option>
          <option value="activas">Activas</option>
          <option value="inactivas">Inactivas</option>
        </Select>
        <Boton onClick={() => setForm({ id: null, nombre: '', codigo: '', error: '' })}>
          <Plus size={16} aria-hidden="true" /> Nueva caja
        </Boton>
      </div>

      <div className="cjr-tabla-envoltorio">
        <table className="cjr-tabla">
          <thead>
            <tr>
              <th>Caja / punto de venta</th>
              <th>Ahora</th>
              <th>Principal</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {visibles.length === 0 && (
              <tr>
                <td colSpan={5} className="cjr-vacio">
                  Ninguna caja coincide con la búsqueda.
                </td>
              </tr>
            )}
            {visibles.map((c) => (
              <tr key={c.id} className={c.activo ? '' : 'cjr-inactiva'}>
                <td>
                  <div className="cjr-nombre">
                    <span className="cjr-icono" aria-hidden="true">
                      <Monitor size={20} />
                    </span>
                    <div>
                      <strong>{c.nombre}</strong>
                      {c.principal && <span className="cjr-default">Principal</span>}
                      <code className="cjr-codigo">{c.codigo}</code>
                    </div>
                  </div>
                </td>
                <td>
                  {c.turnoAbierto ? (
                    <span className="cjr-abierta">Abierta por {c.turnoAbierto.cajeroNombre ?? 'un cajero'}</span>
                  ) : (
                    <span className="cjr-cerrada">Cerrada</span>
                  )}
                </td>
                <td>
                  <Interruptor
                    etiqueta={`${c.nombre} es la caja principal`}
                    checked={c.principal}
                    disabled={c.principal || !c.activo}
                    onChange={() => ejecutar(() => definirCajaPrincipal(token, c.id), `${c.nombre} es ahora la principal`)}
                  />
                </td>
                <td>
                  <Interruptor
                    etiqueta={`${c.nombre} activa`}
                    checked={c.activo}
                    disabled={c.principal}
                    onChange={(e) =>
                      ejecutar(
                        () => (e.target.checked ? activarCaja(token, c.id) : desactivarCaja(token, c.id)),
                        e.target.checked ? 'Caja activada' : 'Caja desactivada',
                      )
                    }
                  />
                </td>
                <td>
                  <div className="cjr-acciones">
                    <button
                      type="button"
                      className="cjr-icono-boton"
                      aria-label={`Editar ${c.nombre}`}
                      onClick={() => setForm({ id: c.id, nombre: c.nombre, codigo: c.codigo, error: '' })}
                    >
                      <Pencil size={18} aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      className="cjr-icono-boton"
                      aria-label={`Borrar ${c.nombre}`}
                      disabled={c.principal}
                      title={c.principal ? 'La principal no se borra' : undefined}
                      onClick={() => setABorrar(c)}
                    >
                      <Trash2 size={18} aria-hidden="true" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <aside className="cjr-ayuda">
        <Info size={20} aria-hidden="true" />
        <div>
          <strong>Cómo se usan las cajas</strong>
          <p>
            Cada cajón físico es una caja y solo una persona puede tenerla abierta a la vez. La caja <b>Principal</b> viene seleccionada al abrir
            turno en el POS. Una caja que ya tuvo turnos no se borra: se desactiva y deja de ofrecerse, pero conserva su historial.
          </p>
        </div>
      </aside>

      {form && (
        <Modal
          abierto
          titulo={form.id ? 'Editar caja' : 'Nueva caja'}
          onCerrar={() => setForm(null)}
          pie={
            <>
              <Boton variante="secundario" onClick={() => setForm(null)}>
                Cancelar
              </Boton>
              <Boton cargando={guardando} onClick={guardar}>
                {form.id ? 'Guardar cambios' : 'Crear caja'}
              </Boton>
            </>
          }
        >
          <div className="ped-dialogo">
            {form.error && <Aviso tono="error">{form.error}</Aviso>}
            <Campo
              etiqueta="Nombre"
              value={form.nombre}
              maxLength={60}
              placeholder="Caja Barra"
              onChange={(e) => setForm({ ...form, nombre: e.target.value, error: '' })}
            />
            <Campo
              etiqueta={form.id ? 'Código' : 'Código (opcional)'}
              value={form.codigo}
              maxLength={20}
              placeholder="CAJ-BA"
              ayuda={form.id ? undefined : 'Si lo dejas vacío se asigna el siguiente (CAJ-02, CAJ-03…).'}
              onChange={(e) => setForm({ ...form, codigo: e.target.value.toUpperCase(), error: '' })}
            />
          </div>
        </Modal>
      )}

      <ModalDeConfirmacion
        abierto={aBorrar !== null}
        titulo="Borrar caja"
        mensaje={`Se borra «${aBorrar?.nombre ?? ''}». Solo se puede si nunca tuvo turnos; si ya se usó, desactívala en su lugar.`}
        textoConfirmar="Borrar caja"
        peligro
        onCancelar={() => setABorrar(null)}
        onConfirmar={async () => {
          const caja = aBorrar
          setABorrar(null)
          if (caja) await ejecutar(() => eliminarCaja(token, caja.id), 'Caja borrada')
        }}
      />
    </section>
  )
}
