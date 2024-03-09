package mvega.dev.cuentas.src.service;

import mvega.dev.cuentas.src.service.mapper.dto.AdConceptoDTO;
import mvega.dev.cuentas.src.service.mapper.dto.ConceptoDTO;

import java.util.List;

public interface IConceptoServicio {
    public void adicionar(AdConceptoDTO adConceptoDTO);

    List<ConceptoDTO> obtenerTodos();

    ConceptoDTO obtenerPorId(Long id);
    ConceptoDTO obtenerPorNombre(String nombre);

    void eliminar(Long id);

    void actualizar(Long id, AdConceptoDTO adConceptoDTO);
}
