package mvega.dev.cuentas.src.service.mapper.dto;

import lombok.*;
import mvega.dev.cuentas.src.persistence.entity.Concepto;
import mvega.dev.cuentas.src.persistence.entity.EPrioridad;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DetalleDTO {
    private Long id;
    private String nombre;
    private String medida;
    private BigDecimal price;
    private String proveedor;
    private EPrioridad prioridad;
    private Concepto concepto;
}
