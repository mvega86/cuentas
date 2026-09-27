package mvega.dev.cuentas;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base de los tests de integracion: un PostgreSQL 18 real en Testcontainers, la misma
 * version que usa el docker-compose local (CLAUDE §12, §15).
 *
 * <p>Se prueba contra PostgreSQL de verdad y no contra una base en memoria porque el
 * esquema lo gobierna Flyway y hay indices parciales que otras bases no reproducen.
 *
 * <p>El contenedor se arranca a mano en un bloque estatico y <strong>no</strong> se
 * anota con {@code @Testcontainers}/{@code @Container} a proposito. Con la anotacion,
 * JUnit para el contenedor al terminar la primera clase de test y lo vuelve a levantar
 * con otro puerto aleatorio para la siguiente; como Spring reutiliza el contexto en
 * cache, el DataSource seguia apuntando al puerto viejo y todo fallaba con
 * "Connection refused". Arrancandolo una sola vez por JVM, el contexto cacheado sigue
 * siendo valido. De retirarlo al final se encarga Ryuk.
 */
@SpringBootTest
public abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
