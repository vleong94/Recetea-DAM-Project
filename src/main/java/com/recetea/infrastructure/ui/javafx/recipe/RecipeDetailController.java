package com.recetea.infrastructure.ui.javafx.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.infrastructure.ui.javafx.shared.NavigationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;

/**
 * Controller de solo lectura para la vista de detalle de recetas.
 * Actúa como un Inbound Adapter pasivo que consume datos estructurados (DTOs)
 * provenientes de los Use Cases y los proyecta en los componentes visuales.
 * Garantiza el aislamiento del Domain al trabajar únicamente con Records inmutables.
 */
public class RecipeDetailController {

    @FXML private Label titleLabel;
    @FXML private Label prepTimeLabel;
    @FXML private Label servingsLabel;
    @FXML private Label descriptionLabel;

    /**
     * Tabla de composición de ingredientes.
     * Utiliza el DTO RecipeIngredientResponse para asegurar que la UI sea agnóstica
     * a la lógica interna de los Aggregate Roots.
     */
    @FXML private TableView<RecipeIngredientResponse> ingredientsTable;
    @FXML private TableColumn<RecipeIngredientResponse, String> colIngredientId;
    @FXML private TableColumn<RecipeIngredientResponse, String> colUnitId;
    @FXML private TableColumn<RecipeIngredientResponse, BigDecimal> colQuantity;

    private RecipeContext context;
    private NavigationService nav;

    /**
     * Dependency Injection delegada.
     * Vincula el Controller con el Context de ejecución y el motor de Routing.
     */
    public void init(RecipeContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
    }

    /**
     * Configura el Data Binding reactivo para las columnas del TableView.
     * Extrae los valores utilizando los accessors directos del Java Record para
     * popular la UI de forma eficiente.
     */
    @FXML
    public void initialize() {
        colIngredientId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ingredientName()));
        colUnitId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().unitName()));
        colQuantity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));
    }

    /**
     * Orquesta la recuperación y renderizado del payload de la receta.
     * Ejecuta la Query al Core mediante el ID proporcionado y utiliza los
     * accessors del Record de respuesta para hidratar el FXML.
     *
     * @param recipeId Identificador unívoco del registro consultado.
     */
    public void loadRecipeDetails(int recipeId) {
        context.getRecipeById().execute(recipeId).ifPresent(recipe -> {
            titleLabel.setText(recipe.title());
            prepTimeLabel.setText(recipe.prepTimeMinutes() + " min");
            servingsLabel.setText(String.valueOf(recipe.servings()));
            descriptionLabel.setText(recipe.description());

            // Actualiza la colección del TableView con la lista inmutable de Records del Core.
            ingredientsTable.setItems(FXCollections.observableArrayList(recipe.ingredients()));
        });
    }

    /**
     * Transfiere el control de flujo al NavigationService para retornar al Dashboard.
     */
    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }
}