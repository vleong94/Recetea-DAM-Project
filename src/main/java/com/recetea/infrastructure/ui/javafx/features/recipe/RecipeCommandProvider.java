package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.interop.IExportRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.interop.IImportRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.report.IGenerateRecipeTechnicalSheetUseCase;
import com.recetea.core.shared.application.ports.in.IUserSessionService;

/**
 * Umbrella command-side provider. Inherits the four granular write contracts
 * ({@link IRecipeWriteProvider}, {@link IMediaWriteProvider},
 * {@link IRatingWriteProvider}, {@link IFavoriteWriteProvider}) and adds the
 * cross-cutting accessors that haven't been split out (interop, reports, session).
 *
 * <p>New code should depend on the smallest subinterface it needs — e.g. a
 * controller that only adds ratings should accept {@link IRatingWriteProvider}.
 * The umbrella interface is preserved as a backward-compatibility seam for
 * existing controllers, which receive a single provider via {@code init(...)}.
 */
public interface RecipeCommandProvider extends
        IRecipeWriteProvider,
        IMediaWriteProvider,
        IRatingWriteProvider,
        IFavoriteWriteProvider,
        // Catalogue reads — kept on the command umbrella because the recipe-form
        // controllers (BaseRecipeFormController and subclasses) need both the
        // write surface (attachMedia) and catalogue reads (categories, difficulties,
        // ingredients, units) to populate ComboBoxes for create/update flows.
        // New code that doesn't need writes should depend on ITaxonomyQueryProvider /
        // IIngredientQueryProvider directly via RecipeQueryProvider instead.
        ITaxonomyQueryProvider,
        IIngredientQueryProvider {

    // Cross-cutting accessor — not yet segregated into a granular interface
    // because every command-side controller path needs it for ownership checks.
    IUserSessionService sessionService();

    // Interop — XML import/export operations on the recipe aggregate.
    IImportRecipeUseCase importRecipe();
    IExportRecipeUseCase exportRecipe();

    // Reports — byte[] document synthesis (read-shaped despite living on the command side).
    IGenerateRecipeTechnicalSheetUseCase generateTechnicalSheet();
}
