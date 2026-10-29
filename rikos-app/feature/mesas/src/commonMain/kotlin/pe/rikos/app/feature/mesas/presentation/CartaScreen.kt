package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.feature.mesas.domain.CategoriaCarta
import pe.rikos.app.feature.mesas.domain.ProductoCarta
import pe.rikos.app.feature.mesas.domain.TipoIlustracionPlato
import pe.rikos.app.feature.mesas.domain.obtenerCartaInicial

/**
 * M08 · Agregar con un toque.
 * Carta táctil rápida para añadir platos y bebidas a un comensal específico.
 */
@Composable
fun CartaScreen(
    mesaNumero: String,
    asientoEtiqueta: String,
    enLinea: Boolean = true,
    cantidadesIniciales: Map<String, Int> = emptyMap(),
    alVolver: () -> Unit,
    alConfirmarPlatos: (platosSeleccionados: Map<String, Int>) -> Unit,
    alBuscarEnCarta: () -> Unit = {},
    alVerOpcionesMenu: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val cartaCompleta = remember { obtenerCartaInicial() }
    var categoriaActiva by remember { mutableStateOf(CategoriaCarta.PLATOS) }

    // Cantidades seleccionadas por producto
    val cantidades = remember {
        mutableStateMapOf<String, Int>().apply {
            putAll(cantidadesIniciales)
        }
    }

    // Calcular el total y cantidad de ítems seleccionados
    val totalMonto = remember(cantidades.values.sum(), cantidades.keys.size) {
        var total = Dinero.CERO
        for ((nombre, cant) in cantidades) {
            val prod = cartaCompleta.find { it.nombre == nombre }
            if (prod != null && cant > 0) {
                total += prod.precio * cant
            }
        }
        total
    }
    val totalItems = cantidades.values.sum()

    val productosFiltrados = remember(categoriaActiva) {
        when (categoriaActiva) {
            CategoriaCarta.FAVORITOS -> cartaCompleta.filter {
                it.nombre in listOf("Lomo saltado", "Limonada", "Postre del día", "Chicha jarra")
            }
            else -> cartaCompleta.filter { it.categoria == categoriaActiva }
        }
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
                .padding(bottom = 120.dp),
        ) {
            // 1. Cabecera (Toolbar + Título + Subtítulo)
            CartaHeader(
                asientoEtiqueta = asientoEtiqueta,
                mesaNumero = mesaNumero,
                enLinea = enLinea,
                alVolver = alVolver,
            )

            Spacer(Modifier.height(10.dp))

            // 2. Buscador en la carta (.fx)
            BuscadorCartaBar(
                onClick = alBuscarEnCarta,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(14.dp))

            // 3. Plato estrella: Menú del día (.pz)
            PlatoEstrellaBanner(
                onClick = alVerOpcionesMenu,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(14.dp))

            // 4. Selector de categorías horizontal (.chips)
            SelectorCategoriasChips(
                categoriaActiva = categoriaActiva,
                alSeleccionarCategoria = { categoriaActiva = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(14.dp))

            // 5. Grilla 2x2 de productos (.g2 + .pc)
            GrillaProductosCarta(
                productos = productosFiltrados,
                cantidades = cantidades,
                alModificarCantidad = { nombre, delta ->
                    val actual = cantidades[nombre] ?: 0
                    val nuevo = maxOf(0, actual + delta)
                    if (nuevo > 0) {
                        cantidades[nombre] = nuevo
                    } else {
                        cantidades.remove(nombre)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // 6. Barra inferior flotante (.fbar) con total y botón Listo
        AnimatedVisibility(
            visible = totalItems > 0,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BarraInferiorCarta(
                asientoEtiqueta = asientoEtiqueta,
                totalItems = totalItems,
                totalMonto = totalMonto,
                onListo = { alConfirmarPlatos(cantidades.toMap()) },
            )
        }
    }
}

/**
 * Cabecera con botón volver, estado en línea, título `Para P{N}` y subtítulo.
 */
@Composable
private fun CartaHeader(
    asientoEtiqueta: String,
    mesaNumero: String,
    enLinea: Boolean,
    alVolver: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val volverInteraction = remember { MutableInteractionSource() }
    val isVolverPressed by volverInteraction.collectIsPressedAsState()
    val volverScale by animateFloatAsState(
        targetValue = if (isVolverPressed) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "volverScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Botón volver (<)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { scaleX = volverScale; scaleY = volverScale }
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = "Volver al pedido" }
                    .clickable(
                        interactionSource = volverInteraction,
                        indication = null,
                        role = Role.Button,
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
                    drawPath(p, color = colors.ink, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round))
                }
            }

            // Badge En línea
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (enLinea) Color(0xFF10B981) else colors.ink3),
                )
                BasicText(
                    text = if (enLinea) "En línea" else "Sin conexión",
                    style = type.caption.copy(
                        color = colors.ink2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    ),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // H1 Para P{N}
        BasicText(
            text = "Para $asientoEtiqueta",
            style = type.title1.copy(
                color = colors.ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02).sp,
            ),
        )

        Spacer(Modifier.height(2.dp))

        // Subtítulo (.sub)
        BasicText(
            text = "Mesa $mesaNumero, toca + para sumar",
            style = type.subhead.copy(
                color = colors.ink3,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * Barra rápida de búsqueda en la carta (.fx)
 */
@Composable
private fun BuscadorCartaBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = RikosMotion.press(),
        label = "searchBarScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface2)
            .border(1.5.dp, colors.line, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Canvas(Modifier.size(20.dp)) {
                val sx = size.width / 24f
                val sy = size.height / 24f
                val col = colors.ink3
                drawCircle(
                    color = col,
                    radius = 8f * sx,
                    center = Offset(11f * sx, 11f * sy),
                    style = Stroke(width = 2f * sx),
                )
                drawLine(
                    color = col,
                    start = Offset(16.7f * sx, 16.7f * sy),
                    end = Offset(21f * sx, 21f * sy),
                    strokeWidth = 2.2f * sx,
                    cap = StrokeCap.Round,
                )
            }

            BasicText(
                text = "Buscar en la carta",
                style = type.body.copy(
                    color = colors.ink3,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

/**
 * Banner de plato estrella: Menú del día (.pz)
 */
@Composable
private fun PlatoEstrellaBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = RikosMotion.press(),
        label = "bannerPzScale",
    )

    // Paleta chalk del prototipo: fondo #1E2B24, texto #EEF3EC, subtítulo #A9BCAF
    val fondoChalk = Color(0xFF1E2B24)
    val textoChalk = Color(0xFFEEF3EC)
    val subtituloChalk = Color(0xFFA9BCAF)
    val type = Rikos.type

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(22.dp))
            .background(fondoChalk)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "Menú del día",
                    style = type.title2.copy(
                        color = textoChalk,
                    ),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicText(
                        text = "S/ 12.00",
                        style = type.title2.copy(
                            color = textoChalk,
                        ),
                    )

                    Canvas(Modifier.size(16.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val p = Path().apply {
                            moveTo(9f * sx, 6f * sy)
                            lineTo(15f * sx, 12f * sy)
                            lineTo(9f * sx, 18f * sy)
                        }
                        drawPath(p, color = textoChalk, style = Stroke(width = 2.4f * sx, cap = StrokeCap.Round))
                    }
                }
            }

            BasicText(
                text = "Sopa o ensalada, lomo saltado o ají de gallina, chicha o maracuyá",
                style = type.subhead.copy(
                    color = subtituloChalk,
                    lineHeight = 18.sp,
                ),
            )
        }
    }
}

