package mvega.dev.cuentas.auth.service;

import mvega.dev.cuentas.auth.persistence.entity.User;
import mvega.dev.cuentas.auth.service.mapper.dto.AddUserDTO;
import mvega.dev.cuentas.auth.service.mapper.dto.UserDTO;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    List<UserDTO> findAll();

    UserDTO findById(UUID id);

    void update(AddUserDTO userDTO);
}
