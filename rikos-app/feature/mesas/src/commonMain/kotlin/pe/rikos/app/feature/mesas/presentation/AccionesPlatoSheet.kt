package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.feature.mesas.domain.AsientoPersona
import pe.rikos.app.feature.mesas.domain.EstadoLineaPedido
import pe.rikos.app.feature.mesas.domain.LineaPedido

/**
 * Pantalla / Hoja emergente M11b · Acciones del plato (`b41b94d4-3ee8-4e1c-89d6-cd3363cff69a`).
 *
 * Muestra las acciones táctiles rápidas y opciones avanzadas de una línea de comanda:
 * - Cabecera con avatar, nombre, persona/asiento, subtotal y estado.
 * - Fila de acciones táctiles: Repetir, Nota (M11), Enviar.
 * - Tarjeta con opciones: Cambiar de asiento, Cambiar de tiempo, Dividir entre asientos, Descuento.
 * - Botón de acción destructiva: Anular plato.
 */
@Composable
fun AccionesPlatoSheet(
    linea: LineaPedido,
    asientoActual: AsientoPersona,
    todosLosAsientos: List<AsientoPersona>,
    alCerrar: () -> Unit,
    alRepetir: () -> Unit,
    alEditarNota: () -> Unit,
    alEnviar: () -> Unit,
    alCambiarAsiento: (nuevoAsientoId: String) -> Unit,
    alCambiarTiempo: (nuevoTiempo: String) -> Unit = {},
    alDividir: () -> Unit = {},
    alDescontar: () -> Unit = {},
    alAnular: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    var submodalCambiarAsiento by remember { mutableStateOf(false) }
    var submodalCambiarTiempo by remember { mutableStateOf(false) }
    var tiempoSeleccionado by remember { mutableStateOf("Postres, sale al final") }

    val estadoTexto = when (linea.estado) {
        EstadoLineaPedido.POR_ENVIAR -> "por enviar"
        EstadoLineaPedido.EN_COCINA -> "en cocina"
        EstadoLineaPedido.LISTO_PARA_SERVIR -> "listo para servir"
        EstadoLineaPedido.SERVIDO -> "servido"
        EstadoLineaPedido.ANULADO -> "anulado"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = alCerrar,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Contenedor principal de la hoja (sheet)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Evitar que el clic en el contenido cierre el sheet */ },
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Manija superior (Grab handle)
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(colors.line, CircleShape)
                    .align(Alignment.CenterHorizontally),
            )

            // 2. Cabecera del plato con Avatar, Nombre, Info y botón Cerrar (✕)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar plato (cuadrado redondeado ámbar con icono cubiertos)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(colors.amberSoft, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconoCubiertos(
                            color = colors.amberText,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText(
                            text = linea.nombre,
                            style = type.headline.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.ink,
                            ),
                        )
                        BasicText(
                            text = "${asientoActual.etiqueta}, ${linea.subtotal.formatear()}, $estadoTexto",
                            style = type.subhead.copy(
                                color = colors.ink3,
                            ),
                        )
                    }
                }

                // Botón Cerrar (✕)
                BotonCerrar(alCerrar = alCerrar)
            }

            // 3. Fila de 3 acciones táctiles directas (Repetir, Nota, Enviar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Botón Repetir (secundario)
                BotonAccionRapida(
                    texto = "Repetir",
                    icono = { IconoRepetir(color = colors.ink, modifier = Modifier.size(18.dp)) },
                    esPrimario = false,
                    fondoPersonalizado = colors.surface2,
                    colorTexto = colors.ink,
                    onClick = alRepetir,
                    modifier = Modifier.weight(1f),
                )

                // Botón Nota (secundario, abre M11)
                BotonAccionRapida(
                    texto = "Nota",
                    icono = { IconoLapiz(color = colors.ink, modifier = Modifier.size(18.dp)) },
                    esPrimario = false,
                    fondoPersonalizado = colors.surface2,
                    colorTexto = colors.ink,
                    onClick = alEditarNota,
                    modifier = Modifier.weight(1f),
                )

                // Botón Enviar (primario verde ají)
                BotonAccionRapida(
                    texto = "Enviar",
                    icono = { IconoAvionEnviar(color = Color.White, modifier = Modifier.size(18.dp)) },
                    esPrimario = true,
                    fondoPersonalizado = colors.primary,
                    colorTexto = Color.White,
                    onClick = alEnviar,
                    modifier = Modifier.weight(1f),
                )
            }

            // 4. Tarjeta de opciones avanzadas (Cambiar de asiento, tiempo, dividir, descuento)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
            ) {
                // Opción 1: Cambiar de asiento
                FilaOpcionPlato(
                    titulo = "Cambiar de asiento",
                    subtitulo = "Ahora en ${asientoActual.etiqueta}",
                    colorFondoAvatar = colors.blueSoft,
                    colorIconoAvatar = colors.blue,
                    icono = { IconoAsiento(color = colors.blue, modifier = Modifier.size(20.dp)) },
                    onClick = { submodalCambiarAsiento = true },
                )

                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))

                // Opción 2: Cambiar de tiempo
                FilaOpcionPlato(
                    titulo = "Cambiar de tiempo",
                    subtitulo = tiempoSeleccionado,
                    colorFondoAvatar = colors.amberSoft,
                    colorIconoAvatar = colors.amberText,
                    icono = { IconoRelojTiempo(color = colors.amberText, modifier = Modifier.size(20.dp)) },
                    onClick = { submodalCambiarTiempo = true },
                )

                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))

                // Opción 3: Dividir entre asientos
                FilaOpcionPlato(
                    titulo = "Dividir entre asientos",
                    subtitulo = "Partes iguales o por monto",
                    colorFondoAvatar = colors.primarySoft,
                    colorIconoAvatar = colors.primary,
                    icono = { IconoDividir(color = colors.primary, modifier = Modifier.size(20.dp)) },
                    onClick = alDividir,
                )

                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))

                // Opción 4: Descuento a este plato
                FilaOpcionPlato(
                    titulo = "Descuento a este plato",
                    subtitulo = "Pide motivo",
                    colorFondoAvatar = colors.greenSoft,
                    colorIconoAvatar = colors.greenText,
                    icono = { IconoPorcentaje(color = colors.greenText, modifier = Modifier.size(20.dp)) },
                    onClick = alDescontar,
                )
            }

            // 5. Botón destructivo: Anular plato
            BotonAnularPlato(
                onClick = alAnular,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
        }

        // Sub-modal emergente: Cambiar de asiento
        AnimatedVisibility(
            visible = submodalCambiarAsiento,
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
            exit = fadeOut(tween(140)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(160)),
        ) {
            SubmodalSeleccionAsiento(
                asientos = todosLosAsientos,
                asientoActualId = asientoActual.id,
                alSeleccionar = { nuevoId ->
                    submodalCambiarAsiento = false
                    alCambiarAsiento(nuevoId)
                },
                alCerrar = { submodalCambiarAsiento = false },
            )
        }

        // Sub-modal emergente: Cambiar de tiempo
        AnimatedVisibility(
            visible = submodalCambiarTiempo,
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
            exit = fadeOut(tween(140)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(160)),
        ) {
            SubmodalSeleccionTiempo(
                tiempoActual = tiempoSeleccionado,
                alSeleccionar = { nuevoTiempo ->
                    tiempoSeleccionado = nuevoTiempo
                    submodalCambiarTiempo = false
                    alCambiarTiempo(nuevoTiempo)
                },
                alCerrar = { submodalCambiarTiempo = false },
            )
        }
    }
}

