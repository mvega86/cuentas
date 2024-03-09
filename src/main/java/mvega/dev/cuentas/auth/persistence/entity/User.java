package mvega.dev.cuentas.auth.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import mvega.dev.cuentas.src.persistence.entity.Casa;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users", schema = "admin", uniqueConstraints = {@UniqueConstraint(columnNames = {"username"}), @UniqueConstraint(columnNames = {"email"})})
public class User/* implements UserDetails*/ {
    @Id
    @GeneratedValue
    private UUID uuid;
    @Column(nullable = false)
    private String username;
    String password;
    String email;
    String name;
    String lastname;
    String country;
    @ManyToMany(cascade=CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = { @JoinColumn(name = "id_user", referencedColumnName = "uuid") },
            inverseJoinColumns = { @JoinColumn(name = "id_rol", referencedColumnName = "id") }
    )
    @JsonIgnore
    private List<Role> roles = new ArrayList<>();

    @ManyToMany(mappedBy = "users")
    @JsonIgnore
    private List<Casa> casas = new ArrayList<>();

    public void addCasa(Casa casa){
        casas.add(casa);
        casa.getUsers().add(this);
    }

    public void addRole(Role role){
        roles.add(role);
        role.getUsers().add(this);
    }

    /*@Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }*/
}
