package mvega.dev.cuentas.auth.service.mapper.dto;

import lombok.*;

import java.util.UUID;
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddUserDTO {
    String username;
    String password;
    String email;
    String name;
    String lastname;
    String country;
}
