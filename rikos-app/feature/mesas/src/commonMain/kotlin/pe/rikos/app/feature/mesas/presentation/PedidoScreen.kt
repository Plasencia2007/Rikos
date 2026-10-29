package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.feature.mesas.domain.AsientoPersona
import pe.rikos.app.feature.mesas.domain.EstadoLineaPedido
import pe.rikos.app.feature.mesas.domain.LineaPedido
import pe.rikos.app.feature.mesas.domain.Mesa
import pe.rikos.app.feature.mesas.domain.PedidoMesa
import pe.rikos.app.feature.mesas.domain.crearPedidoInicialMesa5
import pe.rikos.app.feature.mesas.domain.obtenerCartaInicial

/**
 * M07 · Pedido con mapa de asientos.
 * Vista principal de toma de comanda por asientos para una mesa ocupada.
 * Incluye cabecera, mapa de asientos interactivo (.tmap), lista de platos por persona,
 * botón para agregar platos al asiento seleccionado y barra de acciones inferior (.fbar).
 */
@Composable
fun PedidoScreen(
    mesa: Mesa,
    enLinea: Boolean = true,
    alVolver: () -> Unit,
    alVerAcciones: () -> Unit = {},
    alVerCuenta: () -> Unit = {},
    alAgregarPlato: (asientoId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    // Estado del pedido (inicia con el modelo del prototipo M07 o personalizado para la mesa)
    var pedido by remember(mesa.id) {
        mutableStateOf(
            if (mesa.numero == "5") {
                crearPedidoInicialMesa5()
            } else {
                val asientos = (1..maxOf(mesa.comensales, 2)).map { num ->
                    AsientoPersona(id = "p-$num", numero = num, etiqueta = "P$num")
                }
                PedidoMesa(
                    id = "ped-${mesa.id}",
                    mesaId = mesa.id,
                    mesaNumero = mesa.numero,
                    minutosTranscurridos = mesa.minutosTranscurridos ?: 0,
                    asientos = asientos,
                    asientoSeleccionadoId = asientos.first().id,
                )
            },
        )
    }

    val asientoActivo = pedido.asientoActivo ?: pedido.asientos.first()
    var asientoCartaActivo by remember { mutableStateOf<AsientoPersona?>(null) }
    var buscandoEnCarta by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
        ) {
            // 1. Cabecera (Toolbar + Título + Subtítulo)
            PedidoHeader(
                mesaNumero = pedido.mesaNumero,
                personasCount = pedido.asientos.size,
                minutosTranscurridos = pedido.minutosTranscurridos,
                enLinea = enLinea,
                alVolver = alVolver,
                alVerAcciones = alVerAcciones,
            )

            Spacer(Modifier.height(10.dp))

            // 2. Mapa interactivo de asientos (.tmap)
            MapaAsientosMesa(
                mesaNumero = pedido.mesaNumero,
                totalMonto = pedido.total,
                asientos = pedido.asientos,
                asientoSeleccionadoId = pedido.asientoSeleccionadoId,
                alSeleccionarAsiento = { nuevoId ->
                    pedido = pedido.copy(asientoSeleccionadoId = nuevoId)
                },
                alAgregarPersona = {
                    val nuevoNum = pedido.asientos.size + 1
                    val nuevoAsiento = AsientoPersona(
                        id = "p-$nuevoNum",
                        numero = nuevoNum,
                        etiqueta = "P$nuevoNum",
                    )
                    pedido = pedido.copy(
                        asientos = pedido.asientos + nuevoAsiento,
                        asientoSeleccionadoId = nuevoAsiento.id,
                    )
                },
                alVincularPensionista = {
                    val actual = pedido.asientoActivo
                    if (actual != null) {
                        val actualizado = actual.copy(esPensionista = !actual.esPensionista)
                        pedido = pedido.copy(
                            asientos = pedido.asientos.map { if (it.id == actual.id) actualizado else it },
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 14.dp),
            )

            Spacer(Modifier.height(14.dp))

            // 3. Fila de Persona seleccionada y su Subtotal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BasicText(
                        text = asientoActivo.nombreMostrar,
                        style = type.title3.copy(
                            color = colors.ink,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                    if (asientoActivo.esPensionista) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.purpleSoft)
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "Pensionista",
                                style = type.caption.copy(
                                    color = colors.purple,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                            )
                        }
                    }
                }
                BasicText(
                    text = asientoActivo.subtituloPersonalizado ?: asientoActivo.total.formatear(),
                    style = type.headline.copy(
                        color = colors.ink2,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))

            // 4. Tarjeta con las líneas de pedido de la persona (.card + .ln)
            LineasPersonaCard(
                lineas = asientoActivo.lineas,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            // 5. Botón Agregar a P{N} (.btn.sec.blk)
            BotonAgregarPlato(
                personaEtiqueta = asientoActivo.etiqueta,
                onClick = {
                    alAgregarPlato(asientoActivo.id)
                    asientoCartaActivo = asientoActivo
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // 6. Barra de navegación y acción flotante inferior (.fbar)
        BarraInferiorPedido(
            cantidadPorEnviar = pedido.cantidadPorEnviar,
            totalMonto = pedido.total,
            alVerCuenta = alVerCuenta,
            alEnviarACocina = {
                // Marcar todas las líneas pendientes como enviadas a cocina
                pedido = pedido.copy(
                    asientos = pedido.asientos.map { a ->
                        a.copy(
                            lineas = a.lineas.map { l ->
                                if (l.estado == EstadoLineaPedido.POR_ENVIAR) {
                                    l.copy(estado = EstadoLineaPedido.EN_COCINA)
                                } else l
                            },
                        )
                    },
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // 7. Pantalla M08: Agregar con un toque (Carta)
        AnimatedVisibility(
            visible = asientoCartaActivo != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(220, easing = RikosMotion.Ease)) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(180, easing = RikosMotion.Ease)) + fadeOut(),
        ) {
            val asientoParaCarta = asientoCartaActivo ?: asientoActivo
            val cartaCompleta = remember { obtenerCartaInicial() }
            CartaScreen(
                mesaNumero = pedido.mesaNumero,
                asientoEtiqueta = asientoParaCarta.etiqueta,
                enLinea = enLinea,
                cantidadesIniciales = emptyMap(),
                alVolver = { asientoCartaActivo = null },
                alConfirmarPlatos = { seleccionados ->
                    val nuevasLineas = seleccionados.mapNotNull { (nombre, cant) ->
                        val producto = cartaCompleta.find { it.nombre == nombre }
                        if (producto != null && cant > 0) {
                            LineaPedido(
                                id = "ln-${System.currentTimeMillis()}-${producto.id}",
                                cantidad = cant,
                                nombre = producto.nombre,
                                observacion = null,
                                precioUnitario = producto.precio,
                                estado = EstadoLineaPedido.POR_ENVIAR,
                            )
                        } else null
                    }
                    if (nuevasLineas.isNotEmpty()) {
                        pedido = pedido.copy(
                            asientos = pedido.asientos.map { a ->
                                if (a.id == asientoParaCarta.id) {
                                    a.copy(lineas = a.lineas + nuevasLineas)
                                } else a
                            },
                        )
                    }
                    asientoCartaActivo = null
                },
                alBuscarEnCarta = { buscandoEnCarta = true },
                alVerOpcionesMenu = { /* Opciones */ },
            )
        }

        // 8. Pantalla M08b: Buscar en la carta
        AnimatedVisibility(
            visible = buscandoEnCarta && asientoCartaActivo != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(220, easing = RikosMotion.Ease)) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(180, easing = RikosMotion.Ease)) + fadeOut(),
        ) {
            val asientoParaCarta = asientoCartaActivo ?: asientoActivo
            BuscarCartaScreen(
                mesaNumero = pedido.mesaNumero,
                asientoEtiqueta = asientoParaCarta.etiqueta,
                enLinea = enLinea,
                alVolver = { buscandoEnCarta = false },
                alAgregarProducto = { prod ->
                    val nuevaLinea = LineaPedido(
                        id = "ln-${System.currentTimeMillis()}-${prod.id}",
                        cantidad = 1,
                        nombre = prod.nombre,
                        observacion = null,
                        precioUnitario = prod.precio,
                        estado = EstadoLineaPedido.POR_ENVIAR,
                    )
                    pedido = pedido.copy(
                        asientos = pedido.asientos.map { a ->
                            if (a.id == asientoParaCarta.id) {
                                a.copy(lineas = a.lineas + nuevaLinea)
                            } else a
                        },
                    )
                },
                alDeshacerAgregar = { prod ->
                    pedido = pedido.copy(
                        asientos = pedido.asientos.map { a ->
                            if (a.id == asientoParaCarta.id) {
                                val idx = a.lineas.indexOfLast { it.nombre == prod.nombre }
                                if (idx >= 0) {
                                    a.copy(lineas = a.lineas.filterIndexed { i, _ -> i != idx })
                                } else a
                            } else a
                        },
                    )
                },
                alAgregarProductoLibre = { /* M09 */ },
            )
        }
    }
}

/**
 * Cabecera superior con botón volver, estado En línea, botón de acciones (...), título y subtítulo.
 */
@Composable
private fun PedidoHeader(
    mesaNumero: String,
    personasCount: Int,
    minutosTranscurridos: Int,
    enLinea: Boolean,
    alVolver: () -> Unit,
    alVerAcciones: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val volverInteraction = remember { MutableInteractionSource() }
    val isVolverPressed by volverInteraction.collectIsPressedAsState()
    val volverScale by animateFloatAsState(
        targetValue = if (isVolverPressed) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "volverScale",
    )

    val accionesInteraction = remember { MutableInteractionSource() }
    val isAccionesPressed by accionesInteraction.collectIsPressedAsState()
    val accionesScale by animateFloatAsState(
        targetValue = if (isAccionesPressed) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "accionesScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Toolbar fila
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Botón volver (<)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { scaleX = volverScale; scaleY = volverScale }
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = "Volver" }
                    .clickable(
                        interactionSource = volverInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = alVolver,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(20.dp)) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    val p = Path().apply {
                        moveTo(15f * sx, 6f * sy)
                        lineTo(9f * sx, 12f * sy)
                        lineTo(15f * sx, 18f * sy)
                    }
                    drawPath(p, color = colors.ink, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round))
                }
            }

            // En línea + Botón Acciones (...)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Badge En línea
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (enLinea) Color(0xFF10B981) else colors.ink3),
                    )
                    BasicText(
                        text = if (enLinea) "En línea" else "Sin conexión",
                        style = type.caption.copy(
                            color = colors.ink2,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        ),
                    )
                }

                // Botón acciones (...)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { scaleX = accionesScale; scaleY = accionesScale }
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .semantics { contentDescription = "Acciones de mesa" }
                        .clickable(
                            interactionSource = accionesInteraction,
                            indication = null,
                            role = Role.Button,
                            onClick = alVerAcciones,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(20.dp)) {
                        val col = colors.ink
                        val r = 2.dp.toPx()
                        drawCircle(col, radius = r, center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.5f))
                        drawCircle(col, radius = r, center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f))
                        drawCircle(col, radius = r, center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.5f))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Título H1
        BasicText(
            text = "Mesa $mesaNumero",
            style = type.title1.copy(
                color = colors.ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02).sp,
            ),
        )

        Spacer(Modifier.height(2.dp))

        // Subtítulo (.sub)
        BasicText(
            text = "$personasCount personas, $minutosTranscurridos min",
            style = type.subhead.copy(
                color = colors.ink3,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * Mapa de asientos interactivo (.tmap).
 * Mesa central con monto y asientos circulares (.seat) posicionados a su alrededor.
 */
@Composable
private fun MapaAsientosMesa(
    mesaNumero: String,
    totalMonto: Dinero,
    asientos: List<AsientoPersona>,
    asientoSeleccionadoId: String,
    alSeleccionarAsiento: (String) -> Unit,
    alAgregarPersona: () -> Unit,
    alVincularPensionista: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(184.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Mesa central (.tbl)
        Box(
            modifier = Modifier
                .width(190.dp)
                .height(96.dp)
                .shadow(6.dp, RoundedCornerShape(26.dp), spotColor = Color(0x100B1426))
                .clip(RoundedCornerShape(26.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                BasicText(
                    text = "Mesa $mesaNumero",
                    style = type.caption.copy(
                        color = colors.ink3,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                BasicText(
                    text = totalMonto.formatear(),
                    style = type.title1.copy(
                        color = colors.ink,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.02).sp,
                    ),
                )
            }
        }

        // Asientos superiores (P1 a la izquierda, P2 a la derecha)
        val asientoP1 = asientos.getOrNull(0)
        val asientoP2 = asientos.getOrNull(1)
        val asientoP3 = asientos.getOrNull(2)
        val asientoP4 = asientos.getOrNull(3)

        // Asiento P1 (Top-Left)
        if (asientoP1 != null) {
            BotonAsiento(
                asiento = asientoP1,
                seleccionado = asientoP1.id == asientoSeleccionadoId,
                onClick = { alSeleccionarAsiento(asientoP1.id) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = (-56).dp, y = 0.dp),
            )
        }

        // Asiento P2 (Top-Right)
        if (asientoP2 != null) {
            BotonAsiento(
                asiento = asientoP2,
                seleccionado = asientoP2.id == asientoSeleccionadoId,
                onClick = { alSeleccionarAsiento(asientoP2.id) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 56.dp, y = 0.dp),
            )
        }

        // Asiento P3 (Bottom-Left)
        if (asientoP3 != null) {
            BotonAsiento(
                asiento = asientoP3,
                seleccionado = asientoP3.id == asientoSeleccionadoId,
                onClick = { alSeleccionarAsiento(asientoP3.id) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = (-56).dp, y = 0.dp),
            )
        }

        // Asiento P4 (Bottom-Right)
        if (asientoP4 != null) {
            BotonAsiento(
                asiento = asientoP4,
                seleccionado = asientoP4.id == asientoSeleccionadoId,
                onClick = { alSeleccionarAsiento(asientoP4.id) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = 56.dp, y = 0.dp),
            )
        }

        // Botón lateral izquierdo (+) para agregar persona (.seat.add)
        BotonAccionLateral(
            descripcion = "Agregar persona",
            onClick = alAgregarPersona,
            modifier = Modifier.align(Alignment.CenterStart),
        ) { sx, sy, col ->
            drawLine(col, androidx.compose.ui.geometry.Offset(12f * sx, 5f * sy), androidx.compose.ui.geometry.Offset(12f * sx, 19f * sy), strokeWidth = 2.2f * sx, cap = StrokeCap.Round)
            drawLine(col, androidx.compose.ui.geometry.Offset(5f * sx, 12f * sy), androidx.compose.ui.geometry.Offset(19f * sx, 12f * sy), strokeWidth = 2.2f * sx, cap = StrokeCap.Round)
        }

        // Botón lateral derecho (☆) para pensionista (.seat.add)
        BotonAccionLateral(
            descripcion = "Vincular pensionista",
            onClick = alVincularPensionista,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) { sx, sy, col ->
            val starPath = Path().apply {
                moveTo(12f * sx, 2f * sy)
                lineTo(15.1f * sx, 8.3f * sy)
                lineTo(22f * sx, 9.3f * sy)
                lineTo(17f * sx, 14.2f * sy)
                lineTo(18.2f * sx, 21f * sy)
                lineTo(12f * sx, 17.8f * sy)
                lineTo(5.8f * sx, 21f * sy)
                lineTo(7f * sx, 14.2f * sy)
                lineTo(2f * sx, 9.3f * sy)
                lineTo(8.9f * sx, 8.3f * sy)
                close()
            }
            drawPath(starPath, color = col, style = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * Botón circular que representa un asiento (.seat) con soporte para anillo de selección,
 * punto de notificación (.pd) y etiqueta de pensionista (.st).
 */
@Composable
private fun BotonAsiento(
    asiento: AsientoPersona,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "asientoScale_${asiento.id}",
    )

    val fondoAnim by animateColorAsState(
        targetValue = if (seleccionado) colors.primary else colors.surface,
        animationSpec = tween(200),
        label = "asientoBg_${asiento.id}",
    )

    val textoColorAnim by animateColorAsState(
        targetValue = if (seleccionado) colors.onPrimary else colors.ink,
        animationSpec = tween(200),
        label = "asientoText_${asiento.id}",
    )

    Box(
        modifier = modifier
            .size(58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center,
    ) {
        // Anillo de selección externa (.seat.on { box-shadow: 0 0 0 5px var(--aji-soft); })
        if (seleccionado) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .border(5.dp, colors.primarySoft, CircleShape),
            )
        }

        // Círculo principal del asiento
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(fondoAnim)
                .border(
                    width = 2.dp,
                    color = if (seleccionado) colors.primary else colors.line,
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = asiento.etiqueta,
                style = type.title2.copy(
                    color = textoColorAnim,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }

        // Punto de notificación (.pd) para asientos con platos por enviar a cocina
        if (asiento.tienePendientes) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF6B4A))
                    .border(3.dp, colors.bg, CircleShape),
            )
        }

        // Badge de pensionista (.st)
        if (asiento.esPensionista) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF6D28D9))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Pens.",
                    style = type.caption.copy(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
        }
    }
}

/**
 * Botón lateral con borde discontinuo para agregar asiento o pensionista (.seat.add)
 */
@Composable
private fun BotonAccionLateral(
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    drawIcon: androidx.compose.ui.graphics.drawscope.DrawScope.(Float, Float, Color) -> Unit,
) {
    val colors = Rikos.colors

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "btnLateralScale",
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .semantics { contentDescription = descripcion }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 1.5.dp.toPx()
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            drawCircle(
                color = colors.line,
                radius = size.minDimension / 2f - strokeWidth / 2f,
                style = Stroke(width = strokeWidth, pathEffect = dashPathEffect),
            )
            val sx = size.width / 24f
            val sy = size.height / 24f
            drawIcon(sx, sy, colors.ink3)
        }
    }
}

