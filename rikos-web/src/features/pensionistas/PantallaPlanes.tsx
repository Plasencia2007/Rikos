import { useEffect, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Aviso,
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  EstadoVacio,
  IconoLapiz,
  IconoMas,
  Insignia,
  Modal,
} from '../../comun/componentes'
import {
  ErrorApi,
  cambiarPreciosPlan,
  crearPlan,
  listarPlanes,
  type PlanTarifario,
  type VersionPlan,
} from '../../api/cliente'
import { aCentimos, desdeCentimos, enSoles } from './dinero'
import { SimuladorDePrecios } from './SimuladorDePrecios'

/**
 * US-017. Un plan con sus tres precios por comida y su historial de
 * versiones. Cambiar un precio no reescribe la version que rige: abre una
 * nueva con su fecha de inicio (RN-021). Solo el administrador central
 * (pensionistas.administrar) crea y cambia; el resto ve.
 */

const COMIDAS = [
  { clave: 'precioDesayuno', titulo: 'Desayuno' },
  { clave: 'precioAlmuerzo', titulo: 'Almuerzo' },
  { clave: 'precioCena', titulo: 'Cena' },
] as const

type ClavePrecio = (typeof COMIDAS)[number]['clave']

const ESTADO_VERSION: Record<VersionPlan['estado'], { texto: string; tono: 'activo' | 'inactivo' | 'pendiente' }> = {
  vigente: { texto: 'Vigente', tono: 'activo' },
  programada: { texto: 'Programada', tono: 'pendiente' },
  cerrada: { texto: 'Cerrada', tono: 'inactivo' },
}

function mensajeError(e: unknown, porDefecto: string): string {
  if (!(e instanceof ErrorApi)) return porDefecto
  switch (e.cuerpo?.error) {
    case 'NOMBRE_OBLIGATORIO':
      return 'Ponle un nombre al plan.'
    case 'NOMBRE_DUPLICADO':
      return 'Ya existe un plan con ese nombre.'
    case 'PRECIO_INVALIDO':
      return 'Cada precio tiene que ser mayor que cero.'
    case 'VIGENCIA_INVALIDA':
      return 'La fecha de inicio no puede ser anterior a hoy ni a la de la última versión del plan.'
    case 'SIN_CAMBIOS':
      return 'Esos son los mismos precios que ya tiene el plan.'
    case 'NO_ENCONTRADO':
      return 'Este plan ya no existe. Recarga la página.'
    default:
      return e.estado === 403 ? 'Tus permisos no alcanzan para esto.' : porDefecto
  }
}

function fecha(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number)
  return new Date(a, m - 1, d).toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function PantallaPlanes() {
  const { sesion, tienePermiso } = useSesion()
  const token = sesion!.token
  const puedeAdministrar = tienePermiso('pensionistas.administrar')

  const [planes, setPlanes] = useState<PlanTarifario[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState<{ modo: 'nuevo' } | { modo: 'cambiar'; plan: PlanTarifario } | null>(null)

  function cargar() {
    setCargando(true)
    setError('')
    listarPlanes(token)
      .then(setPlanes)
      .catch(() => setError('No se pudieron cargar los planes.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Pensionistas</p>
          <h1>Planes tarifarios</h1>
          <p className="panel-admin-subtitulo">
            Lo que paga un pensionista por desayuno, almuerzo y cena cuando saca su ticket. El mismo plan y los mismos
            precios en todos los locales. Cambiar un precio guarda una versión nueva: lo ya cobrado no se toca.
          </p>
        </div>
        {puedeAdministrar && !cargando && !error && (
          <Boton onClick={() => setModal({ modo: 'nuevo' })}>
            <IconoMas width={16} height={16} aria-hidden="true" /> Nuevo plan
          </Boton>
        )}
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={cargar}>Reintentar</Boton>} />
      ) : planes.length === 0 ? (
        <div className="panel-admin-tarjeta">
          <EstadoVacio
            titulo="Todavía no hay planes"
            descripcion="Crea el plan de los pensionistas con el precio de cada comida. Sin plan no se puede dar de alta a ningún pensionista."
            accion={
              puedeAdministrar ? <Boton onClick={() => setModal({ modo: 'nuevo' })}>Nuevo plan</Boton> : undefined
            }
          />
        </div>
      ) : (
        <div className="plan-lista">
          {!puedeAdministrar && <Aviso tono="informacion">Puedes ver los planes, pero no cambiarlos con tus permisos.</Aviso>}
          {planes.map((p) => (
            <TarjetaPlan
              key={p.id}
              plan={p}
              puedeAdministrar={puedeAdministrar}
              onCambiar={() => setModal({ modo: 'cambiar', plan: p })}
            />
          ))}
          <SimuladorDePrecios planes={planes} />
        </div>
      )}

      {modal && (
        <ModalPlan
          token={token}
          plan={modal.modo === 'cambiar' ? modal.plan : null}
          onCancelar={() => setModal(null)}
          onGuardado={(planes) => {
            setModal(null)
            setPlanes(planes)
          }}
        />
      )}
    </section>
  )
}

