package pe.rikos.contratos;

/**
 * Importe monetario representado en centimos (entero), nunca en punto
 * flotante. Un double no puede representar exactamente 0.10, y ese error
 * de redondeo es inaceptable en dinero real.
 */
public record Dinero(long centimos) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(0L);

    public static Dinero deCentimos(long centimos) {
        return new Dinero(centimos);
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(this.centimos + otro.centimos);
    }

    public Dinero restar(Dinero otro) {
        return new Dinero(this.centimos - otro.centimos);
    }

    public Dinero multiplicarPor(int cantidad) {
        return new Dinero(this.centimos * cantidad);
    }

    public boolean esMayorQue(Dinero otro) {
        return this.centimos > otro.centimos;
    }

    public boolean esMenorQue(Dinero otro) {
        return this.centimos < otro.centimos;
    }

    @Override
    public int compareTo(Dinero otro) {
        return Long.compare(this.centimos, otro.centimos);
    }
}
