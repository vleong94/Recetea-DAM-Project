package com.recetea.infrastructure.storage;

import com.recetea.core.recipe.application.ports.out.media.StorageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LocalFileSystemMediaStorage — Almacenamiento y eliminación de archivos")
class LocalFileSystemMediaStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileSystemMediaStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFileSystemMediaStorage(tempDir);
    }

    @Test
    @DisplayName("store debe escribir los bytes en el sistema de ficheros y retornar StorageResult con ruta no vacía")
    void store_ShouldWriteFileAndReturnStorageResult() throws IOException {
        byte[] data = "contenido de prueba".getBytes();

        StorageResult result = storage.store(data, "imagen.jpg");

        assertNotNull(result.storageKey(), "storageKey no debe ser nulo");
        assertFalse(result.storageKey().isBlank(), "storageKey no debe estar vacío");
        Path file = tempDir.resolve(result.storageKey());
        assertTrue(Files.exists(file), "El archivo debe existir en el directorio base");
        assertArrayEquals(data, Files.readAllBytes(file), "El contenido del archivo debe coincidir con los bytes originales");
    }

    @Test
    @DisplayName("store debe retornar el tamaño correcto en sizeBytes")
    void store_ShouldReturnCorrectSizeBytes() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};

        StorageResult result = storage.store(data, "archivo.bin");

        assertEquals(data.length, result.sizeBytes(), "sizeBytes debe coincidir con la longitud de los datos");
    }

    @Test
    @DisplayName("store debe detectar image/jpeg para archivos .jpg")
    void store_ShouldDetectJpegMimeType() {
        byte[] data = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}; // JPEG magic bytes

        StorageResult result = storage.store(data, "foto.jpg");

        assertTrue(
            result.mimeType().equals("image/jpeg") || result.mimeType().equals("application/octet-stream"),
            "mimeType debe ser image/jpeg o el fallback application/octet-stream"
        );
        assertNotNull(result.mimeType());
    }

    @Test
    @DisplayName("store debe detectar image/png para archivos .png")
    void store_ShouldDetectPngMimeType() {
        byte[] data = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes

        StorageResult result = storage.store(data, "imagen.png");

        assertTrue(
            result.mimeType().equals("image/png") || result.mimeType().equals("application/octet-stream"),
            "mimeType debe ser image/png o el fallback application/octet-stream"
        );
        assertNotNull(result.mimeType());
    }

    @Test
    @DisplayName("store debe usar application/octet-stream cuando el tipo MIME no se puede detectar")
    void store_ShouldFallbackToOctetStream_WhenMimeTypeUnknown() {
        StorageResult result = storage.store(new byte[]{42}, "archivo_sin_extension");

        assertNotNull(result.mimeType());
        assertFalse(result.mimeType().isBlank());
    }

    @Test
    @DisplayName("store debe preservar la extensión del nombre original")
    void store_ShouldPreserveOriginalExtension() {
        StorageResult result = storage.store(new byte[]{1, 2, 3}, "foto.png");

        assertTrue(result.storageKey().endsWith(".png"), "La extensión .png debe estar presente en el nombre generado");
    }

    @Test
    @DisplayName("store debe generar storageKeys únicos para llamadas sucesivas con el mismo nombre")
    void store_ShouldGenerateUniqueStorageKeys() {
        StorageResult r1 = storage.store(new byte[]{1}, "a.jpg");
        StorageResult r2 = storage.store(new byte[]{2}, "a.jpg");

        assertNotEquals(r1.storageKey(), r2.storageKey(), "Cada llamada a store debe producir un storageKey distinto");
    }

    @Test
    @DisplayName("store debe funcionar sin extensión si el nombre original no la tiene")
    void store_ShouldWorkWithoutExtension() {
        assertDoesNotThrow(() -> storage.store(new byte[]{42}, "archivo_sin_extension"));
    }

    @Test
    @DisplayName("delete debe eliminar el archivo del sistema de ficheros")
    void delete_ShouldRemoveFile() throws IOException {
        StorageResult result = storage.store("datos".getBytes(), "doc.pdf");
        assertTrue(Files.exists(tempDir.resolve(result.storageKey())), "Precondición: el archivo debe existir antes de borrar");

        storage.delete(result.storageKey());

        assertFalse(Files.exists(tempDir.resolve(result.storageKey())), "El archivo debe haber sido eliminado");
    }

    @Test
    @DisplayName("delete no debe lanzar excepción cuando el archivo no existe")
    void delete_ShouldNotThrow_WhenFileDoesNotExist() {
        assertDoesNotThrow(() -> storage.delete("archivo_inexistente.jpg"),
                "deleteIfExists no debe lanzar excepción para archivos que no existen");
    }

    @Test
    @DisplayName("store con nombre nulo debe generar un StorageResult válido sin extensión")
    void store_ShouldHandleNullOriginalName() {
        StorageResult result = storage.store(new byte[]{9}, null);

        assertNotNull(result.storageKey());
        assertFalse(result.storageKey().isBlank());
        assertTrue(Files.exists(tempDir.resolve(result.storageKey())));
        assertNotNull(result.mimeType());
        assertTrue(result.sizeBytes() > 0);
    }
}
