package pe.rikos.operacion.produccion.aplicacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.CalculadoraCostoReceta;
import pe.rikos.operacion.catalogo.infraestructura.IngredienteReceta;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenu;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenuRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.inventario.infraestructura.Existencia;
import pe.rikos.operacion.inventario.infraestructura.ExistenciaRepositorio;
import pe.rikos.operacion.produccion.aplicacion.dto.DetalleProduccionDTO;
import pe.rikos.operacion.produccion.aplicacion.dto.IngredientePrecargaDTO;
import pe.rikos.operacion.produccion.aplicacion.dto.PrecargaProduccionDTO;
import pe.rikos.operacion.produccion.aplicacion.dto.ProductoConRecetaDTO;
import pe.rikos.operacion.produccion.dominio.ErrorProduccion;
import pe.rikos.operacion.produccion.dominio.Produccion;
import pe.rikos.operacion.produccion.infraestructura.ProduccionRepositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsultarProducciones {

    private final ProduccionRepositorio producciones;
    private final LocalRepositorio locales;
    private final RecetaRepositorio recetas;
    private final ExistenciaRepositorio existencias;
    private final OpcionMenuRepositorio opcionesMenu;
    private final CalculadoraCostoReceta calculadoraCostoReceta;

    @Autowired
    public ConsultarProducciones(ProduccionRepositorio producciones,
                                 LocalRepositorio locales,
                                 RecetaRepositorio recetas,
                                 ExistenciaRepositorio existencias,
                                 OpcionMenuRepositorio opcionesMenu,
                                 CalculadoraCostoReceta calculadoraCostoReceta) {
        this.producciones = producciones;
        this.locales = locales;
        this.recetas = recetas;
        this.existencias = existencias;
        this.opcionesMenu = opcionesMenu;
        this.calculadoraCostoReceta = calculadoraCostoReceta;
    }

    @Transactional(readOnly = true)
    public Resultado<List<DetalleProduccionDTO>, ErrorProduccion> listarPorLocal(String localId, LocalDate fecha) {
        if (localId == null || !locales.existsById(localId)) {
            return Resultado.fracaso(ErrorProduccion.LOCAL_NO_ENCONTRADO);
        }

        List<Produccion> lista = fecha != null
                ? producciones.findByLocalIdAndFechaOrderByCreadoEnDesc(localId, fecha)
                : producciones.findByLocalIdOrderByFechaDescCreadoEnDesc(localId);

        List<DetalleProduccionDTO> dtos = lista.stream().map(RegistrarProduccion::mapearDetalle).toList();
        return Resultado.exito(dtos);
    }

    @Transactional(readOnly = true)
    public Resultado<PrecargaProduccionDTO, ErrorProduccion> obtenerPrecarga(String localId, LocalDate fecha, String servicio) {
        Local local = locales.findById(localId).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorProduccion.LOCAL_NO_ENCONTRADO);
        }

        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();

        // 1. Obtener porciones planificadas de la programación del menú (D2)
        Map<String, Integer> planificadas = new HashMap<>();
        List<OpcionMenu> opcionesDelDia = opcionesMenu.findDelDia(fechaConsulta);
        for (OpcionMenu op : opcionesDelDia) {
            if (op.getProducto() != null && op.getPorcionesEstimadas() != null) {
                planificadas.put(op.getProducto().getId(), op.getPorcionesEstimadas());
            }
        }

        // 2. Listar productos que tienen receta activa
        List<Receta> recetasActivas = recetas.findByActivoTrue();
        List<ProductoConRecetaDTO> productosConReceta = new ArrayList<>();

        for (Receta rec : recetasActivas) {
            Producto prod = rec.getProducto();
            if (prod == null || !prod.isActivo()) {
                continue;
            }

            List<IngredientePrecargaDTO> ingDtos = new ArrayList<>();
            if (rec.getIngredientes() != null) {
                for (IngredienteReceta ing : rec.getIngredientes()) {
                    Producto item = ing.getProducto();
                    if (item == null) continue;

                    BigDecimal stock = existencias.findByLocalIdAndProductoId(localId, item.getId())
                            .map(Existencia::getStockActual)
                            .orElse(BigDecimal.ZERO);

                    BigDecimal costo = item.getCostoUltimaCompra() != null ? item.getCostoUltimaCompra() : BigDecimal.ZERO;
                    String unidadSimbolo = item.getUnidadBase() != null ? item.getUnidadBase().getCodigo() : "";

                    BigDecimal cantBase = calculadoraCostoReceta.aUnidadBase(item, ing.getCantidad(), ing.getUnidad());
                    if (cantBase == null) {
                        cantBase = ing.getCantidad();
                    }

                    ingDtos.add(new IngredientePrecargaDTO(
                            item.getId(),
                            item.getNombre(),
                            cantBase,
                            unidadSimbolo,
                            stock,
                            costo
                    ));
                }
            }

            String unidadProd = prod.getUnidadBase() != null ? prod.getUnidadBase().getCodigo() : "";
            productosConReceta.add(new ProductoConRecetaDTO(
                    prod.getId(),
                    prod.getCodigo(),
                    prod.getNombre(),
                    unidadProd,
                    prod.isEsSubreceta(),
                    rec.getId(),
                    rec.getRendimiento(),
                    rec.getPorciones(),
                    ingDtos
            ));
        }

        return Resultado.exito(new PrecargaProduccionDTO(productosConReceta, planificadas));
    }
}
