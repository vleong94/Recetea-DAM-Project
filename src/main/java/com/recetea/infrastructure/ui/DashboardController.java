package com.recetea.infrastructure.ui;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import com.recetea.core.ports.in.IGetAllRecipesUseCase;
import com.recetea.core.ports.in.IGetRecipeByIdUseCase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class DashboardController {

    @FXML private TableView<Recipe> recipeTable;
    @FXML private TableColumn<Recipe, Integer> idColumn;
    @FXML private TableColumn<Recipe, String> titleColumn;
    @FXML private TableColumn<Recipe, Integer> prepColumn;
    @FXML private TableColumn<Recipe, Integer> servingsColumn;

    // Puertos de entrada (Casos de uso)
    private IGetAllRecipesUseCase getAllRecipesUseCase;
    private ICreateRecipeUseCase createRecipeUseCase;
    private IGetRecipeByIdUseCase getRecipeByIdUseCase; // <--- NUEVA DEPENDENCIA

    // --- INYECCIÓN DE DEPENDENCIAS ---

    public void setGetAllRecipesUseCase(IGetAllRecipesUseCase useCase) { this.getAllRecipesUseCase = useCase; }
    public void setCreateRecipeUseCase(ICreateRecipeUseCase useCase) { this.createRecipeUseCase = useCase; }
    public void setGetRecipeByIdUseCase(IGetRecipeByIdUseCase useCase) { this.getRecipeByIdUseCase = useCase; } // <--- NUEVO SETTER

    // --- CICLO DE VIDA Y EVENTOS ---

    @FXML
    public void initialize() {
        // Event Listener: Captura el doble clic en la tabla
        recipeTable.setOnMouseClicked(this::handleTableDoubleClick);
    }

    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Recipe selectedRecipe = recipeTable.getSelectionModel().getSelectedItem();
            if (selectedRecipe != null) {
                navigateToRecipeDetail(selectedRecipe.getId(), event);
            }
        }
    }

    // --- LÓGICA DE PRESENTACIÓN ---

    public void loadData() {
        if (getAllRecipesUseCase != null) {
            idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
            titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
            prepColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getPreparationTimeMinutes()));
            servingsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getServings()));

            List<Recipe> recipes = getAllRecipesUseCase.execute();
            ObservableList<Recipe> observableRecipes = FXCollections.observableArrayList(recipes);
            recipeTable.setItems(observableRecipes);
        }
    }

    // --- NAVEGACIÓN (ROUTING) ---

    @FXML
    public void onNewRecipeClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/create_recipe.fxml"));
            Parent root = loader.load();

            CreateRecipeController controller = loader.getController();
            // Pasamos ambas dependencias para asegurar el retorno
            controller.setUseCases(this.createRecipeUseCase, this.getAllRecipesUseCase);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("Recetea - Creación de Recetas");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Enrutamiento al Visor de Detalles inyectando el ID seleccionado.
     */
    private void navigateToRecipeDetail(int recipeId, MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/recipe_detail.fxml"));
            Parent root = loader.load();

            RecipeDetailController controller = loader.getController();
            // 1. Inyectamos las dependencias para que el visor pueda leer y luego volver
            controller.setUseCases(this.getRecipeByIdUseCase, this.getAllRecipesUseCase, this.createRecipeUseCase);

            // 2. Disparamos la extracción de datos pasándole el ID que hemos cazado del doble clic
            controller.loadRecipeDetails(recipeId);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 700, 600));
            stage.setTitle("Recetea - Detalles de la Receta");

        } catch (IOException e) {
            System.err.println("Fallo al cargar la vista de detalles.");
            e.printStackTrace();
        }
    }
}