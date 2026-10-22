package pe.rikos.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.core.designsystem.theme.RikosRadius
import pe.rikos.app.core.designsystem.theme.RikosSpace
import pe.rikos.app.core.designsystem.theme.RikosTouch

enum class RkButtonKind { Primary, Secondary, Danger, DangerSolid }

@Composable
fun RkButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: RkButtonKind = RkButtonKind.Primary,
    habilitado: Boolean = true,
) {
    val c = Rikos.colors
    val fondo = when (kind) {
        RkButtonKind.Primary -> c.primary
        RkButtonKind.Secondary -> c.surface
        RkButtonKind.Danger -> c.redSoft
        RkButtonKind.DangerSolid -> c.red
    }
    val textoColor = when (kind) {
        RkButtonKind.Primary -> c.onPrimary
        RkButtonKind.Secondary -> c.ink
        RkButtonKind.Danger -> c.red
        RkButtonKind.DangerSolid -> c.surface
    }
    val interaccion = remember { MutableInteractionSource() }
    val presionado = interaccion.collectIsPressedAsState().value
    val escala = animateFloatAsState(if (presionado) 0.96f else 1f, RikosMotion.press(), label = "press").value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RikosTouch.button)
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .alpha(if (habilitado) 1f else 0.45f)
            .background(fondo, RikosRadius.button)
            .then(if (kind == RkButtonKind.Secondary) Modifier.border(BorderStroke(1.dp, c.line), RikosRadius.button) else Modifier)
            .clickable(interaccion, indication = null, enabled = habilitado, role = Role.Button, onClick = onClick)
            .padding(horizontal = RikosSpace.s16, vertical = RikosSpace.s12),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = texto,
            style = Rikos.type.headline.copy(color = textoColor, textAlign = TextAlign.Center),
        )
    }
}

