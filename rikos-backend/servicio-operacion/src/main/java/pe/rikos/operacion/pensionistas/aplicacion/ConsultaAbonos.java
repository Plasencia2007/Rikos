package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.catalogo.motivos.ConsultaMotivos;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Abono;
import pe.rikos.operacion.pensionistas.infraestructura.AbonoRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsultaAbonos {

    private final AbonoRepositorio abonos;
    private final PensionistaRepositorio pensionistas;
    private final UsuarioRepositorio usuarios;
    private final ConsultaMotivos motivos;

    public ConsultaAbonos(AbonoRepositorio abonos, PensionistaRepositorio pensionistas,
                          UsuarioRepositorio usuarios, ConsultaMotivos motivos) {
        this.abonos = abonos;
        this.pensionistas = pensionistas;
        this.usuarios = usuarios;
        this.motivos = motivos;
    }

    @Transactional(readOnly = true)
    public List<FilaAbono> listar(String estado, String busqueda) {
        List<Abono> lista;
        if (estado != null && !estado.isBlank() && !estado.equalsIgnoreCase("todos")) {
            if (Abono.ESTADO_PENDIENTE.equalsIgnoreCase(estado)) {
                lista = abonos.findByEstadoOrderByCreadoEnAsc(Abono.ESTADO_PENDIENTE);
            } else {
                lista = abonos.findByEstadoOrderByCreadoEnDesc(estado.toLowerCase());
            }
        } else {
            lista = abonos.findAllByOrderByCreadoEnDesc();
        }

        Map<String, Pensionista> pensionistasMap = new HashMap<>();
        Map<String, String> usuariosMap = new HashMap<>();

        String filtro = busqueda != null && !busqueda.isBlank() ? busqueda.strip().toLowerCase() : null;

        return lista.stream()
                .map(a -> {
                    Pensionista p = pensionistasMap.computeIfAbsent(a.getPensionistaId(),
                            id -> pensionistas.findById(id).orElse(null));
                    String pNombre = p != null ? p.getNombres() + " " + p.getApellidos() : "Desconocido";
                    String pDni = p != null ? p.getDni() : "";

                    String regNombre = a.getRegistradoPor() == null ? null :
                            usuariosMap.computeIfAbsent(a.getRegistradoPor(),
                                    id -> usuarios.findById(id).map(Usuario::getNombreUsuario).orElse(null));

                    String aprNombre = a.getAprobadoPor() == null ? null :
                            usuariosMap.computeIfAbsent(a.getAprobadoPor(),
                                    id -> usuarios.findById(id).map(Usuario::getNombreUsuario).orElse(null));

                    String motivoDesc = a.getMotivoRechazoId() == null ? null :
                            motivos.descripcion(a.getMotivoRechazoId()).orElse(null);

                    return new FilaAbono(
                            a.getId(),
                            a.getPensionistaId(),
                            pNombre,
                            pDni,
                            a.getLocalId(),
                            a.getMedio(),
                            a.getMonto(),
                            a.getFechaOperacion(),
                            a.getNumeroOperacion(),
                            a.getCuentaDestino(),
                            a.getRutaAdjunto(),
                            a.getOrigen(),
                            a.getEstado(),
                            a.getRegistradoPor(),
                            regNombre,
                            a.getAprobadoPor(),
                            aprNombre,
                            a.getAprobadoEn(),
                            a.getMotivoRechazoId(),
                            motivoDesc,
                            a.getObservacion(),
                            a.getCreadoEn()
                    );
                })
                .filter(f -> {
                    if (filtro == null) return true;
                    return f.pensionistaNombre().toLowerCase().contains(filtro)
                            || f.pensionistaDni().toLowerCase().contains(filtro)
                            || (f.numeroOperacion() != null && f.numeroOperacion().toLowerCase().contains(filtro))
                            || (f.cuentaDestino() != null && f.cuentaDestino().toLowerCase().contains(filtro));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarPendientes() {
        return abonos.countByEstado(Abono.ESTADO_PENDIENTE);
    }
}
