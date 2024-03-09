package mvega.dev.cuentas.src.persistence.repository;

import mvega.dev.cuentas.src.persistence.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICuentaRepository extends JpaRepository<Cuenta, Long> {
}
