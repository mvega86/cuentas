package mvega.dev.cuentas.receipt.importer;

/** Estado de un intento de importación. La interfaz los muestra tal cual. */
public enum ImportStatus {
    /** Detectado y registrado, todavía sin procesar. */
    PENDING,
    /** Se está leyendo y parseando ahora mismo. */
    PROCESSING,
    /** Importado y persistido correctamente. */
    PROCESSED,
    /** El mismo PDF ya estaba importado. No genera un gasto nuevo. */
    DUPLICATE,
    /**
     * El PDF se pudo leer, pero no es un ticket de un comercio soportado.
     *
     * <p>Se distingue de {@link #ERROR} a propósito: aquí no hay nada roto, simplemente el
     * documento no es de los que esta aplicación sabe interpretar. Nunca genera gasto.
     */
    UNSUPPORTED,
    /**
     * Se esperaba un ticket procesable y no se pudo procesar: el fichero no era un PDF
     * legible, o era del comercio correcto pero faltaban los datos mínimos. El motivo queda
     * en {@code errorMessage}.
     */
    ERROR
}
