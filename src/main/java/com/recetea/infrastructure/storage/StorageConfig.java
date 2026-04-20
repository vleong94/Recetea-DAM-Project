package com.recetea.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class StorageConfig {

    private StorageConfig() {}

    public static Path getBasePath() {
        String env = System.getProperty("env", "local");
        String fileName = "application-" + env + ".properties";
        Properties props = new Properties();
        try (InputStream in = StorageConfig.class.getClassLoader().getResourceAsStream(fileName)) {
            if (in == null) throw new IllegalStateException("Archivo de configuración no encontrado: " + fileName);
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error al leer la configuración de almacenamiento.", e);
        }
        String rawPath = props.getProperty("storage.base-path");
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalStateException("La propiedad 'storage.base-path' no está configurada en " + fileName);
        }
        Path path = Path.of(rawPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("No se puede crear el directorio de almacenamiento: " + rawPath, e);
        }
        return path;
    }
}
