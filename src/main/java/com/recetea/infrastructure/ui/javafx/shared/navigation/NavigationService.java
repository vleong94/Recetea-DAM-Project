package com.recetea.infrastructure.ui.javafx.shared.navigation;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.application.ports.in.ILoginUseCase;
import com.recetea.core.user.application.ports.in.IRegisterUserUseCase;
import com.recetea.infrastructure.ui.javafx.features.identity.controllers.LoginController;
import com.recetea.infrastructure.ui.javafx.features.identity.controllers.RegisterController;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeCreateController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeDashboardController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeDetailController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeUpdateController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.UserProfileController;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class NavigationService {

    private final Stage stage;
    private final RecipeQueryProvider queryProvider;
    private final RecipeCommandProvider commandProvider;
    private final ILoginUseCase loginUseCase;
    private final IRegisterUserUseCase registerUseCase;
    private final IUserSessionService sessionService;
    private final ExecutorService ioExecutor;

    public NavigationService(Stage stage, RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider,
                             ILoginUseCase loginUseCase, IRegisterUserUseCase registerUseCase,
                             IUserSessionService sessionService, ExecutorService ioExecutor) {
        this.stage = stage;
        this.queryProvider = queryProvider;
        this.commandProvider = commandProvider;
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.sessionService = sessionService;
        this.ioExecutor = ioExecutor;
    }

    public void toLogin() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/identity/pages/login.fxml",
                I18n.get("nav.title.login"),
                loader -> loader.<LoginController>getController().init(loginUseCase, sessionService, this));
    }

    public void toRegister() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/identity/pages/register.fxml",
                I18n.get("nav.title.register"),
                loader -> loader.<RegisterController>getController().init(registerUseCase, this),
                this::toLogin, null);
    }

    public void toDashboard() {
        RecipeDashboardController[] ref = {null};
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_dashboard.fxml",
                I18n.get("nav.title.dashboard"),
                loader -> { ref[0] = loader.getController(); ref[0].init(queryProvider, commandProvider, this, ioExecutor); },
                null, () -> ref[0].focusSearch());
    }

    public void toRecipeCreate() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_create.fxml",
                I18n.get("nav.title.recipeCreate"),
                loader -> loader.<RecipeCreateController>getController().init(commandProvider, this),
                this::toDashboard, null);
    }

    public void toRecipeUpdate(RecipeDetailResponse recipe) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_update.fxml",
                I18n.get("nav.title.recipeUpdate"),
                loader -> { RecipeUpdateController c = loader.getController(); c.init(commandProvider, this); c.loadRecipeData(recipe); },
                this::toDashboard, null);
    }

    public void toRecipeDetail(RecipeId recipeId) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_detail.fxml",
                I18n.get("nav.title.recipeDetail"),
                loader -> { RecipeDetailController c = loader.getController(); c.init(queryProvider, commandProvider, this, ioExecutor); c.loadRecipeDetails(recipeId); },
                this::toDashboard, null);
    }

    public void toUserProfile() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/user_profile.fxml",
                I18n.get("nav.title.profile"),
                loader -> loader.<UserProfileController>getController().init(queryProvider, commandProvider, this, ioExecutor),
                this::toDashboard, null);
    }

    public void logout() {
        sessionService.logout();
        toLogin();
    }

    public void deleteRecipe(RecipeId id) {
        commandProvider.deleteRecipe().execute(id);
    }

    private static final String STYLESHEET =
            NavigationService.class.getResource(
                    "/com/recetea/infrastructure/ui/javafx/css/app.css").toExternalForm();

    private void loadScene(String fxmlPath, String title, Consumer<FXMLLoader> config) {
        loadScene(fxmlPath, title, config, null, null);
    }

    private void loadScene(String fxmlPath, String title, Consumer<FXMLLoader> config,
                           Runnable backAction, Runnable ctrlFAction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setResources(I18n.bundle());
            Parent root = loader.load();
            config.accept(loader);
            Scene scene = new Scene(root);
            if (root instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }
            scene.getStylesheets().add(STYLESHEET);
            if (backAction != null || ctrlFAction != null) {
                scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (backAction != null && e.getCode() == KeyCode.ESCAPE) {
                        e.consume();
                        backAction.run();
                    } else if (ctrlFAction != null && e.isControlDown() && e.getCode() == KeyCode.F) {
                        e.consume();
                        ctrlFAction.run();
                    }
                });
            }
            stage.setTitle("Recetea - " + title);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            if (!stage.isMaximized()) {
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                stage.setX((bounds.getWidth()  - stage.getWidth())  / 2);
                stage.setY((bounds.getHeight() - stage.getHeight()) / 2);
            }
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } catch (IOException e) {
            throw new RuntimeException("Critical I/O failure loading view: " + fxmlPath, e);
        }
    }
}
