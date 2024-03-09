package mvega.dev.cuentas.src.service.mapper.dto;

import lombok.*;
import mvega.dev.cuentas.src.persistence.entity.EPrioridad;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdConceptoDTO {
    private String nombre;
    private EPrioridad prioridad;
}
