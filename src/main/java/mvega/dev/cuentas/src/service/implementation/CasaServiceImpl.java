package mvega.dev.cuentas.src.service.implementation;

import mvega.dev.cuentas.exceptions.CuentasException;
import mvega.dev.cuentas.src.persistence.entity.Casa;
import mvega.dev.cuentas.src.persistence.repository.ICasaRepository;
import mvega.dev.cuentas.src.service.ICasaService;
import mvega.dev.cuentas.src.service.mapper.AdCasaDTOToCasa;
import mvega.dev.cuentas.src.service.mapper.CasaToCasaDTO;
import mvega.dev.cuentas.src.service.mapper.dto.AdCasaDTO;
import mvega.dev.cuentas.src.service.mapper.dto.CasaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CasaServiceImpl implements ICasaService {
    @Autowired
    private ICasaRepository casaRepository;
    @Autowired
    private AdCasaDTOToCasa adCasaDTOToCasa;
    @Autowired
    private CasaToCasaDTO casaToCasaDTO;
    @Override
    public void adicionar(AdCasaDTO casaDTO) {
        if(casaDTO.getCalle().isBlank() ||
                casaDTO.getNumero().isBlank() ||
                casaDTO.getCiudad().isBlank() ||
                casaDTO.getProvincia().isBlank()){
            throw new CuentasException("La calle, el número, la ciudad y la provincia no pueden estar en blanco", HttpStatus.BAD_REQUEST);
        }else {
            casaRepository.save(adCasaDTOToCasa.map(casaDTO));
        }
    }

    @Override
    public List<CasaDTO> obtenerTodos() {
        return casaRepository.findAll()
                .stream()
                .map(casa -> casaToCasaDTO.map(casa))
                .toList();
    }

    @Override
    public CasaDTO obtenerPorId(Long id) {
        Optional<Casa> optionalCasa = casaRepository.findById(id);
        if(optionalCasa.isPresent()){
            return casaToCasaDTO.map(optionalCasa.get());
        }
        throw new CuentasException("No se encontró la casa.", HttpStatus.NOT_FOUND);
    }

    @Override
    public void eliminar(Long id) {
        Optional<Casa> optionalCasa = casaRepository.findById(id);
        if(optionalCasa.isPresent()){
            casaRepository.deleteById(id);
        }else {
            throw new CuentasException("No se encontró la casa.", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public void actualizar(Long id, AdCasaDTO casaDTO) {
        Optional<Casa> optionalCasa = casaRepository.findById(id);
        if(optionalCasa.isPresent()){
            if(casaDTO.getCalle().isBlank() ||
                    casaDTO.getNumero().isBlank() ||
                    casaDTO.getCiudad().isBlank() ||
                    casaDTO.getProvincia().isBlank()){
                throw new CuentasException("La calle, el número, la ciudad y la provincia no pueden estar en blanco", HttpStatus.BAD_REQUEST);
            }else {
                Casa casa = optionalCasa.get();
                casa.setCalle(casaDTO.getCalle());
                casa.setNumero(casaDTO.getNumero());
                casa.setEscalera(casaDTO.getEscalera());
                casa.setPiso(casaDTO.getPiso());
                casa.setLetra(casaDTO.getLetra());
                casa.setCiudad(casaDTO.getCiudad());
                casa.setProvincia(casaDTO.getProvincia());
                casaRepository.save(casa);
            }
        }else {
            throw new CuentasException("No se encontró la casa.", HttpStatus.NOT_FOUND);
        }
    }
}
