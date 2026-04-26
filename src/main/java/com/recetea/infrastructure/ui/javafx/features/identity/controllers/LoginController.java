package com.recetea.infrastructure.ui.javafx.features.identity.controllers;

import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.application.ports.in.ILoginUseCase;
import com.recetea.core.user.application.ports.in.dto.LoginRequest;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.application.Platform;
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

    @FXML
    public void initialize() {
        Platform.runLater(usernameOrEmailField::requestFocus);
    }

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
            errorLabel.setText(I18n.get("error.requiredFields"));
            return;
        }

        loginUseCase.execute(new LoginRequest(identifier, password))
                .ifPresentOrElse(
                        user -> {
                            sessionService.login(user.id());
                            nav.toDashboard();
                        },
                        () -> {
                            errorLabel.setText(I18n.get("login.error.invalidCredentials"));
                            passwordField.clear();
                        }
                );
    }

    @FXML
    public void onRegisterLinkClick() {
        nav.toRegister();
    }
}
