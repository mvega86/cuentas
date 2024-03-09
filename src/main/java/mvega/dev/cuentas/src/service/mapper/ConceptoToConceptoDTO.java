package mvega.dev.cuentas.src.service.mapper;

import lombok.*;
import mvega.dev.cuentas.mapper.IMapper;
import mvega.dev.cuentas.src.persistence.entity.Concepto;
import mvega.dev.cuentas.src.service.mapper.dto.ConceptoDTO;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@Builder
public class ConceptoToConceptoDTO implements IMapper<Concepto, ConceptoDTO> {
    @Override
    public ConceptoDTO map(Concepto in) {
        return new ConceptoDTO(
                in.getId(),
                in.getNombre(),
                in.getPrioridad(),
                in.getCuentas(),
                in.getDetalles()
        );
    }
}
