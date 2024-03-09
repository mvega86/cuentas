package mvega.dev.cuentas.src.service.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import mvega.dev.cuentas.mapper.IMapper;
import mvega.dev.cuentas.src.persistence.entity.Casa;
import mvega.dev.cuentas.src.service.mapper.dto.CasaDTO;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@Builder
public class CasaToCasaDTO implements IMapper<Casa, CasaDTO> {
    @Override
    public CasaDTO map(Casa in) {
        return new CasaDTO(
                 in.getId(),
                 in.getCalle(),
                in.getNumero(),
                in.getEscalera(),
                in.getPiso(),
                in.getLetra(),
                in.getCiudad(),
                in.getProvincia(),
                in.getUsers(),
                in.getCuentas()
                );
    }
}
