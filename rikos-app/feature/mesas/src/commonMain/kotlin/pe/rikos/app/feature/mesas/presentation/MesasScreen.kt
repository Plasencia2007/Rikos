package pe.rikos.app.feature.mesas.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pe.rikos.app.core.designsystem.components.MesaCard
import pe.rikos.app.core.designsystem.components.MesaEstadoUi
import pe.rikos.app.core.designsystem.components.RkButton
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosSpace
import pe.rikos.app.feature.mesas.domain.EstadoMesa
import pe.rikos.app.feature.mesas.domain.Mesa

@Composable
fun MesasScreen(
    viewModel: MesasViewModel,
    alElegirMesa: (Mesa) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsState()
    val c = Rikos.colors
    Column(modifier.fillMaxSize().background(c.bg).padding(RikosSpace.screenMargin)) {
        BasicText("Mesas", style = Rikos.type.title1.copy(color = c.ink))
        Box(Modifier.fillMaxSize().padding(top = RikosSpace.s16), contentAlignment = Alignment.TopStart) {
            when (val e = estado) {
                MesasState.Cargando -> BasicText("Cargando mesas…", style = Rikos.type.body.copy(color = c.ink3))
                is MesasState.Error -> Column(verticalArrangement = Arrangement.spacedBy(RikosSpace.s12)) {
                    BasicText("No se pudieron cargar las mesas.", style = Rikos.type.body.copy(color = c.red))
                    RkButton("Reintentar", onClick = viewModel::cargar)
                }
                is MesasState.Listo -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = RikosSpace.s32 * 4),
                    horizontalArrangement = Arrangement.spacedBy(RikosSpace.cardGap),
                    verticalArrangement = Arrangement.spacedBy(RikosSpace.cardGap),
                ) {
                    items(e.mesas, key = { it.id }) { mesa ->
                        MesaCard(
                            numero = "Mesa ${mesa.numero}",
                            estado = mesa.estado.aUi(),
                            detalle = detalle(mesa),
                            onClick = { alElegirMesa(mesa) },
                        )
                    }
                }
            }
        }
    }
}

private fun EstadoMesa.aUi() = when (this) {
    EstadoMesa.LIBRE -> MesaEstadoUi.Libre
    EstadoMesa.OCUPADA -> MesaEstadoUi.Ocupada
    EstadoMesa.POR_COBRAR -> MesaEstadoUi.PorCobrar
    EstadoMesa.LISTO -> MesaEstadoUi.Listo
    EstadoMesa.FUERA_DE_SERVICIO -> MesaEstadoUi.FueraDeServicio
}

private fun detalle(m: Mesa): String = when (m.estado) {
    EstadoMesa.LIBRE -> "Libre"
    EstadoMesa.FUERA_DE_SERVICIO -> "Fuera de servicio"
    EstadoMesa.LISTO -> "Listo · ${m.total.formatear()}"
    EstadoMesa.POR_COBRAR -> "Por cobrar · ${m.total.formatear()}"
    EstadoMesa.OCUPADA -> "${m.comensales} personas · ${m.total.formatear()}"
}
