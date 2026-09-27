package mvega.dev.cuentas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita el sondeo periódico de la carpeta de entrada (CLAUDE §7). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
