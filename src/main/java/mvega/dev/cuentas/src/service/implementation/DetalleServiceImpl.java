package mvega.dev.cuentas.src.service.implementation;

import mvega.dev.cuentas.exceptions.CuentasException;
import mvega.dev.cuentas.src.persistence.entity.Detalle;
import mvega.dev.cuentas.src.persistence.repository.IDetalleRepository;
import mvega.dev.cuentas.src.service.IDetalleService;
import mvega.dev.cuentas.src.service.mapper.AdDetalleDTOToDetalle;
import mvega.dev.cuentas.src.service.mapper.DetalleToDetalleDTO;
import mvega.dev.cuentas.src.service.mapper.dto.AdDetalleDTO;
import mvega.dev.cuentas.src.service.mapper.dto.DetalleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class DetalleServiceImpl implements IDetalleService {
    @Autowired
    private IDetalleRepository detalleRepository;
    @Autowired
    private DetalleToDetalleDTO detalleToDetalleDTO;
    @Autowired
    private AdDetalleDTOToDetalle adDetalleDTOToDetalle;
    @Override
    public List<DetalleDTO> obtenerTodos() {
        return detalleRepository.findAll()
                .stream()
                .map(detalle -> detalleToDetalleDTO.map(detalle))
                .toList();
    }

    @Override
    public DetalleDTO obtenerPorId(Long id) {
        Optional<Detalle> optionalDetalle = detalleRepository.findById(id);
        if (optionalDetalle.isPresent()) {
            return detalleToDetalleDTO.map(optionalDetalle.get());
        }
        throw new CuentasException("Detalle no encontrado.", HttpStatus.NOT_FOUND);
    }

    @Override
    public void adicionar(AdDetalleDTO adDetalleDTO) {
        if(adDetalleDTO.getNombre().isBlank() || adDetalleDTO.getPrecio().equals(BigDecimal.valueOf(0)) || adDetalleDTO.getConcepto_detalle() == null){
            throw new CuentasException("Debe especificar nombre, precio y concepto del detalle.", HttpStatus.BAD_REQUEST);
        }
        else {
            detalleRepository.save(adDetalleDTOToDetalle.map(adDetalleDTO));
        }
    }

    @Override
    public void actualizar(Long id, AdDetalleDTO adDetalleDTO) {
        Optional<Detalle> optionalDetalle = detalleRepository.findById(id);
        if(optionalDetalle.isPresent()){
            if(adDetalleDTO.getNombre().isBlank() || adDetalleDTO.getPrecio().equals(BigDecimal.valueOf(0)) || adDetalleDTO.getConcepto_detalle() == null) {
                throw new CuentasException("Debe especificar nombre, precio y concepto del detalle.", HttpStatus.BAD_REQUEST);
            }else{
                Detalle detalle = optionalDetalle.get();
                detalle.setNombre(adDetalleDTO.getNombre());
                detalle.setPrecio(adDetalleDTO.getPrecio());
                detalle.setProveedor(adDetalleDTO.getProveedor());
                detalle.setConcepto_detalle(adDetalleDTO.getConcepto_detalle());
                detalleRepository.save(detalle);
            }
        }else {
            throw new CuentasException("Detalle no encontrado.", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public void eliminar(Long id) {
        Optional<Detalle> optionalDetalle = detalleRepository.findById(id);
        if(optionalDetalle.isPresent()){
            detalleRepository.deleteById(id);
        }
        else {
            throw new CuentasException("Detalle no encontrado.", HttpStatus.NOT_FOUND);
        }
    }
}
