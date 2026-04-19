package com.recetea.infrastructure.ui.javafx.features.identity.controllers;

import com.recetea.core.user.application.ports.in.IRegisterUserUseCase;
import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.domain.DuplicateIdentityException;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.regex.Pattern;

public class RegisterController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private IRegisterUserUseCase registerUseCase;
    private NavigationService nav;

    public void init(IRegisterUserUseCase registerUseCase, NavigationService nav) {
        this.registerUseCase = registerUseCase;
        this.nav = nav;
    }

    @FXML
    public void onRegisterButtonClick() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            errorLabel.setText("Por favor, completa todos los campos.");
            return;
        }

        if (username.length() < 3) {
            errorLabel.setText("El nombre de usuario debe tener al menos 3 caracteres.");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errorLabel.setText("El email no tiene un formato válido.");
            return;
        }

        if (!password.equals(confirm)) {
            errorLabel.setText("Las contraseñas no coinciden.");
            confirmPasswordField.clear();
            return;
        }

        try {
            registerUseCase.execute(new RegisterUserRequest(username, email, password));
            nav.toLogin();
        } catch (DuplicateIdentityException e) {
            errorLabel.setText("Nombre de usuario o email ya en uso.");
        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    public void onBackToLoginLinkClick() {
        nav.toLogin();
    }
}
