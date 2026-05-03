package com.recetea.infrastructure.ui.javafx.shared.i18n;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Static facade over the {@code i18n.messages} resource bundle. Loaded
 * once at class-initialisation time using the JVM default locale —
 * locale switching at runtime is not supported (would require unloading
 * + reloading every controller's text bindings).
 *
 * <p><b>Missing-key behaviour.</b> {@link #get(String)} returns
 * {@code [KEY: <key>]} instead of throwing. The placeholder shape is
 * intentional — it appears verbatim in the UI, making the missing key
 * visible to QA without crashing the screen, and
 * {@code GlobalExceptionHandlerTest} asserts no production code paths
 * surface this shape.
 *
 * <p>{@link #format(String, Object...)} runs the looked-up template
 * through {@link MessageFormat} so calls like
 * {@code I18n.format("error.VALIDATION_ERROR.header", 3)} produce
 * "3 validation errors found".
 *
 * <p><b>ES — </b>Fachada estática sobre el resource bundle
 * {@code i18n.messages}. Cargado una vez en la inicialización de la
 * clase usando el locale por defecto de la JVM — el cambio de
 * locale en runtime no se soporta (requeriría descargar + recargar
 * los bindings de texto de cada controlador).
 *
 * <p><b>Comportamiento ante clave ausente.</b> {@link #get(String)}
 * devuelve {@code [KEY: <key>]} en lugar de lanzar. La forma de
 * placeholder es intencional — aparece literal en la UI, haciendo
 * visible la clave ausente para QA sin tirar la pantalla, y
 * {@code GlobalExceptionHandlerTest} comprueba que ninguna ruta
 * de código en producción exponga esta forma.
 *
 * <p>{@link #format(String, Object...)} pasa la plantilla buscada
 * por {@link MessageFormat}, de modo que llamadas como
 * {@code I18n.format("error.VALIDATION_ERROR.header", 3)} producen
 * "3 validation errors found".
 */
public final class I18n {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("i18n.messages");

    private I18n() {}

    /**
     * Exposed for callers that need {@link ResourceBundle} directly (e.g. {@code FXMLLoader.setResources}).
     *
     * <p><b>ES — </b>Expuesto para llamadores que necesitan el
     * {@link ResourceBundle} directamente (p. ej.
     * {@code FXMLLoader.setResources}).
     */
    public static ResourceBundle bundle() { return BUNDLE; }

    /**
     * Lookup by key. Returns {@code [KEY: <key>]} on miss to keep the UI alive while making the gap visible.
     *
     * <p><b>ES — </b>Búsqueda por clave. Devuelve {@code [KEY: <key>]}
     * en caso de fallo para mantener viva la UI haciendo visible el
     * hueco.
     */
    public static String get(String key) {
        try {
            return BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return "[KEY: " + key + "]";
        }
    }

    /**
     * Lookup + {@link MessageFormat} substitution. Wraps {@link #get} so the same miss-handling applies.
     *
     * <p><b>ES — </b>Búsqueda + sustitución con {@link MessageFormat}.
     * Envuelve {@link #get} para que aplique el mismo manejo de
     * fallo.
     */
    public static String format(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }
}
