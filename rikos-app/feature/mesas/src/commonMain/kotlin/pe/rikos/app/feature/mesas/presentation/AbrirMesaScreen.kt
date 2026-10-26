package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.feature.mesas.domain.Mesa

/**
 * M04 · Abrir mesa.
 * Pantalla para configurar la apertura de una mesa libre (comensales, mozo, cliente, pensionistas, comentario)
 * antes de tomar el pedido.
 */
@Composable
fun AbrirMesaScreen(
    mesa: Mesa,
    mozoNombre: String = "Luis Quispe",
    salonNombre: String = "Salón principal",
    capacidad: Int = 8,
    enLinea: Boolean = true,
    alVolver: () -> Unit,
    alAbrirMesa: (mesa: Mesa, comensales: Int, cliente: String, pensionistas: Boolean, comentario: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    var comensales by remember { mutableIntStateOf(if (mesa.comensales > 0) mesa.comensales else 4) }
    var cliente by remember { mutableStateOf("Clientes varios") }
    var hayPensionistas by remember { mutableStateOf(false) }
    var comentario by remember { mutableStateOf("") }

    val dockInteraction = remember { MutableInteractionSource() }
    val isDockPressed by dockInteraction.collectIsPressedAsState()
    val dockScale by animateFloatAsState(
        targetValue = if (isDockPressed) 0.98f else 1f,
        animationSpec = RikosMotion.press(),
        label = "dockScale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp),
        ) {
            // 1. Cabecera (Header: Toolbar + Título + Subtítulo)
            AbrirMesaHeader(
                mesaNumero = mesa.numero,
                salonNombre = salonNombre,
                capacidad = capacidad,
                enLinea = enLinea,
                alVolver = alVolver,
            )

            Spacer(Modifier.height(14.dp))

            // 2. Tarjeta con Mesa Dibujada Interactiva y Selector de Personas
            MesaVisualCard(
                mesaNumero = mesa.numero,
                comensales = comensales,
                alCambiarComensales = { comensales = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(14.dp))

            // 3. Lista Agrupada (.gl: Cliente, Mozo, Pensionistas)
            GroupedOptionsList(
                cliente = cliente,
                mozoNombre = mozoNombre,
                hayPensionistas = hayPensionistas,
                alAlternarPensionistas = { hayPensionistas = !hayPensionistas },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(14.dp))

            // 4. Campo de Comentario Opcional
            ComentarioField(
                valor = comentario,
                alCambiar = { comentario = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(16.dp))
        }

        // 5. Dock inferior flotante con botón primario
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.bg.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .graphicsLayer {
                        scaleX = dockScale
                        scaleY = dockScale
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primary)
                    .clickable(
                        interactionSource = dockInteraction,
                        indication = null,
                        onClick = {
                            alAbrirMesa(mesa, comensales, cliente, hayPensionistas, comentario)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    BasicText(
                        text = "Abrir mesa y tomar pedido",
                        style = type.headline.copy(
                            color = colors.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Canvas(Modifier.size(18.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val path = Path().apply {
                            moveTo(9f * sx, 18f * sy)
                            lineTo(15f * sx, 12f * sy)
                            lineTo(9f * sx, 6f * sy)
                        }
                        drawPath(
                            path = path,
                            color = colors.onPrimary,
                            style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cabecera superior con botón volver, estado de red «En línea», título y subtítulo.
 */
@Composable
private fun AbrirMesaHeader(
    mesaNumero: String,
    salonNombre: String,
    capacidad: Int,
    enLinea: Boolean,
    alVolver: () -> Unit,
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
            // Botón volver (.ib)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = volverScale
                        scaleY = volverScale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = volverInteraction,
                        indication = null,
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
                    drawPath(
                        p,
                        color = colors.ink,
                        style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round),
                    )
                }
            }

            // Conexión badge (.conn)
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
        }

        Spacer(Modifier.height(14.dp))

        // Título H1
        BasicText(
            text = "Abrir mesa $mesaNumero",
            style = type.title2.copy(
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).sp,
            ),
        )

        Spacer(Modifier.height(2.dp))

        // Subtítulo (.sub)
        BasicText(
            text = "$salonNombre, para $capacidad personas",
            style = type.subhead.copy(
                color = colors.ink3,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * Tarjeta que contiene la mesa dibujada con sillas interactivas y la botonera numérica de comensales.
 */
@Composable
private fun MesaVisualCard(
    mesaNumero: String,
    comensales: Int,
    alCambiarComensales: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0D0B1426))
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .padding(top = 14.dp, bottom = 18.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Mesa dibujada vectorial con animación de sillas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseW = 320f
                val baseH = 200f
                val scale = minOf(size.width / baseW, size.height / baseH)
                val ox = (size.width - baseW * scale) / 2f
                val oy = (size.height - baseH * scale) / 2f

                // Color tokens para la mesa y sillas
                val tableFill = if (colors.isDark) Color(0xFF042F2E) else Color(0xFFE0F5EC)
                val tableStroke = if (colors.isDark) Color(0xFF2DD4BF) else Color(0xFF059669)
                val chairActive = if (colors.isDark) Color(0xFF2DD4BF) else Color(0xFF0B7A6C)
                val chairEmpty = if (colors.isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

                // Posición de las 10 sillas: 4 arriba, 4 abajo, 1 izquierda, 1 derecha
                data class SillaDef(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float, val orden: Int)
                val sillas = listOf(
                    SillaDef(52f, 14f, 36f, 16f, 8f, 1),   // Top 1
                    SillaDef(112f, 14f, 36f, 16f, 8f, 3),  // Top 2
                    SillaDef(172f, 14f, 36f, 16f, 8f, 6),  // Top 3
                    SillaDef(232f, 14f, 36f, 16f, 8f, 8),  // Top 4
                    SillaDef(52f, 170f, 36f, 16f, 8f, 2),  // Bot 1
                    SillaDef(112f, 170f, 36f, 16f, 8f, 4),  // Bot 2
                    SillaDef(172f, 170f, 36f, 16f, 8f, 7),  // Bot 3
                    SillaDef(232f, 170f, 36f, 16f, 8f, 9),  // Bot 4
                    SillaDef(10f, 80f, 16f, 40f, 8f, 5),   // Left
                    SillaDef(294f, 80f, 16f, 40f, 8f, 10), // Right
                )

                // 1. Dibujar sillas con color condicional a comensales
                sillas.forEach { s ->
                    val activa = comensales >= s.orden
                    val col = if (activa) chairActive else chairEmpty
                    drawRoundRect(
                        color = col,
                        topLeft = Offset(ox + s.x * scale, oy + s.y * scale),
                        size = Size(s.w * scale, s.h * scale),
                        cornerRadius = CornerRadius(s.rx * scale, s.rx * scale),
                    )
                }

                // 2. Tablero de mesa central
                drawRoundRect(
                    color = tableFill,
                    topLeft = Offset(ox + 40f * scale, oy + 38f * scale),
                    size = Size(240f * scale, 124f * scale),
                    cornerRadius = CornerRadius(22f * scale, 22f * scale),
                )
                drawRoundRect(
                    color = tableStroke,
                    topLeft = Offset(ox + 40f * scale, oy + 38f * scale),
                    size = Size(240f * scale, 124f * scale),
                    cornerRadius = CornerRadius(22f * scale, 22f * scale),
                    style = Stroke(width = 2.5f * scale),
                )
            }

            // Texto central dentro de la mesa ("Mesa X", "N pers.")
            val centerTextColor = if (colors.isDark) Color(0xFF2DD4BF) else Color(0xFF047857)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = "Mesa $mesaNumero",
                    style = type.callout.copy(
                        color = centerTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                BasicText(
                    text = "$comensales pers.",
                    style = type.title1.copy(
                        color = centerTextColor,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.02).sp,
                    ),
                )
            }
        }

        // Fila de controles numéricos: Botón (-), Chips (2..6), Botón (+)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Botón (-)
            CirculoIconButton(
                enabled = comensales > 1,
                onClick = { if (comensales > 1) alCambiarComensales(comensales - 1) },
            ) { sx, sy, col ->
                drawLine(
                    color = col,
                    start = Offset(6f * sx, 12f * sy),
                    end = Offset(18f * sx, 12f * sy),
                    strokeWidth = 2.2f * sx,
                    cap = StrokeCap.Round,
                )
            }

            // Chips 2, 3, 4, 5, 6
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(2, 3, 4, 5, 6).forEach { num ->
                    val seleccionado = comensales == num
                    ChipNumero(
                        numero = num,
                        seleccionado = seleccionado,
                        onClick = { alCambiarComensales(num) },
                    )
                }
            }

            // Botón (+)
            CirculoIconButton(
                enabled = comensales < 10,
                onClick = { if (comensales < 10) alCambiarComensales(comensales + 1) },
            ) { sx, sy, col ->
                drawLine(
                    color = col,
                    start = Offset(12f * sx, 6f * sy),
                    end = Offset(12f * sx, 18f * sy),
                    strokeWidth = 2.2f * sx,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = col,
                    start = Offset(6f * sx, 12f * sy),
                    end = Offset(18f * sx, 12f * sy),
                    strokeWidth = 2.2f * sx,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * Chip numérico circular (.chip)
 */
@Composable
private fun ChipNumero(
    numero: Int,
    seleccionado: Boolean,
    onClick: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "chipScale_$numero",
    )

    val bgAnim by animateColorAsState(
        targetValue = if (seleccionado) {
            if (colors.isDark) Color(0xFFE5ECF6) else colors.navy
        } else {
            colors.surface
        },
        animationSpec = tween(200),
        label = "chipBg_$numero",
    )

    val textColorAnim by animateColorAsState(
        targetValue = if (seleccionado) {
            if (colors.isDark) Color(0xFF0B1220) else Color.White
        } else {
            colors.ink
        },
        animationSpec = tween(200),
        label = "chipText_$numero",
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bgAnim)
            .border(
                width = 1.dp,
                color = if (seleccionado) bgAnim else colors.line,
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "$numero",
            style = type.callout.copy(
                color = textColorAnim,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * Botón circular (.ib) para - y +
 */
@Composable
private fun CirculoIconButton(
    enabled: Boolean,
    onClick: () -> Unit,
    drawIcon: androidx.compose.ui.graphics.drawscope.DrawScope.(Float, Float, Color) -> Unit,
) {
    val colors = Rikos.colors

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "circuloScale",
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.line, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val sx = size.width / 24f
            val sy = size.height / 24f
            drawIcon(sx, sy, colors.ink)
        }
    }
}

/**
 * Lista agrupada (.gl) con filas para Cliente, Mozo y Pensionistas.
 */
@Composable
private fun GroupedOptionsList(
    cliente: String,
    mozoNombre: String,
    hayPensionistas: Boolean,
    alAlternarPensionistas: () -> Unit,
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
        // Fila 1: Cliente (opcional)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clickable { /* Selección de cliente */ }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = "Cliente (opcional)",
                    style = type.caption.copy(
                        color = colors.ink3,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                BasicText(
                    text = cliente,
                    style = type.callout.copy(
                        color = colors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            ChevronRightIcon()
        }

        // Divisor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line),
        )

        // Fila 2: Mozo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clickable { /* Traspasar mozo */ }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = "Mozo",
                    style = type.caption.copy(
                        color = colors.ink3,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                BasicText(
                    text = mozoNombre,
                    style = type.callout.copy(
                        color = colors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            ChevronRightIcon()
        }

        // Divisor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line),
        )

        // Fila 3: Hay pensionistas con Toggle Switch (.sw)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clickable(onClick = alAlternarPensionistas)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = "Hay pensionistas",
                    style = type.callout.copy(
                        color = colors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                BasicText(
                    text = "Los vinculas por asiento",
                    style = type.caption.copy(
                        color = colors.ink3,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            // Switch (.sw) animado exactamente según el prototipo CSS
            RkSwitch(
                activo = hayPensionistas,
                alAlternar = alAlternarPensionistas,
            )
        }
    }
}

/**
 * Switch Toggle animado (.sw)
 */
@Composable
private fun RkSwitch(
    activo: Boolean,
    alAlternar: () -> Unit,
) {
    val colors = Rikos.colors

    val switchBgAnim by animateColorAsState(
        targetValue = if (activo) colors.primary else if (colors.isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
        animationSpec = tween(250),
        label = "switchBg",
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (activo) 28.dp else 4.dp,
        animationSpec = tween(250),
        label = "switchThumb",
    )

    val thumbColorAnim by animateColorAsState(
        targetValue = if (activo) Color.White else if (colors.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
        animationSpec = tween(250),
        label = "switchThumbColor",
    )

    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(switchBgAnim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = alAlternar,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(thumbColorAnim),
        )
    }
}

/**
 * Campo de texto para comentario opcional (.fl + .fx)
 */
@Composable
private fun ComentarioField(
    valor: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        BasicText(
            text = "Comentario (opcional)",
            style = type.callout.copy(
                color = colors.ink2,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (valor.isEmpty()) {
                BasicText(
                    text = "Ej. cumpleaños, alergia al maní",
                    style = TextStyle(
                        color = colors.ink3,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
            BasicTextField(
                value = valor,
                onValueChange = alCambiar,
                textStyle = TextStyle(
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                singleLine = true,
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Icono chevron derecho reutilizable
 */
@Composable
private fun ChevronRightIcon() {
    val colors = Rikos.colors
    Canvas(Modifier.size(18.dp)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val p = Path().apply {
            moveTo(9f * sx, 18f * sy)
            lineTo(15f * sx, 12f * sy)
            lineTo(9f * sx, 6f * sy)
        }
        drawPath(
            p,
            color = colors.ink3,
            style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round),
        )
    }
}
