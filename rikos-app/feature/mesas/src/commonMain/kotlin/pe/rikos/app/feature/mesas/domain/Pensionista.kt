package pe.rikos.app.feature.mesas.domain

/**
 * Representa el color semántico de la barra de crédito del pensionista.
 */
enum class ColorCredito {
    VERDE,
    ROJO,
    NARANJA,
}

/**
 * Modelo de datos para un cliente Pensionista asociado a la comensalía.
 * Medido exactamente del prototipo de diseño M10.
 */
data class Pensionista(
    val id: String,
    val iniciales: String,
    val nombre: String,
    val dni: String,
    val tipoPension: String,
    val creditoTexto: String,
    val porcentajeUso: Float,
    val colorCredito: ColorCredito,
    val correspondeHoy: String,
    val advertencia: String? = null,
) {
    val primerNombre: String get() = nombre.split(" ").firstOrNull().orEmpty()
}

/**
 * Lista de pensionistas de ejemplo según el lienzo de diseño M10.
 */
fun obtenerPensionistasEjemplo(): List<Pensionista> = listOf(
    Pensionista(
        id = "pen-1",
        iniciales = "RT",
        nombre = "Rosa Torres",
        dni = "45871234",
        tipoPension = "Pensión almuerzo, tope S/ 80",
        creditoTexto = "S/ 78 de 80",
        porcentajeUso = 0.97f,
        colorCredito = ColorCredito.ROJO,
        correspondeHoy = "1 menú",
        advertencia = "Pasaría su límite por S/ 4.00 si suma la bebida. Puedes seguir; el admin recibe el aviso.",
    ),
    Pensionista(
        id = "pen-2",
        iniciales = "CR",
        nombre = "Carmen Rojas",
        dni = "72145698",
        tipoPension = "Pensión almuerzo, tope S/ 100",
        creditoTexto = "Saldo a favor S/ 38",
        porcentajeUso = 0.0f,
        colorCredito = ColorCredito.VERDE,
        correspondeHoy = "1 menú",
        advertencia = null,
    ),
    Pensionista(
        id = "pen-3",
        iniciales = "RV",
        nombre = "Rodrigo Vela",
        dni = "10984521",
        tipoPension = "Pensión completa, tope S/ 150",
        creditoTexto = "S/ 30 de 150",
        porcentajeUso = 0.20f,
        colorCredito = ColorCredito.NARANJA,
        correspondeHoy = "Desayuno y almuerzo",
        advertencia = null,
    ),
)
