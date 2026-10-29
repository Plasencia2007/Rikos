package pe.rikos.app.feature.mesas.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.feature.mesas.domain.DestinoPreparacion
import pe.rikos.app.feature.mesas.domain.ProductoLibre

/**
 * M08c · Producto libre.
 * Permite registrar un ítem personalizado que no existe en la carta,
 * asignándole nombre, precio libre, estación de preparación (o no enviar a cocina)
 * con aviso de bitácora y auditoría con nombre del mozo.
 */
@Composable
fun ProductoLibreScreen(
    mesaNumero: String,
    asientoEtiqueta: String,
    enLinea: Boolean = true,
    nombreInicial: String = "Torta de cumpleaños (traída)",
    precioInicial: Dinero = Dinero(1500),
    alVolver: () -> Unit,
    alConfirmarProductoLibre: (ProductoLibre) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    val focusManager = LocalFocusManager.current

    var nombre by remember { mutableStateOf(nombreInicial) }
    var precio by remember { mutableStateOf(precioInicial) }
    var destino by remember { mutableStateOf(DestinoPreparacion.NO_VA_A_COCINA) }

    // Modo de edición manual de precio
    var editandoPrecioManual by remember { mutableStateOf(false) }
    var textoPrecioManual by remember { mutableStateOf(precio.soles.toString()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp),
        ) {
            // 1. Cabecera (.hd .tbar)
            CabeceraProductoLibre(
                enLinea = enLinea,
                alVolver = alVolver,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            Spacer(Modifier.height(4.dp))

            // 2. Campo Nombre (.fl + .fx)
            CampoNombreProducto(
                valor = nombre,
                alCambiarValor = { nombre = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(14.dp))

            // 3. Tarjeta central de Precio (.card.pad.col)
            TarjetaPrecioProductoLibre(
                precio = precio,
                alSeleccionarPrecio = { nuevoPrecio ->
                    precio = nuevoPrecio
                    textoPrecioManual = nuevoPrecio.soles.toString()
                },
                editandoManual = editandoPrecioManual,
                textoManual = textoPrecioManual,
                alCambiarTextoManual = { txt ->
                    textoPrecioManual = txt.filter { it.isDigit() }
                    val entero = textoPrecioManual.toLongOrNull() ?: 0L
                    precio = Dinero(entero * 100)
                },
                alAlternarEdicionManual = { editandoPrecioManual = !editandoPrecioManual },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(14.dp))

            // 4. Dónde se prepara (.gh + .gl)
            GrupoDestinoPreparacion(
                destinoSeleccionado = destino,
                alSeleccionarDestino = { destino = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(14.dp))

            // 5. Nota de auditoría (.nt.nt-warn)
            NotaAuditoriaProductoLibre(
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // 6. Dock inferior fijo (.dock) con Botón Agregar
        DockInferiorProductoLibre(
            asientoEtiqueta = asientoEtiqueta,
            precio = precio,
            habilitado = nombre.isNotBlank(),
            alAgregar = {
                val producto = ProductoLibre(
                    nombre = nombre.trim().ifBlank { "Producto libre" },
                    precio = precio,
                    destino = destino,
                )
                focusManager.clearFocus()
                alConfirmarProductoLibre(producto)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.96f))
                .border(width = 1.dp, color = colors.line)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

/**
 * Cabecera con botón volver, estado En línea, título grande y subtítulo.
 */
@Composable
private fun CabeceraProductoLibre(
    enLinea: Boolean,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    text = if (enLinea) "En línea" else "Sin conexión",
                    style = type.caption.copy(
                        color = if (enLinea) colors.greenText else colors.ink3,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // Título y subtítulo
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(
                text = "Producto libre",
                style = type.title1.copy(
                    color = colors.ink,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).sp,
                ),
            )
            BasicText(
                text = "Algo que no está en la carta",
                style = type.subhead.copy(
                    color = colors.ink3,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        }
    }
}

/**
 * Campo de texto para el nombre del producto libre con ícono de lápiz (.fl + .fx).
 */
@Composable
private fun CampoNombreProducto(
    valor: String,
    alCambiarValor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type
    var estaEnfocado by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        BasicText(
            text = "Nombre",
            style = type.footnote.copy(
                color = colors.ink2,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        // Contenedor .fx
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(
                    width = 1.5.dp,
                    color = if (estaEnfocado) colors.primary else colors.line,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Ícono de lápiz / edición
            Canvas(Modifier.size(18.dp)) {
                val sx = size.width / 24f
                val sy = size.height / 24f
                val p = Path().apply {
                    moveTo(3f * sx, 17.25f * sy)
                    lineTo(3f * sx, 21f * sy)
                    lineTo(6.75f * sy, 21f * sy)
                    lineTo(17.81f * sx, 9.94f * sy)
                    lineTo(14.06f * sx, 6.19f * sy)
                    close()
                }
                drawPath(p, color = if (estaEnfocado) colors.primary else colors.ink3, style = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(
                    color = if (estaEnfocado) colors.primary else colors.ink3,
                    start = Offset(14.06f * sx, 6.19f * sy),
                    end = Offset(17.81f * sx, 9.94f * sy),
                    strokeWidth = 2f * sx,
                )
            }

            BasicTextField(
                value = valor,
                onValueChange = alCambiarValor,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = type.fontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                ),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { estaEnfocado = it.isFocused },
                decorationBox = { innerTextField ->
                    if (valor.isEmpty()) {
                        BasicText(
                            text = "Ej: Torta de cumpleaños (traída)",
                            style = type.body.copy(color = colors.ink3.copy(alpha = 0.6f)),
                        )
                    }
                    innerTextField()
                },
            )

            if (valor.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .clickable(role = Role.Button, onClick = { alCambiarValor("") }),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(10.dp)) {
                        val stroke = 1.8.dp.toPx()
                        drawLine(colors.ink3, Offset(0f, 0f), Offset(size.width, size.height), stroke, StrokeCap.Round)
                        drawLine(colors.ink3, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta central con precio grande de 52sp y selector de chips (.card.pad.col).
 */
@Composable
private fun TarjetaPrecioProductoLibre(
    precio: Dinero,
    alSeleccionarPrecio: (Dinero) -> Unit,
    editandoManual: Boolean,
    textoManual: String,
    alCambiarTextoManual: (String) -> Unit,
    alAlternarEdicionManual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val chips = listOf(
        Pair("S/ 5", Dinero(500)),
        Pair("S/ 10", Dinero(1000)),
        Pair("S/ 15", Dinero(1500)),
        Pair("Gratis", Dinero.CERO),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .padding(vertical = 20.dp, horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Etiqueta 'Precio' (.c2.b)
            BasicText(
                text = "Precio",
                style = type.subhead.copy(
                    color = colors.ink2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            // Visualizador numérico .mny de 52sp
            if (!editandoManual) {
                val entero = precio.soles
                val decimales = precio.resto.toString().padStart(2, '0')

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = alAlternarEdicionManual)
                        .padding(vertical = 4.dp),
                ) {
                    BasicText(
                        text = "S/ ",
                        style = type.title2.copy(
                            color = colors.ink3,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    BasicText(
                        text = entero.toString(),
                        style = type.display.copy(
                            color = colors.ink,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.02).sp,
                        ),
                    )
                    BasicText(
                        text = ".$decimales",
                        style = type.title2.copy(
                            color = colors.ink3,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            } else {
                // Entrada numérica manual
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    BasicText(
                        text = "S/ ",
                        style = type.title2.copy(
                            color = colors.primary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    BasicTextField(
                        value = textoManual,
                        onValueChange = alCambiarTextoManual,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = type.fontFamily,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.ink,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { alAlternarEdicionManual() },
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.width(120.dp),
                    )
                    BasicText(
                        text = ".00",
                        style = type.title2.copy(
                            color = colors.ink3,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            // Chips de montos rápidos
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                chips.forEach { (etiqueta, monto) ->
                    val seleccionado = precio == monto
                    ChipPrecio(
                        etiqueta = etiqueta,
                        seleccionado = seleccionado,
                        onClick = {
                            alSeleccionarPrecio(monto)
                            if (editandoManual) alAlternarEdicionManual()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Chip individual para monto rápido (.chip).
 */
@Composable
private fun ChipPrecio(
    etiqueta: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 100, easing = RikosMotion.Ease),
    )

    Box(
        modifier = Modifier
            .height(40.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(20.dp))
            .background(if (seleccionado) colors.navy else colors.surface2)
            .border(
                width = 1.dp,
                color = if (seleccionado) colors.navy else colors.line,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = etiqueta,
            style = type.subhead.copy(
                color = if (seleccionado) Color.White else colors.ink,
                fontSize = 14.sp,
                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * Grupo: Dónde se prepara con radio buttons (.gh + .gl).
 */
@Composable
private fun GrupoDestinoPreparacion(
    destinoSeleccionado: DestinoPreparacion,
    alSeleccionarDestino: (DestinoPreparacion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Encabezado (.gh)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Dónde se prepara",
                style = type.body.copy(
                    color = colors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )

            // Píldora (.pl)
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.amberSoft)
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Elige 1",
                    style = type.caption.copy(
                        color = colors.amberText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // Contenedor lista (.gl)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(18.dp)),
        ) {
            Column {
                DestinoPreparacion.entries.forEachIndexed { index, dest ->
                    val esUltimo = index == DestinoPreparacion.entries.lastIndex
                    val seleccionado = dest == destinoSeleccionado

                    FilaDestinoPreparacion(
                        destino = dest,
                        seleccionado = seleccionado,
                        onClick = { alSeleccionarDestino(dest) },
                    )

                    if (!esUltimo) {
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
}

/**
 * Fila individual con radio button (.rr) para el destino de preparación.
 */
@Composable
private fun FilaDestinoPreparacion(
    destino: DestinoPreparacion,
    seleccionado: Boolean,
    onClick: () -> Unit,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 100, easing = RikosMotion.Ease),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = destino.etiqueta,
            style = type.body.copy(
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.weight(1f),
        )

        // Radio button (.rr)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (seleccionado) colors.primary else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (seleccionado) colors.primary else colors.line,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (seleccionado) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}

/**
 * Nota de advertencia y auditoría (.nt.nt-warn).
 */
@Composable
private fun NotaAuditoriaProductoLibre(
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.amberSoft)
            .border(1.dp, colors.amberText.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Ícono de escudo / advertencia
        Canvas(Modifier.size(20.dp).padding(top = 1.dp)) {
            val sx = size.width / 24f
            val sy = size.height / 24f
            val shield = Path().apply {
                moveTo(12f * sx, 2f * sy)
                lineTo(4f * sx, 5f * sy)
                lineTo(4f * sx, 11f * sy)
                cubicTo(4f * sx, 16.5f * sy, 7.5f * sx, 21f * sy, 12f * sx, 22.5f * sy)
                cubicTo(16.5f * sx, 21f * sy, 20f * sx, 16.5f * sy, 20f * sx, 11f * sy)
                lineTo(20f * sx, 5f * sy)
                close()
            }
            drawPath(
                shield,
                color = colors.amberText,
                style = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Punto de exclamación
            drawLine(
                color = colors.amberText,
                start = Offset(12f * sx, 8f * sy),
                end = Offset(12f * sx, 12.5f * sy),
                strokeWidth = 2f * sx,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = colors.amberText,
                radius = 1.2f * sx,
                center = Offset(12f * sx, 16f * sy),
            )
        }

        BasicText(
            text = "Queda en la bitácora como producto libre, con tu nombre.",
            style = type.subhead.copy(
                color = colors.amberText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 19.sp,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Dock inferior fijo (.dock) con botón Agregar a PN · S/ XX.XX (.btn.pri.blk).
 */
@Composable
private fun DockInferiorProductoLibre(
    asientoEtiqueta: String,
    precio: Dinero,
    habilitado: Boolean,
    alAgregar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Rikos.colors
    val type = Rikos.type

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && habilitado) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100, easing = RikosMotion.Ease),
    )

    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .shadow(elevation = if (habilitado) 3.dp else 0.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(if (habilitado) colors.primary else colors.surface2)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = habilitado,
                    role = Role.Button,
                    onClick = alAgregar,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "Agregar a $asientoEtiqueta · ${precio.formatear()}",
                style = type.headline.copy(
                    color = if (habilitado) Color.White else colors.ink3,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }
    }
}
