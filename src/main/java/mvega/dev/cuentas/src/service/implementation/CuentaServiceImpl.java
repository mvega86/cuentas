package mvega.dev.cuentas.src.service.implementation;

import mvega.dev.cuentas.exceptions.CuentasException;
import mvega.dev.cuentas.src.persistence.entity.Cuenta;
import mvega.dev.cuentas.src.persistence.repository.ICuentaRepository;
import mvega.dev.cuentas.src.service.ICuentaService;
import mvega.dev.cuentas.src.service.mapper.AdCuentaDTOToCuenta;
import mvega.dev.cuentas.src.service.mapper.CuentaToCuentaDTO;
import mvega.dev.cuentas.src.service.mapper.dto.AdCuentaDTO;
import mvega.dev.cuentas.src.service.mapper.dto.CuentaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CuentaServiceImpl implements ICuentaService {
    @Autowired
    private ICuentaRepository cuentaRepository;
    @Autowired
    private AdCuentaDTOToCuenta adCuentaDTOToCuenta;
    @Autowired
    private CuentaToCuentaDTO cuentaToCuentaDTO;

    @Override
    public List<CuentaDTO> obtenerTodos() {

        return cuentaRepository.findAll()
                .stream()
                .map(cuenta -> cuentaToCuentaDTO.map(cuenta))
                .toList();
    }

    @Override
    public CuentaDTO obtenerPorId(Long id) {
        Optional<Cuenta> optionalCuenta = cuentaRepository.findById(id);
        if(optionalCuenta.isPresent()){
            return cuentaToCuentaDTO.map(optionalCuenta.get());
        }
        throw new CuentasException("No se encontró la cuenta.", HttpStatus.NOT_FOUND);
    }

    @Override
    public void adicionar(AdCuentaDTO cuentaDTO) {
        if(cuentaDTO.getFecha() == null ||
                cuentaDTO.getCasa() == null ||
                cuentaDTO.getConcepto() == null){
            throw new CuentasException("Debe especificar fecha, casa y concepto de la cuenta.", HttpStatus.BAD_REQUEST);
        }else {
            cuentaRepository.save(adCuentaDTOToCuenta.map(cuentaDTO));
        }
    }

    @Override
    public void eliminar(Long id) {
        Optional<Cuenta> optionalCuenta = cuentaRepository.findById(id);
        if(optionalCuenta.isPresent()){
            cuentaRepository.deleteById(id);
            return;
        }
        throw new CuentasException("No se encontró la cuenta.", HttpStatus.NOT_FOUND);
    }

    @Override
    public void actualizar(Long id, AdCuentaDTO cuentaDTO) {
        Optional<Cuenta> optionalCuenta = cuentaRepository.findById(id);
        if(optionalCuenta.isPresent()){
            if(cuentaDTO.getFecha() == null ||
                    cuentaDTO.getPrecio().equals(BigDecimal.valueOf(0)) ||
                    cuentaDTO.getCasa() == null ||
                    cuentaDTO.getConcepto() == null){
                throw new CuentasException("Debe especificar fecha, casa y concepto de la cuenta.", HttpStatus.BAD_REQUEST);
            }else {
                Cuenta cuenta = optionalCuenta.get();
                cuenta.setFecha(cuentaDTO.getFecha());
                cuenta.setPrecio(cuentaDTO.getPrecio());
                cuenta.setCasa(cuentaDTO.getCasa());
                cuenta.setConcepto(cuentaDTO.getConcepto());
                cuenta.setFrecuencia(cuentaDTO.getFrecuencia());
                cuentaRepository.save(cuenta);
                return;
            }
        }
        throw new CuentasException("No se encontró la cuenta.", HttpStatus.NOT_FOUND);
    }
}
