package mvega.dev.cuentas.src.service;

import mvega.dev.cuentas.src.service.mapper.dto.AdCasaDTO;
import mvega.dev.cuentas.src.service.mapper.dto.CasaDTO;

import java.util.List;

public interface ICasaService {
    public void adicionar(AdCasaDTO casaDTO);

    List<CasaDTO> obtenerTodos();

    CasaDTO obtenerPorId(Long id);

    void eliminar(Long id);

    void actualizar(Long id, AdCasaDTO casaDTO);
}
