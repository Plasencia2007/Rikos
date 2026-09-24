package pe.rikos.operacion.comun.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import pe.rikos.operacion.identidad.infraestructura.seguridad.FiltroAutenticacionJwt;
import pe.rikos.operacion.identidad.infraestructura.seguridad.JwtServicio;

/**
 * Reemplaza la seguridad abierta del esqueleto del Sprint 0: US-002
 * (autenticacion con usuario y contrasena) y US-003 (PIN) ya estan
 * implementadas. La autorizacion sigue siendo mayormente por rol fijo
 * (las rutas de administracion generales usan "hasAnyRole"), con una
 * excepcion puntual: editar permisos de un rol o excepciones de un
 * usuario exige el permiso "identidad.administrar_permisos" resuelto de
 * verdad (US-004, caso critico 19), no solo el rol. El dominio de
 * negocio (venta, caja, inventario...) todavia no tiene rutas propias
 * que proteger con permisos -- eso llega con esos modulos.
 *
 * ADM-29: la pantalla/API de administracion de usuarios (US-005) permite
 * asignar roles y locales, y ahora tambien EXCEPCIONES de permiso por
 * usuario individual (otorgar o retirar un permiso puntual por encima
 * de su rol, US-004/3.9).
 *
 * API sin estado: el token viaja en el header Authorization y no hay
 * sesion de servidor, asi que csrf no aplica.
 */
@Configuration
public class ConfiguracionSeguridad {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, JwtServicio jwtServicio) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rutas -> rutas
                .requestMatchers("/salud", "/auth/login", "/auth/pin", "/locales/publico/**").permitAll()
                // HT-05: la especificacion OpenAPI solo describe la forma de
                // la API (rutas, DTOs), no datos -- la usa el generador de
                // tipos del frontend, que corre sin token.
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Caso critico 19: editar permisos (de un rol, o excepciones de
                // un usuario) exige el permiso puntual, no solo el rol fijo de
                // administrador -- distinto de las demas rutas de /roles/** y
                // /usuarios/**, que un administrador de local sigue pudiendo
                // usar. Va antes del matcher amplio: el primero que hace match gana.
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/roles/*/permisos", "/usuarios/*/permisos")
                    .hasAuthority("PERM_identidad.administrar_permisos")
                .requestMatchers("/locales/**", "/usuarios/**", "/roles/**", "/permisos/**")
                    // El nombre real del rol es "administrador de local" (con "de"):
                    // FiltroAutenticacionJwt lo convierte a ROLE_ADMINISTRADOR_DE_LOCAL.
                    // "ADMINISTRADOR_LOCAL" (sin "de") nunca hacia match -- bug real
                    // encontrado al probar el 403 de administrar_permisos.
                    .hasAnyRole("ADMINISTRADOR_CENTRAL", "ADMINISTRADOR_DE_LOCAL")
                // Catalogo (sprint 2): el permiso manda, no el rol fijo --
                // cajero lee pero no escribe, mozo/cocina/motorizado no
                // entran ni a leer. GET primero (mas especifico que nada
                // mas lo cubre), luego el resto por metodo.
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/categorias/**", "/unidades/**", "/productos/**", "/presentaciones/**",
                        "/conversiones/**", "/proveedores/**")
                    .hasAuthority("PERM_catalogo.ver")
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/categorias/*/desactivar", "/categorias/*/activar", "/productos/*/desactivar",
                        "/productos/*/activar", "/proveedores/*/desactivar", "/proveedores/*/activar")
                    .hasAuthority("PERM_catalogo.desactivar")
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/categorias/**", "/unidades/**", "/productos/**", "/presentaciones/**",
                        "/conversiones/**", "/proveedores/**")
                    .hasAuthority("PERM_catalogo.crear")
                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                        "/categorias/**", "/unidades/**", "/productos/**", "/presentaciones/**",
                        "/conversiones/**", "/proveedores/**")
                    .hasAuthority("PERM_catalogo.modificar")
                .anyRequest().authenticated())
            .addFilterBefore(new FiltroAutenticacionJwt(jwtServicio), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
