package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.domain.RecipeStep;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class StepTableComponent extends VBox {

    @FXML private TextArea instructionArea;
    @FXML private TableView<RecipeStep> stepsTable;
    @FXML private TableColumn<RecipeStep, Integer> colOrder;
    @FXML private TableColumn<RecipeStep, String> colInstruction;

    private final ObservableList<RecipeStep> stepsData = FXCollections.observableArrayList();

    public StepTableComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/step_table.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar StepTableComponent.", e);
        }
        setupTable();
    }

    private void setupTable() {
        colOrder.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(cellData.getValue().stepOrder()));

        colInstruction.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().instruction()));

        stepsTable.setItems(stepsData);
    }

    @FXML
    private void onAddStepClick() {
        String text = instructionArea.getText();
        if (text != null && !text.trim().isEmpty()) {
            stepsData.add(new RecipeStep(stepsData.size() + 1, text.trim()));
            instructionArea.clear();
        }
    }

    @FXML
    private void onUpdateStepClick() {
        RecipeStep selected = stepsTable.getSelectionModel().getSelectedItem();
        int index = stepsTable.getSelectionModel().getSelectedIndex();
        if (selected != null && instructionArea.getText() != null) {
            stepsData.set(index, new RecipeStep(selected.stepOrder(), instructionArea.getText().trim()));
            instructionArea.clear();
        }
    }

    @FXML
    private void onDeleteStepClick() {
        int index = stepsTable.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            stepsData.remove(index);
            reorderSteps();
        }
    }

    private void reorderSteps() {
        List<RecipeStep> reordered = stepsData.stream()
                .map(s -> new RecipeStep(stepsData.indexOf(s) + 1, s.instruction()))
                .collect(Collectors.toList());
        stepsData.setAll(reordered);
    }

    public void loadSteps(List<RecipeDetailResponse.RecipeStepResponse> steps) {
        stepsData.clear();
        if (steps != null) {
            steps.forEach(s -> stepsData.add(new RecipeStep(s.stepOrder(), s.instruction())));
        }
    }

    public List<RecipeStep> getSteps() {
        return List.copyOf(stepsData);
    }

    @FXML
    private void onTableClick() {
        RecipeStep selected = stepsTable.getSelectionModel().getSelectedItem();
        if (selected != null) instructionArea.setText(selected.instruction());
    }
}