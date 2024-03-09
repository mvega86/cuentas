package mvega.dev.cuentas.src.persistence.repository;

import mvega.dev.cuentas.src.persistence.entity.Concepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface IConceptoRepository extends JpaRepository<Concepto, Long> {
    public Optional<Concepto> findByNombre(String nombre);
}