function TarjetaPlan({
  plan,
  puedeAdministrar,
  onCambiar,
}: {
  plan: PlanTarifario
  puedeAdministrar: boolean
  onCambiar: () => void
}) {
  const vigente = plan.versiones.find((v) => v.estado === 'vigente')
  const programada = plan.versiones.find((v) => v.estado === 'programada')
  const mostrada = vigente ?? plan.versiones[0]
  const [verHistorial, setVerHistorial] = useState(false)

  return (
    <article className="panel-admin-tarjeta plan-tarjeta" aria-label={plan.nombre}>
      <header className="plan-cabecera">
        <div>
          <h2>{plan.nombre}</h2>
          {plan.descripcion && <p className="prod-nota">{plan.descripcion}</p>}
        </div>
        {puedeAdministrar && (
          <Boton variante="secundario" onClick={onCambiar}>
            <IconoLapiz width={15} height={15} aria-hidden="true" /> Cambiar precios
          </Boton>
        )}
      </header>

      {!vigente && programada && (
        <Aviso>Este plan todavía no rige: sus precios empiezan el {fecha(programada.vigenteDesde)}.</Aviso>
      )}

      <dl className="plan-precios">
        {COMIDAS.map((c) => (
          <div key={c.clave} className="plan-precio">
            <dt>{c.titulo}</dt>
            <dd className={mostrada[c.clave] == null ? 'plan-precio-vacio' : ''}>{enSoles(mostrada[c.clave])}</dd>
          </div>
        ))}
      </dl>

      {vigente && programada && (
        <Aviso tono="informacion">
          Desde el {fecha(programada.vigenteDesde)} rigen otros precios: {enSoles(programada.precioDesayuno)} desayuno,{' '}
          {enSoles(programada.precioAlmuerzo)} almuerzo y {enSoles(programada.precioCena)} cena.
        </Aviso>
      )}

      <button type="button" className="plan-historial-boton" onClick={() => setVerHistorial(!verHistorial)}>
        {verHistorial ? 'Ocultar historial' : `Ver historial (${plan.versiones.length})`}
      </button>
      {verHistorial && (
        <table className="prod-tabla plan-historial">
          <thead>
            <tr>
              <th>Rige</th>
              <th>Estado</th>
              <th className="prod-num">Desayuno</th>
              <th className="prod-num">Almuerzo</th>
              <th className="prod-num">Cena</th>
            </tr>
          </thead>
          <tbody>
            {plan.versiones.map((v) => (
              <tr key={v.vigenteDesde}>
                <td>
                  {fecha(v.vigenteDesde)}
                  {v.vigenteHasta ? ` al ${fecha(v.vigenteHasta)}` : ' en adelante'}
                </td>
                <td>
                  <Insignia tono={ESTADO_VERSION[v.estado].tono}>{ESTADO_VERSION[v.estado].texto}</Insignia>
                </td>
                <td className="prod-num">{enSoles(v.precioDesayuno)}</td>
                <td className="prod-num">{enSoles(v.precioAlmuerzo)}</td>
                <td className="prod-num">{enSoles(v.precioCena)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </article>
  )
}

function ModalPlan({
  token,
  plan,
  onCancelar,
  onGuardado,
}: {
  token: string
  plan: PlanTarifario | null
  onCancelar: () => void
  onGuardado: (planes: PlanTarifario[]) => void
}) {
  const actual = plan ? (plan.versiones.find((v) => v.estado === 'vigente') ?? plan.versiones[0]) : null
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [precios, setPrecios] = useState<Record<ClavePrecio, string>>({
    precioDesayuno: desdeCentimos(actual?.precioDesayuno ?? null),
    precioAlmuerzo: desdeCentimos(actual?.precioAlmuerzo ?? null),
    precioCena: desdeCentimos(actual?.precioCena ?? null),
  })
  const [desde, setDesde] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar() {
    const valores = {} as Record<ClavePrecio, number | null>
    for (const c of COMIDAS) {
      const v = aCentimos(precios[c.clave])
      if (Number.isNaN(v)) {
        setError(`El precio de ${c.titulo.toLowerCase()} no es un monto válido. Usa un número mayor que cero, o déjalo vacío.`)
        return
      }
      valores[c.clave] = v
    }
    if (!plan && !nombre.trim()) {
      setError('Ponle un nombre al plan.')
      return
    }
    setGuardando(true)
    setError('')
    try {
      const vigenteDesde = desde || null
      if (plan) {
        await cambiarPreciosPlan(token, plan.id, { ...valores, vigenteDesde })
      } else {
        await crearPlan(token, { ...valores, nombre: nombre.trim(), descripcion: descripcion.trim() || null, vigenteDesde })
      }
      onGuardado(await listarPlanes(token))
    } catch (e) {
      setError(mensajeError(e, 'No se pudo guardar el plan.'))
      setGuardando(false)
    }
  }

  return (
    <Modal
      abierto
      titulo={plan ? `Cambiar precios · ${plan.nombre}` : 'Nuevo plan tarifario'}
      onCerrar={onCancelar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCancelar} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton cargando={guardando} onClick={guardar}>
            {plan ? 'Guardar precios' : 'Crear plan'}
          </Boton>
        </>
      }
    >
      {!plan && (
        <>
          <Campo etiqueta="Nombre" value={nombre} autoFocus placeholder="Ej. Plan pensionista" onChange={(e) => setNombre(e.target.value)} />
          <Campo etiqueta="Descripción (opcional)" value={descripcion} maxLength={250} onChange={(e) => setDescripcion(e.target.value)} />
        </>
      )}
      <div className="plan-campos-precios">
        {COMIDAS.map((c) => (
          <Campo
            key={c.clave}
            etiqueta={c.titulo}
            inputMode="decimal"
            placeholder="0.00"
            value={precios[c.clave]}
            icono={<span aria-hidden="true">S/</span>}
            onChange={(e) => setPrecios({ ...precios, [c.clave]: e.target.value })}
          />
        ))}
      </div>
      <p className="prod-nota">Una comida sin precio queda "sin precio": la caja avisa y no inventa uno.</p>
      <Campo
        etiqueta="Rige desde (opcional)"
        type="date"
        value={desde}
        onChange={(e) => setDesde(e.target.value)}
        ayuda={
          plan
            ? 'Vacío = desde hoy. Una fecha futura programa el cambio; el precio actual sigue hasta el día anterior.'
            : 'Vacío = desde hoy.'
        }
        error={error || undefined}
      />
    </Modal>
  )
}
