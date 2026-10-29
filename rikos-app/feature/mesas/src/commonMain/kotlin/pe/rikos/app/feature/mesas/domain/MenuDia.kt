package pe.rikos.app.feature.mesas.domain

import pe.rikos.app.core.common.Dinero

data class OpcionMenu(
    val id: String,
    val nombre: String,
    val subtitulo: String? = null,
    val estaAgotado: Boolean = false,
)

data class ExtraMenu(
    val id: String,
    val nombre: String,
    val precio: Dinero,
)

data class MenuDiaConfig(
    val precioBase: Dinero = Dinero(1200),
    val entradas: List<OpcionMenu> = listOf(
        OpcionMenu("ent-sopa", "Sopa de verduras"),
        OpcionMenu("ent-ensalada", "Ensalada fresca"),
    ),
    val segundos: List<OpcionMenu> = listOf(
        OpcionMenu("seg-lomo", "Lomo saltado", subtitulo = "Quedan 4"),
        OpcionMenu("seg-aji", "Ají de gallina"),
        OpcionMenu("seg-tallarin", "Tallarín rojo", subtitulo = "Agotado", estaAgotado = true),
        OpcionMenu("seg-pollo", "Pollo a la plancha"),
    ),
    val refrescos: List<OpcionMenu> = listOf(
        OpcionMenu("ref-chicha", "Chicha morada"),
        OpcionMenu("ref-maracuya", "Maracuyá"),
    ),
    val extras: List<ExtraMenu> = listOf(
        ExtraMenu("ext-huevo", "Huevo frito", Dinero(200)),
        ExtraMenu("ext-arroz", "Porción de arroz", Dinero(400)),
        ExtraMenu("ext-papas", "Papas fritas", Dinero(500)),
    ),
    val notasRapidas: List<String> = listOf(
        "Sin cebolla",
        "Poco picante",
        "Bien caliente",
        "Para llevar",
    ),
)

data class PedidoMenuDia(
    val entrada: OpcionMenu,
    val segundo: OpcionMenu,
    val refresco: OpcionMenu,
    val extras: List<ExtraMenu> = emptyList(),
    val notas: List<String> = emptyList(),
    val cantidad: Int = 1,
) {
    fun precioUnitario(precioBase: Dinero): Dinero {
        var unit = precioBase
        extras.forEach { unit += it.precio }
        return unit
    }

    fun precioTotal(precioBase: Dinero): Dinero {
        return precioUnitario(precioBase) * cantidad
    }

    fun formatearObservacion(): String {
        val partes = mutableListOf<String>()
        partes.add("${entrada.nombre} · ${segundo.nombre} · ${refresco.nombre}")
        if (extras.isNotEmpty()) {
            partes.add("+ " + extras.joinToString(", ") { it.nombre })
        }
        if (notas.isNotEmpty()) {
            partes.add(notas.joinToString(", "))
        }
        return partes.joinToString(" · ")
    }
}
