package mvega.dev.cuentas.src.controller;

import mvega.dev.cuentas.src.service.IConceptoServicio;
import mvega.dev.cuentas.src.service.mapper.dto.AdConceptoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conceptos")
public class ConceptoController {
    @Autowired
    private IConceptoServicio conceptoServicio;
    @PostMapping("/adicionar")
    public ResponseEntity<?> adicionar(@RequestBody AdConceptoDTO adConceptoDTO){
        conceptoServicio.adicionar(adConceptoDTO);
        return ResponseEntity.ok("Concepto creado");
    }
    @GetMapping
    public ResponseEntity<?> obtenerTodos(){
        return ResponseEntity.ok(conceptoServicio.obtenerTodos());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id){
        return ResponseEntity.ok(conceptoServicio.obtenerPorId(id));
    }
    @GetMapping("/{name}")
    public ResponseEntity<?> obtenerPorNombre(@PathVariable String name){
        return ResponseEntity.ok(conceptoServicio.obtenerPorNombre(name));
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody AdConceptoDTO adConceptoDTO){
        conceptoServicio.actualizar(id, adConceptoDTO);
        return ResponseEntity.ok("Concepto actualizado");
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id){
        conceptoServicio.eliminar(id);
        return ResponseEntity.ok("Concepto eliminado.");
    }
}
