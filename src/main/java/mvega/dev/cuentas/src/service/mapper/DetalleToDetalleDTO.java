package mvega.dev.cuentas.src.service.mapper;

import mvega.dev.cuentas.mapper.IMapper;
import mvega.dev.cuentas.src.persistence.entity.Detalle;
import mvega.dev.cuentas.src.service.mapper.dto.DetalleDTO;
import org.springframework.stereotype.Component;

@Component
public class DetalleToDetalleDTO implements IMapper<Detalle, DetalleDTO> {
    @Override
    public DetalleDTO map(Detalle in) {
        return new DetalleDTO(
                in.getId(),
                in.getNombre(),
                in.getMedida(),
                in.getPrecio(),
                in.getProveedor(),
                in.getPrioridad(),
                in.getConcepto_detalle()
        );
    }
}
