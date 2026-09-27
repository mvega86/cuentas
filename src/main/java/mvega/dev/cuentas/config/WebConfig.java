package mvega.dev.cuentas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS en un unico sitio, en lugar de un {@code @CrossOrigin} repetido por
 * controlador como en el codigo anterior (CLAUDE §2).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CuentasProperties properties;

    WebConfig(CuentasProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
