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
 * Controlador principal (Dashboard) encargado de proyectar el catálogo de recetas.
 * Opera como un adaptador de entrada (Inbound Adapter) que consume objetos inmutables
 * (DTOs) para poblar la vista, asegurando que el modelo de dominio permanezca aislado.
 * Centraliza el enrutamiento hacia las vistas de detalle, creación y edición,
 * manteniendo el estado de la tabla sincronizado con la persistencia.
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
     * Inicializa el controlador inyectando el contexto de operaciones y el servicio de enrutamiento.
     * Configura el mapeo de columnas y ejecuta la carga inicial de datos desde la infraestructura.
     */
    public void init(RecipeContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
        setupTable();
        loadData();
    }

    /**
     * Configura la representación visual de los datos en la tabla.
     * Define envoltorios de solo lectura (ReadOnlyObjectWrapper) para proteger la inmutabilidad
     * de los DTOs y construye dinámicamente los componentes interactivos de la columna de acciones.
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
     * Ejecuta el caso de uso de consulta global y refresca el estado del componente visual.
     * Garantiza que la lista observable refleje el estado más reciente de la persistencia.
     */
    public void loadData() {
        List<RecipeSummaryResponse> recipes = context.getAllRecipes().execute();
        recipeTable.setItems(FXCollections.observableArrayList(recipes));
    }

    /**
     * Delega el control de la navegación hacia el formulario especializado en la creación de nuevas entidades.
     */
    @FXML
    public void onCreateButtonClick() {
        nav.toRecipeCreate();
    }

    /**
     * Orquesta la transición hacia el entorno de actualización.
     * Coordina la recuperación profunda de la entidad mediante su identificador
     * antes de ceder el control al servicio de navegación, asegurando que el editor
     * reciba el estado inmutable completo para hidratar la vista.
     *
     * @param recipe Proyección resumida de la entidad seleccionada.
     */
    private void handleEditAction(RecipeSummaryResponse recipe) {
        context.getRecipeById().execute(recipe.id()).ifPresent(nav::toRecipeUpdate);
    }

    /**
     * Gestiona el ciclo de vida de eliminación de un registro.
     * Implementa un mecanismo de confirmación previo y, tras la ejecución del caso de uso,
     * sincroniza inmediatamente la vista para reflejar la purga en la base de datos.
     *
     * @param recipe Proyección resumida de la entidad a eliminar.
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
     * Captura interacciones primarias sobre las filas para disparar la vista de detalle
     * de forma ágil sin requerir interacción con la columna de acciones.
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
     * Renderiza notificaciones modales ante excepciones no controladas durante la ejecución
     * de los casos de uso, evitando cierres abruptos de la aplicación.
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}