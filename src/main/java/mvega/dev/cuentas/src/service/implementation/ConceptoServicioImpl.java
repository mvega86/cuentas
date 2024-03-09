package mvega.dev.cuentas.src.service.implementation;

import mvega.dev.cuentas.exceptions.CuentasException;
import mvega.dev.cuentas.src.persistence.entity.Concepto;
import mvega.dev.cuentas.src.persistence.repository.IConceptoRepository;
import mvega.dev.cuentas.src.service.IConceptoServicio;
import mvega.dev.cuentas.src.service.mapper.AdConceptoDTOToConcepto;
import mvega.dev.cuentas.src.service.mapper.ConceptoToConceptoDTO;
import mvega.dev.cuentas.src.service.mapper.dto.AdConceptoDTO;
import mvega.dev.cuentas.src.service.mapper.dto.ConceptoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConceptoServicioImpl implements IConceptoServicio {
    @Autowired
    private IConceptoRepository conceptoRepository;
    @Autowired
    private ConceptoToConceptoDTO conceptoToConceptoDTO;
    @Autowired
    private AdConceptoDTOToConcepto adConceptoDTOToConcepto;
    @Override
    public void adicionar(AdConceptoDTO adConceptoDTO) {
        if(adConceptoDTO.getNombre().isBlank()){
            throw new CuentasException("El nombre no puede estar en blanco.", HttpStatus.BAD_REQUEST);
        }else {
            conceptoRepository.save(adConceptoDTOToConcepto.map(adConceptoDTO));
        }

    }

    @Override
    public List<ConceptoDTO> obtenerTodos() {
        return conceptoRepository.findAll()
                .stream()
                .map(concepto -> conceptoToConceptoDTO.map(concepto))
                .toList();
    }

    @Override
    public ConceptoDTO obtenerPorId(Long id) {
        Optional<Concepto> optionalConcepto = conceptoRepository.findById(id);
        if(optionalConcepto.isPresent()){
            return conceptoToConceptoDTO.map(optionalConcepto.get());
        }
        throw new CuentasException("No se encontró el concepto.", HttpStatus.NOT_FOUND);
    }

    @Override
    public ConceptoDTO obtenerPorNombre(String name) {
        Optional<Concepto> optionalConcepto = conceptoRepository.findByNombre(name);
        if(optionalConcepto.isPresent()){
            return conceptoToConceptoDTO.map(optionalConcepto.get());
        }
        throw new CuentasException("No se encontró el concepto.", HttpStatus.NOT_FOUND);
    }

    @Override
    public void eliminar(Long id) {
        Optional<Concepto> optionalConcepto = conceptoRepository.findById(id);
        if(optionalConcepto.isPresent()){
            conceptoRepository.deleteById(id);
            return;
        }
        throw new CuentasException("No se encontró el concepto.", HttpStatus.NOT_FOUND);
    }

    @Override
    public void actualizar(Long id, AdConceptoDTO adConceptoDTO) {
        Optional<Concepto> optionalConcepto = conceptoRepository.findById(id);
        if(optionalConcepto.isPresent()) {
            if (adConceptoDTO.getNombre().isBlank()) {
                throw new CuentasException("El nombre no puede estar en blanco.", HttpStatus.BAD_REQUEST);
            }
            else {
                Concepto concepto = optionalConcepto.get();
                concepto.setNombre(adConceptoDTO.getNombre());
                conceptoRepository.save(concepto);
            }
        }else {
            throw new CuentasException("No se encontró el concepto.", HttpStatus.NOT_FOUND);
        }

    }
}
