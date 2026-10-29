package pe.rikos.app.feature.bienvenida

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.StartOffset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rikos.app.core.designsystem.components.RkButton
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosMotion
import pe.rikos.app.core.designsystem.theme.RikosRadius
import pe.rikos.app.core.designsystem.theme.RikosSpace
import pe.rikos.app.core.designsystem.theme.RikosTouch

/** M00. La navegación a M01 se inyectará desde App cuando esa pantalla exista. */
@Composable
fun BienvenidaScreen(alConfigurar: () -> Unit, modifier: Modifier = Modifier) {
    var paso by rememberSaveable { mutableIntStateOf(0) }
    BienvenidaContent(paso, { paso = (paso + 1).coerceAtMost(2) }, alConfigurar, modifier)
}

@Composable
fun BienvenidaContent(
    paso: Int,
    alSiguiente: () -> Unit,
    alConfigurar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Rikos.colors
    val type = Rikos.type
    val (titulo, descripcion) = when (paso) {
        0 -> "Tus mesas, de un vistazo" to "Ves qué mesa está libre, cuál espera y cuál ya tiene la comida lista."
        1 -> "Cocina te avisa" to "Cuando un plato está listo, la mesa late en verde y te llega el aviso."
        else -> "Cobra sin ir a caja" to "Si tu perfil lo permite, cobras en la mesa con efectivo, Yape o tarjeta."
    }

    BoxWithConstraints(modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.TopCenter) {
        // Mantiene las medidas del lienzo; en horizontal o con letra grande permite desplazar.
        val altoDisponible = maxHeight
        Column(
            Modifier.widthIn(max = 430.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = altoDisponible)
                .padding(horizontal = RikosSpace.screenMargin)
                .padding(top = 30.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                IlustracionBienvenida()
                BasicText(
                    titulo,
                    Modifier.semantics { heading() },
                    style = type.title1.copy(color = c.ink, lineHeight = 35.2.sp, textAlign = TextAlign.Center),
                )
                BasicText(
                    descripcion,
                    Modifier.widthIn(max = 320.dp),
                    style = type.body.copy(color = c.ink2, fontWeight = FontWeight.Normal, lineHeight = 24.sp, textAlign = TextAlign.Center),
                )
                Row(
                    Modifier.clearAndSetSemantics { contentDescription = "Paso ${paso + 1} de 3" },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) { indice ->
                        Box(Modifier.size(if (indice == paso) 26.dp else 8.dp, 8.dp)
                            .background(if (indice == paso) c.primary else c.ink3.copy(alpha = 0.25f), CircleShape))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RkButton(texto = if (paso < 2) "Siguiente" else "Empezar", onClick = if (paso < 2) alSiguiente else alConfigurar)
                val interaccion = remember { MutableInteractionSource() }
                val presionado by interaccion.collectIsPressedAsState()
                val escala by animateFloatAsState(if (presionado) 0.96f else 1f, RikosMotion.press(), label = "saltar")
                Box(
                    Modifier.fillMaxWidth().heightIn(min = RikosTouch.control)
                        .graphicsLayer { scaleX = escala; scaleY = escala }
                        .clickable(interaccion, indication = null, role = Role.Button, onClick = alConfigurar),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText("Saltar", style = type.headline.copy(color = c.ink2))
                }
            }
        }
    }
}

@Composable
private fun IlustracionBienvenida() {
    val c = Rikos.colors
    val reducirMovimiento = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor == 0f
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entradaMesa by animateFloatAsState(if (visible || reducirMovimiento) 1f else 0f, RikosMotion.welcome(), label = "entradaMesa")
    val entradaPedido by animateFloatAsState(if (visible || reducirMovimiento) 1f else 0f, RikosMotion.welcome(100), label = "entradaPedido")
    val pulso = if (reducirMovimiento) null else rememberInfiniteTransition(label = "comidaLista").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 1800
            0f at 0 using RikosMotion.RingEase
            1f at 1260
            1f at 1800
        }, initialStartOffset = StartOffset(50)),
        label = "halo",
    )
    Box(Modifier.size(300.dp, 280.dp).clearAndSetSemantics {
        contentDescription = "Mesa 7 libre, mesa 12 con comida lista y resumen del pedido"
    }) {
        MesaIlustrada(
            lista = false,
            modifier = Modifier.offset(y = 30.dp).graphicsLayer {
                alpha = entradaMesa
                translationY = if (reducirMovimiento) 0f else (1f - entradaMesa) * 10.dp.toPx()
            }.dropShadow(RikosRadius.card, Shadow(radius = 2.dp, color = c.ink.copy(alpha = 0.05f), offset = DpOffset(0.dp, 1.dp))),
        )
        MesaIlustrada(
            lista = true,
            modifier = Modifier.align(Alignment.TopEnd).graphicsLayer { rotationZ = 5f }
                .drawBehind {
                    val progreso = pulso?.value ?: 0f
                    val expansion = if (reducirMovimiento) 2.dp.toPx() else 12.dp.toPx() * progreso
                    drawRoundRect(
                        color = c.green.copy(alpha = if (reducirMovimiento) 0.45f else 0.45f * (1f - progreso)),
                        topLeft = Offset(-expansion, -expansion),
                        size = Size(size.width + expansion * 2, size.height + expansion * 2),
                        cornerRadius = CornerRadius(20.dp.toPx() + expansion),
                    )
                },
        )
        ResumenIlustrado(Modifier.align(Alignment.BottomCenter).offset(y = (-10).dp).graphicsLayer {
            alpha = entradaPedido
            translationY = if (reducirMovimiento) 0f else (1f - entradaPedido) * 10.dp.toPx()
        })
    }
}

