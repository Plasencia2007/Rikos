package pe.rikos.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalRikosColors = staticCompositionLocalOf { RikosLight }

/** Acceso a los tokens: `Rikos.colors.primary`. Ningún componente usa colores sueltos. */
object Rikos {
    val colors: RikosColors
        @Composable @ReadOnlyComposable get() = LocalRikosColors.current
    val type: RikosType
        @Composable get() = rememberRikosType()
}

@Composable
fun RikosTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRikosColors provides if (oscuro) RikosDark else RikosLight) {
        content()
    }
}
