package com.recetea.infrastructure.ui.controllers;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.infrastructure.ui.services.NavigationService;
import com.recetea.infrastructure.ui.services.RecipeServiceContext;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;

/**
 * Controller de Detalle: Muestra la información completa de una receta seleccionada.
 * Implementa la inyección de dependencias mediante el método init.
 */
public class RecipeDetailController {

    @FXML private Label titleLabel, prepTimeLabel, servingsLabel, descriptionLabel;
    @FXML private TableView<RecipeIngredient> ingredientsTable;
    @FXML private TableColumn<RecipeIngredient, String> colIngredientId, colUnitId;
    @FXML private TableColumn<RecipeIngredient, BigDecimal> colQuantity;

    private RecipeServiceContext context;
    private NavigationService nav;

    /**
     * Inicialización de dependencias delegada por el NavigationService.
     */
    public void init(RecipeServiceContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
    }

    @FXML
    public void initialize() {
        // Mapeamos a los nombres descriptivos para mejorar la experiencia de usuario (UX)
        colIngredientId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getIngredientName()));
        colUnitId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getUnitName()));
        colQuantity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getQuantity()));
    }

    /**
     * Carga los detalles de la receta desde el caso de uso y actualiza los labels.
     */
    public void loadRecipeDetails(int recipeId) {
        context.getRecipeById().execute(recipeId).ifPresent(recipe -> {
            titleLabel.setText(recipe.getTitle());
            prepTimeLabel.setText(recipe.getPreparationTimeMinutes() + " min");
            servingsLabel.setText(String.valueOf(recipe.getServings()));
            descriptionLabel.setText(recipe.getDescription());

            // Llenamos la tabla con los ingredientes hidratados
            ingredientsTable.setItems(FXCollections.observableArrayList(recipe.getIngredients()));
        });
    }

    /**
     * Navegación de vuelta al catálogo principal.
     */
    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }
}