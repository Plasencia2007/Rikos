package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Responsable;
import pe.rikos.operacion.pensionistas.infraestructura.ResponsableRepositorio;

/**
 * US-016 CA1-CA5: alta de la ficha con su responsable y su cuenta corriente
 * vacia. El PIN lo genera el sistema, se guarda cifrado (RN-090) y se devuelve
 * EN CLARO esta unica vez, para entregarselo al pensionista: no hay forma de
 * leerlo despues. La contrasena del portal no se pide todavia (RF-027).
 */
@Service
public class CrearPensionista {

    /** pin: el PIN en claro, solo en esta respuesta. */
    public record Creado(Pensionista pensionista, String pin) {
    }

    private final PensionistaRepositorio pensionistas;
    private final ResponsableRepositorio responsables;
    private final CuentaPensionistaRepositorio cuentas;
    private final ValidadorDePensionista validador;
    private final GeneradorDePin generadorDePin;
    private final PasswordEncoder codificador;

    public CrearPensionista(PensionistaRepositorio pensionistas, ResponsableRepositorio responsables,
                            CuentaPensionistaRepositorio cuentas, ValidadorDePensionista validador,
                            GeneradorDePin generadorDePin, PasswordEncoder codificador) {
        this.pensionistas = pensionistas;
        this.responsables = responsables;
        this.cuentas = cuentas;
        this.validador = validador;
        this.generadorDePin = generadorDePin;
        this.codificador = codificador;
    }

    @Transactional
    public Resultado<Creado, ErrorPensionista> ejecutar(DatosPensionista datos, String actorId) {
        ErrorPensionista invalido = validador.validar(datos, null);
        if (invalido != null) {
            return Resultado.fracaso(invalido);
        }
        String pin = generadorDePin.nuevo();
        Pensionista pensionista = new Pensionista(Identificador.nuevo().valor(), codificador.encode(pin), actorId);
        pensionista.asignarHuellaPin(generadorDePin.huella(pin));
        pensionista.actualizarFicha(ActualizarPensionista.fichaDe(datos), actorId);
        pensionistas.save(pensionista);
        DatosResponsable r = datos.responsable();
        responsables.save(new Responsable(Identificador.nuevo().valor(), pensionista.getId(), r.nombreCompleto().trim(),
                r.dni(), r.telefono().trim(), r.correo().trim(), r.parentesco().trim(), r.direccion().trim()));
        cuentas.save(new CuentaPensionista(Identificador.nuevo().valor(), pensionista.getId()));
        return Resultado.exito(new Creado(pensionista, pin));
    }
}
