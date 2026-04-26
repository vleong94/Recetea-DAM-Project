package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.IngredientTableComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.MediaUploadComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RecipeHeaderComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.StepTableComponent;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import com.recetea.infrastructure.ui.javafx.shared.notification.NotificationService;
import javafx.application.Platform;
import javafx.fxml.FXML;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseRecipeFormController {

    @FXML protected RecipeHeaderComponent headerComponent;
    @FXML protected IngredientTableComponent ingredientTableComponent;
    @FXML protected StepTableComponent stepTableComponent;
    @FXML protected MediaUploadComponent mediaUploadComponent;

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
        Platform.runLater(headerComponent::requestTitleFocus);
    }

    @FXML
    public void onSaveButtonClick() {
        if (!headerComponent.isValid()) {
            showError(I18n.get("form.error.header.required"));
            return;
        }

        List<SaveRecipeRequest.IngredientRequest> ingredients = ingredientTableComponent.getIngredients();
        if (ingredients.isEmpty()) {
            showError(I18n.get("form.error.noIngredients"));
            return;
        }

        List<SaveRecipeRequest.StepRequest> steps = stepTableComponent.getSteps().stream()
                .map(step -> new SaveRecipeRequest.StepRequest(step.stepOrder(), step.instruction()))
                .collect(Collectors.toList());

        if (steps.isEmpty()) {
            showError(I18n.get("form.error.noSteps"));
            return;
        }

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

        RecipeId savedId = handleSave(request);
        attachPendingMedia(savedId);
        nav.toDashboard();
    }

    /** Persists the recipe and returns its identity (new or existing). */
    protected abstract RecipeId handleSave(SaveRecipeRequest request);

    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    protected void showError(String message) {
        NotificationService.warning(headerComponent, message);
    }

    // ── Private ───────────────────────────────────────────────

    private void attachPendingMedia(RecipeId recipeId) {
        List<File> pending = mediaUploadComponent.getPendingFiles();
        if (pending.isEmpty()) return;
        for (File file : pending) {
            try (FileInputStream fis = new FileInputStream(file)) {
                context.attachMedia().execute(recipeId, fis, file.getName());
            } catch (IOException e) {
                throw new RuntimeException(
                        I18n.format("media.error.fileRead", file.getName()), e);
            }
        }
        mediaUploadComponent.clearPending();
    }
}
