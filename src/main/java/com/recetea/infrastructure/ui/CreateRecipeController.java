package com.recetea.infrastructure.ui;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.in.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UI Controller: Formulario Dual (Creación y Edición).
 * Gestiona el estado local de los ingredientes y bifurca la persistencia
 * según el modo de operación.
 */
public class CreateRecipeController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField prepTimeField;
    @FXML private TextField servingsField;

    @FXML private TextField ingredientIdField;
    @FXML private TextField unitIdField;
    @FXML private TextField quantityField;
    @FXML private TableView<CreateRecipeCommand.IngredientCommand> ingredientsTable;
    @FXML private TableColumn<CreateRecipeCommand.IngredientCommand, Integer> colIngredientId;
    @FXML private TableColumn<CreateRecipeCommand.IngredientCommand, Integer> colUnitId;
    @FXML private TableColumn<CreateRecipeCommand.IngredientCommand, Double> colQuantity;

    private final ObservableList<CreateRecipeCommand.IngredientCommand> temporaryIngredients = FXCollections.observableArrayList();

    // --- ESTADO DE MODO DUAL ---
    private boolean isEditMode = false;
    private int currentRecipeId;

    // --- PUERTOS (DEPENDENCIAS) ---
    private ICreateRecipeUseCase createRecipeUseCase;
    private IGetAllRecipesUseCase getAllRecipesUseCase;
    private IUpdateRecipeUseCase updateRecipeUseCase;
    private IGetRecipeByIdUseCase getRecipeByIdUseCase; // Para propagar al volver
    private IDeleteRecipeUseCase deleteRecipeUseCase;   // Para propagar al volver

    /**
     * Inyección exhaustiva de dependencias para mantener la navegación íntegra.
     */
    public void setUseCases(ICreateRecipeUseCase create,
                            IGetAllRecipesUseCase getAll,
                            IUpdateRecipeUseCase update,
                            IGetRecipeByIdUseCase getById,
                            IDeleteRecipeUseCase delete) {
        this.createRecipeUseCase = create;
        this.getAllRecipesUseCase = getAll;
        this.updateRecipeUseCase = update;
        this.getRecipeByIdUseCase = getById;
        this.deleteRecipeUseCase = delete;
    }

    @FXML
    public void initialize() {
        colIngredientId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ingredientId()));
        colUnitId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().unitId()));
        colQuantity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));

        ingredientsTable.setItems(temporaryIngredients);
    }

    /**
     * Activa el modo edición y puebla los campos con los datos de la receta.
     */
    public void loadRecipeData(Recipe recipe) {
        this.isEditMode = true;
        this.currentRecipeId = recipe.getId();

        titleField.setText(recipe.getTitle());
        descriptionArea.setText(recipe.getDescription());
        prepTimeField.setText(String.valueOf(recipe.getPreparationTimeMinutes()));
        servingsField.setText(String.valueOf(recipe.getServings()));

        // Mapeamos los ingredientes del dominio al formato del comando de la UI
        List<CreateRecipeCommand.IngredientCommand> mappedIngredients = recipe.getIngredients().stream()
                .map(i -> new CreateRecipeCommand.IngredientCommand(i.getIngredientId(), i.getUnitId(), i.getQuantity()))
                .collect(Collectors.toList());

        temporaryIngredients.setAll(mappedIngredients);
    }

    @FXML
    public void onAddIngredientClick() {
        try {
            int ingId = Integer.parseInt(ingredientIdField.getText());
            int unitId = Integer.parseInt(unitIdField.getText());
            double qty = Double.parseDouble(quantityField.getText());

            temporaryIngredients.add(new CreateRecipeCommand.IngredientCommand(ingId, unitId, qty));

            ingredientIdField.clear();
            unitIdField.clear();
            quantityField.clear();
        } catch (NumberFormatException e) {
            showError("Validación Fallida", "Los IDs deben ser enteros y la cantidad numérica.");
        }
    }

    @FXML
    public void onSaveButtonClick() {
        try {
            CreateRecipeCommand command = new CreateRecipeCommand(
                    1, 1, 1, // MVP: IDs de autor/categoría fijos
                    titleField.getText(),
                    descriptionArea.getText(),
                    Integer.parseInt(prepTimeField.getText()),
                    Integer.parseInt(servingsField.getText()),
                    new ArrayList<>(temporaryIngredients)
            );

            if (isEditMode) {
                updateRecipeUseCase.execute(currentRecipeId, command);
                showSuccess("Actualización Exitosa", "La receta ha sido modificada en la base de datos.");
            } else {
                int newId = createRecipeUseCase.execute(command);
                showSuccess("Creación Exitosa", "Receta guardada con ID: " + newId);
            }

            returnToDashboard();
        } catch (Exception e) {
            showError("Error de Persistencia", e.getMessage());
        }
    }

    @FXML
    public void onBackButtonClick() {
        returnToDashboard();
    }

    /**
     * Cierra el círculo restaurando el estado completo del Dashboard.
     */
    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/dashboard.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            // Restauramos el "cerebro" completo del Dashboard
            controller.setGetAllRecipesUseCase(this.getAllRecipesUseCase);
            controller.setCreateRecipeUseCase(this.createRecipeUseCase);
            controller.setUpdateRecipeUseCase(this.updateRecipeUseCase);
            controller.setGetRecipeByIdUseCase(this.getRecipeByIdUseCase);
            controller.setDeleteRecipeUseCase(this.deleteRecipeUseCase);

            controller.loadData();

            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 500));
            stage.setTitle("Recetea - Dashboard Principal");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}