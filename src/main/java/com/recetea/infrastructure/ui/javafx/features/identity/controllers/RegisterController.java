package com.recetea.infrastructure.ui.javafx.features.identity.controllers;

import com.recetea.core.user.application.ports.in.IRegisterUserUseCase;
import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.domain.DuplicateIdentityException;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

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
            errorLabel.setText(I18n.get("error.requiredFields"));
            return;
        }

        if (!password.equals(confirm)) {
            errorLabel.setText(I18n.get("register.error.passwordMismatch"));
            confirmPasswordField.clear();
            return;
        }

        try {
            registerUseCase.execute(new RegisterUserRequest(username, email, password));
            nav.toLogin();
        } catch (DuplicateIdentityException e) {
            errorLabel.setText(I18n.get("register.error.duplicateIdentity"));
        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    public void onBackToLoginLinkClick() {
        nav.toLogin();
    }
}
