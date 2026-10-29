import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, ArrowLeftRight, ChevronRight, Plus, ShoppingCart, Trash2, User, UserCheck } from 'lucide-react'
import {
  Aviso,
  Boton,
  EstadoCargando,
  EstadoError,
  ModalDeConfirmacion,
  useAvisos,
} from '../../comun/componentes'
import { enSoles } from '../../comun/formato/dinero'
import {
  agregarLineaAlPedido,
  agregarPersonaAlPedido,
  anularLineaDelPedido,
  asignarPensionistaAPersona,
  vincularPreTicketAPersona,
  comandarPedido,
  listarComandasDelPedido,
  listarMesas,
  modificarLineaDelPedido,
  moverPedidoDeMesa,
  obtenerComanda,
  obtenerPedido,
  ofertaDelPedido,
  permisosPropios,
  quitarLineaDelPedido,
  quitarPersonaDelPedido,
  registrarImpresionDeComanda,
  separarMesa,
  terminarPedido,
  trasladarLineasDelPedido,
  unirMesa,
  type ComandaDetalle,
  type ComandaResumen,
  type DatosDeLinea,
  type LineaDelPedido,
  type Mesa,
  type OfertaDelDia,
  type Pedido,
  type ResumenPensionista,
} from '../../api/cliente'
import { mensajeDePedido } from './mensajes'
import { CatalogoDelPedido } from './CatalogoDelPedido'
import { nombreDeLinea } from './OrdenDeLaPersona'
import { OrdenDeLaMesa } from './OrdenDeLaMesa'
import { DialogoAnular } from './DialogoAnular'
import { DialogoExtra } from './DialogoExtra'
import { DialogoMenu } from './DialogoMenu'
import { DialogoMoverMesa } from './DialogoMoverMesa'
import { DialogoNota } from './DialogoNota'
import { DialogoPasarA } from './DialogoPasarA'
import { DialogoPensionista } from './DialogoPensionista'
import { DialogoUnirMesa } from './DialogoUnirMesa'
import { VistaComanda } from './VistaComanda'
import { DialogoTicket, TicketComanda, TicketPrecuenta } from './TicketDelPedido'

/**
 * US-028: el pedido de una mesa (o para llevar), en pantalla partida (diseno aprobado, ver
 * docs/producto/ui-ux/03-pos-rediseno.md): a la izquierda el catalogo, a la derecha la ORDEN de la persona elegida.
 * Cada escritura de la API devuelve el pedido completo con sus totales, asi que esta pantalla NO calcula nada: pinta
 * lo que llega. El precio lo decide el servidor (el mozo solo escribe el precio de un "extra"). El menu y el extra
 * abren su dialogo porque tienen opciones; un producto de la carta se agrega directo.
 */

/** "Mesa 4" se muestra "Mesa 04", como en el mapa; un nombre propio o "Mesa 4 + Mesa 5" quedan igual. */
const conNumeroDeDosCifras = (titulo: string) => titulo.replace(/^Mesa (\d)$/, 'Mesa 0$1')

const SERVICIO: Record<string, string> = { desayuno: 'Desayuno', almuerzo: 'Almuerzo', cena: 'Cena' }
type Dialogo =
  | { tipo: 'menu'; cual: 'menu_normal' | 'menu_extra' }
  | { tipo: 'extra' }
  | { tipo: 'pensionista' }
  | { tipo: 'nota'; linea: LineaDelPedido }
  | { tipo: 'anular'; linea: LineaDelPedido }
  | { tipo: 'pasar' }
  | { tipo: 'mover' }
  | { tipo: 'unir' }
  | { tipo: 'separar' }
  | { tipo: 'terminar' }
  | { tipo: 'quitarPersona'; personaId: string }
  | null

