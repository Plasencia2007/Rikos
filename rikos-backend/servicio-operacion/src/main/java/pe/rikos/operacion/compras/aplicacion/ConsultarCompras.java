package pe.rikos.operacion.compras.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.compras.aplicacion.dto.FilaCompraDto;
import pe.rikos.operacion.compras.dominio.Compra;
import pe.rikos.operacion.compras.dominio.CompraRepositorio;
import pe.rikos.operacion.compras.dominio.EstadoCompra;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;

import java.util.List;

@Service
public class ConsultarCompras {

    private final CompraRepositorio compras;
    private final LocalRepositorio locales;

    public ConsultarCompras(CompraRepositorio compras, LocalRepositorio locales) {
        this.compras = compras;
        this.locales = locales;
    }

    @Transactional(readOnly = true)
    public Resultado<List<FilaCompraDto>, ErrorCompra> listarPorLocal(String localId, EstadoCompra estadoFiltro) {
        Local local = locales.findById(localId).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorCompra.LOCAL_NO_ENCONTRADO);
        }

        List<Compra> lista;
        if (estadoFiltro != null) {
            lista = compras.findByLocalIdAndEstadoOrderByFechaDocumentoDescCreadoEnDesc(localId, estadoFiltro);
        } else {
            lista = compras.findByLocalIdOrderByFechaDocumentoDescCreadoEnDesc(localId);
        }

        List<FilaCompraDto> dtos = lista.stream().map(RegistrarCompra::mapearFilaCompra).toList();
        return Resultado.exito(dtos);
    }

    @Transactional(readOnly = true)
    public Resultado<FilaCompraDto, ErrorCompra> obtenerPorId(String localId, String compraId) {
        Compra compra = compras.findByIdAndLocalId(compraId, localId).orElse(null);
        if (compra == null) {
            return Resultado.fracaso(ErrorCompra.COMPRA_NO_ENCONTRADA);
        }
        return Resultado.exito(RegistrarCompra.mapearFilaCompra(compra));
    }
}
