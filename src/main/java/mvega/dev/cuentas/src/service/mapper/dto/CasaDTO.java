package mvega.dev.cuentas.src.service.mapper.dto;

import lombok.*;
import mvega.dev.cuentas.auth.persistence.entity.User;
import mvega.dev.cuentas.src.persistence.entity.Cuenta;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CasaDTO {
    private Long id;
    private String calle;
    private String numero;
    private String escalera;
    private String piso;
    private String letra;
    private String ciudad;
    private String provincia;
    private List<User> users;
    private List<Cuenta> cuentas;
}
