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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion

/**
 * M12a · Pedido por tiempos (retener y enviar)
 * Canvas ID: 4c52bb3b-2d00-47d2-8c09-8c1a4b477bc2
 *
 * Permite al mozo definir qué tiempos del pedido se envían inmediatamente
 * a cocina (marchan) y cuáles quedan retenidos.
 */

enum class TiempoPedido(val titulo: String, val subtitulo: String) {
    ENTRADAS("Entradas", "Salen primero"),
    FONDOS("Fondos", "Retenidos hasta que los marches"),
    POSTRES("Postres", "Al final"),
}

data class LineaTiempo(
    val id: String,
    val cantidad: Int,
    val nombre: String,
    val detalle: String,
    val precio: Dinero,
    val tiempo: TiempoPedido,
    val enCocina: Boolean = false,
)

@Composable
fun PedidoTiemposScreen(
    mesaNumero: Int = 5,
    enLinea: Boolean = true,
    alVolver: () -> Unit,
    alEnviarYVolverAMesas: (enviadosIds: Set<String>) -> Unit,
    alEnviarYSeguir: ((enviadosIds: Set<String>) -> Unit)? = null,
    lineasIniciales: List<LineaTiempo>? = null,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val coroutineScope = rememberCoroutineScope()

    // Datos basados exactamente en el lienzo de referencia M12a
    var lineas by remember {
        mutableStateOf(
            lineasIniciales ?: listOf(
                // Entradas (c1)
                LineaTiempo("a0", 1, "Ceviche clásico", "P1, sin ají", Dinero(2200L), TiempoPedido.ENTRADAS),
                LineaTiempo("a1", 2, "Causa limeña", "P2 y P3", Dinero(2800L), TiempoPedido.ENTRADAS),
                // Fondos (c2)
                LineaTiempo("b0", 1, "Lomo saltado", "P1, término medio", Dinero(1800L), TiempoPedido.FONDOS),
                LineaTiempo("b1", 1, "Ají de gallina", "P2", Dinero(1600L), TiempoPedido.FONDOS),
                LineaTiempo("b2", 1, "Arroz chaufa", "P4", Dinero(1500L), TiempoPedido.FONDOS),
                // Postres (c3)
                LineaTiempo("c0", 2, "Suspiro limeño", "Para compartir", Dinero(1400L), TiempoPedido.POSTRES),
            )
        )
    }

    // Por defecto según el diseño: on: { a0: true, a1: true }
    var seleccionados by remember { mutableStateOf(setOf("a0", "a1")) }

    // Toast flotante al enviar
    var toastVisible by remember { mutableStateOf(false) }
    var toastCount by remember { mutableStateOf(0) }

    fun mostrarToast(cantidad: Int) {
        toastCount = cantidad
        toastVisible = true
        coroutineScope.launch {
            delay(3400)
            toastVisible = false
        }
    }

    val totalSeleccionados = seleccionados.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Barra superior / Cabecera (.hd)
            CabeceraTiempos(
                mesaNumero = mesaNumero,
                enLinea = enLinea,
                alVolver = alVolver,
            )

            // 2. Cuerpo desplazable con los grupos de tiempos (.body)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TiempoPedido.entries.forEach { tiempo ->
                    val lineasDelTiempo = lineas.filter { it.tiempo == tiempo }
                    if (lineasDelTiempo.isNotEmpty()) {
                        GrupoTiempoCard(
                            tiempo = tiempo,
                            lineas = lineasDelTiempo,
                            seleccionados = seleccionados,
                            onToggleLinea = { id ->
                                seleccionados = if (id in seleccionados) {
                                    seleccionados - id
                                } else {
                                    seleccionados + id
                                }
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Dock inferior de acciones (.dock)
            DockAccionesTiempos(
                totalSeleccionados = totalSeleccionados,
                onRetener = {
                    // Deselecciona todos los ítems (hold)
                    seleccionados = emptySet()
                },
                onEnviarYSeguir = {
                    if (seleccionados.isNotEmpty()) {
                        val cantidad = seleccionados.size
                        // Marca como en cocina
                        lineas = lineas.map {
                            if (it.id in seleccionados) it.copy(enCocina = true) else it
                        }
                        alEnviarYSeguir?.invoke(seleccionados)
                        seleccionados = emptySet()
                        mostrarToast(cantidad)
                    }
                },
                onEnviar = {
                    if (seleccionados.isNotEmpty()) {
                        lineas = lineas.map {
                            if (it.id in seleccionados) it.copy(enCocina = true) else it
                        }
                    }
                    alEnviarYVolverAMesas(seleccionados)
                },
            )
        }

        // 4. Toast flotante animado de confirmación de despacho (.toast)
        AnimatedVisibility(
            visible = toastVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(350, easing = RikosMotion.Ease)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(250, easing = RikosMotion.Ease)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 14.dp, end = 14.dp)
        ) {
            ToastCocina(platosEnviados = toastCount)
        }
    }
}

