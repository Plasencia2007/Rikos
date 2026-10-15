package pe.rikos.operacion.inventario.aplicacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.inventario.aplicacion.dto.FilaConsumoInternoDTO;
import pe.rikos.operacion.inventario.aplicacion.dto.ResumenConsumoInternoDTO;
import pe.rikos.operacion.inventario.infraestructura.ConsumoInterno;
import pe.rikos.operacion.inventario.infraestructura.ConsumoInternoRepositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ConsultarConsumosInternos {

    private final ConsumoInternoRepositorio consumos;
    private final LocalRepositorio locales;

    @Autowired
    public ConsultarConsumosInternos(ConsumoInternoRepositorio consumos, LocalRepositorio locales) {
        this.consumos = consumos;
        this.locales = locales;
    }

    @Transactional(readOnly = true)
    public Resultado<List<FilaConsumoInternoDTO>, ErrorInventario> listarPorLocal(String localId, LocalDate fecha) {
        if (localId == null || !locales.existsById(localId)) {
            return Resultado.fracaso(ErrorInventario.LOCAL_NO_ENCONTRADO);
        }

        List<ConsumoInterno> lista = fecha != null
                ? consumos.findByLocalIdAndFechaOrderByCreadoEnDesc(localId, fecha)
                : consumos.findByLocalIdOrderByFechaDescCreadoEnDesc(localId);

        List<FilaConsumoInternoDTO> dtos = lista.stream()
                .map(RegistrarConsumoInterno::mapearDTO)
                .toList();

        return Resultado.exito(dtos);
    }

    @Transactional(readOnly = true)
    public Resultado<ResumenConsumoInternoDTO, ErrorInventario> obtenerResumen(String localId, LocalDate fecha) {
        if (localId == null || !locales.existsById(localId)) {
            return Resultado.fracaso(ErrorInventario.LOCAL_NO_ENCONTRADO);
        }

        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        List<ConsumoInterno> lista = consumos.findByLocalIdAndFechaOrderByCreadoEnDesc(localId, fechaConsulta);

        long totalComidas = lista.size();
        BigDecimal costoTotal = lista.stream()
                .map(ConsumoInterno::getCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalConAutorizacion = lista.stream()
                .filter(ConsumoInterno::isRequirioAutorizacion)
                .count();

        return Resultado.exito(new ResumenConsumoInternoDTO(totalComidas, costoTotal, totalConAutorizacion));
    }
}
