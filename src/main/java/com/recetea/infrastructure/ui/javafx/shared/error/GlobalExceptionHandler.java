package com.recetea.infrastructure.ui.javafx.shared.error;

import com.recetea.core.shared.domain.DomainException;
import com.recetea.core.shared.domain.ErrorCode;
import com.recetea.core.shared.domain.TechnicalException;
import com.recetea.core.shared.domain.utils.ExecutionContext;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.MissingResourceException;

/**
 * Single-entry uncaught-exception handler for the JavaFX runtime.
 * Registered globally via {@link Thread#setDefaultUncaughtExceptionHandler}
 * at startup ({@code Main.start()}); every unhandled throwable from any
 * thread funnels through {@link #categorize(Throwable)} for severity
 * mapping and {@link #showAlert} for UX.
 *
 * <p><b>Architectural cap.</b> The handler depends only on the
 * {@code core.shared.domain} abstractions ({@link DomainException},
 * {@link TechnicalException}) — never on concrete adapters
 * ({@code InfrastructureException}, {@code XmlInteropException}). Adding
 * a new technical-error category is therefore a one-line constructor
 * call at the adapter site plus matching {@code messages.properties}
 * entries; no edit to this class is needed.
 *
 * <p><b>Controllers must not catch exceptions to render alerts.</b> Let
 * them propagate — the handler observes them via the default-uncaught
 * hook and produces the localised dialog.
 *
 * <p><b>ES — </b>Handler de excepciones no capturadas, único punto
 * de entrada para el runtime de JavaFX. Registrado globalmente vía
 * {@link Thread#setDefaultUncaughtExceptionHandler} en el arranque
 * ({@code Main.start()}); todo throwable no manejado de cualquier
 * hilo pasa por {@link #categorize(Throwable)} para el mapeo de
 * severidad y por {@link #showAlert} para la UX.
 *
 * <p><b>Tope arquitectónico.</b> El handler depende sólo de las
 * abstracciones de {@code core.shared.domain}
 * ({@link DomainException}, {@link TechnicalException}) — nunca de
 * adaptadores concretos ({@code InfrastructureException},
 * {@code XmlInteropException}). Añadir una nueva categoría de
 * error técnico es por tanto una sola llamada a constructor en el
 * sitio del adaptador más las entradas correspondientes en
 * {@code messages.properties}; no hace falta editar esta clase.
 *
 * <p><b>Los controladores no deben capturar excepciones para
 * renderizar alertas.</b> Dejen que se propaguen — el handler las
 * observa vía el hook default-uncaught y produce el diálogo
 * localizado.
 */
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Package-private so {@code GlobalExceptionHandlerTest} can drive {@link #categorize}
     * directly without spinning up the JavaFX toolkit.
     */
    record ErrorCategory(String code, Alert.AlertType alertType) {
        boolean isFatal() { return alertType == Alert.AlertType.ERROR; }
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
        if (category.isFatal()) {
            // Pull the per-flow context that ExecutionContext bound at the wrapper boundary.
            // The Throwable is unwinding up the stack — the original ScopedValue scope may
            // already have closed by the time this handler runs (Platform.runLater hops to
            // the FX thread). currentCorrelationId/currentUserId tolerate that case and
            // simply return empty when no scope is bound, in which case we render NONE.
            String trace = ExecutionContext.currentCorrelationId().orElse("NONE");
            String user  = ExecutionContext.currentUserId().orElse("anonymous");
            log.error("Unhandled {} in thread [{}] [TraceID: {}] [User: {}]",
                    category.code(), Thread.currentThread().getName(), trace, user, cause);
        }
        showAlert(cause, category);
    }

    /**
     * Maps each exception to an error category. Domain exceptions dispatch on their
     * declared {@link ErrorCode}; technical exceptions dispatch on their stable
     * {@link TechnicalException#technicalCode()} — the handler no longer references
     * concrete infrastructure types ({@code InfrastructureException},
     * {@code XmlInteropException}), only the abstractions in {@code core.shared.domain}.
     *
     * <p>Implemented as a Java 24 pattern-matching switch: each case binds the
     * thrown subtype directly into the lambda body, so there's no nested
     * {@code instanceof} chain. {@code case null, default} folds the
     * defensive-null path into the catch-all so a stray null Throwable surfaces as
     * a generic {@code ERR-500} rather than throwing NPE inside the handler.
     * Package-private for tests.
     */
    static ErrorCategory categorize(Throwable cause) {
        return switch (cause) {
            case DomainException de    -> forDomainCode(de.errorCode());
            case TechnicalException te -> forTechnicalCode(te.technicalCode());
            case SQLException _        -> new ErrorCategory("DB-500",  Alert.AlertType.ERROR);
            case null, default         -> new ErrorCategory("ERR-500", Alert.AlertType.ERROR);
        };
    }

    /** Severity assignment for each domain {@link ErrorCode}. INTERNAL_ERROR escalates to ERROR; rest are WARNING. */
    private static ErrorCategory forDomainCode(ErrorCode code) {
        Alert.AlertType type = (code == ErrorCode.INTERNAL_ERROR) ? Alert.AlertType.ERROR : Alert.AlertType.WARNING;
        return new ErrorCategory(code.name(), type);
    }

    /**
     * Severity for technical codes follows the HTTP-status convention encoded in the
     * code itself: a {@code -4xx} suffix surfaces a user-recoverable issue (input
     * malformed, schema violation) → WARNING; anything else (including unrecognised
     * code shapes) is treated as a fatal infrastructure failure → ERROR. Failing
     * closed on unknown codes prevents a misnamed adapter code from silently
     * downgrading severity.
     */
    private static ErrorCategory forTechnicalCode(String code) {
        int dash = code.lastIndexOf('-');
        boolean isClientFacing = dash >= 0
                && code.length() > dash + 1
                && code.charAt(dash + 1) == '4';
        Alert.AlertType type = isClientFacing ? Alert.AlertType.WARNING : Alert.AlertType.ERROR;
        return new ErrorCategory(code, type);
    }

    /**
     * Generic alert renderer. The category code drives an i18n lookup of
     * {@code error.{code}.title}, {@code .header}, {@code .content}. Two special cases:
     * <ul>
     *   <li>VALIDATION_ERROR — uses the polymorphic {@link DomainException#errors()}
     *       to render either a single message or an accumulated multi-error list.</li>
     *   <li>INT-400 (XmlInteropException) — content falls through to {@code cause.getMessage()}
     *       to surface the JAXB/XSD detail.</li>
     * </ul>
     */
    private static void showAlert(Throwable cause, ErrorCategory category) {
        try {
            String code = category.code();
            Alert.AlertType type = category.alertType();
            String titleKey   = "error." + code + ".title";
            String headerKey  = "error." + code + ".header";
            String contentKey = "error." + code + ".content";

            if (ErrorCode.VALIDATION_ERROR.name().equals(code) && cause instanceof DomainException de) {
                List<String> errors = de.errors();
                show(type,
                        I18n.get(titleKey),
                        I18n.format(headerKey, errors.size()),
                        String.join("\n", errors));
                return;
            }

            if ("INT-400".equals(code)) {
                show(type, I18n.get(titleKey), I18n.get(headerKey), cause.getMessage());
                return;
            }

            // Standard path: title + header + content (or cause.getMessage() if content key is missing).
            show(type,
                    I18n.get(titleKey),
                    I18n.get(headerKey),
                    lookupOrFallback(contentKey, cause));
        } catch (Throwable t) {
            // Last-resort fallback: never let the error handler itself crash or recurse.
            // Strings are i18n'd via error.fallback.* — no hardcoded English permitted.
            log.error("Failure rendering alert for category [{}]; falling back to localised generic alert",
                    category.code(), t);
            try {
                String fallbackContent = (cause != null && cause.getMessage() != null)
                        ? cause.getMessage()
                        : I18n.get("error.fallback.content");
                show(Alert.AlertType.ERROR,
                        I18n.get("error.fallback.title"),
                        I18n.get("error.fallback.header"),
                        fallbackContent);
            } catch (Throwable ignored) {
                // If even the localised fallback fails, swallow to avoid recursion. The
                // primary failure has already been logged on the line above.
            }
        }
    }

    private static String lookupOrFallback(String key, Throwable cause) {
        try {
            return I18n.get(key);
        } catch (MissingResourceException e) {
            return cause.getMessage();
        }
    }

    private static void show(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
