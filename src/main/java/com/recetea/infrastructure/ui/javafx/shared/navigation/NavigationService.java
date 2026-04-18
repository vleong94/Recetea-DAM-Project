package com.recetea.infrastructure.ui.javafx.shared.navigation;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeDashboardController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeDetailController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeCreateController;
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

    public NavigationService(Stage stage, RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider) {
        this.stage = stage;
        this.queryProvider = queryProvider;
        this.commandProvider = commandProvider;
    }

    public void toDashboard() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_dashboard.fxml", "Panel Principal", (loader) -> {
            RecipeDashboardController controller = loader.getController();
            controller.init(queryProvider, this);
        });
    }

    public void toRecipeCreate() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_create.fxml", "Nueva Receta", (loader) -> {
            RecipeCreateController controller = loader.getController();
            controller.init(commandProvider, this);
        });
    }

    public void toRecipeUpdate(RecipeDetailResponse recipe) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_update.fxml", "Editar Receta", (loader) -> {
            RecipeUpdateController controller = loader.getController();
            controller.init(commandProvider, this);
            controller.loadRecipeData(recipe);
        });
    }

    public void toRecipeDetail(int recipeId) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_detail.fxml", "Detalle de la Receta", (loader) -> {
            RecipeDetailController controller = loader.getController();
            controller.init(queryProvider, this);
            controller.loadRecipeDetails(recipeId);
        });
    }

    public void deleteRecipe(int id) {
        commandProvider.deleteRecipe().execute(id);
    }

    private void loadScene(String fxmlPath, String title, Consumer<FXMLLoader> config) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            config.accept(loader);

            stage.setTitle("Recetea - " + title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Fallo crítico de I/O al resolver la View: " + fxmlPath, e);
        }
    }
}
