package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.feature.mesas.domain.ProductoCarta
import pe.rikos.app.feature.mesas.domain.obtenerCartaInicial

/**
 * M08b · Buscar en la carta.
 * Búsqueda reactiva con filtrado en tiempo real, búsquedas recientes,
 * sugerencias rápidas, estado vacío con acción de producto libre,
 * y toast de confirmación con deshacer.
 */
@Composable
fun BuscarCartaScreen(
    mesaNumero: String,
    asientoEtiqueta: String,
    enLinea: Boolean = true,
    alVolver: () -> Unit,
    alAgregarProducto: (ProductoCarta) -> Unit,
    alDeshacerAgregar: (ProductoCarta) -> Unit = {},
    alAgregarProductoLibre: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val scope = rememberCoroutineScope()

    val cartaCompleta = remember { obtenerCartaInicial() }
    var textoBusqueda by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Estado del toast de confirmación
    var ultimoProductoAgregado by remember { mutableStateOf<ProductoCarta?>(null) }
    var mostrarToast by remember { mutableStateOf(false) }

    // Sugerencias rápidas
    val sugerencias = remember { listOf("Lomo", "Ají", "Chicha", "Postre", "Menú", "Pollo") }
    // Búsquedas recientes
    val recientes = remember { mutableStateListOf("lomo", "chicha", "postre") }

    // Filtrado en vivo (sin distinción de mayúsculas/acentos básicos)
    val queryNormalizada = textoBusqueda.trim().lowercase()
    val resultados = remember(queryNormalizada, cartaCompleta) {
        if (queryNormalizada.isEmpty()) {
            emptyList()
        } else {
            cartaCompleta.filter { producto ->
                producto.nombre.lowercase().contains(queryNormalizada) ||
                    producto.categoria.etiqueta.lowercase().contains(queryNormalizada)
            }
        }
    }

    LaunchedEffect(Unit) {
        // Enfoque automático del campo de búsqueda
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
        ) {
            // 1. Cabecera
            CabeceraBuscarCarta(
                mesaNumero = mesaNumero,
                asientoEtiqueta = asientoEtiqueta,
                enLinea = enLinea,
                alVolver = alVolver,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            // 2. Barra de búsqueda (.fx.focus)
            BarraEntradaBuscar(
                texto = textoBusqueda,
                alCambiarTexto = { textoBusqueda = it },
                alBorrarTexto = { textoBusqueda = "" },
                focusRequester = focusRequester,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(12.dp))

            // 3. Chips de sugerencias rápidas (.chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sugerencias.forEach { sug ->
                    ChipSugerencia(
                        texto = sug,
                        onClick = {
                            textoBusqueda = sug
                            if (!recientes.contains(sug.lowercase())) {
                                recientes.add(0, sug.lowercase())
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 4. Resultados de búsqueda
            if (queryNormalizada.isNotEmpty()) {
                if (resultados.isNotEmpty()) {
                    // Hay resultados
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BasicText(
                            text = "${resultados.size} ${if (resultados.size == 1) "resultado" else "resultados"}",
                            style = type.callout.copy(
                                color = colors.ink2,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        )

                        // Contenedor tarjeta blanca con esquinas redondeadas (.card)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
                        ) {
                            Column {
                                resultados.forEachIndexed { index, producto ->
                                    FilaResultadoBuscar(
                                        producto = producto,
                                        alAgregar = {
                                            alAgregarProducto(producto)
                                            ultimoProductoAgregado = producto
                                            mostrarToast = true
                                            scope.launch {
                                                delay(3400)
                                                if (ultimoProductoAgregado == producto) {
                                                    mostrarToast = false
                                                }
                                            }
                                        },
                                    )
                                    if (index < resultados.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(colors.line),
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Estado sin resultados (.none)
                    EstadoSinResultados(
                        termino = textoBusqueda,
                        alAgregarProductoLibre = alAgregarProductoLibre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 5. Búsquedas recientes
            if (recientes.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BasicText(
                        text = "Recientes",
                        style = type.callout.copy(
                            color = colors.ink2,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recientes.forEach { rec ->
                            ChipReciente(
                                texto = rec,
                                onClick = { textoBusqueda = rec },
                            )
                        }
                    }
                }
            }
        }

        // 6. Toast flotante (.toast) animado al agregar plato
        AnimatedVisibility(
            visible = mostrarToast && ultimoProductoAgregado != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp, start = 16.dp, end = 16.dp),
        ) {
            val prod = ultimoProductoAgregado
            if (prod != null) {
                ToastAgregarPlato(
                    nombreProducto = prod.nombre,
                    asientoEtiqueta = asientoEtiqueta,
                    alDeshacer = {
                        alDeshacerAgregar(prod)
                        mostrarToast = false
                    },
                )
            }
        }
    }
}

/**
 * Cabecera con botón volver, conexión y título contextual.
 */
@Composable
private fun CabeceraBuscarCarta(
    mesaNumero: String,
    asientoEtiqueta: String,
    enLinea: Boolean,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Botón volver '<'
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.line, CircleShape)
                    .clickable(role = Role.Button, onClick = alVolver),
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
                    drawPath(p, color = colors.ink, style = Stroke(width = 2.4f * sx, cap = StrokeCap.Round))
                }
            }

            // Indicador de conexión (.conn)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (enLinea) colors.greenSoft else colors.surface2)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (enLinea) Color(0xFF10B981) else colors.ink3),
                )
                BasicText(
                    text = if (enLinea) "En línea" else "Sin red",
                    style = type.footnote.copy(
                        color = if (enLinea) colors.greenText else colors.ink3,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(
                text = "Buscar",
                style = type.title1.copy(color = colors.ink),
            )
            BasicText(
                text = "Mesa $mesaNumero, para $asientoEtiqueta",
                style = type.subhead.copy(color = colors.ink3),
            )
        }
    }
}

/**
 * Campo de texto enfocado para búsqueda en vivo (.fx.focus).
 */
@Composable
private fun BarraEntradaBuscar(
    texto: String,
    alCambiarTexto: (String) -> Unit,
    alBorrarTexto: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.5.dp, colors.primary, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Ícono lupa
            Canvas(Modifier.size(20.dp)) {
                val sx = size.width / 24f
                val sy = size.height / 24f
                val p = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(3f * sx, 3f * sy, 17f * sx, 17f * sy))
                    moveTo(16f * sx, 16f * sy)
                    lineTo(21f * sx, 21f * sy)
                }
                drawPath(p, color = colors.primary, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round))
            }

            // Entrada de texto
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (texto.isEmpty()) {
                    BasicText(
                        text = "Buscar en la carta",
                        style = type.body.copy(
                            color = colors.ink3,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
                BasicTextField(
                    value = texto,
                    onValueChange = alCambiarTexto,
                    singleLine = true,
                    textStyle = type.body.copy(
                        color = colors.ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }

            // Botón borrar '✕'
            if (texto.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .clickable(role = Role.Button, onClick = alBorrarTexto),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(12.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val p = Path().apply {
                            moveTo(6f * sx, 6f * sy)
                            lineTo(18f * sx, 18f * sy)
                            moveTo(18f * sx, 6f * sy)
                            lineTo(6f * sx, 18f * sy)
                        }
                        drawPath(p, color = colors.ink2, style = Stroke(width = 2.5f * sx, cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}

/**
 * Chip de sugerencia rápida (altura 36dp).
 */
@Composable
private fun ChipSugerencia(
    texto: String,
    onClick: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = RikosMotion.press(),
        label = "chipSugPress",
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = texto,
            style = type.subhead.copy(
                color = colors.ink2,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * Chip de búsqueda reciente con ícono de historial.
 */
@Composable
private fun ChipReciente(
    texto: String,
    onClick: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = RikosMotion.press(),
        label = "chipRecPress",
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Ícono de historial / reloj
            Canvas(Modifier.size(14.dp)) {
                val sx = size.width / 24f
                val sy = size.height / 24f
                val p = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(2f * sx, 2f * sy, 22f * sx, 22f * sy))
                    moveTo(12f * sx, 7f * sy)
                    lineTo(12f * sx, 12f * sy)
                    lineTo(16f * sx, 14f * sy)
                }
                drawPath(p, color = colors.ink3, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round))
            }

            BasicText(
                text = texto,
                style = type.subhead.copy(
                    color = colors.ink2,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

/**
 * Fila de resultado de búsqueda (.li).
 */
@Composable
private fun FilaResultadoBuscar(
    producto: ProductoCarta,
    alAgregar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = RikosMotion.press(),
        label = "btnResultAdd",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Avatar cuadrado (.av.sq) con cubiertos en color ámbar suave
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.amberSoft),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(20.dp)) {
                val sx = size.width / 24f
                val sy = size.height / 24f
                // Dibujo estilizado de cubiertos (tenedor y cuchillo)
                val p = Path().apply {
                    // Tenedor
                    moveTo(6f * sx, 3f * sy)
                    lineTo(6f * sx, 10f * sy)
                    moveTo(4f * sx, 3f * sy)
                    lineTo(4f * sx, 7f * sy)
                    lineTo(8f * sx, 7f * sy)
                    lineTo(8f * sx, 3f * sy)
                    moveTo(6f * sx, 10f * sy)
                    lineTo(6f * sx, 21f * sy)
                    // Cuchillo
                    moveTo(18f * sx, 3f * sy)
                    lineTo(15f * sx, 8f * sy)
                    lineTo(15f * sx, 21f * sy)
                }
                drawPath(p, color = colors.amberText, style = Stroke(width = 1.8f * sx, cap = StrokeCap.Round))
            }
        }

        // Título y categoría / stock
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            BasicText(
                text = producto.nombre,
                style = type.callout.copy(
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BasicText(
                    text = producto.categoria.etiqueta,
                    style = type.footnote.copy(color = colors.ink3),
                )
                if (producto.stockAviso != null) {
                    BasicText(
                        text = "· ${producto.stockAviso}",
                        style = type.footnote.copy(
                            color = Color(0xFFFF6B4A),
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }

        // Precio
        BasicText(
            text = producto.precio.formatear(),
            style = type.callout.copy(
                color = colors.ink,
                fontWeight = FontWeight.ExtraBold,
            ),
        )

        // Botón agregar '+' (.addb)
        if (producto.estaAgotado) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.redSoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                BasicText(
                    text = "Agotado",
                    style = type.caption.copy(
                        color = colors.red,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = alAgregar,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    val p = Path().apply {
                        moveTo(12f * sx, 5f * sy)
                        lineTo(12f * sx, 19f * sy)
                        moveTo(5f * sx, 12f * sy)
                        lineTo(19f * sx, 12f * sy)
                    }
                    drawPath(p, color = colors.onPrimary, style = Stroke(width = 2.4f * sx, cap = StrokeCap.Round))
                }
            }
        }
    }
}

/**
 * Vista de estado vacío cuando no se encuentran platos (.none).
 */
@Composable
private fun EstadoSinResultados(
    termino: String,
    alAgregarProductoLibre: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Avatar grande con lupa
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(28.dp)) {
                val sx = size.width / 24f
                val sy = size.height / 24f
                val p = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(3f * sx, 3f * sy, 17f * sx, 17f * sy))
                    moveTo(16f * sx, 16f * sy)
                    lineTo(21f * sx, 21f * sy)
                }
                drawPath(p, color = colors.ink3, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round))
            }
        }

        BasicText(
            text = "No hay «$termino» en la carta de hoy",
            style = type.headline.copy(
                color = colors.ink,
                fontWeight = FontWeight.ExtraBold,
            ),
        )

        BasicText(
            text = "Prueba con otra palabra o agrega un producto libre.",
            style = type.subhead.copy(
                color = colors.ink3,
                lineHeight = 19.sp,
            ),
        )

        Spacer(Modifier.height(4.dp))

        // Botón secundario (.btn.sec)
        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.5.dp, colors.line, RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = alAgregarProductoLibre)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "Agregar producto libre",
                style = type.callout.copy(
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

/**
 * Toast flotante de confirmación (.toast).
 */
@Composable
private fun ToastAgregarPlato(
    nombreProducto: String,
    asientoEtiqueta: String,
    alDeshacer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(18.dp), spotColor = Color(0x60000000))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.navy)
            .clickable(role = Role.Button, onClick = alDeshacer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Círculo verde con checkmark (.ti)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    val p = Path().apply {
                        moveTo(5f * sx, 12f * sy)
                        lineTo(10f * sx, 17f * sy)
                        lineTo(19f * sx, 7f * sy)
                    }
                    drawPath(p, color = colors.onPrimary, style = Stroke(width = 2.4f * sx, cap = StrokeCap.Round))
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(
                    text = "$nombreProducto agregado a $asientoEtiqueta",
                    style = type.subhead.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                BasicText(
                    text = "Toca para deshacer",
                    style = type.caption.copy(
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
