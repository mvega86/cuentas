package mvega.dev.cuentas.src.service.mapper;

import mvega.dev.cuentas.mapper.IMapper;
import mvega.dev.cuentas.src.persistence.entity.Detalle;
import mvega.dev.cuentas.src.persistence.entity.EPrioridad;
import mvega.dev.cuentas.src.service.mapper.dto.AdDetalleDTO;
import org.springframework.stereotype.Component;

@Component
public class AdDetalleDTOToDetalle implements IMapper<AdDetalleDTO, Detalle> {
    @Override
    public Detalle map(AdDetalleDTO in) {
        return new Detalle(
                null,
                in.getNombre(),
                in.getMedida(),
                in.getPrecio(),
                in.getProveedor(),
                in.getPrioridad(),
                in.getConcepto_detalle()
        );
    }
}
