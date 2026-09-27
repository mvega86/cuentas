package mvega.dev.cuentas.receipt;

/** Resultado del parseo de un ticket (CLAUDE §5, §6). */
public enum ParsingStatus {
    /** Todo el ticket se entendio y los importes cuadran. */
    PARSED,
    /**
     * El ticket se guardo, pero algo no encaja: una linea no reconocida o la suma
     * de lineas no coincide con el total. Necesita repaso manual.
     */
    REVIEW,
    /** No se pudo extraer lo minimo imprescindible del ticket. */
    ERROR
}
