package mvega.dev.cuentas.src.controller;

import mvega.dev.cuentas.src.service.IDetalleService;
import mvega.dev.cuentas.src.service.mapper.dto.AdDetalleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/detalles")
public class DetalleController {
    @Autowired
    private IDetalleService detalleService;

    @GetMapping
    public ResponseEntity<?> obtenerTodos(){
        return ResponseEntity.ok(detalleService.obtenerTodos());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id){
        return ResponseEntity.ok(detalleService.obtenerPorId(id));
    }
    @PostMapping("/adicionar")
    public ResponseEntity<?> adicionar(@RequestBody AdDetalleDTO adDetalleDTO){
        detalleService.adicionar(adDetalleDTO);
        return ResponseEntity.ok("Detalle creado.");
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody AdDetalleDTO adDetalleDTO){
        detalleService.actualizar(id, adDetalleDTO);
        return ResponseEntity.ok("Detalle actualizado.");
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id){
        detalleService.eliminar(id);
        return ResponseEntity.ok("Detalle eliminado.");
    }
}
