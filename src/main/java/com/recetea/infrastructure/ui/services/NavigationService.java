package com.recetea.infrastructure.ui.services;

import com.recetea.core.domain.Recipe;
import com.recetea.infrastructure.ui.controllers.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * UI Service: Motor de Navegación de la aplicación.
 * Centraliza la carga de FXML y la inyección del Context Object en los controladores.
 */
public class NavigationService {
    private final Stage stage;
    private final RecipeServiceContext context;

    public NavigationService(Stage stage, RecipeServiceContext context) {
        this.stage = stage;
        this.context = context;
    }

    /**
     * Navega al catálogo principal y dispara la carga de datos.
     */
    public void toDashboard() {
        loadScene("/com/recetea/infrastructure/ui/dashboard.fxml", "Dashboard", (loader) -> {
            DashboardController controller = loader.getController();
            controller.init(context, this);
            controller.loadData(); // <--- Imprescindible para llenar la tabla al entrar
        });
    }

    /**
     * Navega al formulario de creación/edición.
     * Si recibe una receta, el controlador entra en modo edición.
     */
    public void toCreateRecipe(Recipe recipeToEdit) {
        loadScene("/com/recetea/infrastructure/ui/create_recipe.fxml", "Editor de Receta", (loader) -> {
            CreateRecipeController controller = loader.getController();
            controller.init(context, this);
            if (recipeToEdit != null) {
                controller.loadRecipeData(recipeToEdit); // Requiere este método en el controlador
            }
        });
    }

    /**
     * Navega a la vista de detalle de una receta específica.
     */
    public void toRecipeDetail(int recipeId) {
        loadScene("/com/recetea/infrastructure/ui/recipe_detail.fxml", "Detalle de Receta", (loader) -> {
            RecipeDetailController controller = loader.getController();
            controller.init(context, this);
            controller.loadRecipeDetails(recipeId);
        });
    }

    /**
     * Método genérico de carga de escenas para evitar duplicidad de código.
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
            System.err.println("Error al cargar la vista: " + fxmlPath);
            e.printStackTrace();
        }
    }
}