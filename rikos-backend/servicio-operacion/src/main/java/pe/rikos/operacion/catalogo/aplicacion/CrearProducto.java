package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;

import java.util.regex.Pattern;

@Service
public class CrearProducto {

    // Sin espacios ni minusculas sueltas: "ARROZ" y "arroz " no deben poder
    // coexistir como codigos distintos que en realidad colisionan en
    // cualquier reporte que los muestre juntos. Mismo criterio que el
    // codigo de local (CrearLocal, sprint 1), adaptado: aqui se permiten
    // mayusculas porque el codigo de producto se imprime en etiquetas y
    // comandas, no es parte de una URL.
    private static final Pattern FORMATO_CODIGO = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9-]*$");

    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;
    private final UnidadMedidaRepositorio unidades;

    public CrearProducto(ProductoRepositorio productos, CategoriaProductoRepositorio categorias,
                          UnidadMedidaRepositorio unidades) {
        this.productos = productos;
        this.categorias = categorias;
        this.unidades = unidades;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String codigo, String nombre, String categoriaId,
            String unidadBaseId, boolean esVendible, boolean esInsumo, boolean esSubreceta, Long precioVenta,
            boolean controlaStock, String actorId) {

        if (codigo == null || !FORMATO_CODIGO.matcher(codigo).matches()) {
            return Resultado.fracaso(ErrorCatalogo.CODIGO_FORMATO_INVALIDO);
        }

        if (productos.existsByCodigo(codigo)) {
            return Resultado.fracaso(ErrorCatalogo.CODIGO_DUPLICADO);
        }

        CategoriaProducto categoria = categorias.findById(categoriaId).orElse(null);
        if (categoria == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        // 4.1: los productos cuelgan de la hoja, nunca de una categoria con
        // subcategorias activas -- es el error mas facil de cometer al cargar
        // el catalogo, y aqui se rechaza en vez de permitirse.
        if (categorias.existsByCategoriaPadreIdAndActivoTrue(categoriaId)) {
            return Resultado.fracaso(ErrorCatalogo.CATEGORIA_NO_ES_HOJA);
        }

        UnidadMedida unidad = unidades.findById(unidadBaseId).orElse(null);
        if (unidad == null) {
            return Resultado.fracaso(ErrorCatalogo.UNIDAD_OBLIGATORIA);
        }

        if (esVendible && precioVenta == null) {
            return Resultado.fracaso(ErrorCatalogo.PRECIO_VENTA_OBLIGATORIO);
        }

        Producto producto = new Producto(Identificador.nuevo().valor(), codigo, nombre, categoria, unidad,
                esVendible, esInsumo, esSubreceta, precioVenta, controlaStock, actorId);
        return Resultado.exito(productos.save(producto));
    }
}
