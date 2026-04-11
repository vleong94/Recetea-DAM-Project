package com.recetea.infrastructure.ui;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.in.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    @FXML private TableView<Recipe> recipeTable;
    @FXML private TableColumn<Recipe, Integer> idColumn;
    @FXML private TableColumn<Recipe, String> titleColumn;
    @FXML private TableColumn<Recipe, Integer> prepColumn;
    @FXML private TableColumn<Recipe, Integer> servingsColumn;
    @FXML private TableColumn<Recipe, Void> actionsColumn;

    private IGetAllRecipesUseCase getAllRecipesUseCase;
    private ICreateRecipeUseCase createRecipeUseCase;
    private IGetRecipeByIdUseCase getRecipeByIdUseCase;
    private IDeleteRecipeUseCase deleteRecipeUseCase;
    private IUpdateRecipeUseCase updateRecipeUseCase;

    // --- SETTERS PARA INYECCIÓN ---
    public void setGetAllRecipesUseCase(IGetAllRecipesUseCase useCase) { this.getAllRecipesUseCase = useCase; }
    public void setCreateRecipeUseCase(ICreateRecipeUseCase useCase) { this.createRecipeUseCase = useCase; }
    public void setGetRecipeByIdUseCase(IGetRecipeByIdUseCase useCase) { this.getRecipeByIdUseCase = useCase; }
    public void setDeleteRecipeUseCase(IDeleteRecipeUseCase useCase) { this.deleteRecipeUseCase = useCase; }
    public void setUpdateRecipeUseCase(IUpdateRecipeUseCase useCase) { this.updateRecipeUseCase = useCase; }

    @FXML
    public void initialize() {
        recipeTable.setOnMouseClicked(this::handleTableDoubleClick);
        setupActionsColumn();
    }

    /**
     * Configura la columna de acciones con botones para Editar y Borrar.
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
                    handleEditAction(recipe, event);
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
     * Gestiona la navegación al formulario en modo EDICIÓN.
     */
    private void handleEditAction(Recipe recipe, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/create_recipe.fxml"));
            Parent root = loader.load();

            CreateRecipeController controller = loader.getController();

            // Sincronización de los 5 casos de uso para mantener el estado de la APP
            controller.setUseCases(
                    this.createRecipeUseCase,
                    this.getAllRecipesUseCase,
                    this.updateRecipeUseCase,
                    this.getRecipeByIdUseCase,
                    this.deleteRecipeUseCase
            );

            // Disparo del Modo Edición
            controller.loadRecipeData(recipe);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("Recetea - Editando: " + recipe.getTitle());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gestiona el borrado físico de la receta con confirmación del usuario.
     */
    private void handleDeleteAction(Recipe recipe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("¿Estás seguro de borrar '" + recipe.getTitle() + "'?");
        alert.setContentText("Esta acción es irreversible y borrará todos los ingredientes asociados.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteRecipeUseCase.execute(recipe.getId());
            loadData();
        }
    }

    /**
     * Carga o refresca el catálogo de recetas desde el Repositorio.
     */
    public void loadData() {
        if (getAllRecipesUseCase != null) {
            idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
            titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
            prepColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getPreparationTimeMinutes()));
            servingsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getServings()));

            List<Recipe> recipes = getAllRecipesUseCase.execute();
            recipeTable.setItems(FXCollections.observableArrayList(recipes));
        }
    }

    /**
     * Navegación al visor de detalles mediante doble clic.
     */
    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Recipe selected = recipeTable.getSelectionModel().getSelectedItem();
            if (selected != null) navigateToRecipeDetail(selected.getId(), event);
        }
    }

    /**
     * Navegación al formulario en modo CREACIÓN.
     */
    @FXML
    public void onNewRecipeClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/create_recipe.fxml"));
            Parent root = loader.load();
            CreateRecipeController controller = loader.getController();

            // Sincronización de los 5 casos de uso
            controller.setUseCases(
                    this.createRecipeUseCase,
                    this.getAllRecipesUseCase,
                    this.updateRecipeUseCase,
                    this.getRecipeByIdUseCase,
                    this.deleteRecipeUseCase
            );

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("Recetea - Nueva Receta");
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Navegación al visor de detalles (Solo lectura).
     */
    private void navigateToRecipeDetail(int recipeId, MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/recipe_detail.fxml"));
            Parent root = loader.load();
            RecipeDetailController controller = loader.getController();
            controller.setUseCases(getRecipeByIdUseCase, getAllRecipesUseCase, createRecipeUseCase);
            controller.loadRecipeDetails(recipeId);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 700, 600));
            stage.setTitle("Recetea - Detalles de la Receta");
        } catch (IOException e) { e.printStackTrace(); }
    }
}