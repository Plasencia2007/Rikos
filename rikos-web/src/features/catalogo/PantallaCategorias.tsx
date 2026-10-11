import { useEffect, useMemo, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import {
  Boton,
  Campo,
  EstadoCargando,
  EstadoError,
  IconoArrastrar,
  IconoBorrar,
  IconoBusqueda,
  IconoLapiz,
  IconoMas,
  ModalDeConfirmacion,
} from '../../comun/componentes'
import {
  ErrorApi,
  actualizarCategoria,
  crearCategoria,
  eliminarCategoria,
  listarCategorias,
  reordenarCategorias,
  type Categoria,
} from '../../api/cliente'

// Las 5 naturalezas y las 5 partes del menu son fijas -- las define el
// sistema, no el usuario (ver CrearCategoria.NATURALEZAS_VALIDAS y el
// CHECK de componente_menu en V14__catalogo.sql). El usuario solo crea
// categorias y subcategorias DENTRO de estos tipos.
const NATURALEZAS: { valor: string; titulo: string }[] = [
  { valor: 'componente_menu', titulo: 'Componente de menú' },
  { valor: 'producto_fijo', titulo: 'Producto fijo' },
  { valor: 'extra', titulo: 'Extra' },
  { valor: 'insumo', titulo: 'Insumo' },
  { valor: 'subreceta', titulo: 'Subreceta' },
]

const PARTES_MENU = [
  { valor: 'entrada', titulo: 'Entradas', etiqueta: 'entrada' },
  { valor: 'segundo', titulo: 'Segundos', etiqueta: 'segundo' },
  { valor: 'guarnicion', titulo: 'Guarniciones', etiqueta: 'guarnición' },
  { valor: 'refresco', titulo: 'Refrescos', etiqueta: 'refresco' },
  { valor: 'postre', titulo: 'Postres', etiqueta: 'postre' },
] as const

const OTROS_TIPOS = [
  { valor: 'producto_fijo', titulo: 'Platos de carta', desc: 'Se venden todos los días, fuera del menú.' },
  { valor: 'extra', titulo: 'Extras', desc: 'Adicionales que se cobran aparte.' },
  { valor: 'insumo', titulo: 'Insumos', desc: 'Lo que se compra para cocinar. No se vende.' },
  { valor: 'subreceta', titulo: 'Subrecetas', desc: 'Bases que se usan dentro de otras recetas.' },
] as const

const PARTES_MENU_VALORES = new Set<string>(PARTES_MENU.map((p) => p.valor))

function esParteDeMenu(tipo: string) {
  return PARTES_MENU_VALORES.has(tipo)
}

function naturalezaDelTipo(tipo: string) {
  return esParteDeMenu(tipo) ? 'componente_menu' : tipo
}

function componenteMenuDelTipo(tipo: string) {
  return esParteDeMenu(tipo) ? tipo : undefined
}

const TITULO_COMPONENTE: Record<string, string> = {
  entrada: 'Entrada',
  segundo: 'Segundo',
  guarnicion: 'Guarnición',
  refresco: 'Refresco',
  postre: 'Postre',
}

function tituloNaturaleza(naturaleza: string) {
  return NATURALEZAS.find((n) => n.valor === naturaleza)?.titulo ?? naturaleza
}

function mensajeError(error: unknown, porDefecto: string): string {
  if (error instanceof ErrorApi) {
    switch (error.cuerpo?.error) {
      case 'NOMBRE_DUPLICADO':
        return 'Ya existe una categoría con ese nombre ahí.'
      case 'PADRE_TIENE_PRODUCTOS':
        return 'Esta categoría ya tiene productos propios: no se le pueden agregar subcategorías.'
      case 'PADRE_NO_ES_RAIZ':
        return 'Una subcategoría no puede tener subcategorías propias.'
      case 'CATEGORIA_TIENE_SUBCATEGORIAS':
        return 'No se puede eliminar: todavía tiene subcategorías. Elimínalas primero.'
      case 'CATEGORIA_TIENE_PRODUCTOS':
        return 'No se puede eliminar: tiene productos asignados.'
      case 'CATEGORIA_PADRE_NO_ENCONTRADA':
      case 'NO_ENCONTRADO':
        return 'Esta categoría ya no existe. Recarga la página.'
      default:
        return porDefecto
    }
  }
  return porDefecto
}

export function PantallaCategorias() {
  const { sesion } = useSesion()
  const token = sesion!.token

  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [seleccionId, setSeleccionId] = useState<string | null>(null)
  const [vista, setVista] = useState<'lista' | 'nueva'>('lista')

  function cargar(seleccionarId?: string) {
    setCargando(true)
    setError('')
    listarCategorias(token)
      .then((datos) => {
        setCategorias(datos)
        if (seleccionarId) setSeleccionId(seleccionarId)
      })
      .catch(() => setError('No se pudieron cargar las categorías.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const raices = useMemo(() => categorias.filter((c) => c.categoriaPadreId === null), [categorias])
  const hijasDe = (padreId: string) => categorias.filter((c) => c.categoriaPadreId === padreId)

  const grupos = useMemo(() => {
    const filtro = busqueda.trim().toLowerCase()
    return NATURALEZAS.map((n) => ({
      ...n,
      items: raices
        .filter((r) => r.naturaleza === n.valor)
        .filter((r) => !filtro || r.nombre.toLowerCase().includes(filtro)),
    }))
  }, [raices, busqueda])

  useEffect(() => {
    if (seleccionId || raices.length === 0) return
    setSeleccionId(raices[0].id)
  }, [raices, seleccionId])

  const actual = raices.find((r) => r.id === seleccionId) ?? null

  function abrirNueva() {
    setVista('nueva')
  }

  function volverALista(idSeleccionado?: string) {
    setVista('lista')
    if (idSeleccionado) setSeleccionId(idSeleccionado)
  }

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <div>
          <p className="categorias-migaja">Catálogo y recetas</p>
          <h1>Categorías</h1>
          <p className="panel-admin-subtitulo">Agrupa los productos como aparecen en la carta y en el menú del día.</p>
        </div>
        {vista === 'lista' && (
          <Boton onClick={abrirNueva}>
            <IconoMas width={16} height={16} aria-hidden="true" /> Nueva categoría
          </Boton>
        )}
      </div>

      {cargando ? (
        <EstadoCargando />
      ) : error ? (
        <EstadoError mensaje={error} accion={<Boton onClick={() => cargar()}>Reintentar</Boton>} />
      ) : (
        <div className="categorias-layout">
          <ListaCategorias
            grupos={grupos}
            busqueda={busqueda}
            onBuscar={setBusqueda}
            seleccionId={vista === 'lista' ? seleccionId : null}
            onElegir={(id) => {
              setSeleccionId(id)
              setVista('lista')
            }}
            hijasDe={hijasDe}
          />

          {vista === 'nueva' ? (
            <PanelNuevaCategoria
              token={token}
              totalRaices={raices.length}
              categorias={categorias}
              onCreada={(id) => {
                cargar(id)
                setVista('lista')
              }}
              onCancelar={() => volverALista()}
            />
          ) : actual ? (
            <PanelDetalleCategoria
              key={actual.id}
              token={token}
              categoria={actual}
              subcategorias={hijasDe(actual.id)}
              onCambio={() => cargar(actual.id)}
              onEliminada={() => {
                setSeleccionId(null)
                cargar()
              }}
            />
          ) : (
            <div className="panel-admin-tarjeta categorias-detalle-vacio">
              <p>Crea tu primera categoría para empezar a organizar el catálogo.</p>
              <Boton onClick={abrirNueva}>Nueva categoría</Boton>
            </div>
          )}
        </div>
      )}
    </section>
  )
}

// ---- Columna izquierda: el arbol, agrupado por naturaleza -----------------

interface Grupo {
  valor: string
  titulo: string
  items: Categoria[]
}

function ListaCategorias({
  grupos,
  busqueda,
  onBuscar,
  seleccionId,
  onElegir,
  hijasDe,
}: {
  grupos: Grupo[]
  busqueda: string
  onBuscar: (v: string) => void
  seleccionId: string | null
  onElegir: (id: string) => void
  hijasDe: (padreId: string) => Categoria[]
}) {
  return (
    <div className="panel-admin-tarjeta categorias-lista">
      <div className="categorias-lista-buscador">
        <Campo
          etiqueta="Buscar categoría"
          icono={<IconoBusqueda width={16} height={16} aria-hidden="true" />}
          placeholder="Buscar categoría"
          value={busqueda}
          onChange={(e) => onBuscar(e.target.value)}
        />
      </div>
      <div className="categorias-lista-cuerpo">
        {grupos.map((g) => (
          <div key={g.valor} className="categorias-grupo">
            <div className="categorias-grupo-titulo">{g.titulo}</div>
            {g.items.length === 0 ? (
              <p className="categorias-grupo-vacio">Sin categorías</p>
            ) : (
              g.items.map((r) => {
                const activa = r.id === seleccionId
                return (
                  <button
                    key={r.id}
                    type="button"
                    className={`categorias-fila ${activa ? 'categorias-fila-activa' : ''}`}
                    aria-current={activa || undefined}
                    onClick={() => onElegir(r.id)}
                  >
                    <span className="categorias-fila-nombre">{r.nombre}</span>
                    <span className="categorias-fila-total">{hijasDe(r.id).length}</span>
                  </button>
                )
              })
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

// ---- Columna derecha, modo "ver": detalle + subcategorias -----------------

function PanelDetalleCategoria({
  token,
  categoria,
  subcategorias,
  onCambio,
  onEliminada,
}: {
  token: string
  categoria: Categoria
  subcategorias: Categoria[]
  onCambio: () => void
  onEliminada: () => void
}) {
  const [renombrando, setRenombrando] = useState(false)
  const [nombreEdit, setNombreEdit] = useState(categoria.nombre)
  const [guardandoNombre, setGuardandoNombre] = useState(false)
  const [errorNombre, setErrorNombre] = useState('')
  const [confirmandoBorrado, setConfirmandoBorrado] = useState(false)
  const [eliminando, setEliminando] = useState(false)
  const [errorEliminar, setErrorEliminar] = useState('')

  const motivoBloqueo =
    subcategorias.length > 0
      ? `Tiene ${subcategorias.length} subcategoría${subcategorias.length === 1 ? '' : 's'}. Elimínalas primero.`
      : categoria.totalProductos > 0
        ? 'Tiene productos asignados.'
        : null

  async function confirmarEliminar() {
    setEliminando(true)
    setErrorEliminar('')
    try {
      await eliminarCategoria(token, categoria.id)
      setConfirmandoBorrado(false)
      onEliminada()
    } catch (e) {
      setErrorEliminar(mensajeError(e, 'No se pudo eliminar la categoría.'))
    } finally {
      setEliminando(false)
    }
  }

  async function guardarNombre() {
    const limpio = nombreEdit.trim()
    if (!limpio || limpio === categoria.nombre) {
      setRenombrando(false)
      setNombreEdit(categoria.nombre)
      return
    }
    setGuardandoNombre(true)
    setErrorNombre('')
    try {
      await actualizarCategoria(token, categoria.id, { nombre: limpio, orden: categoria.orden })
      setRenombrando(false)
      onCambio()
    } catch (e) {
      setErrorNombre(mensajeError(e, 'No se pudo renombrar la categoría.'))
    } finally {
      setGuardandoNombre(false)
    }
  }

  return (
    <div className="panel-admin-tarjeta categorias-detalle">
      <div className="categorias-detalle-encabezado">
        <div className="categorias-detalle-titulo">
          {renombrando ? (
            <div className="categorias-detalle-renombrar">
              <Campo
                etiqueta="Nombre de la categoría"
                value={nombreEdit}
                autoFocus
                onChange={(e) => setNombreEdit(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') guardarNombre()
                  if (e.key === 'Escape') {
                    setRenombrando(false)
                    setNombreEdit(categoria.nombre)
                  }
                }}
                error={errorNombre || undefined}
              />
              <Boton tamano="tactil" cargando={guardandoNombre} onClick={guardarNombre}>
                Guardar
              </Boton>
            </div>
          ) : (
            <>
              <h2>{categoria.nombre}</h2>
              <p className="categorias-detalle-tipo">
                Naturaleza: <strong>{tituloNaturaleza(categoria.naturaleza)}</strong>
                {categoria.componenteMenu && (
                  <>
                    {' '}
                    · Va en el menú como: <strong>{TITULO_COMPONENTE[categoria.componenteMenu] ?? categoria.componenteMenu}</strong>
                  </>
                )}
              </p>
            </>
          )}
        </div>
        {!renombrando && (
          <div className="categorias-detalle-acciones">
            <Boton
              variante="secundario"
              onClick={() => {
                setNombreEdit(categoria.nombre)
                setRenombrando(true)
              }}
            >
              <IconoLapiz width={15} height={15} aria-hidden="true" /> Editar categoría
            </Boton>
            <button
              type="button"
              className="categorias-detalle-eliminar"
              disabled={Boolean(motivoBloqueo)}
              title={motivoBloqueo ?? 'Eliminar categoría'}
              aria-label="Eliminar categoría"
              onClick={() => {
                setErrorEliminar('')
                setConfirmandoBorrado(true)
              }}
            >
              <IconoBorrar width={17} height={17} aria-hidden="true" />
            </button>
          </div>
        )}
      </div>

      <ListaSubcategorias
        token={token}
        categoriaPadre={categoria}
        subcategorias={subcategorias}
        onCambio={onCambio}
      />

      <ModalDeConfirmacion
        abierto={confirmandoBorrado}
        titulo="Eliminar categoría"
        mensaje={`¿Eliminar «${categoria.nombre}»? Esta acción no se puede deshacer.`}
        textoConfirmar="Eliminar"
        peligro
        confirmando={eliminando}
        error={errorEliminar || undefined}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setConfirmandoBorrado(false)}
      />
    </div>
  )
}

// ---- Subcategorias: agregar, renombrar y arrastrar para reordenar --------

function ListaSubcategorias({
  token,
  categoriaPadre,
  subcategorias,
  onCambio,
}: {
  token: string
  categoriaPadre: Categoria
  subcategorias: Categoria[]
  onCambio: () => void
}) {
  const [texto, setTexto] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorAgregar, setErrorAgregar] = useState('')
  const [editandoId, setEditandoId] = useState<string | null>(null)
  const [textoEdit, setTextoEdit] = useState('')
  const [arrastradaId, setArrastradaId] = useState<string | null>(null)
  const [ordenLocal, setOrdenLocal] = useState<Categoria[] | null>(null)
  const [guardandoOrden, setGuardandoOrden] = useState(false)
  const [paraEliminar, setParaEliminar] = useState<Categoria | null>(null)
  const [eliminando, setEliminando] = useState(false)
  const [errorEliminar, setErrorEliminar] = useState('')

  const lista = ordenLocal ?? subcategorias

  async function confirmarEliminar() {
    if (!paraEliminar) return
    setEliminando(true)
    setErrorEliminar('')
    try {
      await eliminarCategoria(token, paraEliminar.id)
      setParaEliminar(null)
      onCambio()
    } catch (e) {
      setErrorEliminar(mensajeError(e, 'No se pudo eliminar la subcategoría.'))
    } finally {
      setEliminando(false)
    }
  }

  async function agregar() {
    const limpio = texto.trim()
    if (!limpio) return
    if (subcategorias.some((s) => s.nombre.toLowerCase() === limpio.toLowerCase())) {
      setErrorAgregar(`Ya existe «${limpio}» en ${categoriaPadre.nombre}.`)
      return
    }
    setGuardando(true)
    setErrorAgregar('')
    try {
      await crearCategoria(token, {
        nombre: limpio,
        categoriaPadreId: categoriaPadre.id,
        orden: subcategorias.length + 1,
      })
      setTexto('')
      onCambio()
    } catch (e) {
      setErrorAgregar(mensajeError(e, 'No se pudo agregar la subcategoría.'))
    } finally {
      setGuardando(false)
    }
  }

  async function guardarRenombre(sub: Categoria) {
    const limpio = textoEdit.trim()
    setEditandoId(null)
    if (!limpio || limpio === sub.nombre) return
    try {
      await actualizarCategoria(token, sub.id, { nombre: limpio, orden: sub.orden })
      onCambio()
    } catch {
      // El renombre no explota la pantalla: si falla, la fila se queda con
      // el nombre anterior porque onCambio() no se llamo.
    }
  }

  function soltar(destino: Categoria) {
    if (!arrastradaId || arrastradaId === destino.id) {
      setArrastradaId(null)
      return
    }
    const base = [...lista]
    const desdeIdx = base.findIndex((c) => c.id === arrastradaId)
    const hastaIdx = base.findIndex((c) => c.id === destino.id)
    if (desdeIdx === -1 || hastaIdx === -1) {
      setArrastradaId(null)
      return
    }
    const [movida] = base.splice(desdeIdx, 1)
    base.splice(hastaIdx, 0, movida)
    setOrdenLocal(base)
    setArrastradaId(null)
    setGuardandoOrden(true)
    reordenarCategorias(token, { categoriaPadreId: categoriaPadre.id, ids: base.map((c) => c.id) })
      .then(onCambio)
      .catch(() => setOrdenLocal(null))
      .finally(() => setGuardandoOrden(false))
  }

  return (
    <div className="categorias-subcategorias">
      <div className="categorias-subcategorias-encabezado">
        <span className="categorias-subcategorias-titulo">
          Subcategorías <span className="categorias-subcategorias-contador">({lista.length})</span>
        </span>
        {lista.length > 1 && (
          <span className="categorias-subcategorias-ayuda">
            {guardandoOrden ? 'Guardando orden…' : 'Arrastra para cambiar el orden'}
          </span>
        )}
      </div>

      {lista.length === 0 ? (
        <p className="categorias-subcategorias-vacio">
          Todavía no tiene subcategorías. Son opcionales: si no agregas ninguna, los productos se asignan directo a
          «{categoriaPadre.nombre}».
        </p>
      ) : (
        <div className="categorias-subcategorias-lista">
          {lista.map((s, i) => (
            <div
              key={s.id}
              className={`categorias-subcategoria-fila ${arrastradaId === s.id ? 'categorias-subcategoria-arrastrando' : ''}`}
              draggable
              onDragStart={() => setArrastradaId(s.id)}
              onDragOver={(e) => e.preventDefault()}
              onDrop={() => soltar(s)}
              onDragEnd={() => setArrastradaId(null)}
            >
              <span className="categorias-subcategoria-manija" aria-hidden="true">
                <IconoArrastrar width={16} height={16} />
              </span>
              <span className="categorias-subcategoria-orden">{i + 1}</span>
              {editandoId === s.id ? (
                <input
                  className="categorias-subcategoria-input"
                  value={textoEdit}
                  autoFocus
                  onChange={(e) => setTextoEdit(e.target.value)}
                  onBlur={() => guardarRenombre(s)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') guardarRenombre(s)
                    if (e.key === 'Escape') setEditandoId(null)
                  }}
                />
              ) : (
                <span className="categorias-subcategoria-nombre">{s.nombre}</span>
              )}
              <button
                type="button"
                className="categorias-subcategoria-editar"
                aria-label={`Renombrar ${s.nombre}`}
                onClick={() => {
                  setEditandoId(s.id)
                  setTextoEdit(s.nombre)
                }}
              >
                <IconoLapiz width={15} height={15} aria-hidden="true" />
              </button>
              <button
                type="button"
                className="categorias-subcategoria-editar categorias-subcategoria-eliminar"
                disabled={s.totalProductos > 0}
                title={s.totalProductos > 0 ? 'Tiene productos asignados.' : `Eliminar ${s.nombre}`}
                aria-label={`Eliminar ${s.nombre}`}
                onClick={() => {
                  setErrorEliminar('')
                  setParaEliminar(s)
                }}
              >
                <IconoBorrar width={15} height={15} aria-hidden="true" />
              </button>
            </div>
          ))}
        </div>
      )}

      {lista.length > 0 && (
        <p className="categorias-subcategorias-nota">
          Al tener subcategorías, los productos se asignan a ellas y no a «{categoriaPadre.nombre}» directamente.
        </p>
      )}

      <div className="categorias-subcategoria-agregar">
        <Campo
          etiqueta="Nueva subcategoría"
          placeholder="Nombre de la nueva subcategoría"
          value={texto}
          onChange={(e) => {
            setTexto(e.target.value)
            setErrorAgregar('')
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              agregar()
            }
          }}
          error={errorAgregar || undefined}
        />
        <Boton variante="secundario" cargando={guardando} onClick={agregar}>
          <IconoMas width={16} height={16} aria-hidden="true" /> Agregar
        </Boton>
      </div>

      <ModalDeConfirmacion
        abierto={paraEliminar !== null}
        titulo="Eliminar subcategoría"
        mensaje={`¿Eliminar «${paraEliminar?.nombre}»? Esta acción no se puede deshacer.`}
        textoConfirmar="Eliminar"
        peligro
        confirmando={eliminando}
        error={errorEliminar || undefined}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setParaEliminar(null)}
      />
    </div>
  )
}

// ---- Columna derecha, modo "nueva": crear categoria, luego subcategorias --

function PanelNuevaCategoria({
  token,
  totalRaices,
  categorias,
  onCreada,
  onCancelar,
}: {
  token: string
  totalRaices: number
  categorias: Categoria[]
  onCreada: (id: string) => void
  onCancelar: () => void
}) {
  const [paso, setPaso] = useState<1 | 2>(1)
  const [nombre, setNombre] = useState('')
  const [tipo, setTipo] = useState<string>('refresco')
  const [creando, setCreando] = useState(false)
  const [errorCrear, setErrorCrear] = useState('')
  const [creada, setCreada] = useState<Categoria | null>(null)

  const parteElegida = PARTES_MENU.find((p) => p.valor === tipo)
  const existente = parteElegida
    ? categorias.find((c) => c.categoriaPadreId === null && c.componenteMenu === tipo)
    : null
  const otroElegido = OTROS_TIPOS.find((o) => o.valor === tipo)

  async function crear() {
    const limpio = nombre.trim()
    if (!limpio) {
      setErrorCrear('Ponle un nombre a la categoría.')
      return
    }
    setCreando(true)
    setErrorCrear('')
    try {
      const nueva = await crearCategoria(token, {
        nombre: limpio,
        naturaleza: naturalezaDelTipo(tipo),
        componenteMenu: componenteMenuDelTipo(tipo),
        orden: totalRaices + 1,
      })
      setCreada(nueva)
      setPaso(2)
    } catch (e) {
      setErrorCrear(mensajeError(e, 'No se pudo crear la categoría.'))
    } finally {
      setCreando(false)
    }
  }

  if (paso === 2 && creada) {
    return (
      <div className="panel-admin-tarjeta categorias-detalle">
        <div className="categorias-detalle-encabezado">
          <div className="categorias-detalle-titulo">
            <h2>{creada.nombre}</h2>
            <p className="categorias-detalle-tipo">
              Naturaleza: <strong>{tituloNaturaleza(creada.naturaleza)}</strong>
              {creada.componenteMenu && (
                <>
                  {' '}
                  · Va en el menú como: <strong>{TITULO_COMPONENTE[creada.componenteMenu] ?? creada.componenteMenu}</strong>
                </>
              )}
            </p>
          </div>
          <PasosNueva paso={2} />
        </div>

        <ListaSubcategorias
          token={token}
          categoriaPadre={creada}
          subcategorias={[]}
          onCambio={() => onCreada(creada.id)}
        />

        <div className="categorias-nueva-pie">
          <span className="categorias-nueva-pie-nota">Cada subcategoría se guarda al agregarla.</span>
          <Boton variante="secundario" onClick={() => onCreada(creada.id)}>
            Listo
          </Boton>
        </div>
      </div>
    )
  }

  return (
    <div className="panel-admin-tarjeta categorias-detalle">
      <div className="categorias-detalle-encabezado">
        <h2>Nueva categoría</h2>
        <PasosNueva paso={1} />
      </div>

      <div className="categorias-nueva-cuerpo">
        <Campo
          etiqueta="Nombre"
          placeholder="Ej. Bebidas frías"
          value={nombre}
          autoFocus
          onChange={(e) => setNombre(e.target.value)}
        />

        <fieldset className="categorias-nueva-tipo">
          <legend>¿Qué va a agrupar?</legend>

          <span className="categorias-nueva-subtitulo">Menú del día</span>
          <div className="categorias-nueva-grid categorias-nueva-grid-menu" role="radiogroup" aria-label="Parte del menú">
            {PARTES_MENU.map((p) => (
              <button
                key={p.valor}
                type="button"
                role="radio"
                aria-checked={tipo === p.valor}
                className={`categorias-nueva-opcion ${tipo === p.valor ? 'categorias-nueva-opcion-activa' : ''}`}
                onClick={() => setTipo(p.valor)}
              >
                {p.titulo}
              </button>
            ))}
          </div>

          <span className="categorias-nueva-subtitulo">Otros</span>
          <div className="categorias-nueva-grid categorias-nueva-grid-otros" role="radiogroup" aria-label="Otro tipo">
            {OTROS_TIPOS.map((o) => (
              <button
                key={o.valor}
                type="button"
                role="radio"
                aria-checked={tipo === o.valor}
                className={`categorias-nueva-opcion categorias-nueva-opcion-larga ${tipo === o.valor ? 'categorias-nueva-opcion-activa' : ''}`}
                onClick={() => setTipo(o.valor)}
              >
                <span className="categorias-nueva-opcion-titulo">{o.titulo}</span>
                <span className="categorias-nueva-opcion-desc">{o.desc}</span>
              </button>
            ))}
          </div>

          <p className="categorias-nueva-explica">
            {parteElegida
              ? existente
                ? `Ya tienes «${existente.nombre}» como ${parteElegida.etiqueta}. La nueva se mostrará junto a ella en el menú.`
                : `Sus platos se podrán elegir como ${parteElegida.etiqueta} al armar el menú semanal.`
              : otroElegido?.desc}
          </p>
        </fieldset>

        <div className="categorias-nueva-aviso">
          El tipo no se puede cambiar después, porque los productos dependen de él. Las subcategorías las agregas en
          el siguiente paso.
        </div>

        {errorCrear && <p className="campo-error">{errorCrear}</p>}
      </div>

      <div className="categorias-nueva-pie">
        <Boton variante="secundario" onClick={onCancelar}>
          Cancelar
        </Boton>
        <Boton cargando={creando} onClick={crear}>
          Crear y agregar subcategorías
        </Boton>
      </div>
    </div>
  )
}

function PasosNueva({ paso }: { paso: 1 | 2 }) {
  return (
    <ol className="categorias-nueva-pasos">
      <li className={paso === 1 ? 'categorias-nueva-paso-actual' : 'categorias-nueva-paso-listo'}>
        <span className="categorias-nueva-paso-numero">{paso === 1 ? '1' : '✓'}</span>
        Categoría
      </li>
      <li aria-hidden="true" className="categorias-nueva-paso-linea" />
      <li className={paso === 2 ? 'categorias-nueva-paso-actual' : ''}>
        <span className="categorias-nueva-paso-numero">2</span>
        Subcategorías
      </li>
    </ol>
  )
}
