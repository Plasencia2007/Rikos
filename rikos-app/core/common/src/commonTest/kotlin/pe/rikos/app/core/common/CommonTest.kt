package pe.rikos.app.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonTest {
    @Test fun dineroFormateaCentimos() {
        assertEquals("S/ 12.50", Dinero(1250).formatear())
        assertEquals("S/ 0.05", Dinero(5).formatear())
        assertEquals("S/ 30.00", (Dinero(1500) * 2).formatear())
    }

    @Test fun ulidTiene26CaracteresValidos() {
        val id = Ulid.nuevo()
        assertEquals(26, id.length)
        assertTrue(Ulid.esValido(id))
    }

    @Test fun ulidsOrdenanPorTiempo() {
        val a = Ulid.nuevo(ahoraMs = 1_000)
        val b = Ulid.nuevo(ahoraMs = 2_000)
        assertTrue(a.substring(0, 10) < b.substring(0, 10))
    }
}
