package mvega.dev.cuentas.src.controller;

import mvega.dev.cuentas.src.service.ICasaService;
import mvega.dev.cuentas.src.service.mapper.dto.AdCasaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/casas")
public class CasaController {
    @Autowired
    private ICasaService casaService;

    @GetMapping
    public ResponseEntity<?> obtenerTodos(){
        return ResponseEntity.ok(casaService.obtenerTodos());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id){
        return ResponseEntity.ok(casaService.obtenerPorId(id));
    }
    @PostMapping("/adicionar")
    public ResponseEntity<?> adicionar(@RequestBody AdCasaDTO casaDTO){
        casaService.adicionar(casaDTO);
        return ResponseEntity.ok("Casa creada.");
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody AdCasaDTO casaDTO){
        casaService.actualizar(id, casaDTO);
        return ResponseEntity.ok("Casa actualizada.");
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id){
        casaService.eliminar(id);
        return ResponseEntity.ok("Casa eliminada.");
    }
}
