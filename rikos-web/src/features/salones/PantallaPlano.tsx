import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft, Info, Lock, LockOpen, RotateCcw, Save, Square, Circle, RectangleHorizontal } from 'lucide-react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Aviso, Boton, EstadoCargando, EstadoError, IconoReactivar, Interruptor, Modal, ModalDeConfirmacion } from '../../comun/componentes'
import {
  ErrorApi,
  activarMesa,
  actualizarMesa,
  crearMesa,
  desactivarMesa,
  guardarPlano,
  listarMesas,
  obtenerSalon,
  type FormaDeMesa,
  type Mesa,
  type Salon,
} from '../../api/cliente'
import { FORMAS_DE_MESA, medidasDeForma, mensajeDeError } from './salonesVisual'
import { VistaDeMesa } from './VistaDeMesa'
import { siguienteNumero } from './PantallaMesaFormulario'

/** Una mesa del plano tal como se está editando: lo del servidor más los cambios sin guardar. */
interface MesaEnEdicion {
  id: string
  /** true si todavía no existe en el servidor (se crea al guardar el plano). */
  nueva: boolean
  numero: string
  nombre: string
  capacidad: number
  forma: FormaDeMesa
  vip: boolean
  zonaExterior: boolean
  zonaFumadores: boolean
  activo: boolean
  x: number
  y: number
  ancho: number
  alto: number
}

const desdeMesa = (m: Mesa, indice: number): MesaEnEdicion => ({
  id: m.id,
  nueva: false,
  numero: m.numero,
  nombre: m.nombre ?? '',
  capacidad: m.capacidad ?? 0,
  forma: m.forma,
  vip: m.vip,
  zonaExterior: m.zonaExterior,
  zonaFumadores: m.zonaFumadores,
  activo: m.activo,
  x: m.posX ?? 60 + (indice % 8) * 130,
  y: m.posY ?? 60 + Math.floor(indice / 8) * 130,
  ancho: m.ancho,
  alto: m.alto,
})

let contadorNuevas = 0

/**
 * Editor de plano visual de un salón: las mesas sobre el lienzo del salón. Con "Mover mesas" activo se
 * arrastran; un clic en una mesa abre sus propiedades. Nada se guarda hasta "Guardar plano".
 */