/**
 * Tarjeta que contiene las líneas de pedidos asociadas a la persona (.card + .ln)
 */
@Composable
private fun LineasPersonaCard(
    lineas: List<LineaPedido>,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(18.dp), spotColor = Color(0x0D0B1426))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
    ) {
        if (lineas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "No hay platos agregados a esta persona",
                    style = type.body.copy(
                        color = colors.ink3,
                        fontSize = 14.sp,
                    ),
                )
            }
        } else {
            lineas.forEachIndexed { index, linea ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.line),
                    )
                }

                FilaLineaPedido(linea = linea)
            }
        }
    }
}

/**
 * Fila individual de plato pedido (.ln)
 */
@Composable
private fun FilaLineaPedido(
    linea: LineaPedido,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val esNuevo = linea.estado == EstadoLineaPedido.POR_ENVIAR

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Indicador numérico de cantidad (.q)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (esNuevo) colors.primary else colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "${linea.cantidad}",
                style = type.headline.copy(
                    color = if (esNuevo) colors.onPrimary else colors.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }

        // Descripción del plato (Nombre + Observación + Tag de estado)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = linea.nombre,
                style = type.headline.copy(
                    color = colors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 19.sp,
                ),
            )

            if (!linea.observacion.isNullOrBlank()) {
                BasicText(
                    text = linea.observacion,
                    style = type.subhead.copy(
                        color = colors.ink3,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }

            // Tag de estado operativo
            Box(modifier = Modifier.padding(top = 4.dp)) {
                val (bgTag, textTag, labelTag) = when (linea.estado) {
                    EstadoLineaPedido.POR_ENVIAR -> Triple(colors.primarySoft, colors.primary, "Por enviar")
                    EstadoLineaPedido.EN_COCINA -> Triple(colors.surface2, colors.ink2, "En cocina")
                    EstadoLineaPedido.LISTO_PARA_SERVIR -> Triple(colors.greenSoft, colors.greenText, "Listo para servir")
                    EstadoLineaPedido.SERVIDO -> Triple(colors.surface2, colors.ink2, "Servido")
                    EstadoLineaPedido.ANULADO -> Triple(colors.redSoft, colors.red, "Anulado")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(bgTag)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = labelTag,
                        style = type.caption.copy(
                            color = textTag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }

        // Subtotal
        BasicText(
            text = linea.subtotal.formatear(),
            style = type.headline.copy(
                color = colors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * Botón para agregar plato a la persona activa (.btn.sec.blk)
 */
@Composable
private fun BotonAgregarPlato(
    personaEtiqueta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = RikosMotion.press(),
        label = "btnAgregarScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.5.dp, colors.line, RoundedCornerShape(14.dp))
            .semantics { contentDescription = "Agregar plato a $personaEtiqueta" }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(Modifier.size(18.dp)) {
                val sx = size.width / 24f
                val sy = size.height / 24f
                val col = colors.ink
                drawLine(col, androidx.compose.ui.geometry.Offset(12f * sx, 5f * sy), androidx.compose.ui.geometry.Offset(12f * sx, 19f * sy), strokeWidth = 2.2f * sx, cap = StrokeCap.Round)
                drawLine(col, androidx.compose.ui.geometry.Offset(5f * sx, 12f * sy), androidx.compose.ui.geometry.Offset(19f * sx, 12f * sy), strokeWidth = 2.2f * sx, cap = StrokeCap.Round)
            }

            BasicText(
                text = "Agregar a $personaEtiqueta",
                style = type.callout.copy(
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
    }
}

/**
 * Barra inferior flotante (.fbar) con total, contador de platos pendientes, botón cuenta y enviar.
 */
@Composable
private fun BarraInferiorPedido(
    cantidadPorEnviar: Int,
    totalMonto: Dinero,
    alVerCuenta: () -> Unit,
    alEnviarACocina: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val enviarInteraction = remember { MutableInteractionSource() }
    val isEnviarPressed by enviarInteraction.collectIsPressedAsState()
    val enviarScale by animateFloatAsState(
        targetValue = if (isEnviarPressed) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "enviarScale",
    )

    val cuentaInteraction = remember { MutableInteractionSource() }
    val isCuentaPressed by cuentaInteraction.collectIsPressedAsState()
    val cuentaScale by animateFloatAsState(
        targetValue = if (isCuentaPressed) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "cuentaScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(35.dp), spotColor = Color(0x60000000))
                .clip(RoundedCornerShape(35.dp))
                .background(colors.navy)
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Contador de platos por enviar (.cnt)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "$cantidadPorEnviar",
                    style = type.headline.copy(
                        color = colors.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }

            // Columna con etiqueta y monto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = "Por enviar a cocina",
                    style = type.caption.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                BasicText(
                    text = totalMonto.formatear(),
                    style = type.title2.copy(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.02).sp,
                    ),
                )
            }

            // Botón Recibo / Ver Cuenta (.ib)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { scaleX = cuentaScale; scaleY = cuentaScale }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .semantics { contentDescription = "Ver la cuenta" }
                    .clickable(
                        interactionSource = cuentaInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = alVerCuenta,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(22.dp)) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    val col = Color.White
                    val path = Path().apply {
                        moveTo(4f * sx, 2f * sy)
                        lineTo(4f * sx, 22f * sy)
                        lineTo(7f * sx, 20f * sy)
                        lineTo(9f * sx, 22f * sy)
                        lineTo(12f * sx, 20f * sy)
                        lineTo(15f * sx, 22f * sy)
                        lineTo(17f * sx, 20f * sy)
                        lineTo(20f * sx, 22f * sy)
                        lineTo(20f * sx, 2f * sy)
                        lineTo(17f * sx, 4f * sy)
                        lineTo(15f * sx, 2f * sy)
                        lineTo(12f * sx, 4f * sy)
                        lineTo(9f * sx, 2f * sy)
                        lineTo(7f * sx, 4f * sy)
                        close()
                    }
                    drawPath(path, color = col, style = Stroke(width = 1.8f * sx, cap = StrokeCap.Round))
                    drawLine(col, androidx.compose.ui.geometry.Offset(8f * sx, 9f * sy), androidx.compose.ui.geometry.Offset(16f * sx, 9f * sy), strokeWidth = 1.8f * sx, cap = StrokeCap.Round)
                    drawLine(col, androidx.compose.ui.geometry.Offset(8f * sx, 13f * sy), androidx.compose.ui.geometry.Offset(16f * sx, 13f * sy), strokeWidth = 1.8f * sx, cap = StrokeCap.Round)
                }
            }

            // Botón Enviar (.btn.pri)
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .graphicsLayer { scaleX = enviarScale; scaleY = enviarScale }
                    .clip(RoundedCornerShape(25.dp))
                    .background(colors.primary)
                    .clickable(
                        interactionSource = enviarInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = alEnviarACocina,
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Canvas(Modifier.size(18.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val col = colors.onPrimary
                        val path = Path().apply {
                            moveTo(22f * sx, 2f * sy)
                            lineTo(11f * sx, 13f * sy)
                            moveTo(22f * sx, 2f * sy)
                            lineTo(15f * sx, 22f * sy)
                            lineTo(11f * sx, 13f * sy)
                            lineTo(2f * sx, 9f * sy)
                            close()
                        }
                        drawPath(path, color = col, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }

                    BasicText(
                        text = "Enviar",
                        style = type.callout.copy(
                            color = colors.onPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}
