package mvega.dev.cuentas.src.service.mapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;
import mvega.dev.cuentas.src.persistence.entity.Casa;
import mvega.dev.cuentas.src.persistence.entity.Concepto;
import mvega.dev.cuentas.src.persistence.entity.Frecuencia;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdCuentaDTO {
    private Timestamp fecha;
    private BigDecimal cantidad;
    private BigDecimal total;
    private Concepto concepto;
    private Frecuencia frecuencia;
    private Casa casa;
}
