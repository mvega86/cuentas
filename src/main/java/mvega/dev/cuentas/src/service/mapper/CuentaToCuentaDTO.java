package mvega.dev.cuentas.src.service.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import mvega.dev.cuentas.mapper.IMapper;
import mvega.dev.cuentas.src.persistence.entity.Cuenta;
import mvega.dev.cuentas.src.service.mapper.dto.CuentaDTO;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@Builder
public class CuentaToCuentaDTO implements IMapper<Cuenta, CuentaDTO> {
    @Override
    public CuentaDTO map(Cuenta in) {
        return new CuentaDTO(
                in.getId(),
                in.getFecha(),
                in.getCantidad(),
                in.getPrecio(),
                in.getConcepto(),
                in.getFrecuencia(),
                in.getCasa()
        );
    }
}