/**
 * Fila interactiva para la tarjeta de opciones avanzadas.
 */
@Composable
private fun FilaOpcionPlato(
    titulo: String,
    subtitulo: String,
    colorFondoAvatar: Color,
    colorIconoAvatar: Color,
    icono: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "opcionPlatoScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar icono temático
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(colorFondoAvatar, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            icono()
        }

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = titulo,
                style = type.subhead.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.ink,
                ),
            )
            BasicText(
                text = subtitulo,
                style = type.caption.copy(
                    color = colors.ink3,
                    fontSize = 12.sp,
                ),
            )
        }

        // Chevron derecho (>)
        IconoChevronDerecha(
            color = colors.ink3,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * Botón para las 3 acciones táctiles directas (Repetir, Nota, Enviar).
 */
@Composable
private fun BotonAccionRapida(
    texto: String,
    icono: @Composable () -> Unit,
    esPrimario: Boolean,
    fondoPersonalizado: Color,
    colorTexto: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "btnRapidoScale",
    )

    Row(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(fondoPersonalizado)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icono()
        Spacer(Modifier.width(6.dp))
        BasicText(
            text = texto,
            style = Rikos.type.subhead.copy(
                fontWeight = FontWeight.Bold,
                color = colorTexto,
                fontSize = 13.5.sp,
            ),
        )
    }
}

