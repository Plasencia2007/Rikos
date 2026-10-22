package pe.rikos.app.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosRadius
import pe.rikos.app.core.designsystem.theme.RikosSpace
import pe.rikos.app.feature.auth.domain.ErrorAuth
import pe.rikos.app.feature.auth.domain.IniciarConPin

@Composable
fun PinScreen(viewModel: PinViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsState()
    PinContent(estado, viewModel::alTocarTecla, viewModel::alBorrar, modifier)
}

@Composable
fun PinContent(
    estado: PinState,
    alTocarTecla: (Char) -> Unit,
    alBorrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Rikos.colors
    Column(
        modifier = modifier.fillMaxSize().background(c.bg).padding(RikosSpace.screenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText("Ingresa tu PIN", style = Rikos.type.title1.copy(color = c.ink, textAlign = TextAlign.Center))
        Spacer(Modifier.height(RikosSpace.s24))

        Row(horizontalArrangement = Arrangement.spacedBy(RikosSpace.s16)) {
            repeat(IniciarConPin.LARGO_PIN) { i ->
                val lleno = i < estado.digitos.length
                Box(
                    Modifier.size(18.dp)
                        .background(if (lleno) c.primary else c.line, CircleShape),
                )
            }
        }

        Spacer(Modifier.height(RikosSpace.s16))
        BasicText(
            text = mensaje(estado.error),
            style = Rikos.type.subhead.copy(color = if (estado.error == null) c.ink3 else c.red, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(RikosSpace.s24))

        val filas = listOf("123", "456", "789")
        Column(verticalArrangement = Arrangement.spacedBy(RikosSpace.s12)) {
            filas.forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(RikosSpace.s12)) {
                    fila.forEach { d -> Tecla(d.toString()) { alTocarTecla(d) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(RikosSpace.s12)) {
                Spacer(Modifier.size(TECLA))
                Tecla("0") { alTocarTecla('0') }
                Tecla("⌫", etiqueta = "Borrar", alTocar = alBorrar)
            }
        }
    }
}

private val TECLA = 76.dp

@Composable
private fun Tecla(texto: String, etiqueta: String = texto, alTocar: () -> Unit) {
    val c = Rikos.colors
    Box(
        modifier = Modifier.size(TECLA)
            .background(c.surface, RikosRadius.card)
            .clickable(role = Role.Button, onClickLabel = etiqueta, onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(texto, style = Rikos.type.title2.copy(color = c.ink))
    }
}

private fun mensaje(error: ErrorAuth?): String = when (error) {
    null -> " "
    ErrorAuth.PinIncorrecto -> "PIN incorrecto. Intenta de nuevo."
    ErrorAuth.Bloqueado -> "Demasiados intentos. Pide ayuda al administrador."
    ErrorAuth.SinConexion -> "Sin conexión con el local."
    ErrorAuth.Desconocido -> "Algo salió mal. Intenta de nuevo."
}
