package com.recetea.infrastructure.ui.javafx.shared.error;

import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.InvalidRecipeDataException;
import com.recetea.core.shared.domain.ErrorCode;
import com.recetea.core.shared.domain.ValidationResult;
import com.recetea.core.user.domain.DuplicateIdentityException;
import com.recetea.infrastructure.interop.xml.XmlInteropAdapter;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.scene.control.Alert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * White-box test of {@link GlobalExceptionHandler#categorize} plus an end-to-end
 * resource-bundle audit. Avoids the JavaFX toolkit entirely — categorize() is a
 * pure function and the i18n lookups go through the static {@link I18n} facade.
 *
 * <p>The bundle audit guards against the {@code [KEY: ...]} regression: if any code
 * emitted by {@code categorize} ever loses its corresponding {@code error.{code}.title}
 * / {@code .header} / {@code .content} entry, this test fails loudly instead of letting
 * a placeholder render in the running UI.
 */
@DisplayName("GlobalExceptionHandler — categorisation + i18n key integrity")
class GlobalExceptionHandlerTest {

    // ── Categorise: domain branch ───────────────────────────────────────────

    @Test
    @DisplayName("categorise: NOT_FOUND domain → WARNING + code NOT_FOUND")
    void notFound_resolvesToDomainCategoryWarning() {
        var category = GlobalExceptionHandler.categorize(new RecipeNotFoundException(1));
        assertEquals(ErrorCode.NOT_FOUND.name(), category.code());
        assertEquals(Alert.AlertType.WARNING,    category.alertType());
        assertFalse(category.isFatal());
    }

    @Test
    @DisplayName("categorise: UNAUTHORIZED domain → WARNING + code UNAUTHORIZED")
    void unauthorized_resolvesToDomainCategoryWarning() {
        var category = GlobalExceptionHandler.categorize(
                new UnauthorizedRecipeAccessException("missing ownership"));
        assertEquals(ErrorCode.UNAUTHORIZED.name(), category.code());
        assertEquals(Alert.AlertType.WARNING,       category.alertType());
    }

    @Test
    @DisplayName("categorise: AUTHENTICATION_REQUIRED domain → WARNING")
    void authRequired_resolvesToDomainCategoryWarning() {
        var category = GlobalExceptionHandler.categorize(new AuthenticationRequiredException());
        assertEquals(ErrorCode.AUTHENTICATION_REQUIRED.name(), category.code());
        assertEquals(Alert.AlertType.WARNING,                   category.alertType());
    }

    @Test
    @DisplayName("categorise: VALIDATION_ERROR domain → WARNING + accumulated message list")
    void validationError_resolvesToDomainCategoryWarning() {
        var ex = InvalidRecipeDataException.from(
                ValidationResult.invalid(List.of("first error", "second error")));
        var category = GlobalExceptionHandler.categorize(ex);
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), category.code());
        assertEquals(Alert.AlertType.WARNING,           category.alertType());
        assertEquals(List.of("first error", "second error"), ex.errors());
    }

    @Test
    @DisplayName("categorise: CONFLICT domain → WARNING")
    void conflict_resolvesToDomainCategoryWarning() {
        var category = GlobalExceptionHandler.categorize(
                new DuplicateIdentityException("username already taken"));
        assertEquals(ErrorCode.CONFLICT.name(), category.code());
        assertEquals(Alert.AlertType.WARNING,    category.alertType());
    }

    // ── Categorise: non-domain branch ───────────────────────────────────────

    @Test
    @DisplayName("categorise: XmlInteropException → INT-400 / WARNING")
    void xmlInterop_resolvesToInt400Warning() {
        var category = GlobalExceptionHandler.categorize(
                new XmlInteropAdapter.XmlInteropException("schema violation"));
        assertEquals("INT-400",               category.code());
        assertEquals(Alert.AlertType.WARNING, category.alertType());
    }

    @Test
    @DisplayName("categorise: InfrastructureException → INF-500 / ERROR")
    void infrastructure_resolvesToInf500Error() {
        var category = GlobalExceptionHandler.categorize(
                new InfrastructureException("Supabase upload failed", null));
        assertEquals("INF-500",             category.code());
        assertEquals(Alert.AlertType.ERROR, category.alertType());
        assertTrue(category.isFatal());
    }

    @Test
    @DisplayName("categorise: SQLException → DB-500 / ERROR")
    void sql_resolvesToDb500Error() {
        var category = GlobalExceptionHandler.categorize(new SQLException("connection reset"));
        assertEquals("DB-500",              category.code());
        assertEquals(Alert.AlertType.ERROR, category.alertType());
    }

    @Test
    @DisplayName("categorise: anything else → ERR-500 / ERROR")
    void unknown_resolvesToErr500Error() {
        var category = GlobalExceptionHandler.categorize(new RuntimeException("ought to be impossible"));
        assertEquals("ERR-500",             category.code());
        assertEquals(Alert.AlertType.ERROR, category.alertType());
    }

    // ── i18n bundle integrity ───────────────────────────────────────────────

    /**
     * Every code that {@code categorize} can emit must have at least a
     * {@code error.{code}.title} and {@code error.{code}.header} entry — content is
     * optional because some branches (INT-400, missing key) deliberately fall through
     * to {@code cause.getMessage()}.
     */
    @Test
    @DisplayName("bundle: every emitted code resolves to a real title + header (no placeholder)")
    void everyEmittedCodeHasI18nKeys() {
        ResourceBundle bundle = I18n.bundle();

        // Domain-side codes — all six ErrorCode enum values:
        for (ErrorCode code : ErrorCode.values()) {
            assertResolves(bundle, "error." + code.name() + ".title");
            assertResolves(bundle, "error." + code.name() + ".header");
        }

        // Non-domain codes emitted by categorize():
        for (String code : List.of("INT-400", "INF-500", "DB-500", "ERR-500")) {
            assertResolves(bundle, "error." + code + ".title");
            assertResolves(bundle, "error." + code + ".header");
            assertResolves(bundle, "error." + code + ".content");
        }

        // Catch-of-last-resort path inside showAlert():
        assertResolves(bundle, "error.fallback.title");
        assertResolves(bundle, "error.fallback.header");
        assertResolves(bundle, "error.fallback.content");
    }

    @Test
    @DisplayName("bundle: I18n.get returns canonical entries, not the [KEY: ...] placeholder")
    void i18nGetReturnsCanonicalEntries() {
        for (ErrorCode code : ErrorCode.values()) {
            String title = I18n.get("error." + code.name() + ".title");
            assertFalse(title.startsWith("[KEY:"),
                    "I18n must resolve error." + code.name() + ".title; got: " + title);
        }
        for (String code : List.of("INT-400", "INF-500", "DB-500", "ERR-500")) {
            String title = I18n.get("error." + code + ".title");
            assertFalse(title.startsWith("[KEY:"),
                    "I18n must resolve error." + code + ".title; got: " + title);
        }
    }

    private static void assertResolves(ResourceBundle bundle, String key) {
        try {
            String value = bundle.getString(key);
            assertNotNull(value, "Bundle entry for " + key + " resolved but is null");
            assertFalse(value.isBlank(), "Bundle entry for " + key + " is blank");
        } catch (MissingResourceException e) {
            fail("Missing required bundle entry: " + key);
        }
    }
}
