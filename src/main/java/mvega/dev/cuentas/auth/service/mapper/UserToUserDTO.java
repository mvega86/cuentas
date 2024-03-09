package mvega.dev.cuentas.auth.service.mapper;

import mvega.dev.cuentas.auth.persistence.entity.User;
import mvega.dev.cuentas.auth.service.mapper.dto.UserDTO;
import mvega.dev.cuentas.mapper.IMapper;
import org.springframework.stereotype.Component;

@Component
public class UserToUserDTO implements IMapper<User, UserDTO> {
    @Override
    public UserDTO map(User in) {
        return new UserDTO(
                in.getUuid(),
                in.getUsername(),
                in.getEmail(),
                in.getName(),
                in.getLastname(),
                in.getCountry()
        );
    }
}
