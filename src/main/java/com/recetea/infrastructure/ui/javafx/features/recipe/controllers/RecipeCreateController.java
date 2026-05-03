package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;

/**
 * Form controller in create mode. Sets the locale title / button text
 * and dispatches saves to {@code ICreateRecipeUseCase}. The returned
 * {@link RecipeId} is the freshly-generated database id, used by the
 * base class to attach any pending media uploads.
 *
 * <p><b>ES — </b>Controlador del formulario en modo creación. Fija
 * el título / texto de botón según el locale y despacha los saves
 * a {@code ICreateRecipeUseCase}. El {@link RecipeId} devuelto es
 * el id recién generado por la BD, que la clase base usa para
 * adjuntar cualquier subida de multimedia pendiente.
 */
public class RecipeCreateController extends BaseRecipeFormController {

    @Override
    protected void setupMode() {
        formTitleLabel.setText(I18n.get("recipe.create.title"));
        submitButton.setText(I18n.get("form.button.save"));
    }

    @Override
    protected RecipeId handleSave(SaveRecipeRequest request) {
        return context.createRecipe().execute(request);
    }
}
