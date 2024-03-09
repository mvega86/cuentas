package mvega.dev.cuentas.src.service.mapper;

import lombok.*;
import mvega.dev.cuentas.mapper.IMapper;
import mvega.dev.cuentas.src.persistence.entity.Concepto;
import mvega.dev.cuentas.src.service.mapper.dto.AdConceptoDTO;
import mvega.dev.cuentas.src.service.mapper.dto.ConceptoDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

@Component
@Getter
@Setter
@Builder
public class AdConceptoDTOToConcepto implements IMapper<AdConceptoDTO, Concepto> {
    @Override
    public Concepto map(AdConceptoDTO in) {
        return new Concepto(
                null,
                in.getNombre(),
                in.getPrioridad(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }
}
