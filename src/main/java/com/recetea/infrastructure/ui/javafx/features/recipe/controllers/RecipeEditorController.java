package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeContext;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.IngredientTableComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RecipeHeaderComponent;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller principal del flujo de edición y creación de recetas.
 * Actúa como un Inbound Adapter que coordina los Components visuales
 * y los Use Cases del Core, garantizando que el Data Binding y la Persistence
 * se realicen exclusivamente mediante Data Transfer Objects (DTOs) inmutables.
 */
public class RecipeEditorController {

    @FXML private RecipeHeaderComponent headerComponent;
    @FXML private IngredientTableComponent ingredientTableComponent;

    private RecipeContext context;
    private NavigationService nav;
    private int currentRecipeId;
    private boolean isEditMode = false;

    /**
     * Inyecta las dependencias del Context y el Navigation Service.
     * Configura el State inicial propagando el Context a los sub-components
     * para permitir el Lazy Loading de los catálogos maestros en el Thread de la UI.
     */
    public void init(RecipeContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
        this.ingredientTableComponent.init(context);
    }

    /**
     * Orquesta el flujo de Persistence atómica.
     * Ejecuta el Data Validation sobre los Components antes de ensamblar el Request
     * y delegar el Command al Use Case correspondiente de la Application Layer.
     */
    @FXML
    public void onSaveButtonClick() {
        if (!headerComponent.isValid()) {
            showError("Data Integrity Violation", "Los metadatos requeridos (título, tiempo, raciones) son inválidos o están ausentes.");
            return;
        }

        List<SaveRecipeRequest.IngredientRequest> ingredients = ingredientTableComponent.getIngredients();
        if (ingredients.isEmpty()) {
            showError("Data Integrity Violation", "El payload debe contener al menos un elemento en la colección de ingredientes.");
            return;
        }

        // Construcción del Payload.
        // TODO: Eliminar Hardcoding de userId, categoryId y difficultyId al integrar los Módulos de Identity y Taxonomy.
        SaveRecipeRequest request = new SaveRecipeRequest(
                1,
                1,
                1,
                headerComponent.getTitle(),
                headerComponent.getDescription(),
                headerComponent.getPrepTime(),
                headerComponent.getServings(),
                ingredients
        );

        try {
            if (isEditMode) {
                context.updateRecipe().execute(currentRecipeId, request);
            } else {
                context.createRecipe().execute(request);
            }
            nav.toDashboard();
        } catch (Exception e) {
            showError("Persistence Error", "Transacción abortada: " + e.getMessage());
        }
    }

    /**
     * Interrumpe el flujo de entrada de datos y delega el Routing de retorno
     * al Navigation Service, descartando el State no persistido.
     */
    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    /**
     * Hidrata el Controller para el Update State.
     * Procesa el Response inmutable del Core y ejecuta el Data Binding sobre
     * los Components hijos, activando el flag de mutación en la UI.
     */
    public void loadRecipeData(RecipeDetailResponse recipe) {
        this.isEditMode = true;
        this.currentRecipeId = recipe.id();

        headerComponent.setData(
                recipe.title(),
                recipe.description(),
                recipe.prepTimeMinutes(),
                recipe.servings()
        );

        ingredientTableComponent.loadExistingIngredients(
                recipe.ingredients().stream()
                        .map(i -> new SaveRecipeRequest.IngredientRequest(
                                i.ingredientId(),
                                i.unitId(),
                                i.quantity(),
                                i.ingredientName(),
                                i.unitName()))
                        .collect(Collectors.toList())
        );
    }

    /**
     * Encapsula el Error Handling de la vista, desplegando un Modal nativo
     * ante fallos en la capa de negocio o restricciones de validación.
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}