export function PantallaPedido({
  token,
  pedidoId,
  titulo,
  onVolver,
  onCobrar,
  nombreLocal = '',
}: {
  token: string
  pedidoId: string
  titulo: string
  /** Para la cabecera de la pre-cuenta. */
  nombreLocal?: string
  onVolver: () => void
  /** Lleva a la Caja a cobrar este pedido (solo se ofrece a quien tiene caja.crear). */
  onCobrar?: (pedidoId: string) => void
}) {
  const { mostrar } = useAvisos()
  const [pedido, setPedido] = useState<Pedido | null>(null)
  const [oferta, setOferta] = useState<OfertaDelDia | null>(null)
  const [personaId, setPersonaId] = useState<string | null>(null)
  const [dialogo, setDialogo] = useState<Dialogo>(null)
  const [errorDeCarga, setErrorDeCarga] = useState('')
  const [aviso, setAviso] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [comandas, setComandas] = useState<ComandaResumen[]>([])
  const [comandaAbierta, setComandaAbierta] = useState<string | null>(null)
  // El ticket en pantalla (pre-cuenta o comanda); imprimir = se abrio con el boton sin ojo.
  const [ticket, setTicket] = useState<
    | { tipo: 'precuenta'; imprimir: boolean }
    | { tipo: 'comanda'; comanda: ComandaDetalle; copia: boolean; imprimir: boolean }
    | null
  >(null)
  const [puedeAnular, setPuedeAnular] = useState(false)
  const [puedeCobrar, setPuedeCobrar] = useState(false)
  const [puedeReorganizar, setPuedeReorganizar] = useState(false)
  const [marcadas, setMarcadas] = useState<string[]>([])
  const [tituloActual, setTituloActual] = useState(titulo)
  const [claveDeMesas, setClaveDeMesas] = useState(0)
  const [mesasDelLocal, setMesasDelLocal] = useState<Mesa[]>([])
  // Orden de la mesa: null = mesa completa; si no, filtra por esa persona. El cajon es solo del celular.
  const [cajonAbierto, setCajonAbierto] = useState(false)
  // El nombre de quien se identifico en esta pantalla (el pedido solo trae su id).
  const [nombresDePensionista, setNombresDePensionista] = useState<Record<string, string>>({})

  const cargar = useCallback(() => {
    setErrorDeCarga('')
    Promise.all([
      obtenerPedido(token, pedidoId),
      ofertaDelPedido(token, pedidoId),
      listarComandasDelPedido(token, pedidoId),
      // Solo para saber que mesas estan unidas a la de este pedido; si falla, no se ofrece "Separar mesas".
      listarMesas(token).catch(() => [] as Mesa[]),
    ])
      .then(([p, o, c, m]) => {
        setPedido(p)
        setOferta(o)
        setComandas(c)
        setMesasDelLocal(m)
      })
      .catch((e) => setErrorDeCarga(mensajeDePedido(e, 'No se pudo cargar el pedido.')))
  }, [token, pedidoId])

  useEffect(() => {
    // Solo decide que botones se muestran; el servidor decide cada accion. Si falla, se ocultan.
    permisosPropios(token)
      .then((lista) => {
        setPuedeAnular(lista.includes('venta.anular'))
        setPuedeReorganizar(lista.includes('venta.modificar'))
        setPuedeCobrar(lista.includes('caja.crear'))
      })
      .catch(() => {
        setPuedeAnular(false)
        setPuedeReorganizar(false)
        setPuedeCobrar(false)
      })
  }, [token])

  useEffect(() => {
    setPedido(null)
    setOferta(null)
    setPersonaId(null)
    setMarcadas([])
    setCajonAbierto(false)
    setTituloActual(titulo)
    cargar()
  }, [cargar, titulo])

  // La persona activa: la elegida si sigue existiendo; si no, la primera.
  const persona = pedido?.personas.find((p) => p.id === personaId) ?? pedido?.personas[0] ?? null

  /** Ejecuta una escritura; el pedido que devuelve reemplaza al actual. Ante un error se recarga para no mostrar algo viejo. */
  async function ejecutar(accion: Promise<Pedido>, alExito?: (nuevo: Pedido) => void) {
    if (trabajando) return
    setTrabajando(true)
    setAviso('')
    try {
      const nuevo = await accion
      setPedido(nuevo)
      alExito?.(nuevo)
    } catch (e) {
      setAviso(mensajeDePedido(e, 'No se pudo hacer el cambio. Inténtalo de nuevo.'))
      setDialogo(null)
      cargar()
    } finally {
      setTrabajando(false)
    }
  }

  /** Manda a cocina lo pendiente de todo el pedido, avisa y abre la comanda para imprimirla. */
  async function comandar(items: number) {
    if (trabajando) return
    setTrabajando(true)
    setAviso('')
    try {
      const comanda = await comandarPedido(token, pedidoId)
      const [p, lista] = await Promise.all([obtenerPedido(token, pedidoId), listarComandasDelPedido(token, pedidoId)])
      setPedido(p)
      setComandas(lista)
      setComandaAbierta(comanda.id)
      mostrar(`Comanda ${comanda.numero} enviada (${items} ${items === 1 ? 'ítem' : 'ítems'})`)
    } catch (e) {
      setAviso(mensajeDePedido(e, 'No se pudo comandar. Inténtalo de nuevo.'))
      cargar()
    } finally {
      setTrabajando(false)
    }
  }

  /** La ultima comanda enviada de este pedido, con su detalle, o null si todavia no se envio nada. */
  async function ultimaComanda(lista: ComandaResumen[] = comandas): Promise<ComandaDetalle | null> {
    if (lista.length === 0) return null
    const ultima = lista.reduce((a, b) => (b.numero > a.numero ? b : a))
    return obtenerComanda(token, ultima.id)
  }

  async function verComanda() {
    try {
      const c = await ultimaComanda()
      if (!c) return mostrar('Todavía no se ha enviado nada a cocina.')
      setTicket({ tipo: 'comanda', comanda: c, copia: c.impresiones >= 1, imprimir: false })
    } catch (e) {
      setAviso(mensajeDePedido(e, 'No se pudo cargar la comanda.'))
    }
  }

  /**
   * "Comanda" (adenda 2026-10-29): con productos nuevos, los envia a cocina e imprime esa comanda; sin nada nuevo,
   * reimprime la ultima. Cada impresion se registra antes: desde la segunda sale con COPIA (RN-063).
   */
  async function imprimirComanda() {
    if (trabajando) return
    setTrabajando(true)
    setAviso('')
    try {
      let lista = comandas
      if (itemsNuevos > 0) {
        await comandarPedido(token, pedidoId)
        const [p, nuevas] = await Promise.all([obtenerPedido(token, pedidoId), listarComandasDelPedido(token, pedidoId)])
        setPedido(p)
        setComandas(nuevas)
        lista = nuevas
      }
      const c = await ultimaComanda(lista)
      if (!c) return mostrar('Agrega productos para enviar una comanda a cocina.')
      const registro = await registrarImpresionDeComanda(token, c.id)
      setTicket({ tipo: 'comanda', comanda: { ...c, impresiones: registro.impresiones }, copia: registro.copia, imprimir: true })
    } catch (e) {
      setAviso(mensajeDePedido(e, 'No se pudo enviar ni imprimir la comanda. Inténtalo de nuevo.'))
      cargar()
    } finally {
      setTrabajando(false)
    }
  }

  /** Desde la vista previa de la comanda, "Imprimir" tambien registra la impresion (y pone COPIA si corresponde). */
  async function registrarImpresionDelTicket() {
    if (ticket?.tipo !== 'comanda') return
    const registro = await registrarImpresionDeComanda(token, ticket.comanda.id)
    setTicket({ ...ticket, copia: registro.copia, comanda: { ...ticket.comanda, impresiones: registro.impresiones } })
  }

  function agregarLinea(datos: Omit<DatosDeLinea, 'personaId'>) {
    if (!persona) return
    ejecutar(agregarLineaAlPedido(token, pedidoId, { ...datos, personaId: persona.id }), () => setDialogo(null))
  }

  if (errorDeCarga) {
    return (
      <EstadoError
        mensaje={errorDeCarga}
        accion={
          <>
            <Boton onClick={cargar}>Reintentar</Boton>
            <Boton variante="secundario" onClick={onVolver}>
              Volver a las mesas
            </Boton>
          </>
        }
      />
    )
  }
  if (!pedido || !oferta || !persona) return <EstadoCargando />

  // Un pedido cerrado (desde el Historial) solo se consulta: ninguna accion de escritura.
  const cerrado = pedido.estado === 'cerrado'
  // Las mesas unidas a la de este pedido (el pedido es de la principal). El titulo puede traer "A + B": no se recorta.
  const unidas = pedido.mesaId ? mesasDelLocal.filter((m) => m.mesaPrincipalId === pedido.mesaId) : []
  const mesaPrincipal = pedido.mesaId ? mesasDelLocal.find((m) => m.id === pedido.mesaId) : undefined
  const tituloDeLaPrincipal = mesaPrincipal ? mesaPrincipal.nombre || `Mesa ${mesaPrincipal.numero}` : tituloActual.split(' + ')[0]

  async function refrescarMesas() {
    try {
      setMesasDelLocal(await listarMesas(token))
    } catch {
      /* se queda con la lista anterior */
    }
  }

  /**
   * Separa TODAS las mesas unidas. Si el pedido esta vacio (nadie pidio nada) se cierra solo (sin cobro) y se vuelve al
   * mapa; si ya tiene productos, los conserva y el titulo queda solo con la mesa principal.
   */
  async function separarLasMesas() {
    if (trabajando) return
    setTrabajando(true)
    setAviso('')
    try {
      for (const mesa of unidas) {
        await separarMesa(token, mesa.id)
      }
      const vacio = pedido!.personas.every((p) => p.lineas.length === 0)
      setDialogo(null)
      if (vacio) {
        await terminarPedido(token, pedidoId)
        mostrar('Mesas separadas; el pedido vacío se cerró')
        onVolver()
        return
      }
      setTituloActual(tituloDeLaPrincipal)
      await refrescarMesas()
      setClaveDeMesas((k) => k + 1)
      mostrar('Mesas separadas')
    } catch (e) {
      setAviso(mensajeDePedido(e, 'No se pudieron separar las mesas. Inténtalo de nuevo.'))
      setDialogo(null)
      cargar()
    } finally {
      setTrabajando(false)
    }
  }
  /** Quita a una persona que ya pidio algo (nada en cocina): primero sus lineas por enviar y luego la persona. */
  async function quitarPersonaConLineas(personaAQuitar: string) {
    if (trabajando) return
    setTrabajando(true)
    setAviso('')
    try {
      const dueña = pedido!.personas.find((x) => x.id === personaAQuitar)
      for (const l of dueña?.lineas ?? []) await quitarLineaDelPedido(token, pedidoId, l.id)
      const nuevo = await quitarPersonaDelPedido(token, pedidoId, personaAQuitar)
      setPedido(nuevo)
      setPersonaId(null)
      setDialogo(null)
      mostrar(`${dueña?.etiqueta ?? 'Persona'} quitada`)
    } catch (e) {
      setAviso(mensajeDePedido(e, 'No se pudo quitar a la persona. Inténtalo de nuevo.'))
      setDialogo(null)
      cargar()
    } finally {
      setTrabajando(false)
    }
  }
  const personaPorQuitar = dialogo?.tipo === 'quitarPersona' ? pedido.personas.find((x) => x.id === dialogo.personaId) : undefined
  const esPensionista = persona.tipoCliente === 'pensionista'
  // Solo cuentan las marcadas que siguen siendo de esta persona y no estan anuladas.
  const marcadasValidas = pedido.personas
    .flatMap((p) => p.lineas)
    .filter((l) => marcadas.includes(l.id) && l.estado !== 'anulado')
    .map((l) => l.id)
  // La orden es de toda la mesa: la persona de origen es la dueña de la linea marcada, no la activa.
  const duenaDeLaMarcada = pedido.personas.find((p) => p.lineas.some((l) => l.id === marcadasValidas[0]))
  const otrasPersonas = pedido.personas.filter((p) => p.id !== (duenaDeLaMarcada ?? persona).id)
  // Foto de cada producto de la carta, para las miniaturas de la orden.
  const fotos: Record<string, string> = {}
  for (const p of oferta.carta) if (p.imagen) fotos[p.id] = p.imagen
  const lineasVigentes = pedido.personas.reduce((n, p) => n + p.lineas.filter((l) => l.estado !== 'anulado').length, 0)
  const itemsNuevos = pedido.personas.reduce((n, p) => n + p.lineas.filter((l) => l.estado === 'pendiente').length, 0)
  const puedeMoverMesa = !cerrado && puedeReorganizar && pedido.canal === 'mesa' && !!pedido.mesaId
  const planDelServicio =
    esPensionista && pedido.servicio === 'desayuno'
      ? ('plan_desayuno' as const)
      : esPensionista && pedido.servicio === 'cena'
        ? ('plan_cena' as const)
        : null

  return (
    <section className="ped2 ped3 ped5">
      <header className="ped2-barra ped3-barra ped5-cabecera">
        <div className="ped5-titulo">
          <button type="button" className="ped5-volver" onClick={onVolver}>
            <ArrowLeft size={18} aria-hidden="true" />
            {pedido.canal === 'mesa' ? 'Volver a mesas' : 'Volver'}
          </button>
          <div className="ped5-nombre">
            <h1 className="ped2-titulo">{conNumeroDeDosCifras(tituloActual)}</h1>
            <span className={`ped5-estado ${cerrado ? 'ped5-estado-cerrado' : ''}`}>
              <i aria-hidden="true" />
              {cerrado ? 'Cerrado' : 'En atención'}
            </span>
          </div>
          <p className="ped5-datos" aria-label="Resumen del pedido">
            {[
              `Pedido #${pedido.numero}`,
              SERVICIO[pedido.servicio],
              `${pedido.personas.length} ${pedido.personas.length === 1 ? 'comensal' : 'comensales'}`,
              pedido.mozoNombre ? `Mozo: ${pedido.mozoNombre}` : null,
              pedido.nombreCliente ? `Cliente: ${pedido.nombreCliente}` : null,
            ]
              .filter(Boolean)
              .map((dato) => (
                <span key={dato}>{dato}</span>
              ))}
          </p>
        </div>
        {puedeMoverMesa && (
          <button type="button" className="ped5-boton-borde" onClick={() => setDialogo({ tipo: 'mover' })}>
            <ArrowLeftRight size={18} aria-hidden="true" />
            Cambiar mesa
          </button>
        )}
        {puedeMoverMesa && unidas.length > 0 && (
          <div className="ped3-separar">
            <Boton
              variante="secundario"
              className="ped3-separar-boton"
              disabled={pedido.estado !== 'abierto'}
              onClick={() => setDialogo({ tipo: 'separar' })}
            >
              Separar mesas
            </Boton>
            <span className="cta-nota">Disponible antes de comandar</span>
          </div>
        )}
      </header>

      {cerrado && <Aviso>Este pedido está cerrado: solo se puede consultar.</Aviso>}
      {aviso && <Aviso tono="error">{aviso}</Aviso>}
      {pedido.avisos.includes('TAPER_SIN_MONTO') && (
        <Aviso>Este pedido es para llevar pero todavía no se definió el precio del táper: no se está cobrando.</Aviso>
      )}

      <div className="ped2-personas ped3-personas ped5-comensales" role="tablist" aria-label="Personas del pedido">
        <span className="ped5-etiqueta">Comensales</span>
        {/* En celular la lista es una sola fila que se desliza, para que todas las tarjetas midan lo mismo. */}
        <div className="ped5-comensales-lista">
        {pedido.personas.map((p) => {
          const activa = p.id === persona.id
          const nombre = p.pensionistaId ? nombresDePensionista[p.pensionistaId] : null
          // Se puede quitar mientras nada de lo suyo haya ido a cocina: si ya pidio algo por enviar, se quita junto con eso.
          const sePuedeQuitar = !cerrado && pedido.personas.length > 1 && p.lineas.every((l) => l.estado === 'pendiente')
          return (
            <div key={p.id} className={`ped3-persona-envoltura ${activa ? 'ped3-persona-envoltura-activa' : ''}`}>
              <button
                type="button"
                role="tab"
                aria-selected={activa}
                className={`ped2-persona ${activa ? 'ped2-persona-activa' : ''}`}
                onClick={() => setPersonaId(p.id)}
              >
                {p.tipoCliente === 'pensionista' ? (
                  <UserCheck className="ped5-persona-icono pensionista" size={20} aria-hidden="true" />
                ) : (
                  <User className="ped5-persona-icono" size={20} aria-hidden="true" />
                )}
                {/* Identificada, la etiqueta ya es el nombre del pensionista: se muestra una sola vez. */}
                <span title={nombre ?? p.etiqueta}>{nombre ?? p.etiqueta}</span>
                <strong>
                  {enSoles(p.total)}
                  {p.tipoCliente === 'pensionista' && <em className="ped5-tipo"> · Pensionista</em>}
                </strong>
              </button>
              {sePuedeQuitar && (
                <button
                  type="button"
                  className="ped3-icono ped3-quitar-persona"
                  aria-label={`Quitar a ${p.etiqueta}`}
                  disabled={trabajando}
                  onClick={() =>
                    p.lineas.length === 0
                      ? ejecutar(quitarPersonaDelPedido(token, pedidoId, p.id), () => setPersonaId(null))
                      : setDialogo({ tipo: 'quitarPersona', personaId: p.id })
                  }
                >
                  <Trash2 size={16} aria-hidden="true" />
                </button>
              )}
            </div>
          )
        })}
        <button
          type="button"
          className="ped2-persona ped2-persona-nueva"
          disabled={trabajando || cerrado}
          onClick={() =>
            ejecutar(agregarPersonaAlPedido(token, pedidoId), (nuevo) =>
              setPersonaId(nuevo.personas[nuevo.personas.length - 1].id),
            )
          }
        >
          <Plus size={18} aria-hidden="true" />
          Añadir persona
        </button>
        </div>
      </div>

      <div className="ped2-cuerpo">
        <div className="ped2-catalogo">
          <CatalogoDelPedido
            oferta={oferta}
            servicio={pedido.servicio}
            esPensionista={esPensionista}
            planDelServicio={planDelServicio}
            bloqueado={cerrado}
            trabajando={trabajando}
            onMenu={(cual) => setDialogo({ tipo: 'menu', cual })}
            onPlan={(tipo) => agregarLinea({ tipo, cantidad: 1 })}
            onProducto={(p) => agregarLinea({ tipo: 'producto', productoId: p.id, cantidad: 1 })}
            onExtraLibre={() => setDialogo({ tipo: 'extra' })}
          />

          {comandas.length > 0 && (
            <section className="ped-comandas" aria-label="Comandas del pedido">
              <h3>Comandas</h3>
              <ul>
                {comandas.map((c) => (
                  <li key={c.id}>
                    <button type="button" className="ped-comanda" onClick={() => setComandaAbierta(c.id)}>
                      <strong>Comanda {c.numero}</strong>
                      <span className="cta-nota">
                        {c.impresiones === 0 ? 'Sin imprimir' : `Impresa ${c.impresiones} ${c.impresiones === 1 ? 'vez' : 'veces'}`}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>

        <OrdenDeLaMesa
          pedido={pedido}
          titulo={conNumeroDeDosCifras(tituloActual)}
          filtroId={persona.id}
          nombresDePensionista={nombresDePensionista}
          fotos={fotos}
          cerrado={cerrado}
          puedeAnular={puedeAnular}
          puedeReorganizar={puedeReorganizar}
          trabajando={trabajando}
          itemsNuevos={itemsNuevos}
          abierta={cajonAbierto}
          onCerrar={() => setCajonAbierto(false)}
          onElegirPersona={setPersonaId}
          onCantidad={(l, cantidad) =>
            ejecutar(modificarLineaDelPedido(token, pedidoId, l.id, { cantidad, observacion: l.observacion }))
          }
          onQuitar={(l) => ejecutar(quitarLineaDelPedido(token, pedidoId, l.id))}
          onNota={(l) => setDialogo({ tipo: 'nota', linea: l })}
          onAnular={(l) => setDialogo({ tipo: 'anular', linea: l })}
          onPasarLinea={(l) => {
            setMarcadas([l.id])
            setDialogo({ tipo: 'pasar' })
          }}
          onIdentificar={(id) => {
            setPersonaId(id)
            setDialogo({ tipo: 'pensionista' })
          }}
          onTerminar={() => setDialogo({ tipo: 'terminar' })}
          onComandar={() => comandar(itemsNuevos)}
          onCobrar={onCobrar && puedeCobrar ? () => onCobrar(pedidoId) : undefined}
          caja={
            puedeCobrar
              ? {
                  onVerPrecuenta: () => setTicket({ tipo: 'precuenta', imprimir: false }),
                  onPrecuenta: () => setTicket({ tipo: 'precuenta', imprimir: true }),
                  onVerComanda: () => void verComanda(),
                  onComanda: () => void imprimirComanda(),
                }
              : undefined
          }
        />
      </div>

      <div className={`ped4-fondo ${cajonAbierto ? 'ped4-fondo-abierto' : ''}`} onClick={() => setCajonAbierto(false)} aria-hidden="true" />
      <button type="button" className="ped4-carrito" onClick={() => setCajonAbierto(true)}>
        <span className="ped4-carrito-icono" aria-hidden="true">
          <ShoppingCart size={24} />
          <b>{lineasVigentes}</b>
        </span>
        <span className="ped4-carrito-texto">
          <strong>Ver orden de la mesa</strong>
          <small>
            {itemsNuevos > 0
              ? `${itemsNuevos} ${itemsNuevos === 1 ? 'ítem nuevo' : 'ítems nuevos'}`
              : `${lineasVigentes} ${lineasVigentes === 1 ? 'ítem' : 'ítems'}`}{' '}
            · {pedido.personas.length} {pedido.personas.length === 1 ? 'persona' : 'personas'}
          </small>
        </span>
        <strong className="ped4-carrito-total">{enSoles(pedido.total)}</strong>
        <ChevronRight size={20} aria-hidden="true" />
      </button>

      {ticket && (
        <DialogoTicket
          titulo={ticket.tipo === 'precuenta' ? 'Pre-cuenta' : `Comanda ${ticket.comanda.numero}`}
          imprimirAlAbrir={ticket.imprimir}
          onImprimir={ticket.tipo === 'comanda' ? registrarImpresionDelTicket : undefined}
          onCerrar={() => {
            setTicket(null)
            void listarComandasDelPedido(token, pedidoId).then(setComandas).catch(() => undefined)
          }}
        >
          {ticket.tipo === 'precuenta' ? (
            <TicketPrecuenta pedido={pedido} titulo={conNumeroDeDosCifras(tituloActual)} local={nombreLocal} />
          ) : (
            <TicketComanda comanda={ticket.comanda} copia={ticket.copia} />
          )}
        </DialogoTicket>
      )}
      {comandaAbierta && (
        <VistaComanda
          token={token}
          comandaId={comandaAbierta}
          onCerrar={() => {
            setComandaAbierta(null)
            cargar()
          }}
        />
      )}
      {dialogo?.tipo === 'pasar' && (
        <DialogoPasarA
          cantidad={marcadasValidas.length}
          personas={otrasPersonas}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onPasar={(destino) =>
            ejecutar(trasladarLineasDelPedido(token, pedidoId, marcadasValidas, destino), (nuevo) => {
              setDialogo(null)
              setMarcadas([])
              // Con una persona nueva se salta a ella para ver como quedo la cuenta dividida.
              if (destino === null) setPersonaId(nuevo.personas[nuevo.personas.length - 1].id)
              else setPersonaId(destino)
              mostrar('Productos pasados')
            })
          }
        />
      )}
      {dialogo?.tipo === 'mover' && (
        <DialogoMoverMesa
          token={token}
          mesaActualId={pedido.mesaId}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onMover={(mesa) =>
            ejecutar(moverPedidoDeMesa(token, pedidoId, mesa.id), () => {
              setDialogo(null)
              setTituloActual(`Mesa ${mesa.numero}`)
              mostrar(`Pedido movido a la mesa ${mesa.numero}`)
            })
          }
        />
      )}
      {dialogo?.tipo === 'unir' && pedido.mesaId && (
        <DialogoUnirMesa
          key={claveDeMesas}
          token={token}
          mesaActualId={pedido.mesaId}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onUnir={(mesa) =>
            ejecutar(unirMesa(token, mesa.id, pedido.mesaId!).then(() => pedido), () => {
              setClaveDeMesas((k) => k + 1)
              refrescarMesas()
              mostrar(`Mesa ${mesa.numero} unida`)
            })
          }
          onSeparar={(mesa) =>
            ejecutar(separarMesa(token, mesa.id).then(() => pedido), () => {
              setClaveDeMesas((k) => k + 1)
              refrescarMesas()
              mostrar(`Mesa ${mesa.numero} separada`)
            })
          }
        />
      )}
      <ModalDeConfirmacion
        abierto={dialogo?.tipo === 'separar'}
        titulo="Separar mesas"
        mensaje="Las mesas volverán a su lugar. Si el pedido está vacío se cierra y la mesa queda libre; si ya tiene productos, los conserva."
        textoConfirmar="Separar mesas"
        confirmando={trabajando}
        onCancelar={() => setDialogo(null)}
        onConfirmar={separarLasMesas}
      />
      <ModalDeConfirmacion
        abierto={!!personaPorQuitar}
        titulo={`Quitar a ${personaPorQuitar?.etiqueta ?? 'la persona'}`}
        mensaje={`Se quita con sus ${personaPorQuitar?.lineas.length ?? 0} productos por enviar (${enSoles(personaPorQuitar?.total ?? 0)}). Nada de esto ha ido a cocina.`}
        textoConfirmar="Quitar"
        peligro
        confirmando={trabajando}
        onCancelar={() => setDialogo(null)}
        onConfirmar={() => personaPorQuitar && quitarPersonaConLineas(personaPorQuitar.id)}
      />
      <ModalDeConfirmacion
        abierto={dialogo?.tipo === 'terminar'}
        titulo="Terminar pedido"
        mensaje="Se cierra sin cobrar y la mesa queda libre. El pedido pasa al historial y ya no se puede modificar."
        textoConfirmar="Terminar pedido"
        peligro
        confirmando={trabajando}
        onCancelar={() => setDialogo(null)}
        onConfirmar={() =>
          ejecutar(terminarPedido(token, pedidoId), () => {
            setDialogo(null)
            mostrar(`Pedido ${pedido.numero} terminado sin cobrar`)
            onVolver()
          })
        }
      />
      {dialogo?.tipo === 'anular' && (
        <DialogoAnular
          token={token}
          producto={nombreDeLinea(dialogo.linea)}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onAnular={(motivoId, observacion) =>
            ejecutar(anularLineaDelPedido(token, pedidoId, dialogo.linea.id, { motivoId, observacion }), () => {
              setDialogo(null)
              mostrar('Producto anulado')
            })
          }
        />
      )}
      {dialogo?.tipo === 'menu' && (
        <DialogoMenu
          tipo={dialogo.cual}
          oferta={oferta}
          esPensionista={esPensionista}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onAgregar={(componentes, observacion) => agregarLinea({ tipo: dialogo.cual, cantidad: 1, componentes, observacion })}
        />
      )}
      {dialogo?.tipo === 'extra' && (
        <DialogoExtra
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onAgregar={(descripcion, precio, cantidad) => agregarLinea({ tipo: 'extra_libre', descripcion, precio, cantidad })}
        />
      )}
      {dialogo?.tipo === 'pensionista' && (
        <DialogoPensionista
          token={token}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onVincularTicket={async (codigo: string) => {
            const pedidoActualizado = await vincularPreTicketAPersona(token, pedidoId, persona.id, codigo)
            setPedido(pedidoActualizado)
            setDialogo(null)
          }}
          onElegir={(p: ResumenPensionista) =>
            ejecutar(asignarPensionistaAPersona(token, pedidoId, persona.id, p.id), () => {
              setNombresDePensionista((m) => ({ ...m, [p.id]: `${p.nombres} ${p.apellidos}` }))
              setDialogo(null)
            })
          }
        />
      )}
      {dialogo?.tipo === 'nota' && (
        <DialogoNota
          inicial={dialogo.linea.observacion}
          trabajando={trabajando}
          onCerrar={() => setDialogo(null)}
          onGuardar={(nota) =>
            ejecutar(
              modificarLineaDelPedido(token, pedidoId, dialogo.linea.id, { cantidad: dialogo.linea.cantidad, observacion: nota }),
              () => setDialogo(null),
            )
          }
        />
      )}
    </section>
  )
}
