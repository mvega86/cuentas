package mvega.dev.cuentas.src.persistence.repository;

import mvega.dev.cuentas.src.persistence.entity.Casa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICasaRepository extends JpaRepository<Casa, Long> {
}
