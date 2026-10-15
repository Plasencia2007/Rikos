package pe.rikos.operacion.produccion.aplicacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventarioRepositorio;
import pe.rikos.operacion.produccion.aplicacion.dto.DetalleProduccionDTO;
import pe.rikos.operacion.produccion.aplicacion.dto.LiquidarSobranteComando;
import pe.rikos.operacion.produccion.dominio.ErrorProduccion;
import pe.rikos.operacion.produccion.dominio.Produccion;
import pe.rikos.operacion.produccion.infraestructura.ProduccionRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

@Service
public class RegistrarDestinoSobrante {

    private final ProduccionRepositorio producciones;
    private final ExistenciaRepositorio existencias;
    private final MovimientoInventarioRepositorio movimientos;
    private final MotivoRepositorio motivos;
    private final UsuarioRepositorio usuarios;
    private final RegistradorAuditoria auditoria;

    private static final Set<String> DESTINOS_VALIDOS = Set.of("conservar", "personal", "perdida", "reutilizacion", "otro");

    @Autowired
    public RegistrarDestinoSobrante(ProduccionRepositorio producciones,
                                   ExistenciaRepositorio existencias,
                                   MovimientoInventarioRepositorio movimientos,
                                   MotivoRepositorio motivos,
                                   UsuarioRepositorio usuarios,
                                   @Autowired(required = false) RegistradorAuditoria auditoria) {
        this.producciones = producciones;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.motivos = motivos;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<DetalleProduccionDTO, ErrorProduccion> ejecutar(LiquidarSobranteComando comando) {
        if (comando == null || comando.produccionId() == null || comando.produccionId().isBlank()) {
            return Resultado.fracaso(ErrorProduccion.PRODUCCION_NO_ENCONTRADA);
        }

        Produccion produccion = producciones.findById(comando.produccionId()).orElse(null);
        if (produccion == null) {
            return Resultado.fracaso(ErrorProduccion.PRODUCCION_NO_ENCONTRADA);
        }

        if (comando.localId() != null && !produccion.getLocal().getId().equals(comando.localId())) {
            return Resultado.fracaso(ErrorProduccion.LOCAL_NO_ENCONTRADO);
        }

        if (produccion.getDestinoSobrante() != null) {
            return Resultado.fracaso(ErrorProduccion.SOBRANTE_YA_REGISTRADO);
        }

        if (comando.sobrante() == null || comando.sobrante().compareTo(BigDecimal.ZERO) <= 0) {
            return Resultado.fracaso(ErrorProduccion.SOBRANTE_INVALIDO);
        }

        if (comando.sobrante().compareTo(produccion.getRendimientoObtenido()) > 0) {
            return Resultado.fracaso(ErrorProduccion.SOBRANTE_INVALIDO);
        }

        if (comando.destinoSobrante() == null || comando.destinoSobrante().isBlank()) {
            return Resultado.fracaso(ErrorProduccion.DESTINO_INVALIDO);
        }

        String destino = comando.destinoSobrante().trim().toLowerCase();
        if (!DESTINOS_VALIDOS.contains(destino)) {
            return Resultado.fracaso(ErrorProduccion.DESTINO_INVALIDO);
        }

        Motivo motivo = null;
        if (comando.motivoId() != null && !comando.motivoId().isBlank()) {
            motivo = motivos.findById(comando.motivoId()).orElse(null);
        }

        Usuario usuario = null;
        if (comando.usuarioId() != null && !comando.usuarioId().isBlank()) {
            usuario = usuarios.findById(comando.usuarioId()).orElse(null);
        }
        if (usuario == null) {
            usuario = produccion.getRegistradoPor();
        }

        // Si el destino es 'perdida', 'personal' u 'otro', se descuenta el stock de existencias
        if ("perdida".equals(destino) || "personal".equals(destino) || "otro".equals(destino)) {
            Producto producto = produccion.getProducto();
            Existencia ex = existencias.findByLocalIdAndProductoId(produccion.getLocal().getId(), producto.getId())
                    .orElseGet(() -> new Existencia(produccion.getLocal().getId(), producto.getId(), null));

            BigDecimal stockAnterior = ex.getStockActual() != null ? ex.getStockActual() : BigDecimal.ZERO;
            BigDecimal stockNuevo = stockAnterior.subtract(comando.sobrante());

            BigDecimal costoUnitario = producto.getCostoUltimaCompra() != null ? producto.getCostoUltimaCompra() : BigDecimal.ZERO;

            String movId = Identificador.nuevo().valor();
            OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

            String obs = comando.observacion() != null && !comando.observacion().isBlank()
                    ? comando.observacion().trim()
                    : "Sobrante de producción (" + destino + ") - " + comando.sobrante() + " "
                    + (producto.getUnidadBase() != null ? producto.getUnidadBase().getCodigo() : "und");

            MovimientoInventario salida = new MovimientoInventario(
                    movId,
                    produccion.getLocal(),
                    producto,
                    "produccion",
                    "salida",
                    comando.sobrante(),
                    producto.getUnidadBase(),
                    comando.sobrante(),
                    costoUnitario,
                    stockAnterior,
                    stockNuevo,
                    "produccion",
                    produccion.getId(),
                    motivo,
                    obs,
                    ahora,
                    usuario
            );

            MovimientoInventario movGuardado = movimientos.saveAndFlush(salida);
            ex.actualizarStock(stockNuevo, movGuardado);
            existencias.saveAndFlush(ex);
        }

        produccion.liquidarSobrante(comando.sobrante(), destino, motivo);
        Produccion guardada = producciones.saveAndFlush(produccion);

        if (auditoria != null) {
            auditoria.registrar(
                    "sobrante_produccion",
                    "produccion",
                    guardada.getId(),
                    Map.of("estado", "confirmada"),
                    Map.of("sobrante", comando.sobrante(), "destinoSobrante", destino),
                    motivo != null ? motivo.getId() : null,
                    comando.observacion()
            );
        }

        return Resultado.exito(RegistrarProduccion.mapearDetalle(guardada));
    }
}
