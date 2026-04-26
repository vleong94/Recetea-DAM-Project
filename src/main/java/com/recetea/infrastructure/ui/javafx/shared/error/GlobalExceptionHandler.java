package com.recetea.infrastructure.ui.javafx.shared.error;

import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.InvalidRecipeDataException;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.infrastructure.interop.xml.XmlInteropAdapter;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private record ErrorCategory(String code, Alert.AlertType alertType) {
        boolean is500() { return code.endsWith("-500"); }
    }

    private GlobalExceptionHandler() {}

    public static void register() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Throwable cause = unwrap(throwable);
            Platform.runLater(() -> handle(cause));
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

    private static void handle(Throwable cause) {
        ErrorCategory category = categorize(cause);
        if (category.is500()) {
            log.error("Unhandled {} in thread [{}]", category.code(),
                    Thread.currentThread().getName(), cause);
        }
        showAlert(cause, category);
    }

    /**
     * Maps each exception type to an error code and alert severity.
     * Ordering is significant: specific subtypes must appear before isDomainException.
     */
    private static ErrorCategory categorize(Throwable cause) {
        if (cause instanceof InvalidRecipeDataException)        return new ErrorCategory("VAL-400", Alert.AlertType.WARNING);
        if (cause instanceof AuthenticationRequiredException)   return new ErrorCategory("SEC-401", Alert.AlertType.WARNING);
        if (cause instanceof UnauthorizedRecipeAccessException) return new ErrorCategory("SEC-403", Alert.AlertType.WARNING);
        if (cause instanceof XmlInteropAdapter.XmlInteropException) return new ErrorCategory("INT-400", Alert.AlertType.WARNING);
        if (isDomainException(cause))                           return new ErrorCategory("BIZ-400", Alert.AlertType.WARNING);
        if (cause instanceof InfrastructureException)           return new ErrorCategory("INF-500", Alert.AlertType.ERROR);
        if (cause instanceof SQLException)                      return new ErrorCategory("DB-500",  Alert.AlertType.ERROR);
        return new ErrorCategory("ERR-500", Alert.AlertType.ERROR);
    }

    private static void showAlert(Throwable cause, ErrorCategory category) {
        switch (category.code()) {
            case "VAL-400" -> {
                InvalidRecipeDataException ivde = (InvalidRecipeDataException) cause;
                show(category.alertType(),
                        I18n.get("error.val400.title"),
                        I18n.format("error.val400.header", ivde.getErrors().size()),
                        String.join("\n", ivde.getErrors()));
            }
            case "SEC-401" -> show(category.alertType(),
                    I18n.get("error.sec401.title"),
                    I18n.get("error.sec401.header"),
                    I18n.get("error.sec401.content"));
            case "SEC-403" -> show(category.alertType(),
                    I18n.get("error.sec403.title"),
                    I18n.get("error.sec403.header"),
                    I18n.get("error.sec403.content"));
            case "INT-400" -> show(category.alertType(),
                    I18n.get("error.int400.title"),
                    I18n.get("error.int400.header"),
                    cause.getMessage());
            case "BIZ-400" -> show(category.alertType(),
                    I18n.get("error.biz400.title"),
                    I18n.get("error.biz400.header"),
                    cause.getMessage());
            case "INF-500" -> show(category.alertType(),
                    I18n.get("error.inf500.title"),
                    I18n.get("error.inf500.header"),
                    I18n.get("error.inf500.content"));
            case "DB-500" -> show(category.alertType(),
                    I18n.get("error.db500.title"),
                    I18n.get("error.db500.header"),
                    I18n.get("error.db500.content"));
            default -> show(category.alertType(),
                    I18n.get("error.err500.title"),
                    I18n.get("error.err500.header"),
                    I18n.get("error.err500.content"));
        }
    }

    private static boolean isDomainException(Throwable t) {
        return t.getClass().getPackageName().startsWith("com.recetea.core.");
    }

    private static void show(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