/**
 * Botón destructivo con estilo .btn.dang.blk del lienzo.
 */
@Composable
private fun BotonAnularPlato(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "anularScale",
    )

    Row(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(colors.red)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoBasura(
            color = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text = "Anular plato",
            style = Rikos.type.body.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
            ),
        )
    }
}

/**
 * Botón circular de cierre (✕).
 */
@Composable
private fun BotonCerrar(
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(120),
        label = "cerrarScale",
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(colors.surface2, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = alCerrar,
            )
            .semantics { contentDescription = "Cerrar acciones del plato" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val strokeWidth = 2.dp.toPx()
            drawLine(
                color = colors.ink,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colors.ink,
                start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                end = androidx.compose.ui.geometry.Offset(0f, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Selector emergente para cambiar un plato a otro asiento (P1, P2, P3, etc.).
 */
@Composable
private fun SubmodalSeleccionAsiento(
    asientos: List<AsientoPersona>,
    asientoActualId: String,
    alSeleccionar: (String) -> Unit,
    alCerrar: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(colors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Mover a otro asiento",
                style = type.headline.copy(fontWeight = FontWeight.Bold, color = colors.ink),
            )
            BotonCerrar(alCerrar = alCerrar)
        }

        BasicText(
            text = "Elige a qué persona transferir este plato:",
            style = type.subhead.copy(color = colors.ink3),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface2),
        ) {
            asientos.forEachIndexed { index, asiento ->
                val esActual = asiento.id == asientoActualId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !esActual) { alSeleccionar(asiento.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (esActual) colors.primary else colors.line,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = asiento.etiqueta,
                                style = type.subhead.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (esActual) Color.White else colors.ink,
                                    fontSize = 12.sp,
                                ),
                            )
                        }

                        BasicText(
                            text = asiento.nombreMostrar,
                            style = type.body.copy(
                                fontWeight = if (esActual) FontWeight.Bold else FontWeight.Medium,
                                color = colors.ink,
                            ),
                        )
                    }

                    if (esActual) {
                        BasicText(
                            text = "Actual",
                            style = type.caption.copy(color = colors.primary, fontWeight = FontWeight.Bold),
                        )
                    }
                }
                if (index < asientos.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

/**
 * Selector emergente para cambiar el tiempo del plato.
 */
@Composable
private fun SubmodalSeleccionTiempo(
    tiempoActual: String,
    alSeleccionar: (String) -> Unit,
    alCerrar: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val tiempos = listOf(
        "Entrada (sale primero)",
        "Segundo (plato fuerte)",
        "Postres, sale al final",
        "Bebidas (inmediato)",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(colors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Cambiar tiempo de salida",
                style = type.headline.copy(fontWeight = FontWeight.Bold, color = colors.ink),
            )
            BotonCerrar(alCerrar = alCerrar)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface2),
        ) {
            tiempos.forEachIndexed { index, tiempo ->
                val esActual = tiempo == tiempoActual
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { alSeleccionar(tiempo) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = tiempo,
                        style = type.body.copy(
                            fontWeight = if (esActual) FontWeight.Bold else FontWeight.Medium,
                            color = if (esActual) colors.amberText else colors.ink,
                        ),
                    )

                    if (esActual) {
                        BasicText(
                            text = "✓",
                            style = type.subhead.copy(color = colors.amberText, fontWeight = FontWeight.ExtraBold),
                        )
                    }
                }
                if (index < tiempos.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Iconos vectoriales dibujados a mano con Canvas según el diseño
// ─────────────────────────────────────────────────────────────

@Composable
private fun IconoCubiertos(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Tenedor izquierda
        val tenedor = Path().apply {
            moveTo(w * 0.2f, h * 0.15f)
            lineTo(w * 0.2f, h * 0.45f)
            lineTo(w * 0.4f, h * 0.45f)
            lineTo(w * 0.4f, h * 0.15f)
            moveTo(w * 0.3f, h * 0.15f)
            lineTo(w * 0.3f, h * 0.85f)
        }
        drawPath(tenedor, color = color, style = stroke)

        // Cuchillo derecha
        val cuchillo = Path().apply {
            moveTo(w * 0.75f, h * 0.15f)
            cubicTo(w * 0.85f, h * 0.25f, w * 0.85f, h * 0.45f, w * 0.75f, h * 0.55f)
            lineTo(w * 0.75f, h * 0.85f)
            moveTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.55f)
        }
        drawPath(cuchillo, color = color, style = stroke)
    }
}

@Composable
private fun IconoRepetir(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            // Flecha 1 superior
            moveTo(w * 0.9f, h * 0.2f)
            lineTo(w * 0.9f, h * 0.45f)
            lineTo(w * 0.65f, h * 0.45f)
            // Arco 1
            moveTo(w * 0.2f, h * 0.45f)
            cubicTo(w * 0.25f, h * 0.25f, w * 0.55f, h * 0.15f, w * 0.88f, h * 0.42f)

            // Flecha 2 inferior
            moveTo(w * 0.1f, h * 0.8f)
            lineTo(w * 0.1f, h * 0.55f)
            lineTo(w * 0.35f, h * 0.55f)
            // Arco 2
            moveTo(w * 0.8f, h * 0.55f)
            cubicTo(w * 0.75f, h * 0.75f, w * 0.45f, h * 0.85f, w * 0.12f, h * 0.58f)
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
private fun IconoLapiz(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.85f)
            lineTo(w * 0.85f, h * 0.85f)
            moveTo(w * 0.68f, h * 0.18f)
            lineTo(w * 0.82f, h * 0.32f)
            lineTo(w * 0.32f, h * 0.82f)
            lineTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.18f, h * 0.68f)
            close()
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
private fun IconoAvionEnviar(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            moveTo(w * 0.9f, h * 0.1f)
            lineTo(w * 0.1f, h * 0.45f)
            lineTo(w * 0.45f, h * 0.55f)
            lineTo(w * 0.55f, h * 0.9f)
            close()
            moveTo(w * 0.45f, h * 0.55f)
            lineTo(w * 0.9f, h * 0.1f)
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
private fun IconoAsiento(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Cabeza usuario
        drawCircle(
            color = color,
            radius = w * 0.18f,
            center = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.3f),
            style = stroke,
        )
        // Cuerpo usuario
        val cuerpo = Path().apply {
            moveTo(w * 0.15f, h * 0.8f)
            cubicTo(w * 0.15f, h * 0.6f, w * 0.65f, h * 0.6f, w * 0.65f, h * 0.8f)
        }
        drawPath(cuerpo, color = color, style = stroke)

        // Respaldo asiento
        val respaldo = Path().apply {
            moveTo(w * 0.8f, h * 0.3f)
            lineTo(w * 0.8f, h * 0.8f)
        }
        drawPath(respaldo, color = color, style = stroke)
    }
}

@Composable
private fun IconoRelojTiempo(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(
            color = color,
            radius = w * 0.4f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.5f),
            style = stroke,
        )
        val manecillas = Path().apply {
            moveTo(w * 0.5f, h * 0.25f)
            lineTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.7f, h * 0.5f)
        }
        drawPath(manecillas, color = color, style = stroke)
    }
}