@Composable
private fun MesaIlustrada(lista: Boolean, modifier: Modifier) {
    val c = Rikos.colors
    val type = Rikos.type
    Column(
        modifier.width(150.dp).background(c.surface, RikosRadius.card).padding(top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(87.5.dp, 70.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                // Coordenadas del SVG original (120 × 96), sin rasterizar la ilustración.
                scale(size.height / 96f, pivot = Offset.Zero) {
                    if (lista) {
                        for (x in listOf(36f, 64f)) for (y in listOf(8f, 79f)) {
                            drawRoundRect(Color(0xFFFBBF24), Offset(x, y), Size(20f, 9f), CornerRadius(4f))
                        }
                        drawRoundRect(c.amberSoft, Offset(26f, 20f), Size(68f, 56f), CornerRadius(14f))
                        drawRoundRect(c.amber, Offset(26f, 20f), Size(68f, 56f), CornerRadius(14f), style = Stroke(2.5f))
                        drawCircle(c.green, 11f, Offset(98f, 16f))
                    } else {
                        for (silla in listOf(Offset(100f, 48f), Offset(80f, 82.6f), Offset(40f, 82.6f), Offset(20f, 48f), Offset(40f, 13.4f), Offset(80f, 13.4f))) {
                            drawCircle(Color(0xFF34D399), 7f, silla)
                        }
                        drawCircle(c.greenSoft, 28f, Offset(60f, 48f))
                        drawCircle(c.green, 28f, Offset(60f, 48f), style = Stroke(2.5f))
                    }
                }
            }
            BasicText(if (lista) "12" else "7", style = type.headline.copy(fontSize = 17.5.sp, color = if (lista) c.amberText else c.greenText))
        }
        BasicText(if (lista) "Comida lista" else "Mesa 7", style = type.callout.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 20.25.sp, color = c.ink))
    }
}

@Composable
private fun ResumenIlustrado(modifier: Modifier) {
    val c = Rikos.colors
    val type = Rikos.type
    Row(
        modifier.width(260.dp).heightIn(min = 70.dp)
            .dropShadow(RikosRadius.pill, Shadow(radius = 32.dp, color = Color(0xFF0B1426).copy(alpha = 0.32f), offset = DpOffset(0.dp, 14.dp)))
            .background(c.navy, RikosRadius.pill).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).background(c.primary, CircleShape), contentAlignment = Alignment.Center) {
            BasicText("2", style = type.headline.copy(fontSize = 20.sp, lineHeight = 27.sp, color = c.onPrimary))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("Para P3", style = type.caption.copy(fontWeight = FontWeight.Bold, color = c.onPrimary.copy(alpha = 0.72f)))
            BasicText(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 10.08.sp, color = c.onPrimary.copy(alpha = 0.72f))) { append("S/ ") }
                    append("24")
                    withStyle(SpanStyle(fontSize = 13.2.sp, color = c.onPrimary.copy(alpha = 0.72f))) { append(".50") }
                },
                style = type.title2.copy(fontSize = 24.sp, lineHeight = 22.8.sp, color = c.onPrimary, fontFeatureSettings = "tnum"),
            )
        }
        Box(Modifier.heightIn(min = 54.dp).background(c.primary, CircleShape).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            BasicText("Listo", style = type.headline.copy(color = c.onPrimary))
        }
    }
}
