package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;

/**
 * Controlador de la interfaz de usuario encargado de la visualización detallada de recetas.
 * Actúa como un adaptador de entrada (Inbound Adapter) de solo lectura que consume proyecciones
 * inmutables de datos (DTOs) para hidratar la vista. Su diseño garantiza que el modelo de
 * dominio permanezca aislado de la capa de presentación, siguiendo los principios de la
 * arquitectura hexagonal y fomentando un mantenimiento sencillo y desacoplado.
 */
public class RecipeDetailController {

    @FXML private Label titleLabel;
    @FXML private Label prepTimeLabel;
    @FXML private Label servingsLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label categoryLabel;
    @FXML private Label difficultyLabel;

    @FXML private TableView<RecipeIngredientResponse> ingredientsTable;
    @FXML private TableColumn<RecipeIngredientResponse, String> colIngredientName;
    @FXML private TableColumn<RecipeIngredientResponse, String> colUnit;
    @FXML private TableColumn<RecipeIngredientResponse, BigDecimal> colQuantity;

    @FXML private TableView<RecipeDetailResponse.RecipeStepResponse> stepsTable;
    @FXML private TableColumn<RecipeDetailResponse.RecipeStepResponse, Integer> colStepOrder;
    @FXML private TableColumn<RecipeDetailResponse.RecipeStepResponse, String> colInstruction;

    private RecipeQueryProvider queryProvider;
    private NavigationService nav;

    public void init(RecipeQueryProvider queryProvider, NavigationService nav) {
        this.queryProvider = queryProvider;
        this.nav = nav;

        colIngredientName.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().ingredientName()));
        colUnit.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().unitName()));
        colQuantity.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().quantity()));
        colStepOrder.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().stepOrder()));
        colInstruction.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().instruction()));
    }

    /**
     * Orquesta la recuperación y presentación de los detalles de una receta.
     * Delega la búsqueda al núcleo de la aplicación mediante el identificador proporcionado y
     * gestiona la respuesta de forma reactiva: si la receta existe, se procede a la hidratación
     * de los elementos visuales; en caso contrario, se notifica el fallo al usuario mediante
     * un diálogo de error.
     *
     * @param recipeId Identificador único de la receta a cargar.
     */
    public void loadRecipeDetails(int recipeId) {
        queryProvider.getRecipeById().execute(recipeId).ifPresentOrElse(
                this::populateView,
                () -> showError("Error de Consulta", "El sistema no pudo localizar la receta solicitada.")
        );
    }

    /**
     * Transfiere el estado de la respuesta inmutable a los componentes de la interfaz de usuario.
     * Centraliza la lógica de asignación de textos y la actualización de la lista observable
     * de ingredientes para mantener la consistencia visual del detalle, aplicando formatos
     * específicos para tiempos y raciones.
     */
    private void populateView(RecipeDetailResponse recipe) {
        titleLabel.setText(recipe.title());
        prepTimeLabel.setText(String.format("%d min", recipe.prepTimeMinutes()));
        servingsLabel.setText(String.valueOf(recipe.servings()));
        descriptionLabel.setText(recipe.description());
        categoryLabel.setText(recipe.categoryName());
        difficultyLabel.setText(recipe.difficultyName());

        ingredientsTable.setItems(FXCollections.observableArrayList(recipe.ingredients()));
        stepsTable.setItems(FXCollections.observableArrayList(recipe.steps()));
    }

    /**
     * Ejecuta la navegación de retorno hacia el panel de control principal (Dashboard).
     */
    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    /**
     * Despliega ventanas emergentes modales para informar sobre errores o inconsistencias
     * detectadas durante la comunicación con la capa de aplicación.
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}