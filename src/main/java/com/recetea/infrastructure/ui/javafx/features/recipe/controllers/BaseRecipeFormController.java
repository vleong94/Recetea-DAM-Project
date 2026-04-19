package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.IngredientTableComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RecipeHeaderComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.StepTableComponent;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseRecipeFormController {

    @FXML protected RecipeHeaderComponent headerComponent;
    @FXML protected IngredientTableComponent ingredientTableComponent;
    @FXML protected StepTableComponent stepTableComponent;

    protected RecipeCommandProvider context;
    protected NavigationService nav;

    public void init(RecipeCommandProvider context, NavigationService nav) {
        this.context = context;
        this.nav = nav;

        List<Category> categories = context.getAllCategories().execute();
        List<Difficulty> difficulties = context.getAllDifficulties().execute();
        headerComponent.initTaxonomy(categories, difficulties);
        ingredientTableComponent.init(
                context.getAllIngredients().execute(),
                context.getAllUnits().execute());
    }

    @FXML
    public void onSaveButtonClick() {
        if (!headerComponent.isValid()) {
            showError("Error de Validación", "Complete los campos obligatorios de la cabecera.");
            return;
        }

        List<SaveRecipeRequest.IngredientRequest> ingredients = ingredientTableComponent.getIngredients();
        if (ingredients.isEmpty()) {
            showError("Error de Validación", "La receta requiere al menos un ingrediente.");
            return;
        }

        List<SaveRecipeRequest.StepRequest> steps = stepTableComponent.getSteps().stream()
                .map(step -> new SaveRecipeRequest.StepRequest(step.stepOrder(), step.instruction()))
                .collect(Collectors.toList());

        if (steps.isEmpty()) {
            showError("Error de Validación", "Debe definir los pasos de preparación.");
            return;
        }

        try {
            SaveRecipeRequest request = new SaveRecipeRequest(
                    headerComponent.getSelectedCategoryId(),
                    headerComponent.getSelectedDifficultyId(),
                    headerComponent.getTitle(),
                    headerComponent.getDescription(),
                    headerComponent.getPrepTime(),
                    headerComponent.getServings(),
                    ingredients,
                    steps
            );

            handleSave(request);
            nav.toDashboard();
        } catch (AuthenticationRequiredException e) {
            showWarning("Autenticación Requerida", "Debes iniciar sesión para realizar esta acción.");
        } catch (Exception e) {
            showError("Fallo de Persistencia", "No se pudo procesar la solicitud: " + e.getMessage());
        }
    }

    protected abstract void handleSave(SaveRecipeRequest request);

    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    protected void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    protected void showWarning(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
