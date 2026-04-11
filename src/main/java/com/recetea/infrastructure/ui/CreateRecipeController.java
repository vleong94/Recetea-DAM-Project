package com.recetea.infrastructure.ui;

import com.recetea.core.ports.in.CreateRecipeCommand;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import com.recetea.core.ports.in.IGetAllRecipesUseCase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

/**
 * UI Controller: Gestiona la pantalla de creación con Estado Local (UI State).
 * Acumula ingredientes en memoria antes de ensamblar el Command transaccional.
 */
public class CreateRecipeController {

    // --- BLOQUE 1: Componentes de Metadatos ---
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField prepTimeField;
    @FXML private TextField servingsField;

    // --- BLOQUE 2: Componentes del Sub-formulario de Ingredientes ---
    @FXML private TextField ingredientIdField;
    @FXML private TextField unitIdField;
    @FXML private TextField quantityField;
    @FXML private TableView<CreateRecipeCommand.IngredientCommand> ingredientsTable;
    @FXML private TableColumn<CreateRecipeCommand.IngredientCommand, Integer> colIngredientId;
    @FXML private TableColumn<CreateRecipeCommand.IngredientCommand, Integer> colUnitId;
    @FXML private TableColumn<CreateRecipeCommand.IngredientCommand, Double> colQuantity;

    // --- ESTADO LOCAL (UI STATE) ---
    // Memoria RAM reactiva que mantiene los ingredientes antes de guardarlos en BD.
    private final ObservableList<CreateRecipeCommand.IngredientCommand> temporaryIngredients = FXCollections.observableArrayList();

    // --- DEPENDENCIAS (PUERTOS) ---
    private ICreateRecipeUseCase createRecipeUseCase;
    private IGetAllRecipesUseCase getAllRecipesUseCase;

    public void setUseCases(ICreateRecipeUseCase create, IGetAllRecipesUseCase getAll) {
        this.createRecipeUseCase = create;
        this.getAllRecipesUseCase = getAll;
    }

    // --- CICLO DE VIDA ---
    /**
     * Este método es llamado automáticamente por el motor de JavaFX después de cargar el FXML.
     */
    @FXML
    public void initialize() {
        // Data Binding: Le enseñamos a cada columna cómo extraer el dato específico del Record de Java.
        colIngredientId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ingredientId()));
        colUnitId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().unitId()));
        colQuantity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));

        // Conectamos la tabla visual con nuestra lista en memoria.
        ingredientsTable.setItems(temporaryIngredients);
    }

    // --- LÓGICA DE INTERACCIÓN ---

    @FXML
    public void onAddIngredientClick() {
        try {
            // 1. Recolección
            int ingId = Integer.parseInt(ingredientIdField.getText());
            int unitId = Integer.parseInt(unitIdField.getText());
            double qty = Double.parseDouble(quantityField.getText());

            // 2. Modificación del Estado
            temporaryIngredients.add(new CreateRecipeCommand.IngredientCommand(ingId, unitId, qty));

            // 3. Limpieza de los campos para el siguiente ingreso
            ingredientIdField.clear();
            unitIdField.clear();
            quantityField.clear();

        } catch (NumberFormatException e) {
            showError("Validación Fallida", "Asegúrate de que los IDs sean números enteros y la cantidad un valor numérico (ej. 2.5).");
        }
    }

    @FXML
    public void onSaveButtonClick() {
        try {
            // 1. Recolección de la cabecera
            String title = titleField.getText();
            String desc = descriptionArea.getText();
            int prepTime = Integer.parseInt(prepTimeField.getText());
            int servings = Integer.parseInt(servingsField.getText());

            // 2. Ensamblaje del DTO Maestro (El Collections.emptyList() ha muerto)
            CreateRecipeCommand command = new CreateRecipeCommand(
                    1, 1, 1, // IDs fijos para el MVP (Usuario, Categoría, Dificultad)
                    title, desc, prepTime, servings,
                    new ArrayList<>(temporaryIngredients) // Inyectamos el Estado Local
            );

            // 3. Delegación al Núcleo Transaccional
            int newId = createRecipeUseCase.execute(command);
            showSuccess("Transacción Completada", "Receta ensamblada y guardada con ID: " + newId);

            // 4. Auto-Routing
            returnToDashboard();

        } catch (NumberFormatException e) {
            showError("Error de formato", "El tiempo y las raciones deben ser números enteros.");
        } catch (Exception e) {
            showError("Error Crítico de Integridad", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onBackButtonClick() {
        returnToDashboard();
    }

    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/dashboard.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.setGetAllRecipesUseCase(this.getAllRecipesUseCase);
            controller.setCreateRecipeUseCase(this.createRecipeUseCase);
            controller.loadData();

            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 500));
            stage.setTitle("Recetea - Dashboard Principal");

        } catch (IOException e) {
            showError("Fallo de Enrutamiento", "No se pudo renderizar el Dashboard.");
            e.printStackTrace();
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}