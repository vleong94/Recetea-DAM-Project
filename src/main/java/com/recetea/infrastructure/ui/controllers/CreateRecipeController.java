package com.recetea.infrastructure.ui.controllers;

import com.recetea.core.domain.*;
import com.recetea.core.ports.in.dto.CreateRecipeCommand;
import com.recetea.infrastructure.ui.services.NavigationService;
import com.recetea.infrastructure.ui.services.RecipeServiceContext;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Controller del Editor de Recetas: Gestiona la creación y edición de recetas.
 * Se comunica con el dominio mediante DTOs (Commands) y delega la navegación.
 */
public class CreateRecipeController {

    @FXML private TextField titleField, prepTimeField, servingsField, quantityField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Ingredient> ingredientComboBox;
    @FXML private ComboBox<Unit> unitComboBox;
    @FXML private TableView<CreateRecipeCommand.IngredientCommand> ingredientsTable;
    @FXML private TableColumn<CreateRecipeCommand.IngredientCommand, String> colIngredientId, colUnitId;
    @FXML private TableColumn<CreateRecipeCommand.IngredientCommand, BigDecimal> colQuantity;

    private final ObservableList<CreateRecipeCommand.IngredientCommand> temporaryIngredients = FXCollections.observableArrayList();
    private RecipeServiceContext context;
    private NavigationService nav;
    private int currentRecipeId;
    private boolean isEditMode = false;

    /**
     * Inicialización de dependencias inyectadas desde el NavigationService.
     */
    public void init(RecipeServiceContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
        loadCatalogs();
    }

    @FXML
    public void initialize() {
        // Mapeo de columnas usando los nombres descriptivos del Record
        colIngredientId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ingredientName()));
        colUnitId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().unitName()));
        colQuantity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));

        ingredientsTable.setItems(temporaryIngredients);
    }

    private void loadCatalogs() {
        ingredientComboBox.setItems(FXCollections.observableArrayList(context.getAllIngredients().execute()));
        unitComboBox.setItems(FXCollections.observableArrayList(context.getAllUnits().execute()));
    }

    // --- GESTIÓN DE INGREDIENTES (MÉTODOS REQUERIDOS POR FXML) ---

    @FXML
    public void onAddIngredientClick() {
        try {
            Ingredient ing = ingredientComboBox.getValue();
            Unit unit = unitComboBox.getValue();
            if (ing != null && unit != null && !quantityField.getText().isEmpty()) {
                // Constructor de 5 parámetros para soportar nombres en la UI
                temporaryIngredients.add(new CreateRecipeCommand.IngredientCommand(
                        ing.getId(), unit.getId(), new BigDecimal(quantityField.getText()),
                        ing.getName(), unit.getAbbreviation()));
                clearIngredientFields();
            }
        } catch (Exception e) {
            showError("Error", "La cantidad debe ser un número válido.");
        }
    }

    /**
     * Resuelve el LoadException:onUpdateIngredientClick.
     */
    @FXML
    public void onUpdateIngredientClick() {
        int selectedIndex = ingredientsTable.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            Ingredient ing = ingredientComboBox.getValue();
            Unit unit = unitComboBox.getValue();
            if (ing != null && unit != null) {
                temporaryIngredients.set(selectedIndex, new CreateRecipeCommand.IngredientCommand(
                        ing.getId(), unit.getId(), new BigDecimal(quantityField.getText()),
                        ing.getName(), unit.getAbbreviation()));
                clearIngredientFields();
            }
        } else {
            showError("Información", "Selecciona una fila de la tabla para actualizarla.");
        }
    }

    @FXML
    public void onDeleteIngredientClick() {
        CreateRecipeCommand.IngredientCommand selected = ingredientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) temporaryIngredients.remove(selected);
    }

    @FXML
    public void onTableMouseClicked(MouseEvent event) {
        CreateRecipeCommand.IngredientCommand selected = ingredientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Seleccionamos en los combos el ingrediente y unidad de la fila
            ingredientComboBox.getItems().stream()
                    .filter(i -> i.getId() == selected.ingredientId())
                    .findFirst().ifPresent(ingredientComboBox::setValue);
            unitComboBox.getItems().stream()
                    .filter(u -> u.getId() == selected.unitId())
                    .findFirst().ifPresent(unitComboBox::setValue);
            quantityField.setText(selected.quantity().toString());
        }
    }

    // --- PERSISTENCIA Y FLUJO ---

    @FXML
    public void onSaveButtonClick() {
        try {
            // Generamos el DTO de creación/edición
            CreateRecipeCommand cmd = new CreateRecipeCommand(
                    1, 1, 1, titleField.getText(), descriptionArea.getText(),
                    Integer.parseInt(prepTimeField.getText()), Integer.parseInt(servingsField.getText()),
                    new ArrayList<>(temporaryIngredients));

            if (isEditMode) context.updateRecipe().execute(currentRecipeId, cmd);
            else context.createRecipe().execute(cmd);

            nav.toDashboard(); // Navegación delegada
        } catch (Exception e) {
            showError("Error al guardar", "Verifica que todos los campos numéricos sean correctos.");
        }
    }

    @FXML public void onBackButtonClick() { nav.toDashboard(); }

    private void clearIngredientFields() {
        ingredientComboBox.getSelectionModel().clearSelection();
        unitComboBox.getSelectionModel().clearSelection();
        quantityField.clear();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Hidrata el formulario cuando venimos de una acción de "Editar" en el Dashboard.
     */
    public void loadRecipeData(Recipe recipe) {
        this.isEditMode = true;
        this.currentRecipeId = recipe.getId();
        titleField.setText(recipe.getTitle());
        prepTimeField.setText(String.valueOf(recipe.getPreparationTimeMinutes()));
        servingsField.setText(String.valueOf(recipe.getServings()));
        descriptionArea.setText(recipe.getDescription());

        // Mapeamos los RecipeIngredient de la entidad a IngredientCommand del DTO
        temporaryIngredients.setAll(recipe.getIngredients().stream()
                .map(i -> new CreateRecipeCommand.IngredientCommand(
                        i.getIngredientId(), i.getUnitId(), i.getQuantity(),
                        i.getIngredientName(), i.getUnitName()))
                .collect(Collectors.toList()));
    }
}