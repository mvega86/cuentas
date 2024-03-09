package mvega.dev.cuentas.src.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cuentas", schema = "cuentas")
public class Cuenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Timestamp fecha;
    private BigDecimal cantidad;
    private BigDecimal precio;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_concepto")
    @JsonIgnore
    private Concepto concepto;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_frecuencia")
    @JsonIgnore
    private Frecuencia frecuencia;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name="id_casa")
    @JsonIgnore
    private Casa casa;


}
