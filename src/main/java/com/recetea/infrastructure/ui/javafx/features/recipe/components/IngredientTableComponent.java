package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import com.recetea.core.recipe.application.ports.in.dto.UnitResponse;
import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest.IngredientRequest;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.infrastructure.ui.javafx.shared.notification.NotificationService;
import com.recetea.infrastructure.ui.javafx.utils.AutocompleteHelper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Recipe-form sub-component for editing the ingredient list. Renders a
 * three-input row (ingredient combo, unit combo, quantity) plus an
 * editable {@link TableView} of already-added rows.
 *
 * <p><b>fx:root pattern.</b> The component extends {@link VBox} and
 * loads its own FXML via {@code setRoot(this); setController(this)}.
 * The matching FXML must NOT declare {@code fx:controller} — doing so
 * causes a {@code LoadException} because the loader would try to
 * instantiate a second controller alongside this one.
 *
 * <p><b>DTO boundary.</b> {@link #getIngredientRequests()} returns
 * {@code SaveRecipeRequest.IngredientRequest} instances, never the
 * domain {@code RecipeIngredient}. This keeps the component aligned
 * with the application boundary — controllers feed the result straight
 * into a {@link com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest}
 * without translation.
 *
 * <p><b>ES — </b>Sub-componente del formulario de receta para editar
 * la lista de ingredientes. Renderiza una fila de tres entradas
 * (combo de ingrediente, combo de unidad, cantidad) más una
 * {@link TableView} editable con las filas ya añadidas.
 *
 * <p><b>Patrón fx:root.</b> El componente hereda de {@link VBox} y
 * carga su propio FXML vía {@code setRoot(this); setController(this)}.
 * El FXML correspondiente NO debe declarar {@code fx:controller} —
 * hacerlo provoca una {@code LoadException} porque el loader
 * intentaría instanciar un segundo controlador junto a éste.
 *
 * <p><b>Frontera de DTOs.</b> {@link #getIngredientRequests()}
 * devuelve instancias de
 * {@code SaveRecipeRequest.IngredientRequest}, nunca el
 * {@code RecipeIngredient} del dominio. Esto mantiene el componente
 * alineado con la frontera de aplicación — los controladores
 * alimentan el resultado directamente a un
 * {@link com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest}
 * sin traducción.
 */
public class IngredientTableComponent extends VBox {

    @FXML private ComboBox<IngredientResponse> ingredientComboBox;
    @FXML private ComboBox<UnitResponse> unitComboBox;
    @FXML private TextField quantityField;
    @FXML private TableView<IngredientRequest> ingredientsTable;
    @FXML private TableColumn<IngredientRequest, String> colIngredientName;
    @FXML private TableColumn<IngredientRequest, BigDecimal> colQuantity;
    @FXML private TableColumn<IngredientRequest, String> colUnit;
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;

    private final ObservableList<IngredientRequest> ingredientList = FXCollections.observableArrayList();

    /**
     * Source-of-truth lists for the two combos. {@link AutocompleteHelper#setupSearchableComboBox}
     * wraps each one in its own {@code FilteredList} internally, so the component owns
     * only the source. Both lists are populated once at {@link #init} time — catalogues
     * are immutable at runtime per the project's CLAUDE.md, so no further refresh is needed.
     */
    private final ObservableList<IngredientResponse> allIngredients = FXCollections.observableArrayList();
    private final ObservableList<UnitResponse>       allUnits       = FXCollections.observableArrayList();

    public IngredientTableComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/ingredient_table.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.setResources(I18n.bundle());
        try {
            loader.load();
            setupTable();
            setupSearchableCombos();
            setupSelectionBindings();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure failure: could not instantiate IngredientTableComponent.", e);
        }
    }

    public void init(List<IngredientResponse> ingredients, List<UnitResponse> units) {
        // Mutate the *source* lists so the FilteredLists wired in the constructor see the data.
        allIngredients.setAll(ingredients);
        allUnits.setAll(units);
    }

    private void setupTable() {
        colIngredientName.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().ingredientName()));
        colUnit.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().unitName()));
        colQuantity.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().quantity()));
        ingredientsTable.setItems(ingredientList);
    }

    /**
     * Searchable / autocomplete behaviour for both combos via
     * {@link AutocompleteHelper#setupSearchableComboBox}. Identical configuration for the
     * 85-ingredient catalogue and the 18-unit catalogue: 1-character threshold so typing
     * "S" already narrows the dropdown, 10-row visible cap so a short prefix can't overflow
     * the screen, TAB / ENTER auto-selects the first filtered match, caret stays at end
     * after the popup re-renders. The StringConverter installed by the helper resolves
     * the typed name back to the underlying domain object on commit, so the {@code Add}
     * button's call-chain (which reads {@code combo.getValue()}) keeps working unchanged.
     */
    private void setupSearchableCombos() {
        AutocompleteHelper.setupSearchableComboBox(ingredientComboBox, allIngredients, IngredientResponse::name);
        AutocompleteHelper.setupSearchableComboBox(unitComboBox,       allUnits,       UnitResponse::name);

        // Clear any prior "no match" red border the moment a real value is committed
        // (selection from dropdown OR exact-match Enter/Tab via the StringConverter).
        ingredientComboBox.valueProperty().addListener((obs, o, n) -> {
            if (n != null) AutocompleteHelper.clearDangerState(ingredientComboBox);
        });
        unitComboBox.valueProperty().addListener((obs, o, n) -> {
            if (n != null) AutocompleteHelper.clearDangerState(unitComboBox);
        });
    }

    /**
     * The table's selection state is the single source of truth for "Add vs. Edit" mode.
     * Buttons bind their visible/managed properties to it so layout collapses cleanly when
     * a button is hidden, and a listener mirrors the selection into the input fields.
     */
    private void setupSelectionBindings() {
        ReadOnlyObjectProperty<IngredientRequest> selected =
                ingredientsTable.getSelectionModel().selectedItemProperty();

        btnAdd.visibleProperty().bind(selected.isNull());
        btnAdd.managedProperty().bind(btnAdd.visibleProperty());
        btnUpdate.visibleProperty().bind(selected.isNotNull());
        btnUpdate.managedProperty().bind(btnUpdate.visibleProperty());
        btnDelete.visibleProperty().bind(selected.isNotNull());
        btnDelete.managedProperty().bind(btnDelete.visibleProperty());

        selected.addListener((obs, oldItem, newItem) -> {
            if (newItem == null) {
                clearInputs();
            } else {
                populateFieldsFrom(newItem);
            }
        });
    }

    @FXML
    public void onAddClick() {
        IngredientResponse ing = ingredientComboBox.getValue();
        UnitResponse unit = unitComboBox.getValue();
        BigDecimal qty = parseQuantity(ing, unit);
        if (qty == null) return;

        if (containsIngredient(ing.id(), -1)) {
            showError(I18n.get("error.DUPLICATE_INGREDIENT"));
            return;
        }

        ingredientList.add(new IngredientRequest(ing.id(), unit.id(), qty, ing.name(), unit.name()));
        clearInputs();
    }

    @FXML
    public void onUpdateClick() {
        int index = ingredientsTable.getSelectionModel().getSelectedIndex();
        if (index < 0) return;

        IngredientResponse ing = ingredientComboBox.getValue();
        UnitResponse unit = unitComboBox.getValue();
        BigDecimal qty = parseQuantity(ing, unit);
        if (qty == null) return;

        if (containsIngredient(ing.id(), index)) {
            showError(I18n.get("error.DUPLICATE_INGREDIENT"));
            return;
        }

        ingredientList.set(index, new IngredientRequest(ing.id(), unit.id(), qty, ing.name(), unit.name()));
        ingredientsTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void onDeleteClick() {
        IngredientRequest selected = ingredientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ingredientList.remove(selected);
        ingredientsTable.getSelectionModel().clearSelection();
    }

    /**
     * Clicks that miss every populated row deselect the table, which flips the UI back to
     * Add mode via the selection bindings — no extra Cancel button needed.
     */
    @FXML
    private void onTableClick(MouseEvent event) {
        Node target = event.getPickResult().getIntersectedNode();
        while (target != null && target != ingredientsTable && !(target instanceof TableRow)) {
            target = target.getParent();
        }
        boolean clickedEmptyArea = target == null
                || target == ingredientsTable
                || (target instanceof TableRow<?> row && row.isEmpty());
        if (clickedEmptyArea) {
            ingredientsTable.getSelectionModel().clearSelection();
        }
    }

    public List<IngredientRequest> getIngredients() {
        return List.copyOf(ingredientList);
    }

    /** Live observable list exposed for {@code BooleanBinding} aggregation. Read-only wrapper. */
    public ObservableList<IngredientRequest> getIngredientItems() {
        return FXCollections.unmodifiableObservableList(ingredientList);
    }

    public void loadExistingIngredients(List<IngredientRequest> ingredients) {
        ingredientList.setAll(ingredients);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void populateFieldsFrom(IngredientRequest selected) {
        ingredientComboBox.getItems().stream()
                .filter(i -> i.id().equals(selected.ingredientId()))
                .findFirst().ifPresent(ingredientComboBox::setValue);

        unitComboBox.getItems().stream()
                .filter(u -> u.id().equals(selected.unitId()))
                .findFirst().ifPresent(unitComboBox::setValue);

        quantityField.setText(selected.quantity().toPlainString());
    }

    /**
     * Reads the form fields and surfaces toasts for missing or non-numeric input.
     * Returns {@code null} when validation fails so the caller can short-circuit.
     */
    private BigDecimal parseQuantity(IngredientResponse ing, UnitResponse unit) {
        String qtyText = quantityField.getText().trim();
        if (ing == null || unit == null || qtyText.isEmpty()) {
            showError(I18n.get("ingredient.error.requiredFields"));
            return null;
        }
        try {
            BigDecimal qty = new BigDecimal(qtyText);
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            return qty;
        } catch (NumberFormatException e) {
            showError(I18n.get("ingredient.error.invalidQuantity"));
            return null;
        }
    }

    /** True when {@code id} is already present, ignoring the row at {@code excludeIndex} (use {@code -1} to scan the whole list). */
    private boolean containsIngredient(Object id, int excludeIndex) {
        for (int i = 0; i < ingredientList.size(); i++) {
            if (i == excludeIndex) continue;
            if (ingredientList.get(i).ingredientId().equals(id)) return true;
        }
        return false;
    }

    private void clearInputs() {
        ingredientComboBox.getSelectionModel().clearSelection();
        unitComboBox.getSelectionModel().clearSelection();
        quantityField.clear();
    }

    private void showError(String message) {
        NotificationService.warning(this, message);
    }
}
