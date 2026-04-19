package com.recetea.infrastructure.ui.javafx.features.identity.controllers;

import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.application.ports.in.ILoginUseCase;
import com.recetea.core.user.application.ports.in.dto.LoginRequest;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameOrEmailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private ILoginUseCase loginUseCase;
    private IUserSessionService sessionService;
    private NavigationService nav;

    public void init(ILoginUseCase loginUseCase, IUserSessionService sessionService, NavigationService nav) {
        this.loginUseCase = loginUseCase;
        this.sessionService = sessionService;
        this.nav = nav;
    }

    @FXML
    public void onLoginButtonClick() {
        String identifier = usernameOrEmailField.getText().trim();
        String password = passwordField.getText();

        if (identifier.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Por favor, completa todos los campos.");
            return;
        }

        loginUseCase.execute(new LoginRequest(identifier, password))
                .ifPresentOrElse(
                        user -> {
                            sessionService.login(user.id());
                            nav.toDashboard();
                        },
                        () -> {
                            errorLabel.setText("Credenciales inválidas.");
                            passwordField.clear();
                        }
                );
    }

    @FXML
    public void onRegisterLinkClick() {
        nav.toRegister();
    }
}
