package mvega.dev.cuentas.src.service.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import mvega.dev.cuentas.mapper.IMapper;
import mvega.dev.cuentas.src.persistence.entity.Cuenta;
import mvega.dev.cuentas.src.service.mapper.dto.AdCuentaDTO;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@Builder
public class AdCuentaDTOToCuenta implements IMapper<AdCuentaDTO, Cuenta> {
    @Override
    public Cuenta map(AdCuentaDTO in) {
        return new Cuenta(
                null,
                in.getFecha(),
                in.getCantidad(),
                in.getTotal(),
                in.getConcepto(),
                in.getFrecuencia(),
                in.getCasa()
        );
    }
}
