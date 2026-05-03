package com.recetea.infrastructure.ui.javafx.features.identity.controllers;

import atlantafx.base.theme.Styles;
import com.recetea.core.user.application.ports.in.IRegisterUserUseCase;
import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.domain.DuplicateIdentityException;
import com.recetea.core.user.domain.InvalidUserDataException;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.core.shared.application.ports.in.INavigationPort;
import com.recetea.infrastructure.ui.javafx.shared.notification.NotificationService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.stream.Collectors;

/**
 * Controller for the user-registration screen. Four-field form
 * (username, email, password, confirm password) wired to
 * {@code IRegisterUserUseCase}.
 *
 * <p><b>Enter-key submit.</b> The submit button in
 * {@code register.fxml} carries {@code defaultButton="true"} — Enter
 * from any of the four fields fires the handler without requiring an
 * explicit per-field key listener.
 *
 * <p><b>Cross-scene success toast.</b> On a successful registration the
 * controller fires a {@code NotificationService.success(...)} toast
 * <em>before</em> calling {@code nav.toLogin()}. The toast is owned by
 * the {@link javafx.stage.Window} (not the Scene), so it survives the
 * scene swap and lands overhead on the freshly-rendered login screen
 * — gives the "saved → redirect with confirmation" UX without an
 * intermediate alert dialog.
 *
 * <p><b>Conflict handling.</b> {@code DuplicateIdentityException} is
 * caught here (rather than letting it propagate to
 * {@code GlobalExceptionHandler}) so the conflict message can drive
 * inline danger-state UX on the specific field that collided, rather
 * than a generic dialog.
 *
 * <p><b>ES — </b>Controlador para la pantalla de registro de
 * usuario. Formulario de cuatro campos (username, email, contraseña,
 * confirmar contraseña) cableado a {@code IRegisterUserUseCase}.
 *
 * <p><b>Submit con Enter.</b> El botón de submit en
 * {@code register.fxml} lleva {@code defaultButton="true"} — Enter
 * desde cualquiera de los cuatro campos dispara el handler sin
 * necesidad de un listener de teclado por campo.
 *
 * <p><b>Toast de éxito entre escenas.</b> Tras un registro
 * exitoso, el controlador dispara un
 * {@code NotificationService.success(...)} <em>antes</em> de
 * llamar a {@code nav.toLogin()}. El toast pertenece al
 * {@link javafx.stage.Window} (no a la Scene), así que sobrevive
 * al cambio de escena y aparece por encima de la pantalla de login
 * recién renderizada — ofrece la UX "guardado → redirección con
 * confirmación" sin un diálogo de alerta intermedio.
 *
 * <p><b>Manejo de conflicto.</b>
 * {@code DuplicateIdentityException} se captura aquí (en lugar de
 * dejarla propagar hasta {@code GlobalExceptionHandler}) para que
 * el mensaje de conflicto pueda activar la UX inline de estado de
 * peligro sobre el campo concreto que colisionó, en lugar de un
 * diálogo genérico.
 */
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label lblGlobalError;

    private IRegisterUserUseCase registerUseCase;
    private INavigationPort nav;

    @FXML
    public void initialize() {
        // Reset whenever the user touches any field — drops danger highlighting
        // from all four inputs and hides the global error. Keeps the form back at
        // a neutral state as the user corrects their input.
        usernameField.textProperty().addListener((obs, old, val) -> resetState());
        emailField.textProperty().addListener((obs, old, val) -> resetState());
        passwordField.textProperty().addListener((obs, old, val) -> resetState());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> resetState());
    }

    public void init(IRegisterUserUseCase registerUseCase, INavigationPort nav) {
        this.registerUseCase = registerUseCase;
        this.nav = nav;
    }

    @FXML
    public void onRegisterButtonClick() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        // The password / confirm-password match is the one constraint the use-case
        // cannot enforce (the request DTO carries a single password). Every other
        // guard — required-field, username length, email format, password length —
        // is delegated to RegisterUserRequest.validate() below.
        if (!password.equals(confirm)) {
            showGlobalError(I18n.get("register.error.passwordMismatch"));
            markDanger(passwordField);
            markDanger(confirmPasswordField);
            confirmPasswordField.clear();
            return;
        }

        try {
            RegisterUserRequest request = new RegisterUserRequest(username, email, password);
            request.validate().getOrThrow(InvalidUserDataException::new);
            registerUseCase.execute(request);
            // Toast is a Popup attached to the Stage's Window — it survives the
            // nav.toLogin() scene swap because the Window outlives the Scene.
            // Triggering it BEFORE navigation lets the user land on the login page
            // with the success confirmation already fading in overhead.
            NotificationService.success(usernameField, I18n.get("register.notification.success"));
            nav.toLogin();
        } catch (InvalidUserDataException e) {
            // Errors are i18n keys emitted by RegisterUserRequest.validate(). Resolve each
            // through I18n.get so the user sees localised text, joined by newlines for the
            // single multi-line label. Field highlighting is dispatched off the same key.
            String multiLineMessage = e.getErrors().stream()
                    .map(I18n::get)
                    .collect(Collectors.joining("\n"));
            showGlobalError(multiLineMessage);

            for (String key : e.getErrors()) {
                if (key.contains("username"))      markDanger(usernameField);
                else if (key.contains("email"))    markDanger(emailField);
                else if (key.contains("password")) markDanger(passwordField);
            }
        } catch (DuplicateIdentityException e) {
            // Either username or email is taken — backend doesn't tell us which,
            // so highlight both and let the user see which one we're complaining
            // about by their own context (most users know which is theirs).
            showGlobalError(I18n.get("register.error.duplicateIdentity"));
            markDanger(usernameField);
            markDanger(emailField);
        } catch (IllegalArgumentException e) {
            showGlobalError(e.getMessage());
        }
    }

    private void showGlobalError(String message) {
        lblGlobalError.setText(message);
        lblGlobalError.setVisible(true);
        lblGlobalError.setManaged(true);
    }

    private void resetState() {
        lblGlobalError.setText("");
        lblGlobalError.setVisible(false);
        lblGlobalError.setManaged(false);
        clearDanger(usernameField);
        clearDanger(emailField);
        clearDanger(passwordField);
        clearDanger(confirmPasswordField);
    }

    /** AtlantaFX exposes STATE_DANGER as a javafx.css.PseudoClass. The matching
     *  CSS rule (.text-field:danger) lives in PrimerLight. */
    private static void markDanger(TextField field) {
        field.pseudoClassStateChanged(Styles.STATE_DANGER, true);
    }

    private static void clearDanger(TextField field) {
        field.pseudoClassStateChanged(Styles.STATE_DANGER, false);
    }

    @FXML
    public void onBackToLoginLinkClick() {
        nav.toLogin();
    }
}
