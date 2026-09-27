package mvega.dev.cuentas.receipt.importer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mvega.dev.cuentas.receipt.Receipt;

/**
 * Constancia de un intento de importar un PDF.
 *
 * <p>Queda registrado pase lo que pase: si se procesó, si era duplicado o si falló y por
 * qué. Es lo que alimenta el historial de la pantalla de importaciones, y lo que permite
 * abrir un error para leer el motivo.
 *
 * <p>Sin cascada hacia el ticket: borrar un ticket no debe borrar su rastro de importación,
 * solo dejar la referencia a null.
 */
@Entity
@Table(name = "import_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImportSource source;

    @Column(name = "original_filename", nullable = false, length = 260)
    private String originalFilename;

    /** SHA-256 del PDF. Único entre los que llegaron a PROCESSED. */
    @Column(nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 16)
    private ImportStatus processingStatus;

    /** Motivo del fallo, en lenguaje claro, para poder consultarlo desde la interfaz. */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    private Receipt receipt;

    /** Dónde acabó el fichero: la carpeta de procesados o la de error. */
    @Column(name = "stored_path", length = 1024)
    private String storedPath;

    /** Cuándo se empezó a procesar el fichero. */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /**
     * Cuándo se terminó. Nulo mientras está en marcha: un registro en PROCESSING sin esta
     * marca es una importación que se quedó a medias.
     */
    @Column(name = "finished_at")
    private Instant finishedAt;
}
