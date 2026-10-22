package pe.rikos.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosRadius
import pe.rikos.app.core.designsystem.theme.RikosSpace

enum class RkTone { PorEnviar, EnCocina, Listo, Cuenta, Pensionista, Anulado, Nuevo }

@Composable
fun RkTag(texto: String, tone: RkTone, modifier: Modifier = Modifier) {
    val c = Rikos.colors
    val (fondo: Color, fg: Color) = when (tone) {
        RkTone.PorEnviar -> c.amberSoft to c.amberText
        RkTone.EnCocina -> c.blueSoft to c.blue
        RkTone.Listo -> c.greenSoft to c.greenText
        RkTone.Cuenta -> c.blueSoft to c.blue
        RkTone.Pensionista -> c.purpleSoft to c.purple
        RkTone.Anulado -> c.redSoft to c.red
        RkTone.Nuevo -> c.primarySoft to c.primary
    }
    BasicText(
        text = texto,
        style = Rikos.type.caption.copy(color = fg),
        modifier = modifier
            .background(fondo, RikosRadius.tag)
            .padding(horizontal = RikosSpace.s8, vertical = RikosSpace.s4),
    )
}
