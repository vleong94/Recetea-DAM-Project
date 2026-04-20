/* ==========================================================
 * MIGRACIÓN: recipe_media — Cloud-Ready Schema
 * Aplica el cambio sobre una instalación existente.
 * Ejecutar en recetea y recetea_test antes de mvn test.
 * ==========================================================
 */

-- 1. Renombrar la columna obsoleta
ALTER TABLE recipe_media RENAME COLUMN url TO storage_key;

-- 2. Ampliar la longitud por si ya era varchar menor
ALTER TABLE recipe_media ALTER COLUMN storage_key TYPE varchar(2048);

-- 3. Añadir las nuevas columnas de metadatos
--    DEFAULT temporales para que ALTER funcione con filas existentes
ALTER TABLE recipe_media
    ADD COLUMN storage_provider varchar(50)  NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN mime_type        varchar(100) NOT NULL DEFAULT 'application/octet-stream',
    ADD COLUMN size_bytes       bigint       NOT NULL DEFAULT 0 CHECK (size_bytes >= 0);

-- 4. Eliminar los DEFAULT (la capa de aplicación los provee siempre)
ALTER TABLE recipe_media ALTER COLUMN storage_provider DROP DEFAULT;
ALTER TABLE recipe_media ALTER COLUMN mime_type        DROP DEFAULT;
ALTER TABLE recipe_media ALTER COLUMN size_bytes       DROP DEFAULT;

-- 5. Actualizar comentarios de columna
COMMENT ON COLUMN recipe_media.storage_key      IS 'Clave o ruta interna del archivo (UUID filename o S3 key)';
COMMENT ON COLUMN recipe_media.storage_provider IS 'Proveedor: LOCAL, S3, etc.';
COMMENT ON COLUMN recipe_media.mime_type        IS 'Tipo MIME: image/jpeg, image/png, etc.';
COMMENT ON COLUMN recipe_media.size_bytes       IS 'Tamaño del archivo en bytes. Constraint: CHECK >= 0';
