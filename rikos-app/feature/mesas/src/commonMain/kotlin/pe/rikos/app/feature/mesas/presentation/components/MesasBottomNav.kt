package pe.rikos.app.feature.mesas.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion

enum class NavItem { MESAS, COMANDAS, CAJA, MAS }

@Composable
fun MesasBottomNav(
    itemSeleccionado: NavItem = NavItem.MESAS,
    alSeleccionarItem: (NavItem) -> Unit = {},
    alNuevoPedido: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val centerInteraction = remember { MutableInteractionSource() }
    val centerPressed by centerInteraction.collectIsPressedAsState()
    val centerScale by animateFloatAsState(
        targetValue = if (centerPressed) 0.90f else 1f,
        animationSpec = RikosMotion.press(),
        label = "fabPress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Barra flotante
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color(0x28021E43),
                    spotColor = Color(0x38021E43),
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(28.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Mesas
                NavItemButton(
                    label = "Mesas",
                    activo = itemSeleccionado == NavItem.MESAS,
                    onClick = { alSeleccionarItem(NavItem.MESAS) },
                    modifier = Modifier.weight(1f),
                ) { color ->
                    Canvas(Modifier.size(24.dp)) {
                        val stroke = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(7f * sx, 8f * sy),
                            size = Size(10f * sx, 8f * sy),
                            cornerRadius = CornerRadius(2.5f * sx),
                            style = stroke,
                        )
                        drawLine(color, Offset(8.5f * sx, 4.5f * sy), Offset(10.5f * sx, 4.5f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
                        drawLine(color, Offset(13.5f * sx, 4.5f * sy), Offset(15.5f * sx, 4.5f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
                        drawLine(color, Offset(8.5f * sx, 19.5f * sy), Offset(10.5f * sx, 19.5f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
                        drawLine(color, Offset(13.5f * sx, 19.5f * sy), Offset(15.5f * sx, 19.5f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
                    }
                }

                // 2. Comandas (con badge '2')
                NavItemButton(
                    label = "Comandas",
                    activo = itemSeleccionado == NavItem.COMANDAS,
                    onClick = { alSeleccionarItem(NavItem.COMANDAS) },
                    badgeTexto = "2",
                    badgeColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                ) { color ->
                    Canvas(Modifier.size(24.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val p = Path().apply {
                            moveTo(6f * sx, 3f * sy)
                            lineTo(18f * sx, 3f * sy)
                            lineTo(18f * sx, 21f * sy)
                            lineTo(16f * sx, 19.5f * sy)
                            lineTo(14f * sx, 21f * sy)
                            lineTo(12f * sx, 19.5f * sy)
                            lineTo(10f * sx, 21f * sy)
                            lineTo(8f * sx, 19.5f * sy)
                            lineTo(6f * sx, 21f * sy)
                            close()
                        }
                        drawPath(p, color, style = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawLine(color, Offset(9.5f * sx, 8f * sy), Offset(14.5f * sx, 8f * sy), strokeWidth = 1.8f * sx, cap = StrokeCap.Round)
                        drawLine(color, Offset(9.5f * sx, 12f * sy), Offset(14.5f * sx, 12f * sy), strokeWidth = 1.8f * sx, cap = StrokeCap.Round)
                        drawLine(color, Offset(9.5f * sx, 16f * sy), Offset(13f * sx, 16f * sy), strokeWidth = 1.8f * sx, cap = StrokeCap.Round)
                    }
                }

                // 3. Botón central '+' flotante
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer { scaleX = centerScale; scaleY = centerScale }
                            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x38021E43), spotColor = Color(0x55021E43))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF021E43))
                            .clickable(
                                interactionSource = centerInteraction,
                                indication = null,
                                onClick = alNuevoPedido,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(24.dp)) {
                            drawLine(Color.White, Offset(12f, 5f), Offset(12f, 19f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                            drawLine(Color.White, Offset(5f, 12f), Offset(19f, 12f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                        }
                    }
                }

                // 4. Caja (con punto verde pulsante)
                NavItemButton(
                    label = "Caja",
                    activo = itemSeleccionado == NavItem.CAJA,
                    onClick = { alSeleccionarItem(NavItem.CAJA) },
                    tienePuntoVerde = true,
                    modifier = Modifier.weight(1f),
                ) { color ->
                    Canvas(Modifier.size(24.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val p = Path().apply {
                            moveTo(4f * sx, 7.5f * sy)
                            cubicTo(4f * sx, 6f * sy, 5f * sx, 5f * sy, 6.5f * sx, 5f * sy)
                            lineTo(18f * sx, 5f * sy)
                            lineTo(18f * sx, 8f * sy)
                            moveTo(4f * sx, 7.5f * sy)
                            lineTo(4f * sx, 17f * sy)
                            cubicTo(4f * sx, 18.5f * sy, 5f * sx, 19f * sy, 6.5f * sx, 19f * sy)
                            lineTo(20f * sx, 19f * sy)
                            lineTo(20f * sx, 8f * sy)
                            lineTo(6.5f * sx, 8f * sy)
                            cubicTo(5f * sx, 8f * sy, 4f * sx, 7f * sy, 4f * sx, 7.5f * sy)
                        }
                        drawPath(p, color, style = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawCircle(color, radius = 1.5f * sx, center = Offset(16.5f * sx, 13.5f * sy))
                    }
                }

                // 5. Más
                NavItemButton(
                    label = "Más",
                    activo = itemSeleccionado == NavItem.MAS,
                    onClick = { alSeleccionarItem(NavItem.MAS) },
                    modifier = Modifier.weight(1f),
                ) { color ->
                    Canvas(Modifier.size(24.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        drawCircle(color, radius = 4f * sx, center = Offset(12f * sx, 8f * sy), style = Stroke(2f * sx))
                        val p = Path().apply {
                            moveTo(4.5f * sx, 20.5f * sy)
                            cubicTo(4.5f * sx, 16.5f * sy, 8f * sx, 14.5f * sy, 12f * sx, 14.5f * sy)
                            cubicTo(16f * sx, 14.5f * sy, 19.5f * sx, 16.5f * sy, 19.5f * sx, 20.5f * sy)
                        }
                        drawPath(p, color, style = Stroke(width = 2f * sx, cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItemButton(
    label: String,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeTexto: String? = null,
    badgeColor: Color = Color(0xFFEF4444),
    tienePuntoVerde: Boolean = false,
    icon: @Composable (Color) -> Unit,
) {
    val type = Rikos.type
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = RikosMotion.press(),
        label = "navItemPress",
    )

    // Animación de pulso del punto verde de turno abierto (.tb-dot::after rk-live)
    val infiniteTransition = rememberInfiniteTransition()
    val dotAlpha by if (tienePuntoVerde) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dotPulse",
        )
    } else {
        remember { androidx.compose.runtime.mutableStateOf(1f) }
    }

    val colorIcono = if (activo) Color(0xFF0D9488) else Color(0xFF64748B)
    val colorTexto = if (activo) Color(0xFF0D9488) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(22.dp))
            .then(if (activo) Modifier.background(Color(0xFFE0F5EC)) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon(colorIcono)

            if (badgeTexto != null) {
                Box(
                    modifier = Modifier
                        .offset(x = 10.dp, y = (-6).dp)
                        .size(18.dp)
                        .background(badgeColor, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = badgeTexto,
                        style = type.caption.copy(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            if (tienePuntoVerde) {
                Box(
                    modifier = Modifier
                        .offset(x = 8.dp, y = (-4).dp)
                        .size(8.dp)
                        .background(Color(0xFF10B981).copy(alpha = dotAlpha), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                )
            }
        }

        BasicText(
            text = label,
            style = type.caption.copy(
                color = colorTexto,
                fontSize = 11.sp,
                fontWeight = if (activo) FontWeight.ExtraBold else FontWeight.Bold,
            ),
        )
    }
}
