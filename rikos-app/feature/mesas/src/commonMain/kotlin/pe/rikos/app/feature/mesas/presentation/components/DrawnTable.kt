package pe.rikos.app.feature.mesas.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.feature.mesas.domain.EstadoMesa
import pe.rikos.app.feature.mesas.domain.FormaMesa
import pe.rikos.app.feature.mesas.domain.Mesa

data class MesaColores(
    val silla: Color,
    val mesaFondo: Color,
    val mesaBorde: Color,
    val numero: Color,
    val badgeFondo: Color,
    val badgeTexto: Color,
)

@Composable
fun mesaColores(mesa: Mesa): MesaColores {
    val colors = Rikos.colors
    return when {
        mesa.estado == EstadoMesa.LIBRE -> MesaColores(
            silla = colors.green,
            mesaFondo = colors.greenSoft,
            mesaBorde = colors.green,
            numero = colors.greenText,
            badgeFondo = colors.greenSoft,
            badgeTexto = colors.greenText,
        )
        mesa.estado == EstadoMesa.POR_COBRAR -> MesaColores(
            silla = colors.blue,
            mesaFondo = colors.blueSoft,
            mesaBorde = colors.blue,
            numero = colors.blue,
            badgeFondo = colors.blueSoft,
            badgeTexto = colors.blue,
        )
        mesa.estado == EstadoMesa.FUERA_DE_SERVICIO -> MesaColores(
            silla = if (colors.isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
            mesaFondo = colors.surface2,
            mesaBorde = colors.line,
            numero = colors.ink3,
            badgeFondo = colors.surface2,
            badgeTexto = colors.ink3,
        )
        mesa.mozoNombre != null -> MesaColores(
            silla = colors.amber,
            mesaFondo = colors.amberSoft,
            mesaBorde = colors.amberStrong,
            numero = colors.amberText,
            badgeFondo = colors.surface2,
            badgeTexto = colors.ink3,
        )
        mesa.comidaLista -> MesaColores(
            silla = colors.amber,
            mesaFondo = colors.amberSoft,
            mesaBorde = colors.amberStrong,
            numero = colors.amberText,
            badgeFondo = colors.greenSoft,
            badgeTexto = colors.greenText,
        )
        else -> MesaColores( // Ocupada
            silla = colors.amber,
            mesaFondo = colors.amberSoft,
            mesaBorde = colors.amberStrong,
            numero = colors.amberText,
            badgeFondo = colors.amberSoft,
            badgeTexto = colors.amberText,
        )
    }
}

@Composable
fun DrawnTable(
    mesa: Mesa,
    colores: MesaColores,
    modifier: Modifier = Modifier,
) {
    when (mesa.forma) {
        FormaMesa.UNIDA -> DrawnWideTable(mesa = mesa, colores = colores, modifier = modifier)
        FormaMesa.REDONDA -> DrawnRoundTable(mesa = mesa, colores = colores, modifier = modifier)
        FormaMesa.RECTANGULAR -> DrawnRectTable(mesa = mesa, colores = colores, modifier = modifier)
    }
}

@Composable
private fun DrawnRectTable(
    mesa: Mesa,
    colores: MesaColores,
    modifier: Modifier = Modifier,
) {
    val type = Rikos.type
    Box(
        modifier = modifier.fillMaxWidth().height(84.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val scaleX = size.width / 120f
            val scaleY = size.height / 96f
            val scale = minOf(scaleX, scaleY)
            val offsetX = (size.width - 120f * scale) / 2f
            val offsetY = (size.height - 96f * scale) / 2f

            fun x(v: Float) = offsetX + v * scale
            fun y(v: Float) = offsetY + v * scale
            fun s(v: Float) = v * scale

            // Sillas superiores
            drawRoundRect(
                color = colores.silla,
                topLeft = Offset(x(36f), y(8f)),
                size = Size(s(20f), s(9f)),
                cornerRadius = CornerRadius(s(4f)),
            )
            drawRoundRect(
                color = colores.silla,
                topLeft = Offset(x(64f), y(8f)),
                size = Size(s(20f), s(9f)),
                cornerRadius = CornerRadius(s(4f)),
            )

            // Sillas inferiores
            drawRoundRect(
                color = colores.silla,
                topLeft = Offset(x(36f), y(79f)),
                size = Size(s(20f), s(9f)),
                cornerRadius = CornerRadius(s(4f)),
            )
            drawRoundRect(
                color = colores.silla,
                topLeft = Offset(x(64f), y(79f)),
                size = Size(s(20f), s(9f)),
                cornerRadius = CornerRadius(s(4f)),
            )

            // Silla izquierda
            drawRoundRect(
                color = colores.silla,
                topLeft = Offset(x(10f), y(34f)),
                size = Size(s(9f), s(28f)),
                cornerRadius = CornerRadius(s(4f)),
            )

            // Silla derecha
            drawRoundRect(
                color = colores.silla,
                topLeft = Offset(x(101f), y(34f)),
                size = Size(s(9f), s(28f)),
                cornerRadius = CornerRadius(s(4f)),
            )

            // Superficie de la mesa
            val strokeEffect = if (mesa.estado == EstadoMesa.FUERA_DE_SERVICIO) {
                PathEffect.dashPathEffect(floatArrayOf(s(6f), s(5f)))
            } else null

            drawRoundRect(
                color = colores.mesaFondo,
                topLeft = Offset(x(26f), y(20f)),
                size = Size(s(68f), s(56f)),
                cornerRadius = CornerRadius(s(14f)),
                style = Fill,
            )
            drawRoundRect(
                color = colores.mesaBorde,
                topLeft = Offset(x(26f), y(20f)),
                size = Size(s(68f), s(56f)),
                cornerRadius = CornerRadius(s(14f)),
                style = Stroke(width = s(2.5f), pathEffect = strokeEffect),
            )
        }

        // Número centrado con Plus Jakarta Sans
        BasicText(
            text = mesa.numero,
            style = type.title3.copy(
                color = colores.numero,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

@Composable
private fun DrawnRoundTable(
    mesa: Mesa,
    colores: MesaColores,
    modifier: Modifier = Modifier,
) {
    val type = Rikos.type
    Box(
        modifier = modifier.fillMaxWidth().height(84.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val scaleX = size.width / 120f
            val scaleY = size.height / 96f
            val scale = minOf(scaleX, scaleY)
            val offsetX = (size.width - 120f * scale) / 2f
            val offsetY = (size.height - 96f * scale) / 2f

            fun x(v: Float) = offsetX + v * scale
            fun y(v: Float) = offsetY + v * scale
            fun s(v: Float) = v * scale

            // 6 sillas circulares alrededor de (60, 48)
            val rSilla = s(7f)
            drawCircle(color = colores.silla, radius = rSilla, center = Offset(x(100f), y(48f)))
            drawCircle(color = colores.silla, radius = rSilla, center = Offset(x(80f), y(82.6f)))
            drawCircle(color = colores.silla, radius = rSilla, center = Offset(x(40f), y(82.6f)))
            drawCircle(color = colores.silla, radius = rSilla, center = Offset(x(20f), y(48f)))
            drawCircle(color = colores.silla, radius = rSilla, center = Offset(x(40f), y(13.4f)))
            drawCircle(color = colores.silla, radius = rSilla, center = Offset(x(80f), y(13.4f)))

            // Superficie redonda
            val centerMesa = Offset(x(60f), y(48f))
            val rMesa = s(28f)
            drawCircle(color = colores.mesaFondo, radius = rMesa, center = centerMesa, style = Fill)
            drawCircle(color = colores.mesaBorde, radius = rMesa, center = centerMesa, style = Stroke(width = s(2.5f)))

            // Si tiene comida lista: insignia redonda con check en (98, 16)
            if (mesa.comidaLista) {
                val centerBadge = Offset(x(98f), y(16f))
                drawCircle(color = Color(0xFF059669), radius = s(11f), center = centerBadge, style = Fill)
                val check = Path().apply {
                    moveTo(x(93f), y(16f))
                    lineTo(x(97f), y(20f))
                    lineTo(x(103f), y(12f))
                }
                drawPath(
                    path = check,
                    color = Color.White,
                    style = Stroke(width = s(2.5f), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }

        // Número centrado con Plus Jakarta Sans
        BasicText(
            text = mesa.numero,
            style = type.title3.copy(
                color = colores.numero,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

@Composable
private fun DrawnWideTable(
    mesa: Mesa,
    colores: MesaColores,
    modifier: Modifier = Modifier,
) {
    val type = Rikos.type
    Box(
        modifier = modifier.fillMaxWidth().height(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val scaleX = size.width / 300f
            val scaleY = size.height / 110f
            val scale = minOf(scaleX, scaleY)
            val offsetX = (size.width - 300f * scale) / 2f
            val offsetY = (size.height - 110f * scale) / 2f

            fun x(v: Float) = offsetX + v * scale
            fun y(v: Float) = offsetY + v * scale
            fun s(v: Float) = v * scale

            // 4 sillas superiores
            val topChairs = listOf(52f, 112f, 162f, 222f)
            for (cx in topChairs) {
                drawRoundRect(
                    color = colores.silla,
                    topLeft = Offset(x(cx), y(8f)),
                    size = Size(s(26f), s(10f)),
                    cornerRadius = CornerRadius(s(5f)),
                )
            }

            // 4 sillas inferiores
            for (cx in topChairs) {
                drawRoundRect(
                    color = colores.silla,
                    topLeft = Offset(x(cx), y(92f)),
                    size = Size(s(26f), s(10f)),
                    cornerRadius = CornerRadius(s(5f)),
                )
            }

            // Sillas laterales
            drawRoundRect(
                color = colores.silla,
                topLeft = Offset(x(14f), y(40f)),
                size = Size(s(10f), s(30f)),
                cornerRadius = CornerRadius(s(5f)),
            )
            drawRoundRect(
                color = colores.silla,
                topLeft = Offset(x(276f), y(40f)),
                size = Size(s(10f), s(30f)),
                cornerRadius = CornerRadius(s(5f)),
            )

            // Mesa ancha unida
            drawRoundRect(
                color = colores.mesaFondo,
                topLeft = Offset(x(32f), y(24f)),
                size = Size(s(236f), s(62f)),
                cornerRadius = CornerRadius(s(16f)),
                style = Fill,
            )
            drawRoundRect(
                color = colores.mesaBorde,
                topLeft = Offset(x(32f), y(24f)),
                size = Size(s(236f), s(62f)),
                cornerRadius = CornerRadius(s(16f)),
                style = Stroke(width = s(2.5f)),
            )

            // Línea divisoria discontinua
            val dash = PathEffect.dashPathEffect(floatArrayOf(s(4f), s(5f)))
            drawLine(
                color = colores.mesaBorde,
                start = Offset(x(150f), y(30f)),
                end = Offset(x(150f), y(80f)),
                strokeWidth = s(2f),
                pathEffect = dash,
            )
        }

        // Números duales con Plus Jakarta Sans
        Row(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BasicText(
                    text = mesa.mesaPrincipalNumero ?: "3",
                    style = type.title2.copy(
                        color = colores.numero,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BasicText(
                    text = mesa.unidaCon ?: "4",
                    style = type.title2.copy(
                        color = colores.numero,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
        }
    }
}
