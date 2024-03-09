package mvega.dev.cuentas.src.persistence.repository;

import mvega.dev.cuentas.src.persistence.entity.Detalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDetalleRepository extends JpaRepository<Detalle, Long> {
}
