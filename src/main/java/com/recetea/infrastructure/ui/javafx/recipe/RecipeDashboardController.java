package com.recetea.infrastructure.ui.javafx.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.infrastructure.ui.javafx.shared.NavigationService;
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
 * Controlador principal del catálogo de recetas.
 * Actúa como un Inbound Adapter que consume el Data Transfer Object (DTO) inmutable
 * expuesto por los Use Cases para proyectarlo en el TableView.
 * Garantiza el aislamiento de la UI al no tener dependencias con las entidades del Core.
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
     * Inicializa las dependencias externas del controlador mediante inyección delegada.
     * Vincula la vista con los casos de uso (Context) y el motor de enrutamiento (Routing).
     */
    public void init(RecipeContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
    }

    @FXML
    public void initialize() {
        recipeTable.setOnMouseClicked(this::handleTableDoubleClick);
        setupActionsColumn();
    }

    /**
     * Hidrata la tabla de datos solicitando el catálogo de DTOs al Use Case.
     * Configura el Data Binding reactivo utilizando los accessors del Java Record
     * para mapear las propiedades de lectura hacia las columnas.
     */
    public void loadData() {
        if (context != null) {
            idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
            titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title()));
            prepColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().prepTimeMinutes()));
            servingsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().servings()));

            List<RecipeSummaryResponse> recipes = context.getAllRecipes().execute();
            recipeTable.setItems(FXCollections.observableArrayList(recipes));
        }
    }

    /**
     * Genera dinámicamente controles de acción (Edición/Borrado) para cada registro de la tabla.
     * Emplea un CellFactory para renderizar los botones y asignar los callbacks asíncronos.
     */
    private void setupActionsColumn() {
        Callback<TableColumn<RecipeSummaryResponse, Void>, TableCell<RecipeSummaryResponse, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnDelete = new Button("Borrar");
            private final Button btnEdit = new Button("Editar");
            private final HBox container = new HBox(10, btnEdit, btnDelete);

            {
                btnDelete.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                btnEdit.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

                btnDelete.setOnAction(event -> {
                    RecipeSummaryResponse recipe = getTableView().getItems().get(getIndex());
                    handleDeleteAction(recipe);
                });

                btnEdit.setOnAction(event -> {
                    RecipeSummaryResponse recipe = getTableView().getItems().get(getIndex());
                    // Delega la navegación pasando exclusivamente el identificador primitivo
                    nav.toRecipeEditor(recipe.id());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        };
        actionsColumn.setCellFactory(cellFactory);
    }

    /**
     * Orquesta el flujo de eliminación de un registro.
     * Emite un prompt de confirmación y, en caso afirmativo, transmite el ID al Core
     * para la mutación del estado antes de rehidratar la vista.
     */
    private void handleDeleteAction(RecipeSummaryResponse recipe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Borrar '" + recipe.title() + "'?", ButtonType.OK, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                context.deleteRecipe().execute(recipe.id());
                loadData();
            }
        });
    }

    /**
     * Intercepta eventos de doble clic sobre la cuadrícula de datos.
     * Captura el identificador del Record seleccionado e invoca el enrutamiento
     * hacia la vista detallada de la entidad.
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
     * Punto de entrada para la orquestación del flujo de creación.
     * Indica al NavigationService la transición hacia un entorno de edición inicializado en vacío.
     */
    @FXML
    public void onNewRecipeClick() {
        nav.toRecipeEditor(null);
    }
}