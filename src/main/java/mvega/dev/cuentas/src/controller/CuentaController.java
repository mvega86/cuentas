package mvega.dev.cuentas.src.controller;

import mvega.dev.cuentas.src.service.ICuentaService;
import mvega.dev.cuentas.src.service.mapper.dto.AdCuentaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cuentas")
@CrossOrigin(origins = {"http://localhost:4200"})
public class CuentaController {
    @Autowired
    private ICuentaService cuentaService;

    @GetMapping
    public ResponseEntity<?> obtenerTodos(){
        return ResponseEntity.ok(cuentaService.obtenerTodos());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id){
        return ResponseEntity.ok(cuentaService.obtenerPorId(id));
    }
    @PostMapping("/adicionar")
    public ResponseEntity<?> adicionar(@RequestBody AdCuentaDTO cuentaDTO){
        cuentaService.adicionar(cuentaDTO);
        return ResponseEntity.ok("Cuenta creada.");
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody AdCuentaDTO cuentaDTO){
        cuentaService.actualizar(id,cuentaDTO);
        return ResponseEntity.ok("Cuenta actualizada.");
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id){
        cuentaService.eliminar(id);
        return ResponseEntity.ok("Cuenta eliminada.");
    }
}
