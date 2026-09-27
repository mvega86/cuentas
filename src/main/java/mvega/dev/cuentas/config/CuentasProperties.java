package mvega.dev.cuentas.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion propia de la aplicacion, toda sobrescribible por entorno (CLAUDE §13). */
@ConfigurationProperties(prefix = "cuentas")
public record CuentasProperties(Cors cors, Imports imports) {

    public record Cors(List<String> allowedOrigins) {}

    /**
     * @param mercadona carpetas de importacion de Mercadona
     * @param scanDelayMs cada cuanto se revisa la carpeta de entrada, en milisegundos
     */
    public record Imports(Mercadona mercadona, long scanDelayMs) {}

    /**
     * Las tres carpetas del pipeline, cada una configurable por separado.
     *
     * <p>Van sueltas y no derivadas de la de entrada porque el montaje puede estar
     * organizado de cualquier manera: una carpeta de rclone, un directorio local, un NAS.
     * Cuentas no acopla nada a Google Drive ni a rclone; solo conoce rutas del sistema de
     * ficheros.
     *
     * <p>Los valores por defecto son relativos al backend, para que el proyecto arranque sin
     * configurar nada. Ninguna ruta personal se codifica en el repositorio.
     *
     * @param directory de donde se leen los PDF pendientes
     * @param processedDirectory donde van los que se importaron bien o ya estaban
     * @param errorDirectory donde van los que no se pudieron procesar
     */
    public record Mercadona(String directory, String processedDirectory, String errorDirectory) {}
}
