package mvega.dev.cuentas.receipt;

/** Canal por el que entró el fichero del ticket. */
public enum SourceType {
    /**
     * Apareció en la carpeta de entrada.
     *
     * <p>Ahí lo deja una automatización externa. Cuentas solo ve ficheros: no habla con
     * Gmail ni con Drive.
     */
    FOLDER,
    /** El usuario lo subió a mano desde la aplicación. */
    UPLOAD
}
