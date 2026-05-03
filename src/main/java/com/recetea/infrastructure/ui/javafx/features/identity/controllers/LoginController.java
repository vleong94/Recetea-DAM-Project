package com.recetea.infrastructure.ui.javafx.features.identity.controllers;

import atlantafx.base.theme.Styles;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.domain.DomainException;
import com.recetea.core.user.application.ports.in.ILoginUseCase;
import com.recetea.core.user.application.ports.in.dto.LoginRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.core.shared.application.ports.in.INavigationPort;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

/**
 * Controller for the login screen. Two-field form (username/email +
 * password) plus a global error label that surfaces validation and
 * invalid-credentials feedback.
 *
 * <p><b>Listener-race fix.</b> The invalid-credentials handler clears
 * the password field <em>first</em> and only then renders the error +
 * applies the {@code STATE_DANGER} pseudo-class. Reversing the order
 * triggers the {@code passwordField.textProperty()} change listener
 * (installed to clear errors on user re-typing) which would wipe the
 * just-rendered message before the user sees it.
 *
 * <p><b>STATE_DANGER pseudo-class.</b> AtlantaFX exposes the danger
 * state as a {@link javafx.css.PseudoClass}, NOT a CSS class string —
 * applied via {@code node.pseudoClassStateChanged(Styles.STATE_DANGER, true)},
 * never {@code getStyleClass().add(...)}. Both fields are flagged
 * jointly because the use case can't disclose which value was wrong
 * (account-harvesting prevention). Username is preserved across the
 * error so the user only re-types the password.
 *
 * <p><b>ES — </b>Controlador para la pantalla de login. Formulario
 * de dos campos (username/email + contraseña) más una etiqueta de
 * error global que muestra el feedback de validación y de
 * credenciales inválidas.
 *
 * <p><b>Solución a la carrera de listeners.</b> El handler de
 * credenciales inválidas limpia el campo de contraseña
 * <em>primero</em> y sólo entonces renderiza el error y aplica la
 * pseudoclase {@code STATE_DANGER}. Invertir el orden dispara el
 * listener de {@code passwordField.textProperty()} (instalado para
 * limpiar los errores cuando el usuario vuelve a teclear), que
 * borraría el mensaje recién renderizado antes de que el usuario
 * lo viese.
 *
 * <p><b>Pseudoclase STATE_DANGER.</b> AtlantaFX expone el estado de
 * peligro como una {@link javafx.css.PseudoClass}, NO como cadena
 * de clase CSS — se aplica vía
 * {@code node.pseudoClassStateChanged(Styles.STATE_DANGER, true)},
 * nunca {@code getStyleClass().add(...)}. Ambos campos se marcan
 * conjuntamente porque el caso de uso no puede revelar qué valor
 * era erróneo (prevención de account-harvesting). El username se
 * preserva ante el error para que el usuario sólo vuelva a teclear
 * la contraseña.
 */
public class LoginController {

    @FXML private TextField usernameOrEmailField;
    @FXML private PasswordField passwordField;
    @FXML private Label lblGlobalError;

    private ILoginUseCase loginUseCase;
    private IUserSessionService sessionService;
    private INavigationPort nav;

    @FXML
    public void initialize() {
        Platform.runLater(usernameOrEmailField::requestFocus);

        // Reset whenever the user touches either field — drops danger highlighting
        // from both inputs and hides the global error label. Mirrors RegisterController's
        // neutral-on-input UX: as soon as the user starts correcting their input the
        // form returns to a clean state without an extra click.
        usernameOrEmailField.textProperty().addListener((obs, old, val) -> resetState());
        passwordField.textProperty().addListener((obs, old, val) -> resetState());
    }

    public void init(ILoginUseCase loginUseCase, IUserSessionService sessionService, INavigationPort nav) {
        this.loginUseCase = loginUseCase;
        this.sessionService = sessionService;
        this.nav = nav;
    }

