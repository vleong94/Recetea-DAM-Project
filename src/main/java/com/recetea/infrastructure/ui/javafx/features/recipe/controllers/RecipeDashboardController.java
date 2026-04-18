package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.util.List;

public class RecipeDashboardController {

    @FXML private TableView<RecipeSummaryResponse> recipeTable;
    @FXML private TableColumn<RecipeSummaryResponse, Integer> idColumn;
    @FXML private TableColumn<RecipeSummaryResponse, String> titleColumn;
    @FXML private TableColumn<RecipeSummaryResponse, String> categoryColumn;
    @FXML private TableColumn<RecipeSummaryResponse, String> difficultyColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Integer> prepColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Integer> servingsColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Void> actionsColumn;

    private RecipeQueryProvider queryProvider;
    private NavigationService nav;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        titleColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().title()));
        categoryColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().categoryName()));
        difficultyColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().difficultyName()));
        prepColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().prepTimeMinutes()));
        servingsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().servings()));
    }

    public void init(RecipeQueryProvider queryProvider, NavigationService nav) {
        this.queryProvider = queryProvider;
        this.nav = nav;
        setupActions();
        loadData();
    }

    private void setupActions() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Editar");
            private final Button deleteBtn = new Button("Borrar");
            private final HBox container = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setOnAction(event -> {
                    RecipeSummaryResponse recipe = getTableView().getItems().get(getIndex());
                    handleEditAction(recipe);
                });
                deleteBtn.setOnAction(event -> {
                    RecipeSummaryResponse recipe = getTableView().getItems().get(getIndex());
                    handleDeleteAction(recipe);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        recipeTable.setOnMouseClicked(this::handleTableDoubleClick);
    }

    public void loadData() {
        List<RecipeSummaryResponse> recipes = queryProvider.getAllRecipes().execute();
        recipeTable.setItems(FXCollections.observableArrayList(recipes));
    }

    @FXML
    public void onCreateButtonClick() {
        nav.toRecipeCreate();
    }

    private void handleEditAction(RecipeSummaryResponse recipe) {
        queryProvider.getRecipeById().execute(recipe.id()).ifPresent(nav::toRecipeUpdate);
    }

    private void handleDeleteAction(RecipeSummaryResponse recipe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Estás seguro de que deseas eliminar la receta: " + recipe.title() + "?");
        alert.setHeaderText("Confirm Delete");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    nav.deleteRecipe(recipe.id());
                    loadData();
                } catch (Exception e) {
                    showError("Delete Failure", "No se pudo completar la operación: " + e.getMessage());
                }
            }
        });
    }

    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            RecipeSummaryResponse selected = recipeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                nav.toRecipeDetail(selected.id());
            }
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
