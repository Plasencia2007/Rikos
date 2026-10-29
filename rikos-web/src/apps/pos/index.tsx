import { useEffect, useRef, useState } from 'react'
import { PantallaPin } from '../../features/autenticacion'
import { resumenDePedidos, type SesionIniciada } from '../../api/cliente'
import { ProveedorDeAvisos } from '../../comun/componentes'
import { PantallaCaja } from '../../features/caja'
import { MapaDeMesas, PantallaPedido } from '../../features/venta'
import {
  PantallaHistorial,
  PantallaInicio,
  PantallaOrdenes,
  PantallaPedidoRapido,
  PosShell,
  type VistaDelPos,
} from '../../features/pos'

const MINUTOS_INACTIVIDAD = Number(import.meta.env.VITE_POS_MINUTOS_INACTIVIDAD ?? 5)

// El Inicio trae su propio título grande ("Resumen del local"): el cabezal solo muestra la ruta.
const ENCABEZADOS: Partial<Record<VistaDelPos, { titulo: string; subtitulo: string }>> = {
  inicio: { titulo: 'Inicio', subtitulo: '' },
  mesas: { titulo: 'Mesas', subtitulo: '' },
  rapido: { titulo: 'Pedido rápido', subtitulo: 'Pedidos para llevar, sin mesa' },
  ordenes: { titulo: 'Órdenes activas', subtitulo: 'Pedidos en curso de este local' },
  historial: { titulo: 'Historial de órdenes', subtitulo: 'Consulta órdenes pasadas' },
  caja: { titulo: 'Caja', subtitulo: 'Tu turno, lo que hay por cobrar y el cierre' },
}

/**
 * US-003: "sin perder nada" -- el pedido pertenece a la mesa, no al mozo.
 * Por eso cambiar de mozo (boton o inactividad) solo vuelve a la pantalla
 * de PIN; no cierra la sesion del local ni descarta nada en curso.
 *
 * El POS vive dentro del "shell" (barra lateral + cabezal), ver
 * docs/producto/ui-ux/03-pos-rediseno.md. Las vistas que aun no existen
 * aparecen en el menu atenuadas con "Pronto".
 */
export default function POS() {
  const [mozoActivo, setMozoActivo] = useState<SesionIniciada | null>(null)
  const [vista, setVista] = useState<VistaDelPos>('inicio')
  const [pedidoAbierto, setPedidoAbierto] = useState<{ id: string; titulo: string } | null>(null)
  // El pedido que se lleva a la Caja desde el boton "Cobrar" de la pantalla del pedido.
  const [pedidoACobrar, setPedidoACobrar] = useState<string | null>(null)
  // En pantallas chicas (tableta vertical, movil) la barra arranca colapsada para dejar espacio al contenido.
  const [colapsada, setColapsada] = useState(() => window.innerWidth < 900)
  const [enCurso, setEnCurso] = useState(0)
  const temporizador = useRef<ReturnType<typeof window.setTimeout> | undefined>(undefined)

  function volverAlPin() {
    setMozoActivo(null)
    setPedidoAbierto(null)
    setVista('inicio')
  }

  useEffect(() => {
    if (!mozoActivo) return

    function reiniciar() {
      if (temporizador.current) window.clearTimeout(temporizador.current)
      temporizador.current = window.setTimeout(volverAlPin, MINUTOS_INACTIVIDAD * 60_000)
    }

    reiniciar()
    window.addEventListener('pointerdown', reiniciar)
    window.addEventListener('keydown', reiniciar)
    return () => {
      window.removeEventListener('pointerdown', reiniciar)
      window.removeEventListener('keydown', reiniciar)
      if (temporizador.current) window.clearTimeout(temporizador.current)
    }
  }, [mozoActivo])

  // Contador del menu: pedidos en curso del local, se refresca solo.
  useEffect(() => {
    if (!mozoActivo) return
    const token = mozoActivo.token
    let vivo = true
    const cargar = () =>
      resumenDePedidos(token)
        .then((r) => vivo && setEnCurso(r.enCurso))
        .catch(() => undefined)
    cargar()
    const reloj = window.setInterval(cargar, 15_000)
    return () => {
      vivo = false
      window.clearInterval(reloj)
    }
  }, [mozoActivo])

  if (!mozoActivo) {
    return <PantallaPin onExito={setMozoActivo} />
  }

  const local = mozoActivo.locales.find((l) => l.id === mozoActivo.localActivoId)?.nombre ?? ''
  const encabezado = pedidoAbierto
    ? { titulo: pedidoAbierto.titulo, subtitulo: 'Pedido en curso' }
    : (ENCABEZADOS[vista] ?? ENCABEZADOS.inicio)!

  function irA(siguiente: VistaDelPos) {
    setPedidoAbierto(null)
    setPedidoACobrar(null)
    setVista(siguiente)
  }

  return (
    <ProveedorDeAvisos>
      <PosShell
        vista={pedidoAbierto ? 'mesas' : vista}
        titulo={encabezado.titulo}
        subtitulo={encabezado.subtitulo}
        tituloEnContenido={!pedidoAbierto && vista === 'inicio'}
        tituloDePantalla={!pedidoAbierto && vista === 'mesas'}
        mozo={mozoActivo.nombreUsuario}
        local={local}
        colapsada={colapsada || pedidoAbierto !== null}
        contadorOrdenes={enCurso}
        sinCabezal={pedidoAbierto !== null}
        onAlternarBarra={() => setColapsada((c) => !c)}
        onIr={irA}
        onCambiarDeMozo={volverAlPin}
      >
        {pedidoAbierto ? (
          <PantallaPedido
            token={mozoActivo.token}
            pedidoId={pedidoAbierto.id}
            titulo={pedidoAbierto.titulo}
            nombreLocal={local}
            onVolver={() => setPedidoAbierto(null)}
            onCobrar={(id) => {
              setPedidoAbierto(null)
              setPedidoACobrar(id)
              setVista('caja')
            }}
          />
        ) : vista === 'mesas' ? (
          <MapaDeMesas token={mozoActivo.token} onAbrirPedido={(id, titulo) => setPedidoAbierto({ id, titulo })} />
        ) : vista === 'rapido' ? (
          <PantallaPedidoRapido
            token={mozoActivo.token}
            onAbrirPedido={(id, titulo) => setPedidoAbierto({ id, titulo })}
          />
        ) : vista === 'ordenes' ? (
          <PantallaOrdenes token={mozoActivo.token} onVerPedido={(id, titulo) => setPedidoAbierto({ id, titulo })} />
        ) : vista === 'caja' ? (
          <PantallaCaja key={pedidoACobrar ?? 'caja'} token={mozoActivo.token} pedidoInicial={pedidoACobrar} />
        ) : vista === 'historial' ? (
          <PantallaHistorial token={mozoActivo.token} onVerPedido={(id, titulo) => setPedidoAbierto({ id, titulo })} />
        ) : (
          <PantallaInicio token={mozoActivo.token} onIrA={(v) => irA(v)} />
        )}
      </PosShell>
    </ProveedorDeAvisos>
  )
}