@Composable
private fun IconoDividir(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            // Flechas opuestas
            moveTo(w * 0.7f, h * 0.2f)
            lineTo(w * 0.85f, h * 0.2f)
            lineTo(w * 0.85f, h * 0.35f)
            moveTo(w * 0.85f, h * 0.2f)
            lineTo(w * 0.55f, h * 0.5f)

            moveTo(w * 0.3f, h * 0.8f)
            lineTo(w * 0.15f, h * 0.8f)
            lineTo(w * 0.15f, h * 0.65f)
            moveTo(w * 0.15f, h * 0.8f)
            lineTo(w * 0.45f, h * 0.5f)
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
private fun IconoPorcentaje(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.2f),
            end = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.8f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = color,
            radius = w * 0.1f,
            center = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.3f),
            style = stroke,
        )
        drawCircle(
            color = color,
            radius = w * 0.1f,
            center = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.7f),
            style = stroke,
        )
    }
}

@Composable
private fun IconoBasura(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.25f)
            lineTo(w * 0.8f, h * 0.25f)
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.35f, h * 0.15f)
            lineTo(w * 0.65f, h * 0.15f)
            lineTo(w * 0.65f, h * 0.25f)
            moveTo(w * 0.28f, h * 0.25f)
            lineTo(w * 0.32f, h * 0.85f)
            lineTo(w * 0.68f, h * 0.85f)
            lineTo(w * 0.72f, h * 0.25f)
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
private fun IconoChevronDerecha(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.2f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.8f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
