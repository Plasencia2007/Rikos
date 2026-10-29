package pe.rikos.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.core.designsystem.theme.RikosRadius
import pe.rikos.app.core.designsystem.theme.RikosSpace
import pe.rikos.app.core.designsystem.theme.RikosTouch

/**
 * Campo de texto estándar del sistema de diseño Rikos (`01-sistema-de-diseno.md`).
 * Altura táctil mínima de 56 dp, radio de 16 dp, soporte de ícono prefijo y foco animado.
 */
@Composable
fun RkField(
    valor: String,
    alCambiarValor: (String) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String? = null,
    placeholder: String? = null,
    iconoPrefijo: (@Composable () -> Unit)? = null,
    error: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val c = Rikos.colors
    val type = Rikos.type
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()

    val colorBorde by animateColorAsState(
        targetValue = when {
            error != null -> c.red
            enfocado -> c.primary
            else -> if (c.isDark) c.line else Color(0xFFCBD5E1)
        },
        animationSpec = RikosMotion.color(),
        label = "bordeCampo",
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RikosSpace.s8)) {
        if (etiqueta != null) {
            BasicText(
                text = etiqueta,
                style = type.subhead.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (error != null) c.red else c.ink2,
                ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RikosTouch.button)
                .background(c.surface, RikosRadius.field)
                .border(1.5.dp, colorBorde, RikosRadius.field)
                .padding(horizontal = RikosSpace.s16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (iconoPrefijo != null) {
                Box(contentAlignment = Alignment.Center) {
                    iconoPrefijo()
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (valor.isEmpty() && placeholder != null) {
                    BasicText(
                        text = placeholder,
                        style = type.body.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = c.ink3,
                        ),
                    )
                }

                BasicTextField(
                    value = valor,
                    onValueChange = alCambiarValor,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = type.body.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.ink,
                    ),
                    cursorBrush = SolidColor(c.primary),
                    interactionSource = interaccion,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                )
            }
        }

        if (error != null) {
            BasicText(
                text = error,
                style = type.footnote.copy(color = c.red, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
