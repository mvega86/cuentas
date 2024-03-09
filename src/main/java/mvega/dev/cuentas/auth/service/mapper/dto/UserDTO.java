package mvega.dev.cuentas.auth.service.mapper.dto;

import jakarta.persistence.Column;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String username;
    String email;
    String name;
    String lastname;
    String country;
}
