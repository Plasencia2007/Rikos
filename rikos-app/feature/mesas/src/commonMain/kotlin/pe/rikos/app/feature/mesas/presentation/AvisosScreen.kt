package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion

private enum class CategoriaAviso { TODOS, MESAS, SISTEMA }

private data class ItemAviso(
    val id: String,
    val insignia: String,
    val titulo: String,
    val detalle: String,
    val tiempo: String,
    val categoria: CategoriaAviso,
    val noLeido: Boolean = false,
    val colorInsigniaFondo: @Composable () -> Color,
    val colorInsigniaTexto: @Composable () -> Color,
)

/**
 * M03d · Avisos en tiempo real.
 * Pantalla que lista los avisos operativos agrupados por filtros ("Todos", "Mesas", "Sistema").
 */
@Composable
fun AvisosScreen(
    enLinea: Boolean = true,
    alVolver: () -> Unit,
    alVerAgotados: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    var filtroActivo by remember { mutableStateOf(CategoriaAviso.TODOS) }

    val todosLosAvisos = remember {
        listOf(
            ItemAviso(
                id = "1",
                insignia = "12",
                titulo = "Platos listos en cocina",
                detalle = "Ají de gallina ×2, chicha",
                tiempo = "ahora",
                categoria = CategoriaAviso.MESAS,
                noLeido = true,
                colorInsigniaFondo = { Rikos.colors.greenSoft },
                colorInsigniaTexto = { Rikos.colors.greenText },
            ),
            ItemAviso(
                id = "2",
                insignia = "6",
                titulo = "Pidió la cuenta",
                detalle = "Mesa 6, S/ 54.00",
                tiempo = "2 min",
                categoria = CategoriaAviso.MESAS,
                noLeido = true,
                colorInsigniaFondo = { Rikos.colors.blueSoft },
                colorInsigniaTexto = { Rikos.colors.blue },
            ),
            ItemAviso(
                id = "3",
                insignia = "5",
                titulo = "32 min sin pasar",
                detalle = "¿Ofrecer postre o bebida?",
                tiempo = "5 min",
                categoria = CategoriaAviso.MESAS,
                noLeido = false,
                colorInsigniaFondo = { Rikos.colors.amberSoft },
                colorInsigniaTexto = { Rikos.colors.amberText },
            ),
            ItemAviso(
                id = "4",
                insignia = "!",
                titulo = "Tallarín rojo agotado",
                detalle = "Lo marcó cocina",
                tiempo = "12 min",
                categoria = CategoriaAviso.SISTEMA,
                noLeido = false,
                colorInsigniaFondo = { Rikos.colors.redSoft },
                colorInsigniaTexto = { Rikos.colors.red },
            ),
            ItemAviso(
                id = "5",
                insignia = "R",
                titulo = "Recojo en 20 min",
                detalle = "Ana M., S/ 36.00",
                tiempo = "15 min",
                categoria = CategoriaAviso.MESAS,
                noLeido = false,
                colorInsigniaFondo = { Rikos.colors.amberSoft },
                colorInsigniaTexto = { Rikos.colors.amberText },
            ),
            ItemAviso(
                id = "6",
                insignia = "$",
                titulo = "Abono aprobado",
                detalle = "Jorge Paredes, S/ 100.00",
                tiempo = "1 h",
                categoria = CategoriaAviso.SISTEMA,
                noLeido = false,
                colorInsigniaFondo = { Rikos.colors.greenSoft },
                colorInsigniaTexto = { Rikos.colors.greenText },
            ),
        )
    }

    val avisosFiltrados = remember(filtroActivo) {
        when (filtroActivo) {
            CategoriaAviso.TODOS -> todosLosAvisos
            CategoriaAviso.MESAS -> todosLosAvisos.filter { it.categoria == CategoriaAviso.MESAS }
            CategoriaAviso.SISTEMA -> todosLosAvisos.filter { it.categoria == CategoriaAviso.SISTEMA }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp),
    ) {
        // Cabecera superior: botón volver y badge "En línea"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backInteraction = remember { MutableInteractionSource() }
            val backPressed by backInteraction.collectIsPressedAsState()
            val backScale by animateFloatAsState(
                targetValue = if (backPressed) 0.92f else 1f,
                animationSpec = RikosMotion.press(),
                label = "backPress",
            )

            // Botón volver (.ib)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { scaleX = backScale; scaleY = backScale }
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        onClick = alVolver,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(20.dp)) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    val p = Path().apply {
                        moveTo(15f * sx, 6f * sy)
                        lineTo(9f * sx, 12f * sy)
                        lineTo(15f * sx, 18f * sy)
                    }
                    drawPath(
                        path = p,
                        color = colors.ink,
                        style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round),
                    )
                }
            }

            // Pill "En línea" (.conn)
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(17.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (enLinea) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape),
                )
                BasicText(
                    text = if (enLinea) "En línea" else "Sin conexión",
                    style = type.caption.copy(
                        color = if (enLinea) colors.greenText else Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // Título y subtítulo
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 16.dp)) {
            BasicText(
                text = "Avisos",
                style = type.title1.copy(
                    color = colors.ink,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).sp,
                ),
            )
            BasicText(
                text = "En tiempo real",
                style = type.subhead.copy(
                    color = colors.ink3,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        // Segmented Control (.seg)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface2)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SegmentedBoton(
                label = "Todos",
                activo = filtroActivo == CategoriaAviso.TODOS,
                onClick = { filtroActivo = CategoriaAviso.TODOS },
                modifier = Modifier.weight(1f),
            )
            SegmentedBoton(
                label = "Mesas",
                activo = filtroActivo == CategoriaAviso.MESAS,
                onClick = { filtroActivo = CategoriaAviso.MESAS },
                modifier = Modifier.weight(1f),
            )
            SegmentedBoton(
                label = "Sistema",
                activo = filtroActivo == CategoriaAviso.SISTEMA,
                onClick = { filtroActivo = CategoriaAviso.SISTEMA },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Contenedor de lista (.card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color(0x0C0F172A),
                )
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            avisosFiltrados.forEachIndexed { index, aviso ->
                AvisoFilaItem(
                    aviso = aviso,
                    mostrarSeparador = index < avisosFiltrados.lastIndex,
                    onClick = {
                        if (aviso.id == "4" || aviso.titulo.contains("agotado", ignoreCase = true)) {
                            alVerAgotados()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SegmentedBoton(
    label: String,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = RikosMotion.press(),
        label = "segPress_$label",
    )

    Box(
        modifier = modifier
            .height(36.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (activo) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = Color(0x10000000),
                        spotColor = Color(0x15000000),
                    )
                } else Modifier,
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (activo) colors.surface else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = type.callout.copy(
                color = if (activo) colors.ink else colors.ink3,
                fontSize = 13.5.sp,
                fontWeight = if (activo) FontWeight.ExtraBold else FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun AvisoFilaItem(
    aviso: ItemAviso,
    mostrarSeparador: Boolean,
    onClick: () -> Unit = {},
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = RikosMotion.press(),
        label = "avisoFila_${aviso.id}",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Cuadro avatar (.av.sq)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(aviso.colorInsigniaFondo()),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = aviso.insignia,
                    style = type.headline.copy(
                        color = aviso.colorInsigniaTexto(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }

            // Textos centrales
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = aviso.titulo,
                    style = type.body.copy(
                        color = colors.ink,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = aviso.detalle,
                    style = type.footnote.copy(
                        color = colors.ink3,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            // Columna derecha: tiempo y punto no leído
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicText(
                    text = aviso.tiempo,
                    style = type.caption.copy(
                        color = colors.ink3,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                if (aviso.noLeido) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(colors.primary, CircleShape),
                    )
                } else {
                    Spacer(Modifier.size(9.dp))
                }
            }
        }

        if (mostrarSeparador) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line),
            )
        }
    }
}
