package pe.rikos.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosRadius
import pe.rikos.app.core.designsystem.theme.RikosSpace

enum class MesaEstadoUi { Libre, Ocupada, PorCobrar, Listo, Elegida, FueraDeServicio }

@Composable
fun MesaCard(
    numero: String,
    estado: MesaEstadoUi,
    detalle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Rikos.colors
    val fondo = when (estado) {
        MesaEstadoUi.Libre -> c.surface
        MesaEstadoUi.Ocupada -> c.amberSoft
        MesaEstadoUi.PorCobrar -> c.blueSoft
        MesaEstadoUi.Listo -> c.greenSoft
        MesaEstadoUi.Elegida -> c.primarySoft
        MesaEstadoUi.FueraDeServicio -> c.surface2
    }
    val borde = when (estado) {
        MesaEstadoUi.Libre -> c.green
        MesaEstadoUi.Ocupada -> c.amber
        MesaEstadoUi.PorCobrar -> c.blue
        MesaEstadoUi.Listo -> c.green
        MesaEstadoUi.Elegida -> c.primary
        MesaEstadoUi.FueraDeServicio -> c.line
    }
    val texto = when (estado) {
        MesaEstadoUi.Ocupada -> c.amberText
        MesaEstadoUi.PorCobrar -> c.blue
        MesaEstadoUi.Listo -> c.greenText
        MesaEstadoUi.FueraDeServicio -> c.ink3
        else -> c.ink
    }
    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .background(fondo, RikosRadius.table)
            .border(BorderStroke(2.dp, borde), RikosRadius.table)
            .clickable(enabled = estado != MesaEstadoUi.FueraDeServicio, role = Role.Button, onClick = onClick)
            .padding(RikosSpace.s12),
        horizontalAlignment = Alignment.Start,
    ) {
        BasicText(numero, style = Rikos.type.title3.copy(color = c.ink))
        BasicText(detalle, style = Rikos.type.footnote.copy(color = texto))
    }
}
