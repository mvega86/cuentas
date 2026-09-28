-- Nuevo estado UNSUPPORTED para los documentos que se pudieron leer pero no son tickets de
-- un comercio soportado.
--
-- Se separa de ERROR a propósito: ERROR significa que algo falló procesando un documento que
-- se esperaba procesable, mientras que UNSUPPORTED significa que el documento está bien pero
-- no es de los que esta aplicación sabe interpretar. Mezclarlos hacía que un PDF ajeno
-- apareciera en la interfaz como si la importación estuviera rota.

alter table import_record
    drop constraint if exists ck_import_status;

alter table import_record
    add constraint ck_import_status
        check (processing_status in
               ('PENDING', 'PROCESSING', 'PROCESSED', 'DUPLICATE', 'UNSUPPORTED', 'ERROR'));
