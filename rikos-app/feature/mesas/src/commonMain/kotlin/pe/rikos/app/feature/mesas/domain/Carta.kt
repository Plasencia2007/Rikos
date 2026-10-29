package pe.rikos.app.feature.mesas.domain

import pe.rikos.app.core.common.Dinero

enum class CategoriaCarta(val etiqueta: String) {
    PLATOS("Platos"),
    BEBIDAS("Bebidas"),
    POSTRES("Postres"),
    FAVORITOS("Favoritos"),
}

enum class TipoIlustracionPlato {
    LOMO,
    AJI,
    TALLARIN,
    POLLO,
    CEVICHE,
    CHAUFA,
    VASO,
    BOTELLA,
    CAFE,
    FLAN,
    POSTRE,
}

data class ProductoCarta(
    val id: String,
    val nombre: String,
    val precio: Dinero,
    val categoria: CategoriaCarta,
    val stockAviso: String? = null,
    val estaAgotado: Boolean = false,
    val tipoIlustracion: TipoIlustracionPlato = TipoIlustracionPlato.LOMO,
)

fun obtenerCartaInicial(): List<ProductoCarta> {
    return listOf(
        // Platos
        ProductoCarta("p-lomo", "Lomo saltado", Dinero(1800), CategoriaCarta.PLATOS, stockAviso = "Quedan 4", tipoIlustracion = TipoIlustracionPlato.LOMO),
        ProductoCarta("p-aji", "Ají de gallina", Dinero(1600), CategoriaCarta.PLATOS, tipoIlustracion = TipoIlustracionPlato.AJI),
        ProductoCarta("p-tallarin", "Tallarín rojo", Dinero(1400), CategoriaCarta.PLATOS, estaAgotado = true, tipoIlustracion = TipoIlustracionPlato.TALLARIN),
        ProductoCarta("p-pollo", "Pollo a la plancha", Dinero(1700), CategoriaCarta.PLATOS, tipoIlustracion = TipoIlustracionPlato.POLLO),
        ProductoCarta("p-ceviche", "Ceviche", Dinero(2200), CategoriaCarta.PLATOS, tipoIlustracion = TipoIlustracionPlato.CEVICHE),
        ProductoCarta("p-chaufa", "Arroz chaufa", Dinero(1500), CategoriaCarta.PLATOS, tipoIlustracion = TipoIlustracionPlato.CHAUFA),

        // Bebidas
        ProductoCarta("p-limonada", "Limonada", Dinero(650), CategoriaCarta.BEBIDAS, tipoIlustracion = TipoIlustracionPlato.VASO),
        ProductoCarta("p-chicha", "Chicha jarra", Dinero(1000), CategoriaCarta.BEBIDAS, tipoIlustracion = TipoIlustracionPlato.VASO),
        ProductoCarta("p-gaseosa", "Gaseosa personal", Dinero(400), CategoriaCarta.BEBIDAS, tipoIlustracion = TipoIlustracionPlato.BOTELLA),
        ProductoCarta("p-agua", "Agua sin gas", Dinero(250), CategoriaCarta.BEBIDAS, tipoIlustracion = TipoIlustracionPlato.BOTELLA),
        ProductoCarta("p-cafe", "Café", Dinero(500), CategoriaCarta.BEBIDAS, tipoIlustracion = TipoIlustracionPlato.CAFE),
        ProductoCarta("p-maracuya", "Maracuyá jarra", Dinero(1000), CategoriaCarta.BEBIDAS, estaAgotado = true, tipoIlustracion = TipoIlustracionPlato.VASO),

        // Postres
        ProductoCarta("p-postre-dia", "Postre del día", Dinero(800), CategoriaCarta.POSTRES, tipoIlustracion = TipoIlustracionPlato.FLAN),
        ProductoCarta("p-mazamorra", "Mazamorra", Dinero(600), CategoriaCarta.POSTRES, tipoIlustracion = TipoIlustracionPlato.POSTRE),
        ProductoCarta("p-arroz-leche", "Arroz con leche", Dinero(600), CategoriaCarta.POSTRES, tipoIlustracion = TipoIlustracionPlato.POSTRE),
        ProductoCarta("p-flan", "Flan", Dinero(700), CategoriaCarta.POSTRES, tipoIlustracion = TipoIlustracionPlato.FLAN),
        ProductoCarta("p-helado", "Helado", Dinero(500), CategoriaCarta.POSTRES, stockAviso = "Quedan 2", tipoIlustracion = TipoIlustracionPlato.POSTRE),
        ProductoCarta("p-picarones", "Picarones", Dinero(900), CategoriaCarta.POSTRES, tipoIlustracion = TipoIlustracionPlato.FLAN),
    )
}
