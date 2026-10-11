package pe.rikos.operacion.comun.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
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
    SecurityFilterChain filtros(HttpSecurity http, JwtServicio jwtServicio, RegistradorAuditoria auditoria)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Caso critico 19: el intento denegado tambien se audita.
            .exceptionHandling(manejo -> manejo.accessDeniedHandler(new AuditorDeAccesoDenegado(auditoria)))
            .authorizeHttpRequests(rutas -> rutas
                // P-26: sin esto, cuando algo revienta Spring reenvia la peticion a
                // /error para armar la respuesta -- si esa ruta no esta permitida,
                // la cadena de seguridad bloquea el REENVIO (no la peticion
                // original) y el cliente ve 403 en vez del 500 real. El fallo
                // interno queda invisible detras de un error que parece de permisos.
                .requestMatchers("/salud", "/error", "/auth/login", "/auth/pin", "/locales/publico/**").permitAll()
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
                // US-006 (ADM-31): la bitacora exige su propio permiso, no el rol
                // fijo -- un administrador de local la ve si tiene el permiso,
                // igual que el resto de "identidad", pero es de solo lectura
                // (nunca hay un metodo != GET que proteger aca, CA4).
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/auditoria/**")
                    .hasAuthority("PERM_auditoria.ver")
                .requestMatchers("/locales/**", "/usuarios/**", "/roles/**", "/permisos/**")
                    // El nombre real del rol es "administrador de local" (con "de"):
                    // FiltroAutenticacionJwt lo convierte a ROLE_ADMINISTRADOR_DE_LOCAL.
                    // "ADMINISTRADOR_LOCAL" (sin "de") nunca hacia match -- bug real
                    // encontrado al probar el 403 de administrar_permisos.
                    .hasAnyRole("ADMINISTRADOR_CENTRAL", "ADMINISTRADOR_DE_LOCAL")
                // US-010: recetas con su propio modulo de permisos -- cocina las
                // ve (sin costos, eso lo filtra RecetaController con
                // recetas.ver_costos), solo el administrador central las escribe.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/recetas/**")
                    .hasAuthority("PERM_recetas.ver")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/recetas/**")
                    .hasAuthority("PERM_recetas.crear")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/recetas/**")
                    .hasAuthority("PERM_recetas.modificar")
                // US-014 (sprint 4): el menu lo leen mozo, cajero y cocina; su
                // precio lo cambia solo quien tiene menu.modificar.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/menu/**")
                    .hasAuthority("PERM_menu.ver")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/menu/**")
                    .hasAuthority("PERM_menu.modificar")
                // US-032 (sprint 7): cajas y turnos. Verlas, caja.ver; crear una caja, caja.administrar; abrir un turno, caja.crear
                // (cajero y administradores; el mozo no cobra).
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/cajas", "/cajas/**", "/turnos/**")
                    .hasAuthority("PERM_caja.ver")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/cajas", "/cajas/*/principal", "/cajas/*/activar",
                        "/cajas/*/desactivar")
                    .hasAuthority("PERM_caja.administrar")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/cajas/*")
                    .hasAuthority("PERM_caja.administrar")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/cajas/*")
                    .hasAuthority("PERM_caja.administrar")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/cajas/*/turnos", "/turnos/actual/cierre")
                    .hasAuthority("PERM_caja.crear")
                // US-035 (entrega 5): reabrir un cobro es caja.reabrir; ver los cobros de un pedido, caja.ver.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/cobros/*/reapertura")
                    .hasAuthority("PERM_caja.reabrir")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/pedidos/*/cobros")
                    .hasAuthority("PERM_caja.ver")
                // US-033 (entrega 2): cobrar lineas de un pedido es caja.crear (el mozo no cobra). Antes de /pedidos/**.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/pedidos/*/cobros")
                    .hasAuthority("PERM_caja.crear")
                // US-028: pedidos. Ver, venta.ver; tomar y corregir un pedido, venta.crear.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/pedidos", "/pedidos/**")
                    .hasAuthority("PERM_venta.ver")
                // US-029: reorganizar (trasladar lineas, mover el pedido, unir y separar mesas) es venta.modificar: el
                // mozo tambien lo tiene. Antes de los matchers genericos de /pedidos y /mesas.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/pedidos/*/lineas/trasladar",
                        "/pedidos/*/terminar", "/mesas/*/unir", "/mesas/*/separar")
                    .hasAuthority("PERM_venta.modificar")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/pedidos/*/mesa")
                    .hasAuthority("PERM_venta.modificar")
                // US-031: anular una linea exige venta.anular (cajero y administradores), no venta.crear.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/pedidos/*/lineas/*/anular")
                    .hasAuthority("PERM_venta.anular")
                .requestMatchers("/pedidos", "/pedidos/**")
                    .hasAuthority("PERM_venta.crear")
                // US-030: la comanda. Verla, venta.ver; registrar una impresion, venta.crear (comandar va en /pedidos).
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/comandas", "/comandas/**")
                    .hasAuthority("PERM_venta.ver")
                .requestMatchers("/comandas", "/comandas/**")
                    .hasAuthority("PERM_venta.crear")
                // US-027: mesas. El mapa lo ve quien ve ventas; bloquear y liberar, quien
                // tiene venta.bloquear_mesa (el mozo tambien); crear, editar y activar, solo
                // quien administra las mesas del local.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/mesas", "/mesas/**")
                    .hasAuthority("PERM_venta.ver")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/mesas/*/bloquear", "/mesas/*/liberar")
                    .hasAuthority("PERM_venta.bloquear_mesa")
                .requestMatchers("/mesas", "/mesas/**")
                    .hasAuthority("PERM_venta.administrar")
                // Salones y el plano de sus mesas: se ven con venta.ver; se administran con venta.administrar.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/salones", "/salones/**")
                    .hasAuthority("PERM_venta.ver")
                .requestMatchers("/salones", "/salones/**")
                    .hasAuthority("PERM_venta.administrar")
                // US-018: la cuenta. El saldo inicial es de la oficina central; el ajuste, de quien modifica.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/pensionistas/*/cuenta/saldo-inicial")
                    .hasAuthority("PERM_pensionistas.administrar")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/pensionistas/*/cuenta/ajustes")
                    .hasAuthority("PERM_pensionistas.modificar")
                // US-016: fichas de pensionistas. Cada accion con su permiso. Lo
                // mas especifico primero (el PIN y el estado son sub-rutas del POST).
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/pensionistas/**")
                    .hasAuthority("PERM_pensionistas.ver")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/pensionistas/*/desactivar",
                        "/pensionistas/*/activar")
                    .hasAuthority("PERM_pensionistas.desactivar")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/pensionistas/*/pin")
                    .hasAuthority("PERM_pensionistas.modificar")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/pensionistas")
                    .hasAuthority("PERM_pensionistas.crear")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/pensionistas/**")
                    .hasAuthority("PERM_pensionistas.modificar")
                // Sprint 5: planes tarifarios y parametros de cobro. Los ven
                // quienes ven pensionistas (caja y mozo los necesitan para cobrar);
                // los cambia solo quien tiene pensionistas.administrar.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/planes/**", "/parametros/**")
                    .hasAuthority("PERM_pensionistas.ver")
                // El motor de precios solo calcula (no escribe): lo usa quien ve pensionistas.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/precios/**")
                    .hasAuthority("PERM_pensionistas.ver")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/planes/**", "/parametros/**")
                    .hasAuthority("PERM_pensionistas.administrar")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/planes/**", "/parametros/**")
                    .hasAuthority("PERM_pensionistas.administrar")
                // US-015: la disponibilidad la leen quienes ven el menu; la
                // marcan cocina y administracion (menu.marcar_agotado), siempre
                // en su local activo.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/disponibilidad/**")
                    .hasAuthority("PERM_menu.ver")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/disponibilidad/**")
                    .hasAuthority("PERM_menu.marcar_agotado")
                // US-013: crear la semana es menu.crear; agregar opciones y
                // confirmar son menu.modificar (el exacto va antes que el
                // comodin); quitar opciones y descartar un borrador,
                // menu.desactivar ("Quitar un item del menu programado").
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/menu/semanas")
                    .hasAuthority("PERM_menu.crear")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/menu/**")
                    .hasAuthority("PERM_menu.modificar")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/menu/**")
                    .hasAuthority("PERM_menu.desactivar")
                // Catalogo (sprint 2): el permiso manda, no el rol fijo --
                // cajero lee pero no escribe, mozo/cocina/motorizado no
                // entran ni a leer. GET primero (mas especifico que nada
                // mas lo cubre), luego el resto por metodo.
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/categorias/**", "/unidades/**", "/productos/**", "/presentaciones/**",
                        "/conversiones/**", "/proveedores/**", "/motivos/**")
                    .hasAuthority("PERM_catalogo.ver")
                // Categoria no tiene activar/desactivar (se borra o no se borra,
                // ver EliminarCategoria) -- Producto, Proveedor y Motivo (US-012) lo usan.
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/productos/*/desactivar", "/productos/*/activar",
                        "/proveedores/*/desactivar", "/proveedores/*/activar",
                        "/motivos/*/desactivar", "/motivos/*/activar")
                    .hasAuthority("PERM_catalogo.desactivar")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                        "/categorias/**", "/productos/**", "/proveedores/**")
                    .hasAuthority("PERM_catalogo.desactivar")
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/categorias/**", "/unidades/**", "/productos/**", "/presentaciones/**",
                        "/conversiones/**", "/proveedores/**", "/motivos/**")
                    .hasAuthority("PERM_catalogo.crear")
                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                        "/categorias/**", "/unidades/**", "/productos/**", "/presentaciones/**",
                        "/conversiones/**", "/proveedores/**", "/motivos/**")
                    .hasAuthority("PERM_catalogo.modificar")
                .anyRequest().authenticated())
            .addFilterBefore(new FiltroAutenticacionJwt(jwtServicio), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