export function PantallaPlano() {
  const { salonId } = useParams<{ salonId: string }>()
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [salon, setSalon] = useState<Salon | null>(null)
  const [servidor, setServidor] = useState<MesaEnEdicion[]>([])
  const [mesas, setMesas] = useState<MesaEnEdicion[]>([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [moviendo, setMoviendo] = useState(false)
  const [seleccionada, setSeleccionada] = useState<string | null>(null)
  const [editando, setEditando] = useState<MesaEnEdicion | null>(null)
  const [erroresModal, setErroresModal] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [confirmarSalida, setConfirmarSalida] = useState(false)

  function cargar() {
    setErrorCarga('')
    Promise.all([obtenerSalon(token, salonId!), listarMesas(token, true)])
      .then(([s, todas]) => {
        setSalon(s)
        const deSalon = todas.filter((m) => m.salonId === salonId).map(desdeMesa)
        setServidor(deSalon)
        setMesas(deSalon)
      })
      .catch(() => setErrorCarga('No se pudo cargar el plano.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token, salonId])

  useEffect(() => {
    if (!mensaje) return
    const t = setTimeout(() => setMensaje(''), 2800)
    return () => clearTimeout(t)
  }, [mensaje])

  const sinGuardar = useMemo(() => JSON.stringify(mesas) !== JSON.stringify(servidor), [mesas, servidor])

  // El navegador avisa si se cierra la pestaña con cambios sin guardar.
  useEffect(() => {
    if (!sinGuardar) return
    const aviso = (e: BeforeUnloadEvent) => e.preventDefault()
    window.addEventListener('beforeunload', aviso)
    return () => window.removeEventListener('beforeunload', aviso)
  }, [sinGuardar])

  // ---- arrastre
  const arrastre = useRef<{ id: string; inicioX: number; inicioY: number; x: number; y: number; movido: boolean } | null>(null)

  function alPresionar(e: React.PointerEvent, m: MesaEnEdicion) {
    setSeleccionada(m.id)
    arrastre.current = { id: m.id, inicioX: e.clientX, inicioY: e.clientY, x: m.x, y: m.y, movido: false }
    ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
  }

  function alMover(e: React.PointerEvent) {
    const a = arrastre.current
    if (!a || !moviendo || !salon) return
    const dx = e.clientX - a.inicioX
    const dy = e.clientY - a.inicioY
    if (!a.movido && Math.abs(dx) + Math.abs(dy) < 4) return
    a.movido = true
    setMesas((actuales) =>
      actuales.map((m) =>
        m.id === a.id
          ? {
              ...m,
              x: Math.max(0, Math.min(salon.anchoPlano - m.ancho, Math.round(a.x + dx))),
              y: Math.max(0, Math.min(salon.altoPlano - m.alto, Math.round(a.y + dy))),
            }
          : m,
      ),
    )
  }

  function alSoltar(m: MesaEnEdicion) {
    const a = arrastre.current
    arrastre.current = null
    // Un clic (sin arrastrar) abre las propiedades de la mesa.
    if (a && !a.movido) {
      setErroresModal('')
      setEditando(mesas.find((x) => x.id === m.id) ?? m)
    }
  }

  function anadir(forma: FormaDeMesa) {
    if (!salon) return
    const [ancho, alto] = medidasDeForma(forma)
    // Un lugar libre: la primera casilla de la cuadrícula que no pisa ninguna mesa.
    let x = 60
    let y = 60
    for (let k = 0; k < 200; k++) {
      x = 60 + (k % 8) * 130
      y = 60 + Math.floor(k / 8) * 130
      if (!mesas.some((m) => Math.abs(m.x - x) < 120 && Math.abs(m.y - y) < 100)) break
    }
    x = Math.min(x, Math.max(0, salon.anchoPlano - ancho))
    y = Math.min(y, Math.max(0, salon.altoPlano - alto))
    const nueva: MesaEnEdicion = {
      id: `nueva-${++contadorNuevas}`,
      nueva: true,
      numero: siguienteNumero([...mesas.map((m) => m.numero)]),
      nombre: '',
      capacidad: 4,
      forma,
      vip: false,
      zonaExterior: false,
      zonaFumadores: false,
      activo: true,
      x,
      y,
      ancho,
      alto,
    }
    setMesas((a) => [...a, nueva])
    setSeleccionada(nueva.id)
    setErroresModal('')
    setEditando(nueva)
  }

  function aplicarModal() {
    if (!editando || !salon) return
    if (!editando.numero.trim() || editando.numero.trim().length > 10) return setErroresModal('El número es obligatorio (hasta 10 caracteres).')
    if (mesas.some((m) => m.id !== editando.id && m.numero === editando.numero.trim()))
      return setErroresModal('Ya hay otra mesa con ese número.')
    if (!Number.isInteger(editando.capacidad) || editando.capacidad < 0) return setErroresModal('Las sillas deben ser un número entero.')
    if (!(editando.ancho > 0) || !(editando.alto > 0)) return setErroresModal('El ancho y el alto deben ser mayores que cero.')
    const listo = {
      ...editando,
      numero: editando.numero.trim(),
      x: Math.max(0, Math.min(salon.anchoPlano - editando.ancho, Math.round(editando.x))),
      y: Math.max(0, Math.min(salon.altoPlano - editando.alto, Math.round(editando.y))),
    }
    setMesas((a) => a.map((m) => (m.id === listo.id ? listo : m)))
    setEditando(null)
  }

  function retirar() {
    if (!editando) return
    setMesas((a) =>
      editando.nueva ? a.filter((m) => m.id !== editando.id) : a.map((m) => (m.id === editando.id ? { ...m, activo: false } : m)),
    )
    setEditando(null)
  }

  async function guardar() {
    if (!salon) return
    setGuardando(true)
    setError('')
    try {
      const posiciones: { mesaId: string; x: number; y: number; ancho: number; alto: number }[] = []
      for (const m of mesas) {
        const original = servidor.find((s) => s.id === m.id)
        let id = m.id
        const datos = {
          numero: m.numero,
          nombre: m.nombre.trim() || null,
          capacidad: m.capacidad > 0 ? m.capacidad : null,
          salonId: salon.id,
          forma: m.forma,
          vip: m.vip,
          zonaExterior: m.zonaExterior,
          zonaFumadores: m.zonaFumadores,
          ancho: m.ancho,
          alto: m.alto,
        }
        if (m.nueva) {
          id = (await crearMesa(token, datos)).id
          if (!m.activo) await desactivarMesa(token, id)
        } else if (original) {
          const cambioDatos =
            original.numero !== m.numero || original.nombre !== m.nombre || original.capacidad !== m.capacidad ||
            original.forma !== m.forma || original.vip !== m.vip
          if (cambioDatos) await actualizarMesa(token, id, datos)
          if (original.activo !== m.activo) await (m.activo ? activarMesa(token, id) : desactivarMesa(token, id))
        }
        posiciones.push({ mesaId: id, x: m.x, y: m.y, ancho: m.ancho, alto: m.alto })
      }
      await guardarPlano(token, salon.id, posiciones)
      setMensaje('Plano guardado')
      cargar()
    } catch (e) {
      setError(mensajeDeError(e instanceof ErrorApi ? e.cuerpo?.error : undefined, 'No se pudo guardar el plano.'))
      // Si algo se guardó y otra cosa falló, se vuelve a leer lo real para no mostrar algo falso.
      cargar()
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga || !salon) return <EstadoError mensaje={errorCarga || 'No se pudo cargar el plano.'} accion={<Boton onClick={cargar}>Reintentar</Boton>} />

  const volver = () => navigate(`/admin/configuracion/salones/${salon.id}`)
  const activas = mesas.filter((m) => m.activo)

  return (
    <div className="plano-pagina">
      <header className="plano-barra">
        <button type="button" className="as-boton-borde" onClick={() => (sinGuardar ? setConfirmarSalida(true) : volver())}>
          <ChevronLeft size={17} aria-hidden="true" /> Volver
        </button>
        <span className="plano-separador" aria-hidden="true" />
        <div className="plano-titulo">
          <strong>{salon.nombre}</strong>
          <span>
            {activas.length} {activas.length === 1 ? 'mesa' : 'mesas'} · {salon.anchoPlano}×{salon.altoPlano} px
          </span>
        </div>
        {sinGuardar && <span className="plano-pendiente">Cambios sin guardar</span>}
        <button type="button" className={`as-boton-borde ${moviendo ? 'plano-activo' : ''}`} aria-pressed={moviendo} onClick={() => setMoviendo((v) => !v)}>
          {moviendo ? <LockOpen size={17} aria-hidden="true" /> : <Lock size={17} aria-hidden="true" />} Mover mesas
        </button>
        <button type="button" className="as-boton-borde" disabled={!sinGuardar || guardando} onClick={cargar}>
          <RotateCcw size={17} aria-hidden="true" /> Descartar
        </button>
        <Boton onClick={() => void guardar()} cargando={guardando} disabled={!sinGuardar}>
          <Save size={17} aria-hidden="true" /> Guardar plano
        </Boton>
      </header>

      {error && <Aviso tono="error">{error}</Aviso>}

      <div className="plano-contenedor">
        <div className="plano-desplazable">
          <div
            className="plano-lienzo"
            style={{ width: salon.anchoPlano, height: salon.altoPlano, backgroundColor: salon.colorPiso }}
            onPointerMove={alMover}
            onPointerDown={(e) => e.target === e.currentTarget && setSeleccionada(null)}
          >
            <div className="plano-pista">
              <Info size={15} aria-hidden="true" /> Haz clic en una mesa para ver sus propiedades · Activa «Mover mesas» para arrastrarlas
            </div>
            {mesas.filter((m) => m.activo).map((m) => (
              <VistaDeMesa
                key={m.id}
                forma={m.forma}
                etiqueta={m.nombre.trim() || m.numero}
                sillas={m.capacidad}
                ancho={m.ancho}
                alto={m.alto}
                seleccionada={seleccionada === m.id}
                style={{ position: 'absolute', left: m.x, top: m.y, cursor: moviendo ? 'grab' : 'pointer', touchAction: 'none' }}
                onPointerDown={(e) => alPresionar(e, m)}
                onPointerUp={() => alSoltar(m)}
              />
            ))}
          </div>
        </div>

        <div className="plano-herramientas" role="toolbar" aria-label="Añadir mesa">
          <span>AÑADIR MESA</span>
          {[
            { f: 'cuadrada' as const, t: 'Cuadrada', I: Square },
            { f: 'redonda' as const, t: 'Redonda', I: Circle },
            { f: 'rectangular' as const, t: 'Rectangular', I: RectangleHorizontal },
          ].map(({ f, t, I }) => (
            <button key={f} type="button" onClick={() => anadir(f)}>
              <I size={26} strokeWidth={1.5} aria-hidden="true" />
              {t}
            </button>
          ))}
        </div>
      </div>

      {mensaje && (
        <div className="loc-toast" role="status">
          <IconoReactivar width={16} height={16} aria-hidden="true" /> {mensaje}
        </div>
      )}

      <Modal
        abierto={editando != null}
        titulo={editando?.nueva ? 'Nueva mesa' : 'Propiedades de la mesa'}
        onCerrar={() => setEditando(null)}
        pie={
          editando && (
            <>
              <Boton variante="peligro" onClick={retirar} className="plano-retirar">
                {editando.nueva ? 'Quitar del plano' : 'Desactivar mesa'}
              </Boton>
              <Boton onClick={aplicarModal}>Listo</Boton>
            </>
          )
        }
      >
        {editando && (
          <div className="plano-modal">
            <label className="rf-campo">
              <span>Nombre</span>
              <input type="text" maxLength={60} placeholder="Ej. Ventana" value={editando.nombre} onChange={(e) => setEditando({ ...editando, nombre: e.target.value })} />
            </label>
            <div className="plano-par">
              <label className="rf-campo">
                <span>Ancho (px)</span>
                <input type="number" min={1} value={editando.ancho} onChange={(e) => setEditando({ ...editando, ancho: Number(e.target.value) })} />
              </label>
              <label className="rf-campo">
                <span>Alto (px)</span>
                <input type="number" min={1} value={editando.alto} onChange={(e) => setEditando({ ...editando, alto: Number(e.target.value) })} />
              </label>
            </div>
            <label className="rf-campo">
              <span>
                Número <span className="as-req">*</span>
              </span>
              <input type="text" value={editando.numero} onChange={(e) => setEditando({ ...editando, numero: e.target.value })} />
            </label>
            <div className="plano-par">
              <label className="rf-campo">
                <span>Posición X</span>
                <input type="number" min={0} value={editando.x} onChange={(e) => setEditando({ ...editando, x: Number(e.target.value) })} />
              </label>
              <label className="rf-campo">
                <span>Posición Y</span>
                <input type="number" min={0} value={editando.y} onChange={(e) => setEditando({ ...editando, y: Number(e.target.value) })} />
              </label>
            </div>
            <label className="rf-campo">
              <span>Sillas</span>
              <input type="number" min={0} value={editando.capacidad} onChange={(e) => setEditando({ ...editando, capacidad: Number(e.target.value) })} />
            </label>
            <label className="rf-campo">
              <span>Forma</span>
              <select
                value={editando.forma}
                onChange={(e) => {
                  const forma = e.target.value as FormaDeMesa
                  const [ancho, alto] = medidasDeForma(forma)
                  setEditando({ ...editando, forma, ancho, alto })
                }}
              >
                {FORMAS_DE_MESA.map((f) => (
                  <option key={f.valor} value={f.valor}>
                    {f.etiqueta}
                  </option>
                ))}
              </select>
            </label>
            <div className="plano-cajas">
              <div className="sal-rasgo">
                <div>
                  <strong>Mesa VIP</strong>
                  <span>Exclusiva</span>
                </div>
                <Interruptor etiqueta="Mesa VIP" checked={editando.vip} onChange={(e) => setEditando({ ...editando, vip: e.target.checked })} />
              </div>
              <div className="sal-rasgo">
                <div>
                  <strong>Activa</strong>
                  <span>Se muestra en el mapa del POS</span>
                </div>
                <Interruptor etiqueta="Activa" checked={editando.activo} onChange={(e) => setEditando({ ...editando, activo: e.target.checked })} />
              </div>
            </div>
            {erroresModal && <p className="campo-error plano-error">{erroresModal}</p>}
          </div>
        )}
      </Modal>

      <ModalDeConfirmacion
        abierto={confirmarSalida}
        titulo="Cambios sin guardar"
        mensaje="Si sales ahora, se pierden los cambios que hiciste en el plano."
        textoConfirmar="Salir sin guardar"
        peligro
        onConfirmar={volver}
        onCancelar={() => setConfirmarSalida(false)}
      />
    </div>
  )
}
