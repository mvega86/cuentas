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
    /** No se pudo procesar. El motivo queda en {@code errorMessage}. */
    ERROR
}
