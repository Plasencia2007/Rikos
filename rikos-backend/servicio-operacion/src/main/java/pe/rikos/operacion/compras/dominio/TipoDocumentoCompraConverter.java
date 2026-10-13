package pe.rikos.operacion.compras.dominio;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoDocumentoCompraConverter implements AttributeConverter<TipoDocumentoCompra, String> {

    @Override
    public String convertToDatabaseColumn(TipoDocumentoCompra attribute) {
        return attribute != null ? attribute.getCodigo() : null;
    }

    @Override
    public TipoDocumentoCompra convertToEntityAttribute(String dbData) {
        return dbData != null ? TipoDocumentoCompra.desdeCodigo(dbData) : null;
    }
}