    @FXML
    public void onLoginButtonClick() {
        // Explicit reset at attempt start: while the textProperty listeners on each field
        // already call resetState() on every keystroke, an explicit pre-attempt clear
        // guarantees a stale error never bleeds into a fresh submission — for instance
        // when the user re-clicks Login with the exact same input as a previous failure.
        resetState();

        String identifier = usernameOrEmailField.getText().trim();
        String password = passwordField.getText();

        if (identifier.isEmpty() || password.isEmpty()) {
            showError(I18n.get("error.requiredFields"));
            if (identifier.isEmpty()) markDanger(usernameOrEmailField);
            if (password.isEmpty())   markDanger(passwordField);
            return;
        }

        // ILoginUseCase signals invalid credentials through Optional.empty(), so the empty
        // result is the canonical "wrong username/password" path. The catch (DomainException)
        // covers any future application-level domain failure (account-locked, password-expired,
        // …) — today it's an inert hook routed to the same UX. InfrastructureException and
        // other non-application throwables intentionally bubble up to GlobalExceptionHandler
        // so the operator-facing TraceID alert stays intact.
        try {
            Optional<UserResponse> result = loginUseCase.execute(new LoginRequest(identifier, password));
            if (result.isPresent()) {
                UserResponse user = result.get();
                sessionService.login(user.id(), user.username());
                nav.toDashboard();
            } else {
                handleInvalidCredentials();
            }
        } catch (DomainException e) {
            handleInvalidCredentials();
        }
    }

    /**
     * Unified failure UX for the two paths that count as "invalid credentials":
     * the empty {@link Optional} from {@link ILoginUseCase#execute} and any
     * {@link DomainException} thrown by the use case.
     *
     * <p>Order matters here. {@code passwordField.clear()} fires the
     * {@link #initialize}-installed text-property listener, which itself calls
     * {@link #resetState} — wiping the global error label and danger highlight as
     * a side effect of the clear. We therefore <em>first</em> clear, then
     * re-paint the desired state ({@link #showError} + {@link #markDanger}). The
     * post-clear writes are the final state the user actually sees.
     *
     * <p>Username field is intentionally preserved: the use case can't disclose
     * which of the two values was wrong (account-harvesting prevention), so we
     * highlight both with the AtlantaFX {@link Styles#STATE_DANGER} pseudo-class
     * — same red-border visual that PrimerLight's {@code .text-field:danger} rule
     * paints on the registration form for an invalid email — but only the password
     * is wiped, sparing the user from retyping the half they may have got right.
     * Focus jumps back to the password field so the user can immediately retype.
     */
    private void handleInvalidCredentials() {
        passwordField.clear();                                         // 1. text → "" fires listener → resetState wipes everything
        showError(I18n.get("error.login.invalid_credentials"));        // 2. re-paint the error label
        markDanger(usernameOrEmailField);                              // 3. re-paint the danger borders
        markDanger(passwordField);
        passwordField.requestFocus();                                  // 4. caret returns to the cleared field
    }

    @FXML
    public void onRegisterLinkClick() {
        nav.toRegister();
    }

    // ── State helpers ───────────────────────────────────────────────────────
    // visible / managed flip together every time. With FXML defaults
    // visible="false" / managed="false" the label takes zero layout space at
    // mount and reclaims a single text-line on first error → no jump on initial
    // render, predictable jump only when an error genuinely surfaces.

    private void showError(String message) {
        lblGlobalError.setText(message);
        lblGlobalError.setVisible(true);
        lblGlobalError.setManaged(true);
    }

    private void resetState() {
        lblGlobalError.setText("");
        lblGlobalError.setVisible(false);
        lblGlobalError.setManaged(false);
        clearDanger(usernameOrEmailField);
        clearDanger(passwordField);
    }

    /**
     * AtlantaFX exposes {@link Styles#STATE_DANGER} as a {@link javafx.css.PseudoClass}.
     * Setting it on a TextField triggers PrimerLight's {@code .text-field:danger} rule
     * (red border) without us having to declare any project-side CSS. {@link PasswordField}
     * inherits from {@link TextField}, so the same helper covers both inputs.
     */
    private static void markDanger(TextField field) {
        field.pseudoClassStateChanged(Styles.STATE_DANGER, true);
    }

    private static void clearDanger(TextField field) {
        field.pseudoClassStateChanged(Styles.STATE_DANGER, false);
    }
}
