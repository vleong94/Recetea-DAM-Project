package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;

import java.util.stream.Collectors;

/**
 * Form controller in update mode. {@link #loadRecipeData} pre-populates
 * every sub-component from a {@link RecipeDetailResponse} loaded by the
 * caller; {@link #handleSave(SaveRecipeRequest)} dispatches to
 * {@code IUpdateRecipeUseCase} with the captured {@link RecipeId}.
 *
 * <p>Returns the same {@code currentRecipeId} from the save hook so the
 * base class can attach pending media uploads to the existing aggregate
 * — there is no fresh id to surface in update mode.
 *
 * <p><b>ES — </b>Controlador del formulario en modo actualización.
 * {@link #loadRecipeData} pre-rellena cada sub-componente desde un
 * {@link RecipeDetailResponse} cargado por el llamador;
 * {@link #handleSave(SaveRecipeRequest)} despacha a
 * {@code IUpdateRecipeUseCase} con el {@link RecipeId} capturado.
 *
 * <p>Devuelve el mismo {@code currentRecipeId} desde el hook de
 * save para que la clase base pueda adjuntar las subidas de
 * multimedia pendientes al agregado existente — no hay id nuevo
 * que mostrar en modo actualización.
 */
public class RecipeUpdateController extends BaseRecipeFormController {

    private RecipeId currentRecipeId;

    @Override
    protected void setupMode() {
        formTitleLabel.setText(I18n.get("recipe.update.title"));
        submitButton.setText(I18n.get("form.button.update"));
    }

    public void loadRecipeData(RecipeDetailResponse recipe) {
        this.currentRecipeId = recipe.id();

        headerComponent.setData(
                recipe.title(),
                recipe.description(),
                recipe.prepTimeMinutes(),
                recipe.servings(),
                recipe.categoryId(),
                recipe.difficultyId()
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

        stepTableComponent.loadSteps(recipe.steps());
        mediaUploadComponent.loadExistingMedia(recipe.media());
    }

    @Override
    protected RecipeId handleSave(SaveRecipeRequest request) {
        context.updateRecipe().execute(currentRecipeId, request);
        return currentRecipeId;
    }
}
