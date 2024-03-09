package mvega.dev.cuentas.auth.service.implementation;

import mvega.dev.cuentas.auth.persistence.entity.User;
import mvega.dev.cuentas.auth.persistence.repository.UserRepository;
import mvega.dev.cuentas.auth.service.IUserService;
import mvega.dev.cuentas.auth.service.mapper.UserToUserDTO;
import mvega.dev.cuentas.auth.service.mapper.dto.AddUserDTO;
import mvega.dev.cuentas.auth.service.mapper.dto.UserDTO;
import mvega.dev.cuentas.exceptions.CuentasException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserToUserDTO userToUserDTO;
    @Autowired
    //private PasswordEncoder passwordEncoder;
    @Override
    public List<UserDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(user -> userToUserDTO.map(user))
                .toList();
    }

    @Override
    public UserDTO findById(UUID id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()){
            return userToUserDTO.map(optionalUser.get());
        }
        throw new CuentasException("User not found", HttpStatus.NOT_FOUND);
    }

    @Override
    public void update(AddUserDTO userDTO) {
        Optional<User> optionalUser = userRepository.findByUsername(userDTO.getUsername());
        if(optionalUser.isPresent()){
            if(userDTO.getName().isBlank() ||
            userDTO.getEmail().isBlank() ||
            userDTO.getPassword().isBlank()){
                throw new CuentasException("Los campos obligatorios no pueden estar en blanco.", HttpStatus.BAD_REQUEST);
            }else {
                User user = optionalUser.get();
                user.setName(userDTO.getName());
                user.setLastname(userDTO.getLastname());
                user.setPassword(/*passwordEncoder.encode(*/userDTO.getPassword()/*)*/);
                user.setPassword(userDTO.getPassword());
                user.setEmail(userDTO.getEmail());
                user.setCountry(userDTO.getCountry());
                userRepository.save(user);
                return;
            }
        }
        throw new CuentasException("El usuario no existe.", HttpStatus.NOT_FOUND);
    }
}
