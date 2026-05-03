package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.in.interop.IExportRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.interop.IImportRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.media.IAttachMediaUseCase;
import com.recetea.core.recipe.application.ports.in.report.IGenerateRecipeTechnicalSheetUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IAddRatingUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.social.application.ports.in.IIsFavoriteUseCase;
import com.recetea.core.social.application.ports.in.IToggleFavoriteUseCase;

/**
 * Aggregator DTO bundling every write-side use case the recipe-feature
 * controllers consume. The split between command and query contexts is
 * an Interface Segregation Pattern (ISP) hint: controllers that only
 * read receive {@link RecipeQueryContext} and never see write-side ports
 * they don't use.
 *
 * <p>Implements {@link RecipeCommandProvider} so the same shape is
 * exposed by the raw context (used by tests) and by the
 * {@link RecipeCommandWrapper} (used in production with CID binding).
 * Controllers depend on the interface, not either concretion.
 *
 * <p>{@code IUserSessionService} is included even though it isn't a
 * "command" because the wrapper needs it to resolve the active user id
 * before opening the {@code ExecutionContext} scope.
 *
 * <p><b>ES — </b>DTO agregador que empaqueta cada caso de uso del
 * lado de escritura que consumen los controladores del feature de
 * recetas. La división entre contextos de comando y de consulta es
 * una pista del Interface Segregation Pattern (ISP): los
 * controladores que sólo leen reciben {@link RecipeQueryContext} y
 * nunca ven puertos del lado de escritura que no usan.
 *
 * <p>Implementa {@link RecipeCommandProvider} para que la misma
 * forma la expongan tanto el contexto crudo (usado por tests) como
 * el {@link RecipeCommandWrapper} (usado en producción con binding
 * de CID). Los controladores dependen de la interfaz, no de
 * ninguna de las dos concreciones.
 *
 * <p>{@code IUserSessionService} se incluye aunque no es un
 * "comando" porque el wrapper lo necesita para resolver el id del
 * usuario activo antes de abrir el ámbito de
 * {@code ExecutionContext}.
 */
public record RecipeCommandContext(
        IAddRatingUseCase addRating,
        ICreateRecipeUseCase createRecipe,
        IUpdateRecipeUseCase updateRecipe,
        IDeleteRecipeUseCase deleteRecipe,
        IAttachMediaUseCase attachMedia,
        IGetAllIngredientsUseCase getAllIngredients,
        IGetAllUnitsUseCase getAllUnits,
        IGetAllCategoriesUseCase getAllCategories,
        IGetAllDifficultiesUseCase getAllDifficulties,
        IUserSessionService sessionService,
        IToggleFavoriteUseCase toggleFavorite,
        IIsFavoriteUseCase isFavorite,
        IImportRecipeUseCase importRecipe,
        IExportRecipeUseCase exportRecipe,
        IGenerateRecipeTechnicalSheetUseCase generateTechnicalSheet
) implements RecipeCommandProvider {}
