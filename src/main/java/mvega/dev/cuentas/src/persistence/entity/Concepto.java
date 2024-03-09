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
@Table(name = "conceptos", schema = "cuentas", uniqueConstraints = {@UniqueConstraint(columnNames = {"nombre"})})
public class Concepto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nombre;
    private EPrioridad prioridad;

    @OneToMany(mappedBy = "concepto")
    @JsonIgnore
    private List<Cuenta> cuentas = new ArrayList<>();

    @OneToMany(mappedBy = "concepto_detalle")
    @JsonIgnore
    private List<Detalle> detalles = new ArrayList<>();




}
