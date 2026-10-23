package pe.rikos.app.feature.configuracion.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.components.RkButton
import pe.rikos.app.core.designsystem.components.RkField
import pe.rikos.app.core.designsystem.components.RkOptionCard
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosSpace
import pe.rikos.app.feature.configuracion.domain.Local

/**
 * Pantalla M01 · Configurar local.
 * Vincula el equipo al servidor y permite elegir el local activo.
 */
@Composable
fun ConfigurarLocalScreen(
    vm: ConfigurarLocalViewModel,
    modifier: Modifier = Modifier,
) {
    val estado by vm.estado.collectAsState()
    ConfigurarLocalContent(
        estado = estado,
        alCambiarServidor = vm::cambiarServidor,
        alSeleccionarLocal = vm::seleccionarLocal,
        alConfirmar = vm::confirmar,
        modifier = modifier,
    )
}

/**
 * Versión sin estado de M01 para pruebas y previews.
 */
@Composable
fun ConfigurarLocalContent(
    estado: ConfigurarLocalState,
    alCambiarServidor: (String) -> Unit,
    alSeleccionarLocal: (Local) -> Unit,
    alConfirmar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Rikos.colors
    val type = Rikos.type

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(c.bg),
        contentAlignment = Alignment.TopCenter,
    ) {
        val altoDisponible = maxHeight

        Column(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .heightIn(min = altoDisponible)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RikosSpace.screenMargin)
                .padding(top = 36.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Sección superior con ícono, títulos, campo y opciones
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                // Ícono squircle verde con cubiertos
                IconoCubiertos(
                    modifier = Modifier.size(64.dp),
                    fondo = c.primary,
                    colorTrazo = c.onPrimary,
                )

                // Bloque de títulos
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText(
                        text = "Conecta este equipo",
                        modifier = Modifier.semantics { heading() },
                        style = type.title1.copy(
                            fontSize = 36.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.ink,
                        ),
                    )
                    BasicText(
                        text = "Se hace una sola vez. Pide la dirección al administrador.",
                        style = type.body.copy(
                            fontSize = 17.sp,
                            lineHeight = 24.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = c.ink2,
                        ),
                    )
                }

                // Campo de texto del servidor
                RkField(
                    etiqueta = "Dirección del servidor",
                    valor = estado.servidorUrl,
                    alCambiarValor = alCambiarServidor,
                    iconoPrefijo = {
                        IconoEnlace(
                            color = c.ink3,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )

                // Listado de locales encontrados
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "Locales encontrados",
                            style = type.callout.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = c.ink2,
                            ),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            IconoCheckCirculo(
                                color = c.ink3,
                                modifier = Modifier.size(18.dp),
                            )
                            BasicText(
                                text = "Responde en ${estado.latenciaMs ?: 42} ms",
                                style = type.footnote.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = c.ink3,
                                ),
                            )
                        }
                    }

                    // Tarjetas de locales
                    estado.locales.forEach { local ->
                        val seleccionado = estado.localSeleccionado?.codigo == local.codigo
                        val subtitulo = "${local.direccion}, ${local.salonesTexto}"
                        RkOptionCard(
                            titulo = local.nombre,
                            subtitulo = subtitulo,
                            seleccionado = seleccionado,
                            onClick = { alSeleccionarLocal(local) },
                        )
                    }
                }
            }

            // Botón de acción al pie
            val textoBoton = estado.localSeleccionado?.let { "Usar ${it.nombre}" } ?: "Usar local"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
            ) {
                RkButton(
                    texto = textoBoton,
                    onClick = alConfirmar,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Ícono de cubiertos del lienzo (SVG viewBox 0 0 24 24).
 */
@Composable
private fun IconoCubiertos(
    fondo: Color,
    colorTrazo: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(fondo, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            val escala = size.width / 24f
            scale(escala, pivot = Offset.Zero) {
                // Tenedor (lado izquierdo)
                val tenedor = Path().apply {
                    moveTo(3f, 2f)
                    lineTo(3f, 9f)
                    cubicTo(3f, 10.1f, 3.9f, 11f, 5f, 11f)
                    lineTo(7f, 11f)
                    cubicTo(8.1f, 11f, 9f, 10.1f, 9f, 9f)
                    lineTo(9f, 2f)
                    moveTo(6f, 2f)
                    lineTo(6f, 22f)
                }
                drawPath(
                    path = tenedor,
                    color = colorTrazo,
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                // Cuchillo (lado derecho)
                val cuchillo = Path().apply {
                    moveTo(21f, 15f)
                    lineTo(21f, 2f)
                    cubicTo(18.2f, 2f, 16f, 4.2f, 16f, 7f)
                    lineTo(16f, 13f)
                    cubicTo(16f, 14.1f, 16.9f, 15f, 18f, 15f)
                    close()
                    moveTo(21f, 15f)
                    lineTo(21f, 22f)
                }
                drawPath(
                    path = cuchillo,
                    color = colorTrazo,
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/**
 * Ícono de enlace tipo cadena (SVG viewBox 0 0 24 24).
 */
@Composable
private fun IconoEnlace(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val escala = size.width / 24f
        scale(escala, pivot = Offset.Zero) {
            val path = Path().apply {
                moveTo(10f, 13f)
                cubicTo(11.4f, 14.4f, 13.6f, 14.4f, 15f, 13f)
                lineTo(18f, 10f)
                cubicTo(19.4f, 8.6f, 19.4f, 6.4f, 18f, 5f)
                cubicTo(16.6f, 3.6f, 14.4f, 3.6f, 13f, 5f)
                lineTo(11.3f, 6.7f)

                moveTo(14f, 11f)
                cubicTo(12.6f, 9.6f, 10.4f, 9.6f, 9f, 11f)
                lineTo(6f, 14f)
                cubicTo(4.6f, 15.4f, 4.6f, 17.6f, 6f, 19f)
                cubicTo(7.4f, 20.4f, 9.6f, 20.4f, 11f, 19f)
                lineTo(12.7f, 17.3f)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * Ícono de verificación dentro de círculo (SVG viewBox 0 0 24 24).
 */
@Composable
private fun IconoCheckCirculo(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radio = size.minDimension / 2f - 1.5f
        val centro = Offset(size.width / 2f, size.height / 2f)

        // Círculo exterior
        drawCircle(
            color = color,
            radius = radio,
            center = centro,
            style = Stroke(width = 1.8f),
        )

        // Marca de check
        val escala = size.width / 24f
        val check = Path().apply {
            moveTo(7.5f * escala, 12f * escala)
            lineTo(10.5f * escala, 15f * escala)
            lineTo(16.5f * escala, 9f * escala)
        }
        drawPath(
            path = check,
            color = color,
            style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
