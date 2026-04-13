package com.recetea.infrastructure.ui.javafx.components;

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
 * Componente visual reutilizable que encapsula la lógica de gestión de ingredientes dosificados.
 * Al heredar de VBox e inyectar su propio FXML, opera de forma autónoma aislando el estado interno
 * y la validación estructural de las capas superiores. Mantiene agnosticismo absoluto frente
 * a las entidades de negocio al consumir exclusivamente DTOs (Records).
 */
public class IngredientTableComponent extends VBox {

    @FXML private ComboBox<IngredientResponse> ingredientComboBox;
    @FXML private ComboBox<UnitResponse> unitComboBox;
    @FXML private TextField quantityField;
    @FXML private TableView<IngredientRequest> ingredientsTable;
    @FXML private TableColumn<IngredientRequest, String> colIngredientId;
    @FXML private TableColumn<IngredientRequest, String> colUnitId;
    @FXML private TableColumn<IngredientRequest, BigDecimal> colQuantity;

    private final ObservableList<IngredientRequest> internalList = FXCollections.observableArrayList();

    /**
     * Inicializa el componente cargando su estructura visual de forma síncrona.
     * Configura el componente actual como raíz (Root) y controlador (Controller) del grafo de escena FXML.
     */
    public IngredientTableComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/javafx/fxml/components/ingredient_table.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Fallo crítico de UI: No se pudo cargar el FXML del componente IngredientTableComponent.", e);
        }
    }

    /**
     * Configura el mapeo de datos entre el modelo inmutable y las columnas de la tabla.
     * Este método es invocado automáticamente por el motor de JavaFX tras la inyección de dependencias (@FXML).
     * Se utilizan métodos de acceso directos sobre los Records para extraer descriptores nominales.
     */
    @FXML
    private void initialize() {
        colIngredientId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ingredientName()));
        colUnitId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().unitName()));
        colQuantity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));
        ingredientsTable.setItems(internalList);
    }

    /**
     * Alimenta los selectores desplegables con los datos maestros extraídos de los catálogos.
     * Implementa StringConverters para renderizar correctamente los atributos descriptivos
     * de los Java Records en los componentes visuales de JavaFX.
     *
     * @param availableIngredients Catálogo de ingredientes en formato DTO.
     * @param availableUnits Catálogo de unidades de medida en formato DTO.
     */
    public void setCatalogs(List<IngredientResponse> availableIngredients, List<UnitResponse> availableUnits) {
        ingredientComboBox.setItems(FXCollections.observableArrayList(availableIngredients));
        ingredientComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(IngredientResponse object) {
                return object != null ? object.name() : "";
            }
            @Override
            public IngredientResponse fromString(String string) {
                return null; // Operación unidireccional (solo lectura visual).
            }
        });

        unitComboBox.setItems(FXCollections.observableArrayList(availableUnits));
        unitComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UnitResponse object) {
                return object != null ? object.name() : "";
            }
            @Override
            public UnitResponse fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Expone el estado interno del componente para que el controlador padre
     * pueda recolectar los datos y ensamblar el request de mutación final.
     *
     * @return Lista inmutable de los ingredientes añadidos actualmente en la tabla.
     */
    public List<IngredientRequest> getIngredients() {
        return List.copyOf(internalList);
    }

    /**
     * Hidrata el estado interno del componente con datos preexistentes.
     * Método fundamental para inicializar el componente durante el flujo de edición de entidades.
     *
     * @param ingredients Lista inmutable de ingredientes previamente asociados a la receta.
     */
    public void loadExistingIngredients(List<IngredientRequest> ingredients) {
        internalList.setAll(ingredients);
    }

    /**
     * Agrega un nuevo registro al estado interno verificando la presencia de selecciones
     * válidas y la correcta formatación del valor escalar introducido.
     */
    @FXML
    private void onAddClick() {
        try {
            IngredientResponse ing = ingredientComboBox.getValue();
            UnitResponse unit = unitComboBox.getValue();
            if (ing != null && unit != null && !quantityField.getText().isBlank()) {
                internalList.add(new IngredientRequest(
                        ing.id(), unit.id(), new BigDecimal(quantityField.getText().trim()),
                        ing.name(), unit.name()
                ));
                clearInputs();
            }
        } catch (NumberFormatException e) {
            showError("Validación fallida", "La cantidad ingresada debe ser un valor numérico válido.");
        }
    }

    /**
     * Reemplaza el registro seleccionado en la tabla asegurando la preservación de
     * la integridad de los datos en la posición correspondiente del índice.
     */
    @FXML
    private void onUpdateClick() {
        int selectedIndex = ingredientsTable.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            try {
                IngredientResponse ing = ingredientComboBox.getValue();
                UnitResponse unit = unitComboBox.getValue();
                if (ing != null && unit != null && !quantityField.getText().isBlank()) {
                    internalList.set(selectedIndex, new IngredientRequest(
                            ing.id(), unit.id(), new BigDecimal(quantityField.getText().trim()),
                            ing.name(), unit.name()
                    ));
                    clearInputs();
                }
            } catch (NumberFormatException e) {
                showError("Validación fallida", "La cantidad ingresada debe ser un valor numérico válido.");
            }
        }
    }

    /**
     * Elimina atómicamente el registro actualmente seleccionado en la lista interna.
     */
    @FXML
    private void onDeleteClick() {
        IngredientRequest selected = ingredientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            internalList.remove(selected);
            clearInputs();
        }
    }

    /**
     * Orquesta la selección de filas transfiriendo los valores inmutables del registro
     * hacia los campos de entrada editables del formulario inferior.
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

            quantityField.setText(selected.quantity().toString());
        }
    }

    /**
     * Limpia los valores transitorios del área de entrada.
     */
    private void clearInputs() {
        ingredientComboBox.getSelectionModel().clearSelection();
        unitComboBox.getSelectionModel().clearSelection();
        quantityField.clear();
    }

    /**
     * Delega las notificaciones de validación fallida a la interfaz gráfica del usuario.
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}