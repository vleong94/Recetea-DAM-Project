package com.recetea.infrastructure.ui.javafx.shared;

import com.recetea.infrastructure.ui.javafx.recipe.RecipeEditorController;
import com.recetea.infrastructure.ui.javafx.recipe.RecipeDashboardController;
import com.recetea.infrastructure.ui.javafx.recipe.RecipeDetailController;
import com.recetea.infrastructure.ui.javafx.recipe.RecipeContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Motor de enrutamiento y navegación de la UI (Routing Engine).
 * Gestiona las transiciones del Scene Graph, la instanciación de los Controllers
 * y la Dependency Injection transversal de los Use Cases mediante el Context.
 * Garantiza el aislamiento arquitectónico operando sin acoplamiento a entidades de Domain.
 */
public class NavigationService {

    private final Stage stage;
    private final RecipeContext context;

    /**
     * Inicializa el servicio vinculando el Stage principal de JavaFX y el Context de negocio.
     */
    public NavigationService(Stage stage, RecipeContext context) {
        this.stage = stage;
        this.context = context;
    }

    /**
     * Ejecuta la transición hacia el Dashboard.
     * Dispara el ciclo de vida de inicialización y la carga asíncrona de datos
     * en el Controller destino tras su instanciación.
     */
    public void toDashboard() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/dashboard.fxml", "Dashboard", (loader) -> {
            RecipeDashboardController controller = loader.getController();
            controller.init(context, this);
            controller.loadData();
        });
    }

    /**
     * Ejecuta la transición hacia el entorno de edición.
     * Implementa una estrategia de Deep Load para garantizar la consistencia atómica.
     * Si se recibe un identificador válido, se delega al Core la recuperación y
     * reconstrucción completa del estado (DTO) antes de hidratar el Controller.
     * Si el identificador es nulo, orquesta un flujo de creación en blanco.
     *
     * @param recipeId Identificador primitivo de la entidad a modificar, o null para creación.
     */
    public void toRecipeEditor(Integer recipeId) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/recipe_editor.fxml", "Editor de Receta", (loader) -> {
            RecipeEditorController controller = loader.getController();
            controller.init(context, this);
            if (recipeId != null) {
                context.getRecipeById().execute(recipeId)
                        .ifPresent(controller::loadRecipeData);
            }
        });
    }

    /**
     * Ejecuta la transición hacia la vista de detalle de solo lectura.
     *
     * @param recipeId Identificador primitivo del registro a renderizar.
     */
    public void toRecipeDetail(int recipeId) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/recipe_detail.fxml", "Detalle de Receta", (loader) -> {
            RecipeDetailController controller = loader.getController();
            controller.init(context, this);
            controller.loadRecipeDetails(recipeId);
        });
    }

    /**
     * Resuelve el archivo FXML en el Classpath, construye el Scene Graph y aplica
     * la configuración del Controller mediante callbacks funcionales.
     * Implementa un patrón Fail-Fast: cualquier anomalía de I/O aborta la ejecución
     * de forma inmediata para evitar estados visuales corruptos.
     *
     * @param fxmlPath Ruta absoluta del recurso visual.
     * @param title Título a inyectar en el Stage contenedor.
     * @param config Callback funcional para la inyección de dependencias post-carga.
     */
    private void loadScene(String fxmlPath, String title, java.util.function.Consumer<FXMLLoader> config) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            config.accept(loader);
            stage.setTitle("Recetea - " + title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Fallo crítico en el Inbound Adapter: No se pudo resolver el recurso FXML -> " + fxmlPath, e);
        }
    }
}