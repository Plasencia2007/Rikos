package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion

/**
 * Modelo para plato o bebida en gestión de disponibilidad / agotados.
 */
data class PlatoDisponibilidad(
    val id: String,
    val nombre: String,
    val categoriaDetalle: String,
    val agotado: Boolean,
    val stockRestante: Int? = null,
)

/**
 * Pantalla M06 · Agotados (Disponibilidad).
 * Permite marcar platos y bebidas como agotados o reactivarlos con motivo para la bitácora.
 */
@Composable
fun AgotadosScreen(
    enLinea: Boolean = true,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    // Lista base fiel al lienzo
    var items by remember {
        mutableStateOf(
            listOf(
                PlatoDisponibilidad(id = "1", nombre = "Tallarín rojo", categoriaDetalle = "Fondos, desde 12:10", agotado = true),
                PlatoDisponibilidad(id = "2", nombre = "Chicha morada", categoriaDetalle = "Bebidas, desde 12:40", agotado = true),
                PlatoDisponibilidad(id = "3", nombre = "Lomo saltado", categoriaDetalle = "Fondos, quedan 4", agotado = false, stockRestante = 4),
                PlatoDisponibilidad(id = "4", nombre = "Ají de gallina", categoriaDetalle = "Fondos", agotado = false),
                PlatoDisponibilidad(id = "5", nombre = "Sopa de verduras", categoriaDetalle = "Entradas", agotado = false),
                PlatoDisponibilidad(id = "6", nombre = "Helado", categoriaDetalle = "Postres, quedan 2", agotado = false, stockRestante = 2),
                PlatoDisponibilidad(id = "7", nombre = "Maracuyá", categoriaDetalle = "Bebidas", agotado = false),
            )
        )
    }

    var textoBusqueda by remember { mutableStateOf("") }
    var itemParaReactivar by remember { mutableStateOf<PlatoDisponibilidad?>(null) }
    var motivoSeleccionado by remember { mutableStateOf(0) }
    var toastMensaje by remember { mutableStateOf<String?>(null) }

    // Opciones de motivos para reactivar plato
    val motivos = remember {
        listOf(
            "Llegó mercadería",
            "Se marcó por error",
            "Cocina preparó más",
        )
    }

    // Auto-cierre del Toast
    LaunchedEffect(toastMensaje) {
        if (toastMensaje != null) {
            delay(3200)
            toastMensaje = null
        }
    }

    // Filtro por texto de búsqueda
    val itemsFiltrados = remember(items, textoBusqueda) {
        if (textoBusqueda.isBlank()) items
        else items.filter {
            it.nombre.contains(textoBusqueda, ignoreCase = true) ||
                it.categoriaDetalle.contains(textoBusqueda, ignoreCase = true)
        }
    }

    val totalDisponibles = items.count { !it.agotado }
    val totalAgotados = items.count { it.agotado }
    val totalPocos = items.count { !it.agotado && it.stockRestante != null && it.stockRestante <= 4 }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            // Cabecera superior (.tbar)
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
                            style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }

                // Badge de conexión "En línea"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (enLinea) colors.green else colors.red),
                    )
                    BasicText(
                        text = if (enLinea) "En línea" else "Sin conexión",
                        style = type.caption.copy(
                            color = colors.ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Título y subtítulo
            BasicText(
                text = "Disponibilidad",
                style = type.title1.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.ink,
                ),
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = "Se avisa a todos los equipos al instante",
                style = type.subhead.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink3,
                ),
            )

            Spacer(Modifier.height(14.dp))

            // Barra de búsqueda (.fx)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface2)
                    .border(1.5.dp, colors.line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Ícono lupa
                Canvas(Modifier.size(20.dp)) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    drawCircle(
                        color = colors.ink3,
                        radius = 8f * sx,
                        center = Offset(11f * sx, 11f * sy),
                        style = Stroke(width = 2.2f * sx),
                    )
                    drawLine(
                        color = colors.ink3,
                        start = Offset(16.6f * sx, 16.6f * sy),
                        end = Offset(21f * sx, 21f * sy),
                        strokeWidth = 2.2f * sx,
                        cap = StrokeCap.Round,
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (textoBusqueda.isEmpty()) {
                        BasicText(
                            text = "Buscar plato o bebida",
                            style = TextStyle(
                                color = colors.ink3,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                    BasicTextField(
                        value = textoBusqueda,
                        onValueChange = { textoBusqueda = it },
                        textStyle = TextStyle(
                            color = colors.ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                if (textoBusqueda.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colors.line)
                            .clickable { textoBusqueda = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(12.dp)) {
                            drawLine(colors.ink2, Offset(2f, 2f), Offset(size.width - 2f, size.height - 2f), strokeWidth = 2f, cap = StrokeCap.Round)
                            drawLine(colors.ink2, Offset(size.width - 2f, 2f), Offset(2f, size.height - 2f), strokeWidth = 2f, cap = StrokeCap.Round)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tarjetas de métricas (.tri)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Disponibles
                TarjetaMetrica(
                    etiqueta = "Disponibles",
                    valor = "$totalDisponibles",
                    fondo = colors.surface,
                    colorEtiqueta = colors.ink3,
                    colorValor = colors.ink,
                    borde = 1.dp to colors.line,
                    modifier = Modifier.weight(1f),
                )
                // Agotados
                TarjetaMetrica(
                    etiqueta = "Agotados",
                    valor = "$totalAgotados",
                    fondo = colors.redSoft,
                    colorEtiqueta = colors.red,
                    colorValor = colors.red,
                    modifier = Modifier.weight(1f),
                )
                // Pocos
                TarjetaMetrica(
                    etiqueta = "Pocos",
                    valor = "$totalPocos",
                    fondo = colors.amberSoft,
                    colorEtiqueta = colors.amberStrong,
                    colorValor = colors.amberStrong,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Lista de platos (.gl)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
                    ) {
                        itemsFiltrados.forEachIndexed { index, plato ->
                            FilaPlatoDisponibilidad(
                                plato = plato,
                                esUltimo = index == itemsFiltrados.lastIndex,
                                onToggle = {
                                    if (plato.agotado) {
                                        // Abrir bottom sheet para elegir motivo
                                        itemParaReactivar = plato
                                        motivoSeleccionado = 0
                                    } else {
                                        // Marcar directamente como agotado
                                        items = items.map {
                                            if (it.id == plato.id) it.copy(agotado = true) else it
                                        }
                                        toastMensaje = "${plato.nombre} marcado agotado"
                                    }
                                },
                            )
                        }
                    }
                }

                // Nota informativa (.nt.nt-warn)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.amberSoft)
                            .border(1.dp, colors.amber.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Ícono advertencia triángulo
                        Canvas(Modifier.size(22.dp)) {
                            val sx = size.width / 24f
                            val sy = size.height / 24f
                            val p = Path().apply {
                                moveTo(10.3f * sx, 3.9f * sy)
                                lineTo(1.8f * sx, 18f * sy)
                                cubicTo(1.2f * sx, 19f * sy, 1.8f * sx, 21f * sy, 3.5f * sx, 21f * sy)
                                lineTo(20.5f * sx, 21f * sy)
                                cubicTo(22.2f * sx, 21f * sy, 22.8f * sx, 19f * sy, 22.2f * sx, 18f * sy)
                                lineTo(13.7f * sx, 3.9f * sy)
                                cubicTo(12.9f * sx, 2.5f * sy, 11.1f * sx, 2.5f * sy, 10.3f * sx, 3.9f * sy)
                                close()
                            }
                            drawPath(p, colors.amberStrong, style = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawLine(colors.amberStrong, Offset(12f * sx, 9f * sy), Offset(12f * sx, 13f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
                            drawCircle(colors.amberStrong, radius = 1.2f * sx, center = Offset(12f * sx, 17f * sy))
                        }

                        BasicText(
                            text = "Si igual se vende un plato agotado, se permite y el stock queda en negativo.",
                            style = type.body.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.amberText,
                                lineHeight = 19.sp,
                            ),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Sheet inferior modal de reactivación
        if (itemParaReactivar != null) {
            val platoActual = itemParaReactivar!!
            ReactivarPlatoSheet(
                platoNombre = platoActual.nombre,
                motivos = motivos,
                motivoSeleccionado = motivoSeleccionado,
                alSeleccionarMotivo = { motivoSeleccionado = it },
                alCerrar = { itemParaReactivar = null },
                alConfirmar = {
                    items = items.map {
                        if (it.id == platoActual.id) it.copy(agotado = false) else it
                    }
                    val nombrePlato = platoActual.nombre
                    itemParaReactivar = null
                    toastMensaje = "$nombrePlato disponible otra vez"
                },
            )
        }

        // Toast flotante superior
        AnimatedVisibility(
            visible = toastMensaje != null,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 14.dp, end = 14.dp),
        ) {
            toastMensaje?.let { mensaje ->
                ToastDisponibilidad(mensaje = mensaje)
            }
        }
    }
}

/**
 * Tarjeta de métrica individual (.tri > div).
 */
@Composable
private fun TarjetaMetrica(
    etiqueta: String,
    valor: String,
    fondo: Color,
    colorEtiqueta: Color,
    colorValor: Color,
    modifier: Modifier = Modifier,
    borde: Pair<androidx.compose.ui.unit.Dp, Color>? = null,
) {
    val type = Rikos.type
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(fondo)
            .then(
                if (borde != null) Modifier.border(borde.first, borde.second, RoundedCornerShape(16.dp))
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            BasicText(
                text = etiqueta,
                style = type.caption.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorEtiqueta,
                ),
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = valor,
                style = type.title2.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorValor,
                ),
            )
        }
    }
}

/**
 * Fila individual de plato con switch (.gl .r).
 */
@Composable
private fun FilaPlatoDisponibilidad(
    plato: PlatoDisponibilidad,
    esUltimo: Boolean,
    onToggle: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val rowInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = rowInteraction,
                indication = null,
                onClick = onToggle,
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Avatar cuadrado con ícono de cubiertos (.av .sq)
            val fondoAvatar = if (plato.agotado) colors.redSoft else colors.amberSoft
            val colorIcono = if (plato.agotado) colors.red else colors.amberStrong

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(fondoAvatar),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(20.dp)) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f

                    // Tenedor (izq)
                    val pFork = Path().apply {
                        moveTo(3f * sx, 2f * sy)
                        lineTo(3f * sx, 9f * sy)
                        cubicTo(3f * sx, 10.1f * sy, 3.9f * sx, 11f * sy, 5f * sx, 11f * sy)
                        lineTo(7f * sx, 11f * sy)
                        cubicTo(8.1f * sx, 11f * sy, 9f * sx, 10.1f * sy, 9f * sx, 9f * sy)
                        lineTo(9f * sx, 2f * sy)
                        moveTo(6f * sx, 2f * sy)
                        lineTo(6f * sx, 22f * sy)
                    }
                    drawPath(pFork, colorIcono, style = Stroke(width = 1.8f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // Cuchillo (der)
                    val pKnife = Path().apply {
                        moveTo(21f * sx, 15f * sy)
                        lineTo(21f * sx, 2f * sy)
                        cubicTo(18.2f * sx, 2f * sy, 16f * sx, 4.2f * sy, 16f * sx, 7f * sy)
                        lineTo(16f * sx, 13f * sy)
                        cubicTo(16f * sx, 14.1f * sy, 16.9f * sx, 15f * sy, 18f * sx, 15f * sy)
                        lineTo(21f * sx, 15f * sy)
                        moveTo(21f * sx, 15f * sy)
                        lineTo(21f * sx, 22f * sy)
                    }
                    drawPath(pKnife, colorIcono, style = Stroke(width = 1.8f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            // Datos del plato
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = plato.nombre,
                    style = type.body.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plato.agotado) colors.ink3 else colors.ink,
                        textDecoration = if (plato.agotado) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = plato.categoriaDetalle,
                    style = type.footnote.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink3,
                    ),
                )
            }

            // Switch tipo iOS / Riko's (.sw)
            SwitchDisponibilidad(
                activo = !plato.agotado,
                onToggle = onToggle,
            )
        }

        if (!esUltimo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line)
            )
        }
    }
}

