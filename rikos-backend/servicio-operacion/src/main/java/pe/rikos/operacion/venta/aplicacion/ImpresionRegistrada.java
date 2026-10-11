package pe.rikos.operacion.venta.aplicacion;

import pe.rikos.operacion.venta.infraestructura.Comanda;

/** copia: esta impresion es la segunda o posterior (RN-063), asi que sale con la leyenda COPIA. */
public record ImpresionRegistrada(Comanda comanda, boolean copia) {
}
