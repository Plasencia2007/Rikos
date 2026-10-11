package pe.rikos.operacion.identidad.infraestructura.seguridad;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtServicio jwtServicio;

    public FiltroAutenticacionJwt(JwtServicio jwtServicio) {
        this.jwtServicio = jwtServicio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {

        String encabezado = peticion.getHeader("Authorization");
        if (encabezado != null && encabezado.startsWith(PREFIJO)) {
            String token = encabezado.substring(PREFIJO.length());
            Optional<Claims> claims = jwtServicio.validar(token);
            if (claims.isPresent()) {
                autenticar(claims.get());
            }
        }
        cadena.doFilter(peticion, respuesta);
    }

    @SuppressWarnings("unchecked")
    private void autenticar(Claims claims) {
        String usuarioId = claims.getSubject();
        List<String> roles = claims.get("roles", List.class);
        List<String> permisos = claims.get("permisos", List.class);

        // "administrador central" (como se guarda en rol.nombre) -> ROLE_ADMINISTRADOR_CENTRAL
        // (como lo compara Spring Security con hasRole/hasAnyRole).
        List<GrantedAuthority> autoridades = new java.util.ArrayList<>();
        if (roles != null) {
            roles.stream()
                    .map(rol -> "ROLE_" + rol.toUpperCase().replace(' ', '_'))
                    .map(SimpleGrantedAuthority::new)
                    .forEach(autoridades::add);
        }
        // "identidad.administrar_permisos" -> PERM_identidad.administrar_permisos,
        // para las rutas que exigen un permiso concreto en vez de un rol fijo.
        if (permisos != null) {
            permisos.stream()
                    .map(codigo -> "PERM_" + codigo)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(autoridades::add);
        }

        var autenticacion = new UsernamePasswordAuthenticationToken(usuarioId, null, autoridades);
        // US-006: RegistradorAuditoria resuelve el local activo desde aca --
        // "details" es el unico lugar libre del token de autenticacion, no
        // pensado originalmente para esto pero es el mismo mecanismo que ya
        // usa Spring Security para IP/sesion en otros proyectos.
        autenticacion.setDetails(claims.get("localActivo", String.class));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
