-- Se separa cuándo empezó y cuándo terminó cada importación.
--
-- Antes solo había `imported_at`, que no distinguía el inicio del fin. Con las dos marcas se
-- puede ver cuánto tardó una importación y, sobre todo, detectar una que se quedó a medias:
-- un registro con `started_at` y sin `finished_at` en estado PROCESSING es un proceso que
-- murió mientras trabajaba.

alter table import_record
    rename column imported_at to started_at;

alter table import_record
    add column finished_at timestamptz;

-- Las importaciones que ya estaban cerradas se consideran terminadas en el mismo instante:
-- no se guardó el dato en su momento y no se puede inventar uno distinto.
update import_record
set finished_at = started_at
where processing_status in ('PROCESSED', 'DUPLICATE', 'ERROR');

drop index if exists ix_import_record_imported_at;
create index ix_import_record_started_at on import_record (started_at desc);
