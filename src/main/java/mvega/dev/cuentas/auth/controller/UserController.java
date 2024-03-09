package mvega.dev.cuentas.auth.controller;

import mvega.dev.cuentas.auth.service.IUserService;
import mvega.dev.cuentas.auth.service.mapper.dto.AddUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = {"http://localhost:4200"})
public class UserController {

    @Autowired
    private IUserService userService;
    @GetMapping()
    public ResponseEntity<?> findAll(){
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable UUID id){
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody AddUserDTO userDTO){
        userService.update(userDTO);
        return ResponseEntity.ok("Usuario actualizado.");
    }
}
