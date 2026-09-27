package mvega.dev.cuentas.receipt.importer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import mvega.dev.cuentas.receipt.importer.dto.ImportRecordDto;
import mvega.dev.cuentas.receipt.importer.dto.ImportRunDto;
import mvega.dev.cuentas.receipt.importer.dto.ImportStatusDto;
import mvega.dev.cuentas.shared.web.PageDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * API de importación (CLAUDE §8).
 *
 * <p>Las dos vías, carpeta automática y subida manual, acaban en el mismo
 * {@link MercadonaImportService}. No hay dos implementaciones.
 */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final MercadonaImportService importService;
    private final MercadonaFolderScanner scanner;
    private final ImportQueryService queryService;

    ImportController(MercadonaImportService importService, MercadonaFolderScanner scanner,
            ImportQueryService queryService) {
        this.importService = importService;
        this.scanner = scanner;
        this.queryService = queryService;
    }

    /** Historial de importaciones, lo más reciente primero. */
    @GetMapping
    public PageDto<ImportRecordDto> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queryService.history(page, size);
    }

    /** Detalle de una importación, incluido el motivo si acabó en error. */
    @GetMapping("/{id}")
    public ImportRecordDto detail(@PathVariable Long id) {
        return queryService.detail(id);
    }

    /** Estado de la carpeta automática: si está activa, última comprobación y contadores. */
    @GetMapping("/mercadona/status")
    public ImportStatusDto status() {
        return queryService.status();
    }

    /**
     * Fuerza una búsqueda de tickets nuevos en la carpeta de entrada.
     *
     * <p>El sondeo periódico hace lo mismo cada pocos segundos; esto sirve para no esperar,
     * tanto en desarrollo como desde el botón de la interfaz.
     */
    @PostMapping("/mercadona/scan")
    public ImportRunDto scan() {
        return ImportRunDto.from(scanner.scanNow());
    }

    /** Sube uno o varios PDFs a mano. Pasan por el mismo pipeline que la carpeta. */
    @PostMapping("/mercadona/upload")
    public ImportRunDto upload(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún fichero");
        }
        List<ImportRecord> records = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException(
                        "El fichero %s está vacío".formatted(file.getOriginalFilename()));
            }
            try {
                records.add(importService.importUpload(file.getBytes(),
                        file.getOriginalFilename()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return ImportRunDto.from(records);
    }
}
