package pe.rikos.operacion.compras.aplicacion;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.compras.dominio.Compra;
import pe.rikos.operacion.compras.dominio.CompraRepositorio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Service
public class GestionarAdjuntoCompra {

    private static final Path DIRECTORIO_BASE = Paths.get("uploads", "comprobantes-compra");
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(".pdf", ".png", ".jpg", ".jpeg", ".webp");

    private final CompraRepositorio compras;

    public GestionarAdjuntoCompra(CompraRepositorio compras) {
        this.compras = compras;
    }

    @Transactional
    public Resultado<String, ErrorCompra> guardarAdjunto(String localId, String compraId, MultipartFile archivo) {
        Compra compra = compras.findByIdAndLocalId(compraId, localId).orElse(null);
        if (compra == null) {
            return Resultado.fracaso(ErrorCompra.COMPRA_NO_ENCONTRADA);
        }

        if (archivo == null || archivo.isEmpty()) {
            return Resultado.fracaso(ErrorCompra.DETALLES_VACIOS);
        }

        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null) {
            nombreOriginal = "comprobante.pdf";
        }

        String extension = "";
        int dot = nombreOriginal.lastIndexOf('.');
        if (dot >= 0) {
            extension = nombreOriginal.substring(dot).toLowerCase();
        }

        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Formato de comprobante no permitido: " + extension);
        }

        try {
            if (!Files.exists(DIRECTORIO_BASE)) {
                Files.createDirectories(DIRECTORIO_BASE);
            }

            String nombreDestino = compraId + "_" + System.currentTimeMillis() + extension;
            Path destino = DIRECTORIO_BASE.resolve(nombreDestino);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            String rutaRelativa = "uploads/comprobantes-compra/" + nombreDestino;
            compra.asignarRutaAdjunto(rutaRelativa);
            compras.save(compra);

            return Resultado.exito(rutaRelativa);
        } catch (IOException e) {
            throw new RuntimeException("Error al almacenar el comprobante adjunto", e);
        }
    }

    @Transactional(readOnly = true)
    public Resultado<Resource, ErrorCompra> obtenerAdjunto(String localId, String compraId) {
        Compra compra = compras.findByIdAndLocalId(compraId, localId).orElse(null);
        if (compra == null) {
            return Resultado.fracaso(ErrorCompra.COMPRA_NO_ENCONTRADA);
        }

        if (compra.getRutaAdjunto() == null || compra.getRutaAdjunto().isBlank()) {
            return Resultado.fracaso(ErrorCompra.COMPRA_NO_ENCONTRADA);
        }

        Path rutaArchivo = Paths.get(compra.getRutaAdjunto());
        if (!Files.exists(rutaArchivo)) {
            return Resultado.fracaso(ErrorCompra.COMPRA_NO_ENCONTRADA);
        }

        return Resultado.exito(new FileSystemResource(rutaArchivo));
    }
}
