package mvega.dev.cuentas.src.service;

import mvega.dev.cuentas.src.service.mapper.dto.AdDetalleDTO;
import mvega.dev.cuentas.src.service.mapper.dto.DetalleDTO;

import java.util.List;

public interface IDetalleService {
    List<DetalleDTO> obtenerTodos();
    DetalleDTO obtenerPorId(Long id);
    void adicionar(AdDetalleDTO adDetalleDTO);
    void actualizar(Long id, AdDetalleDTO adDetalleDTO);
    void eliminar(Long id);
}
