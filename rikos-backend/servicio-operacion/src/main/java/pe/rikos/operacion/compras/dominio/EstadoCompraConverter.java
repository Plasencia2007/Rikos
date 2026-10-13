package pe.rikos.operacion.compras.dominio;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoCompraConverter implements AttributeConverter<EstadoCompra, String> {

    @Override
    public String convertToDatabaseColumn(EstadoCompra attribute) {
        return attribute != null ? attribute.getCodigo() : null;
    }

    @Override
    public EstadoCompra convertToEntityAttribute(String dbData) {
        return dbData != null ? EstadoCompra.desdeCodigo(dbData) : null;
    }
}
