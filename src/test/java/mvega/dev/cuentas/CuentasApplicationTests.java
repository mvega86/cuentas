package mvega.dev.cuentas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El contexto arranca de punta a punta: Flyway migra, Hibernate valida el esquema contra
 * las entidades y los beans se resuelven.
 *
 * <p>Antes este test exigia un PostgreSQL arrancado a mano en localhost:5432 y fallaba en
 * cualquier maquina limpia. Ahora se trae el suyo.
 */
class CuentasApplicationTests extends PostgresIntegrationTest {

	@Test
	@DisplayName("el contexto de la aplicacion arranca sobre un PostgreSQL real")
	void contextLoads() {
	}

}
