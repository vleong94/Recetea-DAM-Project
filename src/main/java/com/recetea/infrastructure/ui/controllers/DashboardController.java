package com.recetea.infrastructure.ui.controllers;

import com.recetea.core.domain.Recipe;
import com.recetea.infrastructure.ui.services.NavigationService;
import com.recetea.infrastructure.ui.services.RecipeServiceContext;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.util.List;

/**
 * Controller del Dashboard: Gestiona la visualización del catálogo de recetas.
 * Se ha eliminado toda la lógica de carga de FXML para evitar que sea una "Clase Dios".
 */
public class DashboardController {

    @FXML private TableView<Recipe> recipeTable;
    @FXML private TableColumn<Recipe, Integer> idColumn;
    @FXML private TableColumn<Recipe, String> titleColumn;
    @FXML private TableColumn<Recipe, Integer> prepColumn;
    @FXML private TableColumn<Recipe, Integer> servingsColumn;
    @FXML private TableColumn<Recipe, Void> actionsColumn;

    private RecipeServiceContext context;
    private NavigationService nav;

    /**
     * Punto único de entrada de dependencias.
     * Sustituye a los 7 setters anteriores.
     */
    public void init(RecipeServiceContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
    }

    @FXML
    public void initialize() {
        recipeTable.setOnMouseClicked(this::handleTableDoubleClick);
        setupActionsColumn();
    }

    /**
     * Solicita los datos al Contexto y actualiza la UI.
     * Es invocado por el NavigationService al entrar en esta pantalla.
     */
    public void loadData() {
        if (context != null) {
            idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
            titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
            prepColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getPreparationTimeMinutes()));
            servingsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getServings()));

            List<Recipe> recipes = context.getAllRecipes().execute();
            recipeTable.setItems(FXCollections.observableArrayList(recipes));
        }
    }

    /**
     * Configura la columna de botones (Editar/Borrar) delegando acciones.
     */
    private void setupActionsColumn() {
        Callback<TableColumn<Recipe, Void>, TableCell<Recipe, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnDelete = new Button("Borrar");
            private final Button btnEdit = new Button("Editar");
            private final HBox container = new HBox(10, btnEdit, btnDelete);

            {
                btnDelete.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                btnEdit.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

                btnDelete.setOnAction(event -> {
                    Recipe recipe = getTableView().getItems().get(getIndex());
                    handleDeleteAction(recipe);
                });

                btnEdit.setOnAction(event -> {
                    Recipe recipe = getTableView().getItems().get(getIndex());
                    nav.toCreateRecipe(recipe); // Delegación de navegación
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(container);
            }
        };
        actionsColumn.setCellFactory(cellFactory);
    }

    private void handleDeleteAction(Recipe recipe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Borrar '" + recipe.getTitle() + "'?", ButtonType.OK, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                context.deleteRecipe().execute(recipe.getId());
                loadData(); // Refresco local
            }
        });
    }

    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Recipe selected = recipeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                nav.toRecipeDetail(selected.getId()); // Delegación de navegación
            }
        }
    }

    @FXML
    public void onNewRecipeClick() {
        nav.toCreateRecipe(null);
    }
}