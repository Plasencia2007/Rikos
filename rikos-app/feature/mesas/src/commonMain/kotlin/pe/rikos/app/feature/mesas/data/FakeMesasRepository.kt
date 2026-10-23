package pe.rikos.app.feature.mesas.data

import kotlinx.coroutines.delay
import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.mesas.domain.AvisoMesa
import pe.rikos.app.feature.mesas.domain.ErrorMesas
import pe.rikos.app.feature.mesas.domain.EstadoMesa
import pe.rikos.app.feature.mesas.domain.FormaMesa
import pe.rikos.app.feature.mesas.domain.Mesa
import pe.rikos.app.feature.mesas.domain.MesasRepository
import pe.rikos.app.feature.mesas.domain.Salon
import pe.rikos.app.feature.mesas.domain.TipoAviso

/**
 * Repositorio falso que replica fielmente los datos del lienzo de diseño M03.
 * Incluye mesas dibujadas, unidas, ocupadas, por cobrar, libres y avisos «te necesitan».
 */
class FakeMesasRepository : MesasRepository {

    override suspend fun obtenerMesas(): Resultado<List<Mesa>, ErrorMesas> {
        delay(300)
        return Resultado.Ok(MESAS_SALON_PRINCIPAL)
    }

    override suspend fun obtenerSalones(): List<Salon> = listOf(
        Salon("salon_principal", "Salón principal", 12),
        Salon("terraza", "Terraza", 6),
        Salon("para_llevar", "Para llevar", 2),
        Salon("delivery", "Delivery", 1),
    )

    override suspend fun obtenerAvisos(): List<AvisoMesa> = listOf(
        AvisoMesa(
            id = "av-1",
            mesaNumero = "12",
            tipo = TipoAviso.PLATOS_LISTOS,
            titulo = "2 platos listos",
            subtitulo = "Llévalos a la mesa",
        ),
        AvisoMesa(
            id = "av-2",
            mesaNumero = "6",
            tipo = TipoAviso.PIDIO_CUENTA,
            titulo = "Pidió la cuenta",
            subtitulo = "S/ 54.00",
        ),
        AvisoMesa(
            id = "av-3",
            mesaNumero = "5",
            tipo = TipoAviso.TIEMPO_SIN_PASAR,
            titulo = "32 min sin pasar",
            subtitulo = "¿Postre o bebida?",
        ),
    )

    companion object {
        val MESAS_SALON_PRINCIPAL = listOf(
            Mesa(
                id = "01J0000000000000000000M001",
                numero = "1",
                estado = EstadoMesa.LIBRE,
                forma = FormaMesa.RECTANGULAR,
            ),
            Mesa(
                id = "01J0000000000000000000M002",
                numero = "2",
                estado = EstadoMesa.OCUPADA,
                forma = FormaMesa.REDONDA,
                comensales = 3,
                minutosTranscurridos = 18,
                total = Dinero(2800),
            ),
            Mesa(
                id = "01J0000000000000000000M034",
                numero = "3 + 4",
                estado = EstadoMesa.OCUPADA,
                forma = FormaMesa.UNIDA,
                comensales = 7,
                minutosTranscurridos = 41,
                total = Dinero(9600),
                mesaPrincipalNumero = "3",
            ),
            Mesa(
                id = "01J0000000000000000000M005",
                numero = "5",
                estado = EstadoMesa.OCUPADA,
                forma = FormaMesa.RECTANGULAR,
                comensales = 4,
                minutosTranscurridos = 32,
                total = Dinero(7950),
            ),
            Mesa(
                id = "01J0000000000000000000M006",
                numero = "6",
                estado = EstadoMesa.POR_COBRAR,
                forma = FormaMesa.REDONDA,
                comensales = 3,
                minutosTranscurridos = 58,
                total = Dinero(5400),
                seleccionada = true,
            ),
            Mesa(
                id = "01J0000000000000000000M007",
                numero = "7",
                estado = EstadoMesa.LIBRE,
                forma = FormaMesa.RECTANGULAR,
            ),
            Mesa(
                id = "01J0000000000000000000M008",
                numero = "8",
                estado = EstadoMesa.FUERA_DE_SERVICIO,
                forma = FormaMesa.RECTANGULAR,
            ),
            Mesa(
                id = "01J0000000000000000000M009",
                numero = "9",
                estado = EstadoMesa.OCUPADA,
                forma = FormaMesa.REDONDA,
                comensales = 2,
                total = Dinero(2800),
                mozoNombre = "De Ana",
            ),
            Mesa(
                id = "01J0000000000000000000M010",
                numero = "10",
                estado = EstadoMesa.LIBRE,
                forma = FormaMesa.RECTANGULAR,
            ),
            Mesa(
                id = "01J0000000000000000000M011",
                numero = "11",
                estado = EstadoMesa.OCUPADA,
                forma = FormaMesa.RECTANGULAR,
                comensales = 4,
                total = Dinero(6400),
                mozoNombre = "De Ana",
            ),
            Mesa(
                id = "01J0000000000000000000M012",
                numero = "12",
                estado = EstadoMesa.LISTO,
                forma = FormaMesa.REDONDA,
                comensales = 6,
                minutosTranscurridos = 45,
                total = Dinero(14200),
                comidaLista = true,
            ),
        )
    }
}
