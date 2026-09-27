package mvega.dev.cuentas.receipt.importer.dto;

import java.time.Instant;
import mvega.dev.cuentas.receipt.importer.ImportFolders;

/**
 * Estado de la importación automática, para la cabecera de la pantalla (CLAUDE §9).
 *
 * @param folder estado de la carpeta de entrada
 * @param lastCheckedAt última vez que se revisó la carpeta, o null si aún no se revisó
 * @param lastImportedAt fecha del último ticket importado correctamente
 * @param processedCount cuántas importaciones acabaron bien
 * @param errorCount cuántas acabaron en error
 */
public record ImportStatusDto(
        ImportFolders.FolderState folder,
        Instant lastCheckedAt,
        Instant lastImportedAt,
        long processedCount,
        long errorCount) {}
