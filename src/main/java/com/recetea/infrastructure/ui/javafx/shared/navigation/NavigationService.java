package com.recetea.infrastructure.ui.javafx.shared.navigation;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeContext;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeDashboardController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeDetailController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeEditorController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Motor de enrutamiento central (Routing Engine) de la capa de infraestructura compartida.
 * Orquesta el ciclo de vida de las vistas y centraliza las transiciones de escena.
 * Abstrae a los controladores de la carga de recursos físicos (FXML) y asegura
 * la inyección determinista de dependencias mediante el paso del contexto de aplicación.
 */
public class NavigationService {

    private final Stage stage;
    private final RecipeContext context;

    /**
     * Instancia el servicio de navegación.
     * Vincula la ventana principal de JavaFX y el contenedor de casos de uso (Context)
     * que será propagado a cada vista durante el enrutamiento.
     */
    public NavigationService(Stage stage, RecipeContext context) {
        this.stage = stage;
        this.context = context;
    }

    /**
     * Ejecuta la transición hacia la vista principal del catálogo de recetas.
     * Carga la escena, instancia el controlador y dispara su ciclo de inicialización.
     */
    public void toDashboard() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_dashboard.fxml", "Panel Principal", (loader) -> {
            RecipeDashboardController controller = loader.getController();
            controller.init(context, this);
        });
    }

    /**
     * Ejecuta la transición hacia el entorno de edición en modo "Creación".
     * Prepara un estado en blanco para la captura de una nueva entidad.
     */
    public void toRecipeEditor() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_editor.fxml", "Nueva Receta", (loader) -> {
            RecipeEditorController controller = loader.getController();
            controller.init(context, this);
        });
    }

    /**
     * Ejecuta la transición hacia el entorno de edición en modo "Actualización".
     * Inyecta el DTO de respuesta en el controlador para hidratar el formulario
     * con el estado inmutable de la receta seleccionada.
     */
    public void toRecipeEditor(RecipeDetailResponse recipe) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_editor.fxml", "Editar Receta", (loader) -> {
            RecipeEditorController controller = loader.getController();
            controller.init(context, this);
            controller.loadRecipeData(recipe);
        });
    }

    /**
     * Ejecuta la transición hacia la vista de lectura detallada.
     * Solicita la recuperación profunda de datos (Deep Load) utilizando el identificador.
     */
    public void toRecipeDetail(int recipeId) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_detail.fxml", "Detalle de la Receta", (loader) -> {
            RecipeDetailController controller = loader.getController();
            controller.init(context, this);
            controller.loadRecipeDetails(recipeId);
        });
    }

    /**
     * Implementación genérica de resolución y ensamblaje de vistas.
     * Utiliza un callback funcional (Consumer) para permitir la configuración de
     * cada controlador de forma post-instanciación pero pre-renderizado.
     * Aplica un patrón Fail-Fast abortando el hilo si los recursos críticos no existen.
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
            throw new RuntimeException("Fallo crítico de infraestructura: Imposible cargar o resolver la vista en " + fxmlPath, e);
        }
    }
}