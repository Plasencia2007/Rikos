package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

/**
 * Si el pensionista olvida su PIN, se genera otro (no se puede recuperar el
 * anterior: solo existe cifrado). Se devuelve EN CLARO una unica vez. Queda en
 * la bitacora como cambio de credencial, SIN el PIN.
 */
@Service
public class RegenerarPin {

    private final PensionistaRepositorio pensionistas;
    private final GeneradorDePin generadorDePin;
    private final PasswordEncoder codificador;
    private final RegistradorAuditoria auditoria;

    public RegenerarPin(PensionistaRepositorio pensionistas, GeneradorDePin generadorDePin,
                        PasswordEncoder codificador, RegistradorAuditoria auditoria) {
        this.pensionistas = pensionistas;
        this.generadorDePin = generadorDePin;
        this.codificador = codificador;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<String, ErrorPensionista> ejecutar(String id, String actorId) {
        Pensionista pensionista = pensionistas.findById(id).orElse(null);
        if (pensionista == null) {
            return Resultado.fracaso(ErrorPensionista.NO_ENCONTRADO);
        }
        String pin = generadorDePin.nuevo();
        pensionista.cambiarPin(codificador.encode(pin), actorId);
        pensionistas.save(pensionista);
        auditoria.registrar("cambio_credencial", "pensionista", id, null,
                RegistradorAuditoria.campos("nombre", pensionista.nombreCompleto(), "credencial", "pin_presencial"));
        return Resultado.exito(pin);
    }
}
