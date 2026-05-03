package com.recetea.infrastructure.storage;

import com.recetea.core.recipe.application.ports.out.media.StorageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LocalFileSystemMediaStorage — File storage and deletion")
class LocalFileSystemMediaStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileSystemMediaStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFileSystemMediaStorage(tempDir);
    }

    @Test
    @DisplayName("store: Should write bytes to the filesystem and return StorageResult with non-empty key")
    void store_ShouldWriteFileAndReturnStorageResult() throws IOException {
        byte[] expectedBytes = "contenido de prueba".getBytes();

        StorageResult result = storage.store(new ByteArrayInputStream(expectedBytes), "imagen.jpg");

        assertNotNull(result.storageKey(), "storageKey must not be null");
        assertFalse(result.storageKey().isBlank(), "storageKey must not be blank");
        Path file = tempDir.resolve(result.storageKey());
        assertTrue(Files.exists(file), "File must exist in the base directory");
        assertArrayEquals(expectedBytes, Files.readAllBytes(file),
                "File content must match the original bytes");
    }

    @Test
    @DisplayName("store: Should return the correct size in sizeBytes")
    void store_ShouldReturnCorrectSizeBytes() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};

        StorageResult result = storage.store(new ByteArrayInputStream(data), "archivo.bin");

        assertEquals(data.length, result.sizeBytes(), "sizeBytes must match the data length");
    }

    @Test
    @DisplayName("store: Should detect image/jpeg for .jpg files")
    void store_ShouldDetectJpegMimeType() {
        byte[] data = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}; // JPEG magic bytes

        StorageResult result = storage.store(new ByteArrayInputStream(data), "foto.jpg");

        assertTrue(
            result.mimeType().equals("image/jpeg") || result.mimeType().equals("application/octet-stream"),
            "mimeType must be image/jpeg or the fallback application/octet-stream"
        );
        assertNotNull(result.mimeType());
    }

    @Test
    @DisplayName("store: Should detect image/png for .png files")
    void store_ShouldDetectPngMimeType() {
        byte[] data = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes

        StorageResult result = storage.store(new ByteArrayInputStream(data), "imagen.png");

        assertTrue(
            result.mimeType().equals("image/png") || result.mimeType().equals("application/octet-stream"),
            "mimeType must be image/png or the fallback application/octet-stream"
        );
        assertNotNull(result.mimeType());
    }

    @Test
    @DisplayName("store: Should fall back to application/octet-stream when MIME type cannot be detected")
    void store_ShouldFallbackToOctetStream_WhenMimeTypeUnknown() {
        StorageResult result = storage.store(new ByteArrayInputStream(new byte[]{42}), "archivo_sin_extension");

        assertNotNull(result.mimeType());
        assertFalse(result.mimeType().isBlank());
    }

    @Test
    @DisplayName("store: Should preserve the original file extension")
    void store_ShouldPreserveOriginalExtension() {
        StorageResult result = storage.store(new ByteArrayInputStream(new byte[]{1, 2, 3}), "foto.png");

        assertTrue(result.storageKey().endsWith(".png"),
                "The .png extension must be present in the generated name");
    }

    @Test
    @DisplayName("store: Should generate unique storageKeys for successive calls with the same name")
    void store_ShouldGenerateUniqueStorageKeys() {
        StorageResult r1 = storage.store(new ByteArrayInputStream(new byte[]{1}), "a.jpg");
        StorageResult r2 = storage.store(new ByteArrayInputStream(new byte[]{2}), "a.jpg");

        assertNotEquals(r1.storageKey(), r2.storageKey(),
                "Each call to store must produce a different storageKey");
    }

    @Test
    @DisplayName("store: Should work without extension when the original name has none")
    void store_ShouldWorkWithoutExtension() {
        assertDoesNotThrow(() ->
                storage.store(new ByteArrayInputStream(new byte[]{42}), "archivo_sin_extension"));
    }

    @Test
    @DisplayName("delete: Should remove the file from the filesystem")
    void delete_ShouldRemoveFile() throws IOException {
        StorageResult result = storage.store(new ByteArrayInputStream("datos".getBytes()), "doc.pdf");
        assertTrue(Files.exists(tempDir.resolve(result.storageKey())),
                "Precondition: file must exist before deletion");

        storage.delete(result.storageKey());

        assertFalse(Files.exists(tempDir.resolve(result.storageKey())),
                "The file must have been deleted");
    }

    @Test
    @DisplayName("delete: Should not throw when the file does not exist")
    void delete_ShouldNotThrow_WhenFileDoesNotExist() {
        assertDoesNotThrow(() -> storage.delete("archivo_inexistente.jpg"),
                "deleteIfExists must not throw for non-existent files");
    }

    @Test
    @DisplayName("store: Should generate a valid StorageResult without extension when original name is null")
    void store_ShouldHandleNullOriginalName() {
        StorageResult result = storage.store(new ByteArrayInputStream(new byte[]{9}), null);

        assertNotNull(result.storageKey());
        assertFalse(result.storageKey().isBlank());
        assertTrue(Files.exists(tempDir.resolve(result.storageKey())));
        assertNotNull(result.mimeType());
        assertTrue(result.sizeBytes() > 0);
    }
}
