-- La importación pasa a leer PDFs de una carpeta local de entrada, y la integración
-- directa con Gmail sale del diseño.
--
-- Un Apps Script externo filtra la etiqueta CUENTAS_MERCADONA y deja los adjuntos en
-- Drive; esa carpeta se sincroniza en local y Cuentas solo ve ficheros. El backend nunca
-- habla con Gmail, así que no hay identificadores de mensaje ni de adjunto que guardar.
--
-- Se comprobó antes de escribir esta migración que las dos columnas estaban
-- completamente vacías (0 de 8 filas con valor) y que ninguna fila tenía
-- source_type = 'GMAIL', de modo que no se pierde ningún dato.

-- El antiduplicado se queda en dos capas, que siguen bastando para ficheros:
-- el hash SHA-256 del contenido y el número de factura por comercio.
drop index if exists uq_receipt_gmail_attachment;

alter table receipt
    drop column if exists gmail_message_id,
    drop column if exists gmail_attachment_id;

-- FOLDER sustituye a GMAIL como origen automático.
alter table receipt
    drop constraint if exists ck_receipt_source_type;

alter table receipt
    add constraint ck_receipt_source_type
        check (source_type in ('FOLDER', 'UPLOAD', 'SAMPLE'));
