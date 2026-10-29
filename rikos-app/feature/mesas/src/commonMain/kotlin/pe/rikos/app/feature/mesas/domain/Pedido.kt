package pe.rikos.app.feature.mesas.domain

import pe.rikos.app.core.common.Dinero

enum class EstadoLineaPedido {
    POR_ENVIAR,
    EN_COCINA,
    LISTO_PARA_SERVIR,
    SERVIDO,
    ANULADO,
}

data class LineaPedido(
    val id: String,
    val cantidad: Int,
    val nombre: String,
    val observacion: String? = null,
    val precioUnitario: Dinero,
    val estado: EstadoLineaPedido = EstadoLineaPedido.POR_ENVIAR,
) {
    val subtotal: Dinero get() = precioUnitario * cantidad
}

data class AsientoPersona(
    val id: String,
    val numero: Int,
    val etiqueta: String = "P$numero",
    val nombrePersona: String? = null,
    val esPensionista: Boolean = false,
    val subtituloPersonalizado: String? = null,
    val lineas: List<LineaPedido> = emptyList(),
) {
    val nombreMostrar: String get() = nombrePersona ?: "Persona $numero"
    val total: Dinero get() = lineas.fold(Dinero.CERO) { acc, item -> acc + item.subtotal }
    val tienePendientes: Boolean get() = lineas.any { it.estado == EstadoLineaPedido.POR_ENVIAR }
    val tieneItems: Boolean get() = lineas.isNotEmpty()
}

data class PedidoMesa(
    val id: String,
    val mesaId: String,
    val mesaNumero: String,
    val minutosTranscurridos: Int = 32,
    val asientos: List<AsientoPersona>,
    val asientoSeleccionadoId: String = asientos.firstOrNull()?.id.orEmpty(),
) {
    val total: Dinero get() = asientos.fold(Dinero.CERO) { acc, a -> acc + a.total }
    val cantidadPorEnviar: Int get() = asientos.sumOf { a -> a.lineas.count { it.estado == EstadoLineaPedido.POR_ENVIAR } }
    val totalPorEnviar: Dinero get() = asientos.flatMap { it.lineas }
        .filter { it.estado == EstadoLineaPedido.POR_ENVIAR }
        .fold(Dinero.CERO) { acc, l -> acc + l.subtotal }
    val asientoActivo: AsientoPersona? get() = asientos.find { it.id == asientoSeleccionadoId } ?: asientos.firstOrNull()
}

/** Datos iniciales medidos exactamente del lienzo de diseño M07 */
fun crearPedidoInicialMesa5(): PedidoMesa {
    val asiento1 = AsientoPersona(
        id = "p-1",
        numero = 1,
        etiqueta = "P1",
        esPensionista = false,
        lineas = listOf(
            LineaPedido(
                id = "ln-1",
                cantidad = 1,
                nombre = "Menú del día",
                observacion = "Sopa, lomo saltado, chicha",
                precioUnitario = Dinero(1200),
                estado = EstadoLineaPedido.EN_COCINA,
            ),
            LineaPedido(
                id = "ln-2",
                cantidad = 1,
                nombre = "Gaseosa personal",
                observacion = null,
                precioUnitario = Dinero(400),
                estado = EstadoLineaPedido.SERVIDO,
            ),
            LineaPedido(
                id = "ln-3",
                cantidad = 1,
                nombre = "Postre del día",
                observacion = "Sin azúcar",
                precioUnitario = Dinero(800),
                estado = EstadoLineaPedido.POR_ENVIAR,
            ),
        ),
    )

    val asiento2 = AsientoPersona(
        id = "p-2",
        numero = 2,
        etiqueta = "P2",
        nombrePersona = "Carmen Rojas",
        esPensionista = true,
        subtituloPersonalizado = "Cubre su pensión",
        lineas = listOf(
            LineaPedido(
                id = "ln-4",
                cantidad = 1,
                nombre = "Menú del día",
                observacion = "Ensalada, ají de gallina, maracuyá",
                precioUnitario = Dinero(1200),
                estado = EstadoLineaPedido.POR_ENVIAR,
            ),
        ),
    )

    val asiento3 = AsientoPersona(
        id = "p-3",
        numero = 3,
        etiqueta = "P3",
        esPensionista = false,
        lineas = listOf(
            LineaPedido(
                id = "ln-5",
                cantidad = 1,
                nombre = "Menú ejecutivo",
                observacion = "Sopa, pollo a la plancha",
                precioUnitario = Dinero(1500),
                estado = EstadoLineaPedido.LISTO_PARA_SERVIR,
            ),
            LineaPedido(
                id = "ln-6",
                cantidad = 1,
                nombre = "Gaseosa personal",
                observacion = null,
                precioUnitario = Dinero(400),
                estado = EstadoLineaPedido.LISTO_PARA_SERVIR,
            ),
        ),
    )

    val asiento4 = AsientoPersona(
        id = "p-4",
        numero = 4,
        etiqueta = "P4",
        esPensionista = false,
        lineas = listOf(
            LineaPedido(
                id = "ln-7",
                cantidad = 1,
                nombre = "Lomo saltado",
                observacion = "Término medio",
                precioUnitario = Dinero(1800),
                estado = EstadoLineaPedido.EN_COCINA,
            ),
            LineaPedido(
                id = "ln-8",
                cantidad = 1,
                nombre = "Limonada",
                observacion = "Poco hielo",
                precioUnitario = Dinero(650),
                estado = EstadoLineaPedido.SERVIDO,
            ),
        ),
    )

    return PedidoMesa(
        id = "ped-5",
        mesaId = "01J0000000000000000000M005",
        mesaNumero = "5",
        minutosTranscurridos = 32,
        asientos = listOf(asiento1, asiento2, asiento3, asiento4),
        asientoSeleccionadoId = "p-1",
    )
}
