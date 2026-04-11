package com.recetea.infrastructure.ui;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import com.recetea.core.ports.in.IGetAllRecipesUseCase;
import com.recetea.core.ports.in.IGetRecipeByIdUseCase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class RecipeDetailController {

    @FXML private Label titleLabel;
    @FXML private Label prepTimeLabel;
    @FXML private Label servingsLabel;
    @FXML private Label descriptionLabel;

    @FXML private TableView<RecipeIngredient> ingredientsTable;
    @FXML private TableColumn<RecipeIngredient, Integer> colIngredientId;
    @FXML private TableColumn<RecipeIngredient, Integer> colUnitId;
    @FXML private TableColumn<RecipeIngredient, Double> colQuantity;

    // Dependencias para leer y para poder volver atrás con estado
    private IGetRecipeByIdUseCase getRecipeByIdUseCase;
    private IGetAllRecipesUseCase getAllRecipesUseCase;
    private ICreateRecipeUseCase createRecipeUseCase;

    public void setUseCases(IGetRecipeByIdUseCase getById, IGetAllRecipesUseCase getAll, ICreateRecipeUseCase create) {
        this.getRecipeByIdUseCase = getById;
        this.getAllRecipesUseCase = getAll;
        this.createRecipeUseCase = create;
    }

    @FXML
    public void initialize() {
        // Configuramos cómo se pintan los ingredientes en la tabla
        colIngredientId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getIngredientId()));
        colUnitId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getUnitId()));
        colQuantity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getQuantity()));
    }

    /**
     * Extrae la receta completa de la BD y la pinta en la UI.
     */
    public void loadRecipeDetails(int recipeId) {
        Optional<Recipe> recipeOpt = getRecipeByIdUseCase.execute(recipeId);

        if (recipeOpt.isPresent()) {
            Recipe recipe = recipeOpt.get();
            titleLabel.setText(recipe.getTitle());
            prepTimeLabel.setText(recipe.getPreparationTimeMinutes() + " min");
            servingsLabel.setText(String.valueOf(recipe.getServings()));
            descriptionLabel.setText(recipe.getDescription());

            // Cargamos la lista de ingredientes (Sub-entidades) en la tabla visual
            ObservableList<RecipeIngredient> obsIngredients = FXCollections.observableArrayList(recipe.getIngredients());
            ingredientsTable.setItems(obsIngredients);
        } else {
            showError("Error 404", "La receta solicitada no existe en la base de datos.");
            onBackButtonClick(null);
        }
    }

    @FXML
    public void onBackButtonClick(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/dashboard.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            // Restauramos las dependencias del Dashboard
            controller.setGetAllRecipesUseCase(this.getAllRecipesUseCase);
            controller.setCreateRecipeUseCase(this.createRecipeUseCase);
            // IMPORTANTE: Le pasamos también el de GetById para futuros clics
            controller.setGetRecipeByIdUseCase(this.getRecipeByIdUseCase);
            controller.loadData();

            Stage stage = (Stage) titleLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 500));
            stage.setTitle("Recetea - Dashboard Principal");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}