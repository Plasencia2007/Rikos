package pe.rikos.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.core.designsystem.theme.RikosRadius
import pe.rikos.app.core.designsystem.theme.RikosSpace

/**
 * Tarjeta de opción con selector radial (`.opt` en el lienzo de diseño).
 * Usado en M01 para seleccionar local y en formularios de selección única.
 */
@Composable
fun RkOptionCard(
    titulo: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    enabled: Boolean = true,
) {
    val c = Rikos.colors
    val type = Rikos.type
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado && enabled) 0.98f else 1f,
        animationSpec = RikosMotion.press(),
        label = "escalaOpcion",
    )

    val fondo by animateColorAsState(
        targetValue = when {
            seleccionado -> c.primarySoft
            else -> c.surface
        },
        animationSpec = RikosMotion.color(),
        label = "fondoOpcion",
    )

    val borde by animateColorAsState(
        targetValue = when {
            seleccionado -> c.primary
            else -> if (c.isDark) c.line else Color(0xFFE2E8F0)
        },
        animationSpec = RikosMotion.color(),
        label = "bordeOpcion",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .background(fondo, RikosRadius.button)
            .border(1.5.dp, borde, RikosRadius.button)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Indicador de radio circular (.rd en CSS)
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if (seleccionado) c.primary else Color.Transparent, CircleShape)
                .border(
                    width = 2.dp,
                    color = if (seleccionado) c.primary else if (c.isDark) c.ink3 else Color(0xFFCBD5E1),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (seleccionado) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(c.onPrimary, CircleShape),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = titulo,
                style = type.body.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.ink,
                ),
            )
            if (subtitulo != null) {
                BasicText(
                    text = subtitulo,
                    style = type.footnote.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.ink3,
                    ),
                )
            }
        }
    }
}
