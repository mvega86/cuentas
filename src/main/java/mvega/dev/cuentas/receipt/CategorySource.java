package mvega.dev.cuentas.receipt;

/** Quien decidio la categoria de una linea (CLAUDE §5). */
public enum CategorySource {
    /** La asigno una regla de producto guardada. */
    RULE,
    /** La asigno el usuario a mano. */
    MANUAL,
    /** Todavia sin categoria. */
    UNASSIGNED
}
