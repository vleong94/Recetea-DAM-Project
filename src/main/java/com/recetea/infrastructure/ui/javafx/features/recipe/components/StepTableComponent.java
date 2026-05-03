package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.RecipeStep;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Recipe-form sub-component for editing the step list. {@link TextArea}
 * for the instruction body plus a sortable {@link TableView} of
 * already-added steps.
 *
 * <p>{@link #getStepRequests()} exposes the result as
 * {@code SaveRecipeRequest.StepRequest} (DTO boundary). Step ordinals
 * are 1-based and assigned by row index — the user re-orders by
 * delete + re-add rather than drag-and-drop. Same {@code fx:root}
 * pattern as {@link IngredientTableComponent}.
 *
 * <p><b>ES — </b>Sub-componente del formulario de receta para editar
 * la lista de pasos. {@link TextArea} para el cuerpo de la
 * instrucción más una {@link TableView} ordenable de los pasos ya
 * añadidos.
 *
 * <p>{@link #getStepRequests()} expone el resultado como
 * {@code SaveRecipeRequest.StepRequest} (frontera de DTO). Los
 * ordinales de paso son 1-based y se asignan por índice de fila —
 * el usuario reordena con delete + re-add en lugar de drag-and-drop.
 * Mismo patrón {@code fx:root} que
 * {@link IngredientTableComponent}.
 */
public class StepTableComponent extends VBox {

    @FXML private TextArea instructionArea;
    @FXML private TableView<RecipeStep> stepsTable;
    @FXML private TableColumn<RecipeStep, Integer> colOrder;
    @FXML private TableColumn<RecipeStep, String> colInstruction;
    @FXML private Button btnAddStep;
    @FXML private Button btnUpdateStep;
    @FXML private Button btnDeleteStep;

    private final ObservableList<RecipeStep> stepsData = FXCollections.observableArrayList();

    public StepTableComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/step_table.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.setResources(I18n.bundle());
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure Failure: Cannot instantiate StepTableComponent.", e);
        }
        setupTable();
        setupSelectionBindings();
    }

    private void setupTable() {
        colOrder.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().stepOrder()));

        colInstruction.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().instruction()));

        stepsTable.setItems(stepsData);
    }

    /**
     * Selection state in {@code stepsTable} drives "Add vs. Edit" mode. The three buttons
     * bind their visible/managed properties to the selection model so layout collapses
     * cleanly, and a listener keeps the {@code instructionArea} in sync with the row.
     * Empty-area click + post-action {@code clearSelection()} replace any explicit Cancel.
     */
    private void setupSelectionBindings() {
        ReadOnlyObjectProperty<RecipeStep> selected =
                stepsTable.getSelectionModel().selectedItemProperty();

        btnAddStep.visibleProperty().bind(selected.isNull());
        btnAddStep.managedProperty().bind(btnAddStep.visibleProperty());
        btnUpdateStep.visibleProperty().bind(selected.isNotNull());
        btnUpdateStep.managedProperty().bind(btnUpdateStep.visibleProperty());
        btnDeleteStep.visibleProperty().bind(selected.isNotNull());
        btnDeleteStep.managedProperty().bind(btnDeleteStep.visibleProperty());

        selected.addListener((obs, oldStep, newStep) -> {
            if (newStep == null) {
                instructionArea.clear();
            } else {
                instructionArea.setText(newStep.instruction());
            }
        });
    }

    @FXML
    private void onAddStepClick() {
        String text = instructionArea.getText();
        if (text == null || text.trim().isEmpty()) return;
        stepsData.add(new RecipeStep(stepsData.size() + 1, text.trim()));
        instructionArea.clear();
    }

    @FXML
    private void onUpdateStepClick() {
        RecipeStep selected = stepsTable.getSelectionModel().getSelectedItem();
        int index = stepsTable.getSelectionModel().getSelectedIndex();
        if (selected == null || index < 0) return;
        String text = instructionArea.getText();
        if (text == null || text.trim().isEmpty()) return;

        // Preserve the step's original order — only the instruction text mutates here.
        stepsData.set(index, new RecipeStep(selected.stepOrder(), text.trim()));
        stepsTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void onDeleteStepClick() {
        int index = stepsTable.getSelectionModel().getSelectedIndex();
        if (index < 0) return;
        stepsData.remove(index);
        reorderSteps();
        stepsTable.getSelectionModel().clearSelection();
    }

    /**
     * Empty-area click deselects the table; the selection listener then flips the UI back
     * to Add mode and clears the input. Same pattern used by {@code IngredientTableComponent}.
     */
    @FXML
    private void onTableClick(MouseEvent event) {
        Node target = event.getPickResult().getIntersectedNode();
        while (target != null && target != stepsTable && !(target instanceof TableRow)) {
            target = target.getParent();
        }
        boolean clickedEmptyArea = target == null
                || target == stepsTable
                || (target instanceof TableRow<?> row && row.isEmpty());
        if (clickedEmptyArea) {
            stepsTable.getSelectionModel().clearSelection();
        }
    }

    private void reorderSteps() {
        List<RecipeStep> reordered = new ArrayList<>(stepsData.size());
        for (int i = 0; i < stepsData.size(); i++) {
            reordered.add(new RecipeStep(i + 1, stepsData.get(i).instruction()));
        }
        stepsData.setAll(reordered);
    }

    public void loadSteps(List<RecipeDetailResponse.RecipeStepResponse> steps) {
        stepsData.clear();
        if (steps != null) {
            steps.forEach(s -> stepsData.add(new RecipeStep(s.stepOrder(), s.instruction())));
        }
    }

    public List<SaveRecipeRequest.StepRequest> getStepRequests() {
        return stepsData.stream()
                .map(s -> new SaveRecipeRequest.StepRequest(s.stepOrder(), s.instruction()))
                .toList();
    }

    /** Live observable list exposed for {@code BooleanBinding} aggregation. Read-only wrapper. */
    public ObservableList<RecipeStep> getStepItems() {
        return FXCollections.unmodifiableObservableList(stepsData);
    }
}
