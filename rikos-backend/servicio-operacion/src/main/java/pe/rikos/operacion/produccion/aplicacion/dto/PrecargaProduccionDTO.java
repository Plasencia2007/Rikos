package pe.rikos.operacion.produccion.aplicacion.dto;

import java.util.List;
import java.util.Map;

public record PrecargaProduccionDTO(
        List<ProductoConRecetaDTO> productos,
        Map<String, Integer> porcionesPlanificadasPorProductoId
) {}
