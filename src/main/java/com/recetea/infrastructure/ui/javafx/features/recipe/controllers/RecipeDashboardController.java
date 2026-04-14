package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeContext;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Controller del Dashboard principal de recetas.
 * Actúa como un Inbound Adapter que proyecta el catálogo global
 * utilizando Data Transfer Objects (DTOs) inmutables.
 * Gestiona la interacción del usuario y orquesta el Routing hacia
 * las funcionalidades de detalle, creación y edición.
 */
public class RecipeDashboardController {

    @FXML private TableView<RecipeSummaryResponse> recipeTable;
    @FXML private TableColumn<RecipeSummaryResponse, Integer> idColumn;
    @FXML private TableColumn<RecipeSummaryResponse, String> titleColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Integer> prepColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Integer> servingsColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Void> actionsColumn;

    private RecipeContext context;
    private NavigationService nav;

    /**
     * Inicializa el Controller inyectando el Context de los Use Cases
     * y el Navigation Service. Configura el Layout de la tabla
     * y dispara la carga inicial de datos desde la capa de Persistence.
     */
    public void init(RecipeContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
        setupTable();
        loadData();
    }

    /**
     * Define la lógica del View de la tabla mediante Cell Factories.
     * Vincula las columnas con los Properties de los DTOs
     * y construye dinámicamente la columna de acciones con Buttons.
     */
    private void setupTable() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        titleColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().title()));
        prepColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().prepTimeMinutes()));
        servingsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().servings()));

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

    /**
     * Recupera el State actual del catálogo mediante el Use Case correspondiente.
     * Actualiza la Observable List de la tabla, reflejando cualquier cambio
     * realizado en la Database de forma reactiva.
     */
    public void loadData() {
        List<RecipeSummaryResponse> recipes = context.getAllRecipes().execute();
        recipeTable.setItems(FXCollections.observableArrayList(recipes));
    }

    /**
     * Transfiere el Thread de la UI hacia el formulario de creación.
     */
    @FXML
    public void onCreateButtonClick() {
        nav.toRecipeEditor();
    }

    /**
     * Inicia el flujo de edición recuperando la Entity completa por su ID.
     * El Controller delega la transición al Navigation Service,
     * asegurando que el editor reciba el State hidratado.
     */
    private void handleEditAction(RecipeSummaryResponse recipe) {
        context.getRecipeById().execute(recipe.id()).ifPresent(nav::toRecipeEditor);
    }

    /**
     * Gestiona la eliminación de un Record solicitando confirmación previa.
     * Si se acepta, invoca el Use Case de borrado y
     * refresca el View para garantizar la consistencia visual.
     */
    private void handleDeleteAction(RecipeSummaryResponse recipe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Estás seguro de que deseas eliminar la receta: " + recipe.title() + "?");
        alert.setHeaderText("Confirmar eliminación");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    context.deleteRecipe().execute(recipe.id());
                    loadData();
                } catch (Exception e) {
                    showError("Error al eliminar", "No se pudo completar la operación: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Event Handler para capturar el Double Click y visualizar el detalle.
     */
    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            RecipeSummaryResponse selected = recipeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                nav.toRecipeDetail(selected.id());
            }
        }
    }

    /**
     * Presenta Popups de error informativos ante fallos en la Application Layer.
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}