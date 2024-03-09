package mvega.dev.cuentas.src.service.mapper.dto;

import jakarta.persistence.Column;
import lombok.*;
import mvega.dev.cuentas.src.persistence.entity.Cuenta;
import mvega.dev.cuentas.src.persistence.entity.Detalle;
import mvega.dev.cuentas.src.persistence.entity.EPrioridad;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConceptoDTO {
    private Long id;
    private String nombre;
    private EPrioridad prioridad;
    private List<Cuenta> cuentas;
    private List<Detalle> detalles;
}
