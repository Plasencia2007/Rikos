package pe.rikos.operacion.identidad.infraestructura.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Token stateless firmado (HS256): el gateway no guarda sesion en el
 * servidor, asi que cada peticion se valida sola con lo que trae el token.
 * Ademas de "roles" (para las rutas de administracion, que siguen por rol
 * fijo), el token lleva "permisos" -- la union ya resuelta de los permisos
 * de todos los roles mas las excepciones (US-004, 3.9).
 */
@Component
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class JwtServicio {

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtServicio(PropiedadesSeguridad propiedades) {
        String secreto = propiedades.getJwt().getSecreto();
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "rikos.seguridad.jwt.secreto debe existir y tener al menos 32 bytes");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = propiedades.getJwt().getExpiracionMinutos();
    }

    public String generar(String usuarioId, String nombreUsuario, List<String> roles, List<String> permisos,
                           List<String> localesIds, String localActivoId) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuarioId)
                .claim("nombreUsuario", nombreUsuario)
                .claim("roles", roles)
                // Los permisos efectivos se resuelven una sola vez, al iniciar sesion
                // (3.9), y viajan en el token -- no se recalculan en cada peticion.
                .claim("permisos", permisos)
                .claim("locales", localesIds)
                .claim("localActivo", localActivoId)
                .issuedAt(java.util.Date.from(ahora))
                .expiration(java.util.Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException excepcion) {
            return Optional.empty();
        }
    }
}
