package mvega.dev.cuentas.src.service.mapper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import mvega.dev.cuentas.mapper.IMapper;
import mvega.dev.cuentas.src.persistence.entity.Casa;
import mvega.dev.cuentas.src.service.mapper.dto.AdCasaDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@Getter
@Setter
@Builder
public class AdCasaDTOToCasa implements IMapper<AdCasaDTO, Casa> {
    @Override
    public Casa map(AdCasaDTO in) {
        return new Casa(
                null,
                in.getCalle(),
                in.getNumero(),
                in.getEscalera(),
                in.getPiso(),
                in.getLetra(),
                in.getCiudad(),
                in.getProvincia(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }
}