/**
 * Chips de categorías horizontales (.chips + .chip)
 */
@Composable
private fun SelectorCategoriasChips(
    categoriaActiva: CategoriaCarta,
    alSeleccionarCategoria: (CategoriaCarta) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoriaCarta.entries.forEach { cat ->
            val estaActiva = cat == categoriaActiva

            val interaction = remember { MutableInteractionSource() }
            val isPressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1f,
                animationSpec = RikosMotion.press(),
                label = "chipScale_${cat.name}",
            )

            val fondoAnim by animateColorAsState(
                targetValue = if (estaActiva) colors.ink else colors.surface,
                animationSpec = tween(150),
                label = "chipBg_${cat.name}",
            )

            val textoAnim by animateColorAsState(
                targetValue = if (estaActiva) colors.bg else colors.ink2,
                animationSpec = tween(150),
                label = "chipText_${cat.name}",
            )

            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(fondoAnim)
                    .border(
                        width = 1.5.dp,
                        color = if (estaActiva) colors.ink else colors.line,
                        shape = RoundedCornerShape(22.dp),
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = { alSeleccionarCategoria(cat) },
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = cat.etiqueta,
                    style = type.callout.copy(
                        color = textoAnim,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

/**
 * Grilla de 2 columnas de productos (.g2 + .pc)
 */
@Composable
private fun GrillaProductosCarta(
    productos: List<ProductoCarta>,
    cantidades: Map<String, Int>,
    alModificarCantidad: (nombre: String, delta: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val rows = productos.chunked(2)
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val p1 = row[0]
                val p2 = row.getOrNull(1)

                TarjetaProductoCarta(
                    producto = p1,
                    cantidad = cantidades[p1.nombre] ?: 0,
                    alAgregar = { alModificarCantidad(p1.nombre, 1) },
                    alQuitar = { alModificarCantidad(p1.nombre, -1) },
                    modifier = Modifier.weight(1f),
                )

                if (p2 != null) {
                    TarjetaProductoCarta(
                        producto = p2,
                        cantidad = cantidades[p2.nombre] ?: 0,
                        alAgregar = { alModificarCantidad(p2.nombre, 1) },
                        alQuitar = { alModificarCantidad(p2.nombre, -1) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Tarjeta individual de producto en la carta (.pc)
 */
@Composable
private fun TarjetaProductoCarta(
    producto: ProductoCarta,
    cantidad: Int,
    alAgregar: () -> Unit,
    alQuitar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val tieneCantidad = cantidad > 0 && !producto.estaAgotado

    val bordeColorAnim by animateColorAsState(
        targetValue = when {
            tieneCantidad -> colors.primary
            else -> colors.line
        },
        animationSpec = tween(150),
        label = "pcBorder_${producto.id}",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (producto.estaAgotado) colors.surface2 else colors.surface)
            .border(
                width = if (tieneCantidad) 2.dp else 1.5.dp,
                color = bordeColorAnim,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(8.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Ilustración del plato (.dtile)
            IlustracionPlatoTile(
                tipo = producto.tipoIlustracion,
                agotado = producto.estaAgotado,
            )

            // Nombre y aviso de stock
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(
                    text = producto.nombre,
                    style = type.headline.copy(
                        color = if (producto.estaAgotado) colors.ink3 else colors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 18.sp,
                    ),
                    maxLines = 2,
                )

                if (!producto.stockAviso.isNullOrBlank() && !producto.estaAgotado) {
                    BasicText(
                        text = producto.stockAviso,
                        style = type.caption.copy(
                            color = Color(0xFFFF6B4A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }
            }

            Spacer(Modifier.weight(1f, fill = false))

            // Footer con precio y acción (Botón +, Stepper o Agotado)
            if (producto.estaAgotado) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.redSoft)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    BasicText(
                        text = "Agotado",
                        style = type.caption.copy(
                            color = colors.red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }
            } else if (cantidad == 0) {
                // Precio + Botón + redondo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = producto.precio.formatear(),
                        style = type.headline.copy(
                            color = colors.ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )

                    val addInteraction = remember { MutableInteractionSource() }
                    val isAddPressed by addInteraction.collectIsPressedAsState()
                    val addScale by animateFloatAsState(
                        targetValue = if (isAddPressed) 0.92f else 1f,
                        animationSpec = RikosMotion.press(),
                        label = "addbScale_${producto.id}",
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer { scaleX = addScale; scaleY = addScale }
                            .clip(CircleShape)
                            .background(colors.primary)
                            .clickable(
                                interactionSource = addInteraction,
                                indication = null,
                                role = Role.Button,
                                onClick = alAgregar,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(16.dp)) {
                            val sx = size.width / 24f
                            val sy = size.height / 24f
                            val col = colors.onPrimary
                            drawLine(col, Offset(12f * sx, 5f * sy), Offset(12f * sx, 19f * sy), strokeWidth = 2.4f * sx, cap = StrokeCap.Round)
                            drawLine(col, Offset(5f * sx, 12f * sy), Offset(19f * sx, 12f * sy), strokeWidth = 2.4f * sx, cap = StrokeCap.Round)
                        }
                    }
                }
            } else {
                // Stepper activo (.qs)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BasicText(
                        text = producto.precio.formatear(),
                        style = type.headline.copy(
                            color = colors.ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.primarySoft)
                            .padding(horizontal = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Botón -
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .clickable(role = Role.Button, onClick = alQuitar),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.size(14.dp)) {
                                val sx = size.width / 24f
                                val sy = size.height / 24f
                                drawLine(colors.primary, Offset(5f * sx, 12f * sy), Offset(19f * sx, 12f * sy), strokeWidth = 2.4f * sx, cap = StrokeCap.Round)
                            }
                        }

                        // Número central
                        BasicText(
                            text = "$cantidad",
                            style = type.headline.copy(
                                color = colors.ink,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        )

                        // Botón +
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .clickable(role = Role.Button, onClick = alAgregar),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.size(14.dp)) {
                                val sx = size.width / 24f
                                val sy = size.height / 24f
                                drawLine(colors.primary, Offset(12f * sx, 5f * sy), Offset(12f * sx, 19f * sy), strokeWidth = 2.4f * sx, cap = StrokeCap.Round)
                                drawLine(colors.primary, Offset(5f * sx, 12f * sy), Offset(19f * sx, 12f * sy), strokeWidth = 2.4f * sx, cap = StrokeCap.Round)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ilustración SVG sintética para cada plato según su categoría (.dtile)
 */
@Composable
private fun IlustracionPlatoTile(
    tipo: TipoIlustracionPlato,
    agotado: Boolean,
) {
    val fondo = when (tipo) {
        TipoIlustracionPlato.LOMO -> Color(0xFFFFF1E6)
        TipoIlustracionPlato.AJI -> Color(0xFFFEF9C3)
        TipoIlustracionPlato.TALLARIN -> Color(0xFFFFEDD5)
        TipoIlustracionPlato.POLLO -> Color(0xFFFEF3C7)
        TipoIlustracionPlato.CEVICHE -> Color(0xFFE0F2FE)
        TipoIlustracionPlato.CHAUFA -> Color(0xFFFDF2E9)
        TipoIlustracionPlato.VASO -> Color(0xFFFEF9C3)
        TipoIlustracionPlato.BOTELLA -> Color(0xFFE0E7FF)
        TipoIlustracionPlato.CAFE -> Color(0xFFF5F5F4)
        TipoIlustracionPlato.FLAN -> Color(0xFFFEF3C7)
        TipoIlustracionPlato.POSTRE -> Color(0xFFFCE7F3)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (agotado) Color(0xFFE2E8F0).copy(alpha = 0.5f) else fondo),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(64.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Plato base
            drawOval(
                color = Color.White,
                topLeft = Offset(cx - 24.dp.toPx(), cy - 14.dp.toPx()),
                size = Size(48.dp.toPx(), 28.dp.toPx()),
            )
            drawOval(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(cx - 24.dp.toPx(), cy - 14.dp.toPx()),
                size = Size(48.dp.toPx(), 28.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )

            // Contenido según el plato
            when (tipo) {
                TipoIlustracionPlato.LOMO, TipoIlustracionPlato.CHAUFA -> {
                    // Trozos de carne/arroz y papas
                    drawRoundRect(
                        color = Color(0xFF7C2D12),
                        topLeft = Offset(cx - 12.dp.toPx(), cy - 6.dp.toPx()),
                        size = Size(10.dp.toPx(), 6.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Color(0xFFF59E0B),
                        topLeft = Offset(cx + 2.dp.toPx(), cy - 4.dp.toPx()),
                        size = Size(12.dp.toPx(), 5.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                    drawCircle(Color(0xFF16A34A), radius = 2.dp.toPx(), center = Offset(cx - 2.dp.toPx(), cy + 2.dp.toPx()))
                }
                TipoIlustracionPlato.AJI, TipoIlustracionPlato.POLLO -> {
                    drawOval(
                        color = Color(0xFFFACC15),
                        topLeft = Offset(cx - 12.dp.toPx(), cy - 8.dp.toPx()),
                        size = Size(20.dp.toPx(), 14.dp.toPx()),
                    )
                    drawCircle(Color(0xFFF59E0B), radius = 3.dp.toPx(), center = Offset(cx, cy))
                }
                TipoIlustracionPlato.CEVICHE -> {
                    drawRoundRect(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(cx - 10.dp.toPx(), cy - 6.dp.toPx()),
                        size = Size(8.dp.toPx(), 7.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Color(0xFFBE185D),
                        topLeft = Offset(cx + 2.dp.toPx(), cy - 5.dp.toPx()),
                        size = Size(9.dp.toPx(), 4.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                    drawCircle(Color(0xFFFDE047), radius = 3.dp.toPx(), center = Offset(cx - 8.dp.toPx(), cy + 2.dp.toPx()))
                }
                TipoIlustracionPlato.VASO -> {
                    val p = Path().apply {
                        moveTo(cx - 8.dp.toPx(), cy - 14.dp.toPx())
                        lineTo(cx + 8.dp.toPx(), cy - 14.dp.toPx())
                        lineTo(cx + 6.dp.toPx(), cy + 12.dp.toPx())
                        lineTo(cx - 6.dp.toPx(), cy + 12.dp.toPx())
                        close()
                    }
                    drawPath(p, color = Color(0xFFFDE047))
                    drawPath(p, color = Color(0xFFBAE6FD), style = Stroke(width = 1.5.dp.toPx()))
                    drawCircle(Color(0xFF84CC16), radius = 4.dp.toPx(), center = Offset(cx + 7.dp.toPx(), cy - 14.dp.toPx()))
                }
                TipoIlustracionPlato.BOTELLA -> {
                    drawRoundRect(
                        color = Color(0xFF1E3A8A),
                        topLeft = Offset(cx - 6.dp.toPx(), cy - 14.dp.toPx()),
                        size = Size(12.dp.toPx(), 26.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                    drawRect(
                        color = Color(0xFFEF4444),
                        topLeft = Offset(cx - 6.dp.toPx(), cy - 4.dp.toPx()),
                        size = Size(12.dp.toPx(), 8.dp.toPx()),
                    )
                }
                else -> {
                    // Postres / Café / Flan
                    drawRoundRect(
                        color = Color(0xFFFCD34D),
                        topLeft = Offset(cx - 10.dp.toPx(), cy - 8.dp.toPx()),
                        size = Size(20.dp.toPx(), 14.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                    drawOval(
                        color = Color(0xFFB45309),
                        topLeft = Offset(cx - 8.dp.toPx(), cy - 9.dp.toPx()),
                        size = Size(16.dp.toPx(), 5.dp.toPx()),
                    )
                }
            }
        }
    }
}

/**
 * Barra flotante inferior (.fbar) con contador, total y botón "Listo >"
 */
@Composable
private fun BarraInferiorCarta(
    asientoEtiqueta: String,
    totalItems: Int,
    totalMonto: Dinero,
    onListo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val listoInteraction = remember { MutableInteractionSource() }
    val isListoPressed by listoInteraction.collectIsPressedAsState()
    val listoScale by animateFloatAsState(
        targetValue = if (isListoPressed) 0.94f else 1f,
        animationSpec = RikosMotion.press(),
        label = "listoScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(35.dp), spotColor = Color(0x60000000))
                .clip(RoundedCornerShape(35.dp))
                .background(colors.navy)
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Contador circular (.cnt)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "$totalItems",
                    style = type.headline.copy(
                        color = colors.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }

            // Columna informativa: Para P{N} y Total en soles
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = "Para $asientoEtiqueta",
                    style = type.caption.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                BasicText(
                    text = totalMonto.formatear(),
                    style = type.title2.copy(
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.02).sp,
                    ),
                )
            }

            // Botón primario: Listo > (.btn.pri)
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .graphicsLayer { scaleX = listoScale; scaleY = listoScale }
                    .clip(RoundedCornerShape(25.dp))
                    .background(colors.primary)
                    .clickable(
                        interactionSource = listoInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onListo,
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BasicText(
                        text = "Listo",
                        style = type.callout.copy(
                            color = colors.onPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )

                    Canvas(Modifier.size(16.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val p = Path().apply {
                            moveTo(9f * sx, 6f * sy)
                            lineTo(15f * sx, 12f * sy)
                            lineTo(9f * sx, 18f * sy)
                        }
                        drawPath(p, color = colors.onPrimary, style = Stroke(width = 2.4f * sx, cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}
