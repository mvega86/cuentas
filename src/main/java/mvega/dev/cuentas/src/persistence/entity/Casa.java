package mvega.dev.cuentas.src.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import mvega.dev.cuentas.auth.persistence.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "casas",
        schema = "cuentas")
public class Casa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String calle;
    private String numero;
    private String escalera;
    private String piso;
    private String letra;
    private String ciudad;
    private String provincia;

    @OneToMany(mappedBy = "casa")
    @JsonIgnore
    private List<Cuenta> cuentas = new ArrayList<>();

    @ManyToMany(cascade=CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_casas", schema = "cuentas",
            joinColumns = { @JoinColumn(name = "id_casa", referencedColumnName = "id") },
            inverseJoinColumns = { @JoinColumn(name = "id_user", referencedColumnName = "uuid") }
    )
    @JsonIgnore
    private List<User> users = new ArrayList<>();

    public void addUser(User user){
        users.add(user);
        user.getCasas().add(this);
    }
}
