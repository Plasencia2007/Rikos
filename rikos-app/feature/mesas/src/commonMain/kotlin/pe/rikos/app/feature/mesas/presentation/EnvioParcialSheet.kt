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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion

/**
 * M12c · Envío parcial
 * Canvas ID: 0c3aba45-34c5-4388-aa69-d43f75c6e49c
 *
 * BottomSheet modal que se presenta cuando ocurre una interrupción de red
 * o fallo parcial durante el despacho a cocina, mostrando qué platos sí
 * llegaron y cuáles quedaron pendientes para evitar duplicar pedidos.
 */

data class ItemEnvioParcial(
    val id: String,
    val nombre: String,
    val llegoACocina: Boolean,
)

@Composable
fun EnvioParcialSheet(
    visible: Boolean,
    items: List<ItemEnvioParcial> = listOf(
        ItemEnvioParcial("1", "Ceviche clásico", true),
        ItemEnvioParcial("2", "Causa limeña", true),
        ItemEnvioParcial("3", "Ají de gallina", false),
    ),
    alEnviarPendientes: (pendientes: List<ItemEnvioParcial>) -> Unit,
    alVolverALaCuenta: () -> Unit,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val llegaronCount = items.count { it.llegoACocina }
    val totalCount = items.size
    val pendientes = items.filter { !it.llegoACocina }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180, easing = RikosMotion.Ease)),
        exit = fadeOut(tween(140, easing = RikosMotion.Ease)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .semantics { contentDescription = "Modal de envío parcial a cocina" },
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Fondo atenuado (scrim)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (colors.isDark) 0.65f else 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = alCerrar,
                    ),
            )

            // Contenedor principal de la hoja modal (.sheet)
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(240, easing = RikosMotion.Ease),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(180, easing = RikosMotion.Ease),
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            ambientColor = if (colors.isDark) Color.Black else Color(0x33021E43),
                            spotColor = if (colors.isDark) Color.Black else Color(0x40021E43),
                        )
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(colors.surface)
                        .border(
                            width = 1.dp,
                            color = colors.line,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Evitar que el clic cierre la hoja */ },
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Barra de arrastre (.grab)
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.line)
                        )

                        // Fila de encabezado (.row con avatar de advertencia)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            // Avatar de advertencia ámbar (.av)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.amberSoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.size(22.dp)) {
                                    // Triángulo de advertencia SVG
                                    // Path: M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z
                                    val w = size.width
                                    val h = size.height
                                    val triangle = Path().apply {
                                        moveTo(w * 0.5f, h * 0.16f)
                                        lineTo(w * 0.08f, h * 0.82f)
                                        lineTo(w * 0.92f, h * 0.82f)
                                        close()
                                    }
                                    drawPath(
                                        path = triangle,
                                        color = colors.amberText,
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round,
                                        ),
                                    )

                                    // Signo de exclamación: M12 9v4 M12 17h.01
                                    drawLine(
                                        color = colors.amberText,
                                        start = Offset(w * 0.5f, h * 0.40f),
                                        end = Offset(w * 0.5f, h * 0.58f),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                    )
                                    drawCircle(
                                        color = colors.amberText,
                                        radius = 1.2.dp.toPx(),
                                        center = Offset(w * 0.5f, h * 0.71f),
                                    )
                                }
                            }

                            // Título y explicación
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                BasicText(
                                    text = "Solo $llegaronCount de $totalCount platos llegaron a cocina",
                                    style = type.title3.copy(
                                        color = colors.ink,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 24.sp,
                                    ),
                                )
                                BasicText(
                                    text = "Se cortó la señal mientras enviábamos. Revisamos qué recibió cocina para no repetir platos.",
                                    style = type.footnote.copy(
                                        color = colors.ink3,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 18.sp,
                                    ),
                                )
                            }
                        }

                        // Lista agrupada de platos (.gl)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surface2)
                                .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                items.forEachIndexed { index, item ->
                                    FilaPlatoEnvioParcial(
                                        item = item,
                                        mostrarDivisor = index < items.lastIndex,
                                    )
                                }
                            }
                        }

                        // Botón principal: Enviar solo los pendientes (.btn .pri .blk)
                        val primaryLabel = if (pendientes.size == 1) {
                            "Enviar solo ${pendientes.first().nombre.lowercase()}"
                        } else {
                            "Enviar solo los ${pendientes.size} pendientes"
                        }

                        val priInteraction = remember { MutableInteractionSource() }
                        val priPressed by priInteraction.collectIsPressedAsState()
                        val priScale by animateFloatAsState(
                            targetValue = if (priPressed) 0.98f else 1f,
                            animationSpec = RikosMotion.press(),
                            label = "btnPriPress",
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .graphicsLayer {
                                    scaleX = priScale
                                    scaleY = priScale
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.primary)
                                .clickable(
                                    interactionSource = priInteraction,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = { alEnviarPendientes(pendientes) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = primaryLabel,
                                style = type.headline.copy(
                                    color = Color.White,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        // Botón secundario: Volver a la cuenta (.btn .sec .blk)
                        val secInteraction = remember { MutableInteractionSource() }
                        val secPressed by secInteraction.collectIsPressedAsState()
                        val secScale by animateFloatAsState(
                            targetValue = if (secPressed) 0.98f else 1f,
                            animationSpec = RikosMotion.press(),
                            label = "btnSecPress",
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .graphicsLayer {
                                    scaleX = secScale
                                    scaleY = secScale
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface2)
                                .border(1.5.dp, colors.line, RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = secInteraction,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = alVolverALaCuenta,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "Volver a la cuenta",
                                style = type.headline.copy(
                                    color = colors.ink,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        // Nota explicativa inferior (.xs .c3)
                        BasicText(
                            text = "Lo que ya está en cocina nunca se vuelve a enviar.",
                            style = type.footnote.copy(
                                color = colors.ink3,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                            ),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila individual del listado de platos (.r).
 */
@Composable
private fun FilaPlatoEnvioParcial(
    item: ItemEnvioParcial,
    mostrarDivisor: Boolean,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Ícono de estado (Check verde o Cruz roja)
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    if (item.llegoACocina) {
                        // Check verde: M20 6L9 17l-5-5
                        val checkPath = Path().apply {
                            moveTo(w * 0.18f, h * 0.52f)
                            lineTo(w * 0.42f, h * 0.78f)
                            lineTo(w * 0.85f, h * 0.24f)
                        }
                        drawPath(
                            path = checkPath,
                            color = colors.green,
                            style = Stroke(
                                width = 2.4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    } else {
                        // Cruz roja: M18 6L6 18 M6 6l12 12
                        drawLine(
                            color = colors.red,
                            start = Offset(w * 0.22f, h * 0.22f),
                            end = Offset(w * 0.78f, h * 0.78f),
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = colors.red,
                            start = Offset(w * 0.78f, h * 0.22f),
                            end = Offset(w * 0.22f, h * 0.78f),
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }

            // Nombre del plato
            BasicText(
                text = item.nombre,
                style = type.body.copy(
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.weight(1f),
            )

            // Badge del estado (.xs .b)
            BasicText(
                text = if (item.llegoACocina) "En cocina" else "No llegó",
                style = type.caption.copy(
                    color = if (item.llegoACocina) colors.greenText else colors.red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        if (mostrarDivisor) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line)
            )
        }
    }
}
