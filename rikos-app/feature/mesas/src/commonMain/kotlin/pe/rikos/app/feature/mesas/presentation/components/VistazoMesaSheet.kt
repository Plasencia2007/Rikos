package pe.rikos.app.feature.mesas.presentation.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import pe.rikos.app.feature.mesas.domain.EstadoMesa
import pe.rikos.app.feature.mesas.domain.Mesa

/**
 * M03c · Vistazo de mesa.
 * BottomSheet emergente que se muestra al mantener pulsada o seleccionar una mesa ocupada.
 * Proporciona resumen en tiempo real (total, en cocina, por enviar) y accesos rápidos (abrir pedido, cuenta, cambiar, unir, transferir).
 */
@Composable
fun VistazoMesaSheet(
    mesa: Mesa?,
    visible: Boolean,
    alCerrar: () -> Unit,
    alAbrirPedido: (Mesa) -> Unit,
    alVerCuenta: (Mesa) -> Unit,
    alCambiarMesa: (Mesa) -> Unit,
    alUnirMesas: (Mesa) -> Unit,
    alPasarMozo: (Mesa) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (mesa == null) return

    val type = Rikos.type
    val colors = Rikos.colors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Scrim semitransparente con fade
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = alCerrar,
                    ),
            )
        }

        // Hoja inferior deslizable (.sheet)
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(380, easing = RikosMotion.Ease),
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(280, easing = RikosMotion.Ease),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        ambientColor = Color(0x280B1426),
                        spotColor = Color(0x400B1426),
                    )
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Barra de arrastre (.grab)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(44.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(colors.line),
                )

                // Encabezado: Título de la mesa y badge de tiempo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText(
                            text = "Mesa ${mesa.numero}",
                            style = type.title2.copy(
                                color = colors.ink,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        )
                        val pers = if (mesa.comensales > 0) mesa.comensales else 4
                        val tiempoDesc = when (mesa.estado) {
                            EstadoMesa.OCUPADA -> "Ocupada, $pers personas, desde 12:42"
                            EstadoMesa.POR_COBRAR -> "Por cobrar, $pers personas"
                            EstadoMesa.LISTO -> "Comida lista, $pers personas"
                            else -> "Libre, capacidad $pers"
                        }
                        BasicText(
                            text = tiempoDesc,
                            style = type.footnote.copy(
                                color = colors.ink3,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }

                    // Badge de tiempo sin pasar (.tag .t-aji)
                    val mins = mesa.minutosTranscurridos ?: 32
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.amberSoft)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "$mins min sin pasar",
                            style = type.caption.copy(
                                color = colors.amberText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        )
                    }
                }

                // Resumen métricas en 3 cajas (.tri)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Total
                    val totalTxt = if (mesa.total.centimos > 0) mesa.total.formatear() else "S/ 79.50"
                    MetricaCaja(
                        etiqueta = "Total",
                        valor = totalTxt,
                        modifier = Modifier.weight(1f),
                    )
                    // En cocina
                    MetricaCaja(
                        etiqueta = "En cocina",
                        valor = "3",
                        modifier = Modifier.weight(1f),
                    )
                    // Por enviar
                    MetricaCaja(
                        etiqueta = "Por enviar",
                        valor = "2",
                        modifier = Modifier.weight(1f),
                    )
                }

                // Acciones principales: Abrir pedido y Cuenta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Botón primario: Abrir pedido (.btn.pri.grow)
                    BotonAccionPrimario(
                        texto = "Abrir pedido",
                        onClick = { alAbrirPedido(mesa) },
                        modifier = Modifier.weight(1f),
                    )

                    // Botón secundario: Cuenta con icono (.btn.sec.grow)
                    BotonAccionSecundario(
                        texto = "Cuenta",
                        onClick = { alVerCuenta(mesa) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Canvas(Modifier.size(18.dp)) {
                            val sx = size.width / 24f
                            val sy = size.height / 24f
                            val p = Path().apply {
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
                            drawPath(p, colors.ink, style = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawLine(colors.ink, Offset(8f * sx, 9f * sy), Offset(16f * sx, 9f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
                            drawLine(colors.ink, Offset(8f * sx, 13f * sy), Offset(16f * sx, 13f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
                        }
                    }
                }

                // Grid de 3 acciones secundarias: Cambiar, Unir, Pasar a otro (.g3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Cambiar
                    BotonAccionRapida(
                        texto = "Cambiar",
                        onClick = { alCambiarMesa(mesa) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Canvas(Modifier.size(22.dp)) {
                            val sx = size.width / 24f
                            val sy = size.height / 24f
                            // Flechas de cambio
                            val p = Path().apply {
                                moveTo(16f * sx, 3f * sy)
                                lineTo(20f * sx, 7f * sy)
                                lineTo(16f * sx, 11f * sy)
                            }
                            drawPath(p, colors.ink, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawLine(colors.ink, Offset(20f * sx, 7f * sy), Offset(4f * sx, 7f * sy), strokeWidth = 2.2f * sx, cap = StrokeCap.Round)

                            val p2 = Path().apply {
                                moveTo(8f * sx, 21f * sy)
                                lineTo(4f * sx, 17f * sy)
                                lineTo(8f * sx, 13f * sy)
                            }
                            drawPath(p2, colors.ink, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawLine(colors.ink, Offset(4f * sx, 17f * sy), Offset(20f * sx, 17f * sy), strokeWidth = 2.2f * sx, cap = StrokeCap.Round)
                        }
                    }

                    // Unir
                    BotonAccionRapida(
                        texto = "Unir",
                        onClick = { alUnirMesas(mesa) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Canvas(Modifier.size(22.dp)) {
                            val sx = size.width / 24f
                            val sy = size.height / 24f
                            val p1 = Path().apply {
                                moveTo(10f * sx, 13f * sy)
                                cubicTo(11f * sx, 15f * sy, 13f * sx, 15.5f * sy, 15f * sx, 14.5f * sy)
                                lineTo(18f * sx, 11.5f * sy)
                                cubicTo(20f * sx, 9.5f * sy, 20f * sx, 6.5f * sy, 18f * sx, 4.5f * sy)
                                cubicTo(16f * sx, 2.5f * sy, 13f * sx, 2.5f * sy, 11f * sx, 4.5f * sy)
                                lineTo(9.5f * sx, 6f * sy)
                            }
                            drawPath(p1, colors.ink, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round))
                            val p2 = Path().apply {
                                moveTo(14f * sx, 11f * sy)
                                cubicTo(13f * sx, 9f * sy, 11f * sx, 8.5f * sy, 9f * sx, 9.5f * sy)
                                lineTo(6f * sx, 12.5f * sy)
                                cubicTo(4f * sx, 14.5f * sy, 4f * sx, 17.5f * sy, 6f * sx, 19.5f * sy)
                                cubicTo(8f * sx, 21.5f * sy, 11f * sx, 21.5f * sy, 13f * sx, 19.5f * sy)
                                lineTo(14.5f * sx, 18f * sy)
                            }
                            drawPath(p2, colors.ink, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round))
                        }
                    }

                    // Pasar a otro
                    BotonAccionRapida(
                        texto = "Pasar a otro",
                        onClick = { alPasarMozo(mesa) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Canvas(Modifier.size(22.dp)) {
                            val sx = size.width / 24f
                            val sy = size.height / 24f
                            drawCircle(colors.ink, radius = 4f * sx, center = Offset(12f * sx, 7f * sy), style = Stroke(width = 2.2f * sx))
                            val p = Path().apply {
                                moveTo(4f * sx, 21f * sy)
                                lineTo(4f * sx, 19f * sy)
                                cubicTo(4f * sx, 15f * sy, 7.5f * sx, 14f * sy, 12f * sx, 14f * sy)
                                cubicTo(16.5f * sx, 14f * sy, 20f * sx, 15f * sy, 20f * sx, 19f * sy)
                                lineTo(20f * sx, 21f * sy)
                            }
                            drawPath(p, colors.ink, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round))
                        }
                    }
                }

                // Pie informativo
                BasicText(
                    text = "Se abre al mantener pulsada una mesa.",
                    style = type.caption.copy(
                        color = colors.ink3,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun MetricaCaja(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            text = etiqueta,
            style = type.caption.copy(
                color = colors.ink3,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        BasicText(
            text = valor,
            style = type.callout.copy(
                color = colors.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

@Composable
private fun BotonAccionPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = RikosMotion.press(),
        label = "btnPriPress",
    )
    val colors = Rikos.colors
    val type = Rikos.type
    val btnBg = if (colors.isDark) colors.primary else colors.navy
    val btnFg = if (colors.isDark) colors.onPrimary else Color.White

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(btnBg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = texto,
            style = type.callout.copy(
                color = btnFg,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

@Composable
private fun BotonAccionSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = RikosMotion.press(),
        label = "btnSecPress",
    )
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface2)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        BasicText(
            text = texto,
            style = type.callout.copy(
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

@Composable
private fun BotonAccionRapida(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = RikosMotion.press(),
        label = "btnCardPress",
    )
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        BasicText(
            text = texto,
            style = type.caption.copy(
                color = colors.ink,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
