package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Indice de busqueda con clave del servidor; bcrypt sigue verificando la credencial. */
@Component
public class HuellaPinPensionista {
    private final byte[] clave;
    public HuellaPinPensionista(@Value("${rikos.kiosco.clave-pin:${rikos.seguridad.jwt.secreto}}") String clave) {
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
        if (this.clave.length < 32) throw new IllegalArgumentException("La clave de PIN del kiosco requiere 32 bytes");
    }
    public String de(String pin) { return calcular("pensionista-pin-v1:", pin); }
    public String origen(String origen) { return calcular("kiosco-origen-v1:", origen); }
    private String calcular(String dominio, String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((dominio + valor).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo inicializar la proteccion del PIN", e);
        }
    }
}
