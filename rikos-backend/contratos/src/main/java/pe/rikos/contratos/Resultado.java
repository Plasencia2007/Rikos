package pe.rikos.contratos;

import java.util.function.Function;

/**
 * Resultado de una operacion que puede fallar de forma esperada.
 *
 * Se usa en la capa de aplicacion en lugar de excepciones para los errores
 * que forman parte del flujo normal del negocio (ej. "el pensionista no
 * tiene saldo"), reservando las excepciones para lo verdaderamente
 * excepcional. Sellado a proposito: el compilador obliga a manejar los
 * dos casos en un switch de patrones.
 */
public sealed interface Resultado<T, E> {

    static <T, E> Resultado<T, E> exito(T valor) {
        return new Exito<>(valor);
    }

    static <T, E> Resultado<T, E> fracaso(E error) {
        return new Fracaso<>(error);
    }

    boolean esExito();

    boolean esFracaso();

    T valorOLanza();

    <R> Resultado<R, E> map(Function<T, R> transformacion);

    <F> Resultado<T, F> mapError(Function<E, F> transformacion);

    record Exito<T, E>(T valor) implements Resultado<T, E> {

        @Override
        public boolean esExito() {
            return true;
        }

        @Override
        public boolean esFracaso() {
            return false;
        }

        @Override
        public T valorOLanza() {
            return valor;
        }

        @Override
        public <R> Resultado<R, E> map(Function<T, R> transformacion) {
            return new Exito<>(transformacion.apply(valor));
        }

        @Override
        public <F> Resultado<T, F> mapError(Function<E, F> transformacion) {
            return new Exito<>(valor);
        }
    }

    record Fracaso<T, E>(E error) implements Resultado<T, E> {

        @Override
        public boolean esExito() {
            return false;
        }

        @Override
        public boolean esFracaso() {
            return true;
        }

        @Override
        public T valorOLanza() {
            throw new IllegalStateException("Resultado en fracaso, no tiene valor: " + error);
        }

        @Override
        public <R> Resultado<R, E> map(Function<T, R> transformacion) {
            return new Fracaso<>(error);
        }

        @Override
        public <F> Resultado<T, F> mapError(Function<E, F> transformacion) {
            return new Fracaso<>(transformacion.apply(error));
        }
    }
}
