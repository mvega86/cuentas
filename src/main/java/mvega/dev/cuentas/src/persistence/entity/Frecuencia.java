package mvega.dev.cuentas.src.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "frecuencias", schema = "cuentas")
public class Frecuencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private EFrecuencia frecuencia;
    private BigDecimal cantidad;
    @OneToMany(mappedBy = "frecuencia")
    @JsonIgnore
    private List<Cuenta> cuentas = new ArrayList<>();
}
