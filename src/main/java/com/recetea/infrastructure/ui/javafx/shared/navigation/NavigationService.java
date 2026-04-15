package com.recetea.infrastructure.ui.javafx.shared.navigation;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeContext;
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

/**
 * Routing Engine central de la capa de infraestructura compartida.
 * Orquesta el lifecycle de las views y centraliza las transiciones de Scene.
 * Desacopla la lógica de presentación de la carga física de recursos FXML,
 * asegurando la Dependency Injection mediante el paso del Context.
 */
public class NavigationService {

    private final Stage stage;
    private final RecipeContext context;

    public NavigationService(Stage stage, RecipeContext context) {
        this.stage = stage;
        this.context = context;
    }

    /**
     * Ejecuta el routing hacia el Dashboard principal.
     * Despliega la proyección read-only del catálogo global de entidades.
     */
    public void toDashboard() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_dashboard.fxml", "Panel Principal", (loader) -> {
            RecipeDashboardController controller = loader.getController();
            controller.init(context, this);
        });
    }

    /**
     * Ejecuta el routing hacia el form especializado en la creación de recetas.
     * Inicializa un State limpio y volátil, preparado para el input del usuario.
     */
    public void toRecipeCreate() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_create.fxml", "Nueva Receta", (loader) -> {
            RecipeCreateController controller = loader.getController();
            controller.init(context, this);
        });
    }

    /**
     * Ejecuta el routing hacia el form especializado en la mutación de estado (Update).
     * Ejecuta el Data Binding inicial inyectando el DTO inmutable para hidratar la UI.
     */
    public void toRecipeUpdate(RecipeDetailResponse recipe) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_update.fxml", "Editar Receta", (loader) -> {
            RecipeUpdateController controller = loader.getController();
            controller.init(context, this);
            controller.loadRecipeData(recipe);
        });
    }

    /**
     * Ejecuta el routing hacia la vista de lectura detallada (Detail View).
     * Inicia la solicitud de Deep Load en la base de datos a través del ID.
     */
    public void toRecipeDetail(int recipeId) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_detail.fxml", "Detalle de la Receta", (loader) -> {
            RecipeDetailController controller = loader.getController();
            controller.init(context, this);
            controller.loadRecipeDetails(recipeId);
        });
    }

    /**
     * Generic View Resolver.
     * Emplea un functional callback (Consumer) para configurar cada Controller
     * post-instanciación antes de su rendering en el UI Thread.
     * Aplica el Fail-Fast Pattern abortando la ejecución ante excepciones de I/O.
     */
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