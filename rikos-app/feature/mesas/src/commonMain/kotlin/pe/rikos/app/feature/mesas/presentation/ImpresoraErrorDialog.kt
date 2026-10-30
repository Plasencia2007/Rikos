package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
 * M12d · Impresora sin respuesta
 * Canvas ID: 7c8e4f3b-a8b6-4c16-a6a1-c386c29b3f72
 *
 * Diálogo flotante central de alerta cuando la impresora térmica de cocina caliente
 * no responde (sin papel o desconectada), ofreciendo redirigir la impresión a la
 * impresora de la barra o reintentar la conexión.
 */

@Composable
fun ImpresoraErrorDialog(
    visible: Boolean,
    comandaNumero: Int = 15,
    alUsarBarra: () -> Unit,
    alReintentar: () -> Unit,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180, easing = RikosMotion.Ease)),
        exit = fadeOut(tween(140, easing = RikosMotion.Ease)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .semantics { contentDescription = "Alerta de impresora sin respuesta" },
            contentAlignment = Alignment.Center,
        ) {
            // Scrim (fondo atenuado oscuro)
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

            // Tarjeta de alerta modal (.alert)
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(220, easing = RikosMotion.Ease),
                ) + fadeIn(tween(180)),
                exit = scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(150, easing = RikosMotion.Ease),
                ) + fadeOut(tween(120)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .shadow(
                            elevation = 28.dp,
                            shape = RoundedCornerShape(30.dp),
                            ambientColor = if (colors.isDark) Color.Black else Color(0x33021E43),
                            spotColor = if (colors.isDark) Color.Black else Color(0x4D021E43),
                        )
                        .clip(RoundedCornerShape(30.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.line, RoundedCornerShape(30.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Evitar que el clic en el contenido cierre el diálogo */ },
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Avatar circular con ícono de impresora térmica (.av en rojo suave)
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(colors.redSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.size(30.dp)) {
                                val p = Path().apply {
                                    // M6 9V2h12v7
                                    moveTo(size.width * 0.25f, size.height * 0.375f)
                                    lineTo(size.width * 0.25f, size.height * 0.083f)
                                    lineTo(size.width * 0.75f, size.height * 0.083f)
                                    lineTo(size.width * 0.75f, size.height * 0.375f)
                                    // M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2
                                    moveTo(size.width * 0.25f, size.height * 0.75f)
                                    lineTo(size.width * 0.167f, size.height * 0.75f)
                                    lineTo(size.width * 0.083f, size.height * 0.667f)
                                    lineTo(size.width * 0.083f, size.height * 0.458f)
                                    lineTo(size.width * 0.167f, size.height * 0.375f)
                                    lineTo(size.width * 0.833f, size.height * 0.375f)
                                    lineTo(size.width * 0.917f, size.height * 0.458f)
                                    lineTo(size.width * 0.917f, size.height * 0.667f)
                                    lineTo(size.width * 0.833f, size.height * 0.75f)
                                    lineTo(size.width * 0.75f, size.height * 0.75f)
                                    // M6 14h12v8H6z
                                    moveTo(size.width * 0.25f, size.height * 0.583f)
                                    lineTo(size.width * 0.75f, size.height * 0.583f)
                                    lineTo(size.width * 0.75f, size.height * 0.917f)
                                    lineTo(size.width * 0.25f, size.height * 0.917f)
                                    close()
                                }
                                drawPath(
                                    path = p,
                                    color = colors.red,
                                    style = Stroke(
                                        width = 2.4.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                    ),
                                )
                            }
                        }

                        // Título de la alerta (.alert h2)
                        BasicText(
                            text = "La impresora de cocina no responde",
                            style = type.title2.copy(
                                color = colors.ink,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp,
                            ),
                        )

                        // Descripción con mensaje de tranquilidad (la comanda sí llegó al KDS)
                        BasicText(
                            text = "La comanda $comandaNumero sí llegó a la pantalla de cocina. Solo falta el papel impreso.",
                            style = type.subhead.copy(
                                color = colors.ink2,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                            ),
                        )

                        Spacer(Modifier.height(2.dp))

                        // Tarjeta agrupada con estado de impresoras (.card con .li)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surface2)
                                .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Fila 1: Cocina caliente (Sin papel)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    BasicText(
                                        text = "Cocina caliente",
                                        style = type.body.copy(
                                            color = colors.ink,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.redSoft)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        BasicText(
                                            text = "Sin papel",
                                            style = type.caption.copy(
                                                color = colors.red,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.line)
                                )

                                // Fila 2: Barra (Lista)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    BasicText(
                                        text = "Barra",
                                        style = type.body.copy(
                                            color = colors.ink,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.greenSoft)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        BasicText(
                                            text = "Lista",
                                            style = type.caption.copy(
                                                color = colors.greenText,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Fila de botones de acción (.btns)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Botón secundario: Usar barra (.btn .sec)
                            val secInteraction = remember { MutableInteractionSource() }
                            val secPressed by secInteraction.collectIsPressedAsState()
                            val secScale by animateFloatAsState(
                                targetValue = if (secPressed) 0.98f else 1f,
                                animationSpec = RikosMotion.press(),
                                label = "btnSecPress",
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
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
                                        onClick = alUsarBarra,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text = "Usar barra",
                                    style = type.headline.copy(
                                        color = colors.ink,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }

                            // Botón principal: Reintentar con ícono (.btn .pri)
                            val priInteraction = remember { MutableInteractionSource() }
                            val priPressed by priInteraction.collectIsPressedAsState()
                            val priScale by animateFloatAsState(
                                targetValue = if (priPressed) 0.98f else 1f,
                                animationSpec = RikosMotion.press(),
                                label = "btnPriPress",
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
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
                                        onClick = alReintentar,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // Ícono de reintento/loop: M23 4v6h-6 M1 20v-6h6 M3.5 9a9 9 0 0 1 14.9-3.4L23 10 M1 14l4.6 4.4A9 9 0 0 0 20.5 15
                                    Canvas(modifier = Modifier.size(17.dp)) {
                                        val w = size.width
                                        val h = size.height

                                        val path = Path().apply {
                                            // Top arrow
                                            moveTo(w * 0.95f, h * 0.17f)
                                            lineTo(w * 0.95f, h * 0.42f)
                                            lineTo(w * 0.70f, h * 0.42f)

                                            // Bottom arrow
                                            moveTo(w * 0.05f, h * 0.83f)
                                            lineTo(w * 0.05f, h * 0.58f)
                                            lineTo(w * 0.30f, h * 0.58f)
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color.White,
                                            style = Stroke(
                                                width = 2.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round,
                                            ),
                                        )

                                        // Arcs
                                        drawArc(
                                            color = Color.White,
                                            startAngle = 180f,
                                            sweepAngle = 160f,
                                            useCenter = false,
                                            style = Stroke(
                                                width = 2.dp.toPx(),
                                                cap = StrokeCap.Round,
                                            ),
                                            topLeft = Offset(w * 0.12f, h * 0.15f),
                                            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.70f),
                                        )
                                        drawArc(
                                            color = Color.White,
                                            startAngle = 0f,
                                            sweepAngle = 160f,
                                            useCenter = false,
                                            style = Stroke(
                                                width = 2.dp.toPx(),
                                                cap = StrokeCap.Round,
                                            ),
                                            topLeft = Offset(w * 0.12f, h * 0.15f),
                                            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.70f),
                                        )
                                    }

                                    BasicText(
                                        text = "Reintentar",
                                        style = type.headline.copy(
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
