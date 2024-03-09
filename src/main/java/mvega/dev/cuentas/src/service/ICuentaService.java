package mvega.dev.cuentas.src.service;

import mvega.dev.cuentas.src.service.mapper.dto.AdCuentaDTO;
import mvega.dev.cuentas.src.service.mapper.dto.CuentaDTO;

import java.util.List;

public interface ICuentaService {
    List<CuentaDTO> obtenerTodos();
    CuentaDTO obtenerPorId(Long id);
    void adicionar(AdCuentaDTO cuentaDTO);
    void eliminar(Long id);
    void actualizar(Long id, AdCuentaDTO cuentaDTO);

}
