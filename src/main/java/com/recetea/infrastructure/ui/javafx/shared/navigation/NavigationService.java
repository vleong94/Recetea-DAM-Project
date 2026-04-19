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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class NavigationService {

    private final Stage stage;
    private final RecipeQueryProvider queryProvider;
    private final RecipeCommandProvider commandProvider;
    private final ILoginUseCase loginUseCase;
    private final IRegisterUserUseCase registerUseCase;
    private final IUserSessionService sessionService;

    public NavigationService(Stage stage, RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider,
                             ILoginUseCase loginUseCase, IRegisterUserUseCase registerUseCase,
                             IUserSessionService sessionService) {
        this.stage = stage;
        this.queryProvider = queryProvider;
        this.commandProvider = commandProvider;
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.sessionService = sessionService;
    }

    public void toLogin() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/identity/pages/login.fxml", "Iniciar Sesión", loader -> {
            LoginController controller = loader.getController();
            controller.init(loginUseCase, sessionService, this);
        });
    }

    public void toRegister() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/identity/pages/register.fxml", "Crear Cuenta", loader -> {
            RegisterController controller = loader.getController();
            controller.init(registerUseCase, this);
        });
    }

    public void toDashboard() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_dashboard.fxml", "Panel Principal", loader -> {
            RecipeDashboardController controller = loader.getController();
            controller.init(queryProvider, commandProvider, this);
        });
    }

    public void toRecipeCreate() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_create.fxml", "Nueva Receta", loader -> {
            RecipeCreateController controller = loader.getController();
            controller.init(commandProvider, this);
        });
    }

    public void toRecipeUpdate(RecipeDetailResponse recipe) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_update.fxml", "Editar Receta", loader -> {
            RecipeUpdateController controller = loader.getController();
            controller.init(commandProvider, this);
            controller.loadRecipeData(recipe);
        });
    }

    public void toRecipeDetail(RecipeId recipeId) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_detail.fxml", "Detalle de la Receta", loader -> {
            RecipeDetailController controller = loader.getController();
            controller.init(queryProvider, commandProvider, this);
            controller.loadRecipeDetails(recipeId);
        });
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            config.accept(loader);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(STYLESHEET);
            stage.setTitle("Recetea - " + title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Fallo crítico de I/O al resolver la View: " + fxmlPath, e);
        }
    }
}