/**
 * Cabecera (.hd): Barra superior con botón volver, estado de red y título.
 */
@Composable
private fun CabeceraTiempos(
    mesaNumero: Int,
    enLinea: Boolean,
    alVolver: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val backInteraction = remember { MutableInteractionSource() }
    val backPressed by backInteraction.collectIsPressedAsState()
    val backScale by animateFloatAsState(
        targetValue = if (backPressed) 0.92f else 1f,
        animationSpec = tween(120),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Fila de herramientas superior (.tbar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Botón volver (.ib)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = alVolver,
                    )
                    .semantics { contentDescription = "Volver al pedido" },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val p = Path().apply {
                        // Chevron izquierdo: M15 6l-6 6 6 6
                        moveTo(size.width * 0.625f, size.height * 0.25f)
                        lineTo(size.width * 0.375f, size.height * 0.5f)
                        lineTo(size.width * 0.625f, size.height * 0.75f)
                    }
                    drawPath(
                        path = p,
                        color = colors.ink,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Indicador de conexión (.conn)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (enLinea) Color(0xFF22C55E) else colors.ink3, CircleShape)
                )
                BasicText(
                    text = if (enLinea) "En línea" else "Sin conexión",
                    style = type.footnote.copy(
                        color = colors.ink2,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Título principal (.hd h1)
        BasicText(
            text = "Mesa $mesaNumero · por tiempos",
            style = type.title1.copy(
                color = colors.ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp,
            ),
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Subtítulo (.hd .sub)
        BasicText(
            text = "Marca lo que sale ahora; lo demás queda retenido",
            style = type.subhead.copy(
                color = colors.ink3,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * Grupo de tiempos (.course + .card): Cabecera de sección y tarjeta con las líneas.
 */
@Composable
private fun GrupoTiempoCard(
    tiempo: TiempoPedido,
    lineas: List<LineaTiempo>,
    seleccionados: Set<String>,
    onToggleLinea: (String) -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Cabecera del tiempo (.course)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicText(
                text = tiempo.titulo,
                style = type.callout.copy(
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            BasicText(
                text = tiempo.subtitulo,
                style = type.footnote.copy(
                    color = colors.ink3,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.weight(1f),
            )
        }

        // Tarjeta contenedora (.card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(20.dp))
        ) {
            lineas.forEachIndexed { index, linea ->
                val isChecked = linea.id in seleccionados
                FilaLineaTiempo(
                    linea = linea,
                    isChecked = isChecked,
                    onToggle = { onToggleLinea(linea.id) },
                )
                if (index < lineas.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.line)
                    )
                }
            }
        }
    }
}

/**
 * Fila de plato / producto (.ln): Checkbox + cantidad + nombre/notas/tag + subtotal.
 */
@Composable
private fun FilaLineaTiempo(
    linea: LineaTiempo,
    isChecked: Boolean,
    onToggle: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val rowInteraction = remember { MutableInteractionSource() }
    val rowPressed by rowInteraction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (rowPressed) 0.985f else 1f,
        animationSpec = tween(100),
    )

    // El color de realce para checkboxes y botones primarios en el diseño es ámbar/ají
    val colorAji = colors.amberStrong

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .clickable(
                interactionSource = rowInteraction,
                indication = null,
                role = Role.Checkbox,
                onClick = onToggle,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Checkbox (.ck / .ck.on)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (isChecked) colorAji else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (isChecked) colorAji else colors.ink3.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(9.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isChecked) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    val p = Path().apply {
                        // Checkmark icon: M20 6L9 17l-5-5
                        moveTo(size.width * 0.18f, size.height * 0.52f)
                        lineTo(size.width * 0.42f, size.height * 0.76f)
                        lineTo(size.width * 0.85f, size.height * 0.25f)
                    }
                    drawPath(
                        path = p,
                        color = Color.White,
                        style = Stroke(
                            width = 2.4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }

        // 2. Badge de cantidad (.ln .q)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "${linea.cantidad}",
                style = type.title3.copy(
                    color = colors.ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }

        // 3. Información del plato (nombre, detalle, tag de estado)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = linea.nombre,
                style = type.body.copy(
                    color = colors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )

            BasicText(
                text = linea.detalle,
                style = type.footnote.copy(
                    color = colors.ink3,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tag de estado (.tag)
            val tagTexto: String
            val tagBg: Color
            val tagColor: Color

            when {
                linea.enCocina -> {
                    tagTexto = "En cocina"
                    tagBg = colors.greenSoft
                    tagColor = colors.greenText
                }
                isChecked -> {
                    tagTexto = "Por enviar"
                    tagBg = colors.amberSoft
                    tagColor = colors.amberText
                }
                else -> {
                    tagTexto = "Retenido"
                    tagBg = colors.surface2
                    tagColor = colors.ink2
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tagBg)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = tagTexto,
                    style = type.caption.copy(
                        color = tagColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
        }

        // 4. Subtotal monetario (.money)
        BasicText(
            text = linea.precio.formatear(),
            style = type.callout.copy(
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * Toast flotante (.toast): Cocina ya lo tiene en su pantalla.
 */
@Composable
private fun ToastCocina(platosEnviados: Int) {
    val colors = Rikos.colors
    val type = Rikos.type
    val colorAji = colors.amberStrong

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.navy)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Círculo ají con icono avión de papel (.ti)
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colorAji, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val p = Path().apply {
                    // Avión: M22 2L11 13 M22 2l-7 20-4-9-9-4z
                    moveTo(size.width * 0.92f, size.height * 0.08f)
                    lineTo(size.width * 0.46f, size.height * 0.54f)
                    moveTo(size.width * 0.92f, size.height * 0.08f)
                    lineTo(size.width * 0.63f, size.height * 0.92f)
                    lineTo(size.width * 0.46f, size.height * 0.54f)
                    lineTo(size.width * 0.08f, size.height * 0.38f)
                    close()
                }
                drawPath(
                    path = p,
                    color = Color.White,
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            BasicText(
                text = "$platosEnviados platos enviados a cocina",
                style = type.subhead.copy(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = "Cocina ya lo tiene en su pantalla",
                style = type.footnote.copy(
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

/**
 * Dock inferior de acciones (.dock):
 * Retener | Enviar y seguir | Enviar
 * con leyenda explicativa.
 */
@Composable
private fun DockAccionesTiempos(
    totalSeleccionados: Int,
    onRetener: () -> Unit,
    onEnviarYSeguir: () -> Unit,
    onEnviar: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(
                width = 1.dp,
                color = colors.line,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
            )
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Fila de tres botones (.row)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. Botón Retener (.btn.sec flex:1)
            BotonSecundario(
                texto = "Retener",
                onClick = onRetener,
                mostrarIconoReloj = true,
                modifier = Modifier.weight(1f),
            )

            // 2. Botón Enviar y seguir (.btn.sec flex:1.2)
            BotonSecundario(
                texto = "Enviar y seguir",
                onClick = onEnviarYSeguir,
                modifier = Modifier.weight(1.2f),
            )

            // 3. Botón Enviar (.btn.pri flex:1)
            BotonPrimarioAji(
                texto = "Enviar",
                onClick = onEnviar,
                modifier = Modifier.weight(1f),
            )
        }

        // Texto explicativo (.xs .c3)
        BasicText(
            text = "$totalSeleccionados seleccionados. «Enviar» los manda y vuelve a mesas.",
            style = type.footnote.copy(
                color = colors.ink3,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    mostrarIconoReloj: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(120),
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface2)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (mostrarIconoReloj) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    // Reloj: M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 6v6l4 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 * 0.85f
                    drawCircle(
                        color = colors.ink,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                    // Manecillas
                    val p = Path().apply {
                        moveTo(center.x, center.y - radius * 0.6f)
                        lineTo(center.x, center.y)
                        lineTo(center.x + radius * 0.45f, center.y + radius * 0.25f)
                    }
                    drawPath(
                        path = p,
                        color = colors.ink,
                        style = Stroke(
                            width = 1.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }

            BasicText(
                text = texto,
                style = type.callout.copy(
                    color = colors.ink,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
    }
}

@Composable
private fun BotonPrimarioAji(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(120),
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(colors.amberStrong)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = texto,
            style = type.callout.copy(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}
