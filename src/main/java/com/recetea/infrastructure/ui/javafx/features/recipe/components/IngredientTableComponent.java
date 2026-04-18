package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import com.recetea.core.recipe.application.ports.in.dto.UnitResponse;
import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest.IngredientRequest;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Componente visual modular que gestiona la composición de ingredientes de una receta.
 * Actúa como un Inbound Adapter que encapsula la lógica de validación de entrada
 * y mantiene la integridad del estado local antes de la persistencia.
 */
public class IngredientTableComponent extends VBox {

    @FXML private ComboBox<IngredientResponse> ingredientComboBox;
    @FXML private ComboBox<UnitResponse> unitComboBox;
    @FXML private TextField quantityField;
    @FXML private TableView<IngredientRequest> ingredientsTable;

    // Estos IDs deben coincidir exactamente con el FXML
    @FXML private TableColumn<IngredientRequest, String> colIngredientName;
    @FXML private TableColumn<IngredientRequest, BigDecimal> colQuantity;
    @FXML private TableColumn<IngredientRequest, String> colUnit;

    private final ObservableList<IngredientRequest> ingredientList = FXCollections.observableArrayList();

    /**
     * Inicializa el componente mediante Self-Binding.
     * Carga el grafo de escena y establece la instancia actual como raíz y controlador,
     * garantizando el aislamiento del componente en el Scene Graph.
     */
    public IngredientTableComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/ingredient_table.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
            setupTable();
            setupConverters();
        } catch (IOException e) {
            throw new RuntimeException("Fallo de infraestructura: No se pudo instanciar IngredientTableComponent.", e);
        }
    }

    /**
     * Hidrata los catálogos de selección con los datos de ingredientes y unidades disponibles.
     */
    public void init(List<IngredientResponse> ingredients, List<UnitResponse> units) {
        ingredientComboBox.setItems(FXCollections.observableArrayList(ingredients));
        unitComboBox.setItems(FXCollections.observableArrayList(units));
    }

    /**
     * Configura el mapeo de columnas y la fuente de datos de la tabla.
     * El uso de ReadOnlyObjectWrapper asegura que la proyección visual sea inmutable.
     */
    private void setupTable() {
        colIngredientName.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().ingredientName()));
        colUnit.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().unitName()));
        colQuantity.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().quantity()));
        ingredientsTable.setItems(ingredientList);
    }

    /**
     * Establece los conversores de tipos para la representación nominal en los selectores.
     */
    private void setupConverters() {
        ingredientComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(IngredientResponse i) { return i == null ? "" : i.name(); }
            @Override public IngredientResponse fromString(String s) { return null; }
        });

        unitComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(UnitResponse u) { return u == null ? "" : u.name(); }
            @Override public UnitResponse fromString(String s) { return null; }
        });
    }

    /**
     * Procesa la intención de añadir un nuevo registro.
     */
    @FXML
    public void onAddClick() {
        processInput();
    }

    /**
     * Procesa la intención de actualizar un registro existente.
     */
    @FXML
    public void onUpdateClick() {
        processInput();
    }

    /**
     * Orquesta la validación y transformación de la entrada del usuario.
     * Implementa una lógica de reemplazo para garantizar la unicidad de ingredientes.
     */
    private void processInput() {
        IngredientResponse ing = ingredientComboBox.getValue();
        UnitResponse unit = unitComboBox.getValue();
        String qtyText = quantityField.getText().trim();

        if (ing == null || unit == null || qtyText.isEmpty()) {
            showError("Validación fallida", "Es necesario completar todos los campos del ingrediente.");
            return;
        }

        try {
            BigDecimal qty = new BigDecimal(qtyText);
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

            ingredientList.removeIf(i -> i.ingredientId() == ing.id());
            ingredientList.add(new IngredientRequest(ing.id(), unit.id(), qty, ing.name(), unit.name()));
            clearInputs();
        } catch (NumberFormatException e) {
            showError("Formato inválido", "La cantidad debe ser un valor numérico superior a cero.");
        }
    }

    /**
     * Ejecuta la eliminación del item seleccionado de la colección reactiva.
     */
    @FXML
    public void onDeleteClick() {
        IngredientRequest selected = ingredientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ingredientList.remove(selected);
        }
    }

    /**
     * Sincroniza la selección de la tabla con los campos de entrada para edición.
     */
    @FXML
    private void onTableClick(MouseEvent event) {
        IngredientRequest selected = ingredientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ingredientComboBox.getItems().stream()
                    .filter(i -> i.id() == selected.ingredientId())
                    .findFirst().ifPresent(ingredientComboBox::setValue);

            unitComboBox.getItems().stream()
                    .filter(u -> u.id() == selected.unitId())
                    .findFirst().ifPresent(unitComboBox::setValue);

            quantityField.setText(selected.quantity().toPlainString());
        }
    }

    /**
     * Retorna una proyección inmutable del estado actual de los ingredientes.
     */
    public List<IngredientRequest> getIngredients() {
        return List.copyOf(ingredientList);
    }

    /**
     * Realiza la carga profunda de ingredientes para flujos de edición.
     */
    public void loadExistingIngredients(List<IngredientRequest> ingredients) {
        ingredientList.setAll(ingredients);
    }

    /**
     * Resetea el estado visual de los controles de captura.
     */
    private void clearInputs() {
        ingredientComboBox.getSelectionModel().clearSelection();
        unitComboBox.getSelectionModel().clearSelection();
        quantityField.clear();
    }

    /**
     * Presenta feedback visual ante errores de validación estructural.
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}