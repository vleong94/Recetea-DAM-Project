package com.recetea.infrastructure.ui.javafx.shared.error;

import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.sql.SQLException;

public class GlobalExceptionHandler {

    private GlobalExceptionHandler() {}

    public static void register() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Throwable cause = unwrap(throwable);
            Platform.runLater(() -> showAlert(cause));
        });
    }

    private static Throwable unwrap(Throwable t) {
        // Unwrap one level of generic RuntimeException wrapper (e.g. from JavaFX event dispatch)
        if (t instanceof RuntimeException && t.getCause() != null
                && t.getClass() == RuntimeException.class) {
            return t.getCause();
        }
        return t;
    }

    private static void showAlert(Throwable cause) {
        if (isDomainException(cause)) {
            show(Alert.AlertType.WARNING, "Operación no permitida", null, cause.getMessage());
        } else if (cause instanceof InfrastructureException || cause instanceof SQLException) {
            show(Alert.AlertType.ERROR, "Error técnico",
                    "Fallo de infraestructura",
                    "Ha ocurrido un problema técnico. Inténtalo de nuevo o contacta con soporte.");
        } else {
            System.err.println("Error inesperado en hilo [" + Thread.currentThread().getName() + "]:");
            cause.printStackTrace(System.err);
            show(Alert.AlertType.ERROR, "Error inesperado",
                    "Error inesperado",
                    "Ha ocurrido un error inesperado. Consulta la consola para más detalles.");
        }
    }

    private static boolean isDomainException(Throwable t) {
        String pkg = t.getClass().getPackageName();
        return pkg.startsWith("com.recetea.core.");
    }

    private static void show(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