/**
 * Switch nativo del diseño Riko's (.sw) de 56x32 dp.
 */
@Composable
private fun SwitchDisponibilidad(
    activo: Boolean,
    onToggle: () -> Unit,
) {
    val colors = Rikos.colors

    val thumbOffset by animateDpAsState(
        targetValue = if (activo) 26.dp else 4.dp,
        animationSpec = tween(durationMillis = 180, easing = RikosMotion.Ease),
        label = "switchThumb",
    )

    val fondoSwitch = if (activo) colors.primary else colors.surface2
    val colorThumb = if (activo) colors.onPrimary else colors.ink3

    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fondoSwitch)
            .border(
                width = 1.dp,
                color = if (activo) colors.primary else colors.line,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(colorThumb)
                .shadow(2.dp, CircleShape)
        )
    }
}

/**
 * Modal Bottom Sheet para reactivar plato agotado (.sheet).
 */
@Composable
private fun ReactivarPlatoSheet(
    platoNombre: String,
    motivos: List<String>,
    motivoSeleccionado: Int,
    alSeleccionarMotivo: (Int) -> Unit,
    alCerrar: () -> Unit,
    alConfirmar: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Dialog(
        onDismissRequest = alCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = alCerrar,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Evitar que el clic en el sheet cierre */ },
                    )
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Barra de arrastre (.grab)
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.line)
                )

                // Cabecera: Título y botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            text = "Reactivar ${platoNombre.lowercase()}",
                            style = type.title2.copy(
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.ink,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        BasicText(
                            text = "Elige el motivo; queda en la bitácora",
                            style = type.footnote.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ink3,
                            ),
                        )
                    }

                    // Botón cerrar (X)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surface2)
                            .clickable(onClick = alCerrar),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(16.dp)) {
                            val sx = size.width / 24f
                            val sy = size.height / 24f
                            drawLine(colors.ink, Offset(18f * sx, 6f * sy), Offset(6f * sx, 18f * sy), strokeWidth = 2.4f * sx, cap = StrokeCap.Round)
                            drawLine(colors.ink, Offset(6f * sx, 6f * sy), Offset(18f * sx, 18f * sy), strokeWidth = 2.4f * sx, cap = StrokeCap.Round)
                        }
                    }
                }

                // Lista de motivos con radio buttons (.gl)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.line, RoundedCornerShape(16.dp)),
                ) {
                    motivos.forEachIndexed { index, motivo ->
                        val seleccionado = index == motivoSeleccionado
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { alSeleccionarMotivo(index) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            BasicText(
                                text = motivo,
                                style = type.body.copy(
                                    fontSize = 15.5.sp,
                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (seleccionado) colors.ink else colors.ink2,
                                ),
                            )

                            // Radio button circular (.rr)
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (seleccionado) colors.primary else Color.Transparent)
                                    .border(
                                        width = if (seleccionado) 0.dp else 2.dp,
                                        color = if (seleccionado) colors.primary else colors.ink3.copy(alpha = 0.5f),
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (seleccionado) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.onPrimary)
                                    )
                                }
                            }
                        }

                        if (index < motivos.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.line)
                            )
                        }
                    }
                }

                // Botón principal "Volver a ofrecer"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.primary)
                        .clickable(onClick = alConfirmar),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Volver a ofrecer",
                        style = type.headline.copy(
                            color = colors.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Toast flotante de confirmación (.toast).
 */
@Composable
private fun ToastDisponibilidad(
    mensaje: String,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.navy)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Ícono check con fondo verde
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(18.dp)) {
                val sx = size.width / 24f
                val sy = size.height / 24f
                val p = Path().apply {
                    moveTo(20f * sx, 6f * sy)
                    lineTo(9f * sx, 17f * sy)
                    lineTo(4f * sx, 12f * sy)
                }
                drawPath(p, colors.onPrimary, style = Stroke(width = 2.5f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = mensaje,
                style = type.headline.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
            )
            BasicText(
                text = "Todos los mozos ya lo ven",
                style = type.caption.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.75f),
                ),
            )
        }
    }
}
